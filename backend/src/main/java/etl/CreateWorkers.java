package etl;

import bootstrap.ConfigLoader;
import db.RedisClient;
import db.Repository;
import indexer.InversedIndexer;
import indexer.tokenizer.StandardTokenizationV3;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Used to insert parsed data into the databases
 */
public final class CreateWorkers {
    private final int PARSER_TC = ConfigLoader.getInt("parser.threadCount", "2");
    private final int MONGO_TC = ConfigLoader.getInt("mongo.threadCount", "4");
    private final int REDIS_TC = ConfigLoader.getInt("redis.threadCount", "2");

    private final BlockingQueue<QueueItem> mongoQueue = new LinkedBlockingQueue<>(100);
    private final BlockingQueue<QueueItem> redisQueue = new LinkedBlockingQueue<>(300);

    private final ExecutorService parserThreadPool = Executors.newFixedThreadPool(PARSER_TC);
    private final ExecutorService mongoThreadPool = Executors.newFixedThreadPool(MONGO_TC);
    private final ExecutorService redisThreadPool = Executors.newFixedThreadPool(REDIS_TC);

    /**
     * This method throws unchecked exceptions to caller.
     *
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException   if this future completed
     */
    public void run(CsvParser parser, Repository db) {
        CompletableFuture<Void> producers = runProducers(parser);
        CompletableFuture<Void> consumers = runConsumers(db);

        CompletableFuture.allOf(producers, consumers).join();
    }

    private CompletableFuture<Void> runProducers(CsvParser parser) {
        CompletableFuture<?>[] futures = new CompletableFuture<?>[PARSER_TC];

        for (int i = 0; i < PARSER_TC; i++) {
            final int index = i;

            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    int[] range = parser.getPageRange(index, PARSER_TC);
                    parser.parseDataTo(mongoQueue, redisQueue, range[0], range[1]);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }, parserThreadPool);
        }

        return CompletableFuture.allOf(futures).whenComplete((result, throwable) -> {
            if (throwable == null) {
                try {
                    for (int i = 0; i < MONGO_TC; i++)
                        mongoQueue.put(new QueueItem.PoisonPill());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    abortIngestion("Interrupted while queueing Mongo poison pills");
                    throw new CompletionException(e);
                }
            } else
                abortIngestion("Producer pool failed: " + throwable);

            parserThreadPool.shutdown();
        });

    }

    private CompletableFuture<Void> runConsumers(Repository db) {
        CompletableFuture<?>[] mongoFuturesArray = new CompletableFuture<?>[MONGO_TC];
        CompletableFuture<?>[] redisFuturesArray = new CompletableFuture<?>[REDIS_TC];

        StandardTokenizationV3 strat = new StandardTokenizationV3();

        for (int i = 0; i < MONGO_TC; i++) {
            mongoFuturesArray[i] = CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        QueueItem item = mongoQueue.take();

                        if (item instanceof QueueItem.PoisonPill)
                            break;

                        QueueItem.DocumentBatch batch = (QueueItem.DocumentBatch) item;
                        db.insert(batch.documents());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    abortIngestion("Mongo consumer interrupted");
                    throw new CompletionException(e);
                } catch (Exception e) {
                    abortIngestion("Mongo consumer failed: " + e);
                    throw new CompletionException(e);
                }
            }, mongoThreadPool);
        }

        for (int i = 0; i < REDIS_TC; i++) {
            redisFuturesArray[i] = CompletableFuture.runAsync(() -> {
                try (InversedIndexer indexer = new InversedIndexer(new RedisClient(), strat)) {
                    while (true) {
                        QueueItem item = redisQueue.take();

                        if (item instanceof QueueItem.PoisonPill)
                            break;

                        QueueItem.DocumentBatch batch = (QueueItem.DocumentBatch) item;
                        indexer.tokenizeToIndex(batch.documents());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    abortIngestion("Redis consumer interrupted");
                    throw new CompletionException(e);
                } catch (Exception e) {
                    System.err.println(Thread.currentThread() + " Failed to process batch. Reason: " + e.getMessage());

                    abortIngestion("Redis consumer failed: " + e);
                    throw new CompletionException(e);
                }
            }, redisThreadPool);
        }

        return futuresBatched(mongoFuturesArray, redisFuturesArray);
    }

    /**
     * This will crash the whole pipeline if anything messes up, so only insert good csv datasets.
     *
     * @param m Mongo Futures
     * @param r Redis Futures
     * @return A super wrapped completable future.
     */
    private CompletableFuture<Void> futuresBatched(CompletableFuture<?>[] m, CompletableFuture<?>[] r) {
        CompletableFuture<Void> nestedRedisFutures = CompletableFuture.allOf(r);
        CompletableFuture<Void> nestedMongofutures = CompletableFuture.allOf(m).whenComplete((result, throwable) -> {
            if (throwable == null) {
                try {
                    for (int i = 0; i < REDIS_TC; i++)
                        redisQueue.put(new QueueItem.PoisonPill());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    abortIngestion("Interrupted while queueing Redis poison pills");
                }
            } else
                abortIngestion("Mongo futures failed: " + throwable);
        });

        return CompletableFuture.allOf(nestedMongofutures, nestedRedisFutures).whenComplete((result, throwable) -> {
            mongoThreadPool.shutdown();
            redisThreadPool.shutdown();
        });
    }

    private void abortIngestion(String reason) {
        System.err.println("Aborting ingestion: " + reason);
        parserThreadPool.shutdownNow();
        mongoThreadPool.shutdownNow();
        redisThreadPool.shutdownNow();
    }

}