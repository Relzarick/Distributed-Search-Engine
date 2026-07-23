package etl;

import bootstrap.ConfigLoader;
import db.Index;
import db.RedisClient;
import db.Repository;
import indexer.InversedIndexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Used to insert parsed data into the databases
 */
public final class CreateWorkers {
    private final int PARSER_TC = ConfigLoader.getInt("parser.threadCount", "2");
    private final int MONGO_TC = ConfigLoader.getInt("mongo.threadCount", "4");
    private final int REDIS_TC = ConfigLoader.getInt("redis.threadCount", "2");

    private final BlockingQueue<QueueItem> mongoQueue = new LinkedBlockingQueue<>(100);
    private final BlockingQueue<QueueItem> indexerQueue = new ArrayBlockingQueue<>(200);
    private final BlockingQueue<QueueItem> redisQueue = new ArrayBlockingQueue<>(400);

    private final ExecutorService parserThreadPool = Executors.newFixedThreadPool(PARSER_TC);
    private final ExecutorService indexerThreadPool = Executors.newFixedThreadPool(PARSER_TC);
    private final ExecutorService mongoThreadPool = Executors.newFixedThreadPool(MONGO_TC);
    private final ExecutorService redisThreadPool = Executors.newFixedThreadPool(REDIS_TC);

    private record Target(BlockingQueue<QueueItem> queue, int pillCount) {
    }

    /**
     * This method throws unchecked exceptions to caller.
     *
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException   if this future completed
     */
    public void run(CsvParser parser, InversedIndexer indexer, Repository db) {
        CompletableFuture<Void> producers = runProducers(parser, indexer);
        CompletableFuture<Void> consumers = runConsumers(db);

        CompletableFuture.allOf(producers, consumers).join();
    }

    private CompletableFuture<Void> runProducers(CsvParser parser, InversedIndexer indexer) {
        CompletableFuture<?>[] parserFutures = new CompletableFuture<?>[PARSER_TC];
        CompletableFuture<?>[] indexerFutures = new CompletableFuture<?>[PARSER_TC];
        int indexerBuffer = 5;

        for (int i = 0; i < PARSER_TC; i++) {
            final int index = i;

            parserFutures[i] = CompletableFuture.runAsync(() -> {
                try {
                    int[] range = parser.getPageRange(index, PARSER_TC);
                    parser.parseDataTo(mongoQueue, indexerQueue, range[0], range[1]);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }, parserThreadPool);
        }

        for (int i = 0; i < PARSER_TC; i++) {
            indexerFutures[i] = CompletableFuture.runAsync(() -> {
                try {
                    List<QueueItem.DocumentBatch> batchBuffer = new ArrayList<>(indexerBuffer);

                    while (true) {
                        QueueItem item = indexerQueue.take();

                        if (item instanceof QueueItem.PoisonPill) {
                            indexerQueue.put(item);

                            if (!batchBuffer.isEmpty())
                                indexer.tokenizeToQueue(batchBuffer, redisQueue);

                            break;
                        }

                        batchBuffer.add((QueueItem.DocumentBatch) item);

                        if (batchBuffer.size() >= indexerBuffer) {
                            indexer.tokenizeToQueue(batchBuffer, redisQueue);
                            batchBuffer.clear();
                        }
                    }
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, indexerThreadPool);
        }

        CompletableFuture<Void> allParser = insertPoisonPills(parserFutures, new Target(mongoQueue, MONGO_TC), new Target(indexerQueue, PARSER_TC));
        CompletableFuture<Void> allIndexer = insertPoisonPills(indexerFutures, new Target(redisQueue, REDIS_TC));

        return CompletableFuture.allOf(allParser, allIndexer).whenComplete((result, throwable) -> {
            parserThreadPool.shutdown();
            indexerThreadPool.shutdown();
        });
    }

    private CompletableFuture<Void> insertPoisonPills(CompletableFuture<?>[] futures, Target... targets) {
        return CompletableFuture.allOf(futures).whenComplete((result, throwable) -> {
            if (throwable == null) {
                try {
                    for (Target target : targets) {
                        for (int i = 0; i < target.pillCount(); i++)
                            target.queue().put(new QueueItem.PoisonPill());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    abortIngestion("Interrupted while queueing poison pills");
                    throw new CompletionException(e);
                }
            } else
                abortIngestion("Producer pool failed: " + throwable);
        });
    }

    private CompletableFuture<Void> runConsumers(Repository db) {
        CompletableFuture<?>[] mongoFuturesArray = new CompletableFuture<?>[MONGO_TC];
        CompletableFuture<?>[] redisFuturesArray = new CompletableFuture<?>[REDIS_TC];

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
                try (Index redis = new RedisClient()) {
                    while (true) {
                        QueueItem item = redisQueue.take();

                        if (item instanceof QueueItem.PoisonPill)
                            break;

                        QueueItem.IndexerBatch batch = (QueueItem.IndexerBatch) item;

                        for (Map.Entry<String, List<UUID>> dict : batch.dict().entrySet())
                            redis.set(dict.getKey(), dict.getValue().toArray(new UUID[0]));

                        redis.flush();
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
        CompletableFuture<Void> nestedMongofutures = CompletableFuture.allOf(m);

        return CompletableFuture.allOf(nestedMongofutures, nestedRedisFutures).whenComplete((result, throwable) -> {
            mongoThreadPool.shutdown();
            redisThreadPool.shutdown();
        });
    }

    private void abortIngestion(String reason) {
        System.err.println("Aborting ingestion: " + reason);

        parserThreadPool.shutdownNow();
        indexerThreadPool.shutdownNow();
        mongoThreadPool.shutdownNow();
        redisThreadPool.shutdownNow();
    }

}