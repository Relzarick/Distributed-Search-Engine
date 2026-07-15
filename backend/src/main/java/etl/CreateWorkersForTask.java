package etl;

import bootstrap.ConfigLoader;
import db.RedisClient;
import db.Repository;
import indexer.InversedIndexer;
import tokenizer.TokenStrategy;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Used to insert parsed data into the databases
 */
public final class CreateWorkersForTask {
    private static final int PARSER_TC = ConfigLoader.getInt("parser.threadCount", "2");
    private static final int MONGO_TC = ConfigLoader.getInt("mongo.threadCount", "4");
    private static final int REDIS_TC = ConfigLoader.getInt("redis.threadCount", "2");

    private static final int MONGO_QUEUE_SIZE = 100;
    private static final int REDIS_QUEUE_SIZE = 200;

    public static void run(CsvParser parser, Repository db, TokenStrategy strat) throws InterruptedException, ExecutionException {
        BlockingQueue<QueueItem> tasks = new LinkedBlockingQueue<>(MONGO_QUEUE_SIZE);

        CompletableFuture<Void> producers = runProducers(parser, tasks);
        CompletableFuture<Void> consumers = runConsumers(db, strat, tasks);

        CompletableFuture.allOf(producers, consumers).get();
    }

    private static CompletableFuture<Void> runProducers(CsvParser parser, BlockingQueue<QueueItem> queue) {
        ExecutorService threadPool = Executors.newFixedThreadPool(PARSER_TC);
        CompletableFuture<?>[] futures = new CompletableFuture<?>[PARSER_TC];

        for (int i = 0; i < PARSER_TC; i++) {
            final int index = i;

            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    int[] range = parser.getPageRange(index, PARSER_TC);
                    parser.parseDataTo(queue, range[0], range[1]);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }, threadPool);
        }

        return CompletableFuture.allOf(futures).whenComplete((result, throwable) -> {
            try {
                for (int i = 0; i < MONGO_TC; i++)
                    queue.put(new QueueItem.PoisonPill());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                threadPool.shutdown();
            }
        });

    }

    private static CompletableFuture<Void> runConsumers(Repository db, TokenStrategy strat, BlockingQueue<QueueItem> queue) {
        ExecutorService mongoThreadPool = Executors.newFixedThreadPool(MONGO_TC);
        CompletableFuture<?>[] mongoFutures = new CompletableFuture<?>[MONGO_TC];

        ExecutorService rediThreadPool = Executors.newFixedThreadPool(REDIS_TC);
        CompletableFuture<?>[] redisFutures = new CompletableFuture<?>[REDIS_TC];

        BlockingQueue<QueueItem> redisQueue = new LinkedBlockingQueue<>(REDIS_QUEUE_SIZE);

        for (int i = 0; i < MONGO_TC; i++) {
            mongoFutures[i] = CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        QueueItem item = queue.take();

                        if (item instanceof QueueItem.PoisonPill)
                            break;

                        QueueItem.DocumentBatch batch = (QueueItem.DocumentBatch) item;
                        db.insert(batch.documents());
                        redisQueue.put(batch);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }, mongoThreadPool);
        }

        for (int i = 0; i < REDIS_TC; i++) {
            redisFutures[i] = CompletableFuture.runAsync(() -> {
                try (InversedIndexer indexer = new InversedIndexer(new RedisClient(), strat)) {
                    while (true) {
                        QueueItem item = redisQueue.take();

                        if (item instanceof QueueItem.PoisonPill)
                            break;

                        QueueItem.DocumentBatch batch = (QueueItem.DocumentBatch) item;
                        indexer.tokenizeToIndex(batch.documents());
                    }
                } catch (RuntimeException e) {
                    System.err.println("Failed to process batch. Reason: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }, rediThreadPool);
        }

        return futuresBatched(mongoFutures, redisFutures, mongoThreadPool, rediThreadPool, redisQueue);
    }

    /**
     * This will crash the whole pipeline if anything messes up, so only insert good csv datasets.
     *
     * @param m     Mongo Futures
     * @param r     Redis Futures
     * @param mTp   Mongo ThreadPool
     * @param rTp   Redis ThreadPool
     * @param queue Redis Queue
     * @return A super wrapped completable future.
     */
    private static CompletableFuture<Void> futuresBatched(CompletableFuture<?>[] m, CompletableFuture<?>[] r, ExecutorService mTp, ExecutorService rTp, BlockingQueue<QueueItem> queue) {
        CompletableFuture<Void> redisFutures = CompletableFuture.allOf(r);
        CompletableFuture<Void> mongofutures = CompletableFuture.allOf(m).whenComplete((result, throwable) -> {
            try {
                for (int i = 0; i < REDIS_TC; i++)
                    queue.put(new QueueItem.PoisonPill());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return CompletableFuture.allOf(mongofutures, redisFutures).whenComplete((result, throwable) -> {
            if (throwable != null) {
                mTp.shutdownNow();
                rTp.shutdownNow();
            } else {
                mTp.shutdown();
                rTp.shutdown();
            }
        });
    }

}