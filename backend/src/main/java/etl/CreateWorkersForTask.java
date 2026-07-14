package etl;

import bootstrap.ConfigLoader;
import db.RedisClient;
import db.Repository;
import indexer.InversedIndexer;
import org.bson.Document;
import tokenizer.TokenStrategy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * Used to insert parsed data into the databases
 */
public final class CreateWorkersForTask {
    public static void run(CsvParser parser, Repository db, TokenStrategy strat) throws InterruptedException, ExecutionException {
        int consumerTc = ConfigLoader.getInt("consumer.threadCount", "6");
        int producerTc = ConfigLoader.getInt("producer.threadCount", "2");

        BlockingQueue<List<Document>> tasks = new LinkedBlockingQueue<>(50);

        CompletableFuture<Void> producers = runProducers(parser, tasks, producerTc, consumerTc);
        CompletableFuture<Void> consumers = runConsumers(tasks, db, strat, consumerTc);

        CompletableFuture.allOf(producers, consumers).get();
    }

    private static CompletableFuture<Void> runProducers(CsvParser parser, BlockingQueue<List<Document>> tasks, int pTc, int cTc) {
        ExecutorService threadPool = Executors.newFixedThreadPool(pTc);
        CompletableFuture<?>[] futures = new CompletableFuture<?>[pTc];

        for (int i = 0; i < pTc; i++) {
            final int index = i;

            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    int[] range = parser.getPageRange(index, pTc);
                    parser.parseDataTo(tasks, range[0], range[1]);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }, threadPool);
        }

        return CompletableFuture.allOf(futures).whenComplete((result, throwable) -> {
            try {
                for (int i = 0; i < cTc; i++)
                    tasks.put(CsvParser.POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                threadPool.shutdown();
            }
        });

    }

    private static CompletableFuture<Void> runConsumers(BlockingQueue<List<Document>> queue, Repository db, TokenStrategy strat, int cTc) {
        ExecutorService threadPool = Executors.newFixedThreadPool(cTc);
        CompletableFuture<?>[] futures = new CompletableFuture<?>[cTc];

        for (int i = 0; i < cTc; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try (InversedIndexer indexer = new InversedIndexer(new RedisClient(), strat)) {
                    while (true) {
                        List<Document> batch = queue.take();

                        if (batch == CsvParser.POISON_PILL)
                            break;

                        try {
                            db.insert(batch);
                            indexer.tokenizeToIndex(batch);
                        } catch (Exception e) {
                            System.err.println("Failed to process batch. Reason: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }, threadPool);
        }

        // Create a new threadpool for redis

        return CompletableFuture.allOf(futures).whenComplete((result, throwable) -> {
            if (throwable != null)
                threadPool.shutdownNow();
            else
                threadPool.shutdown();
        });
    }

}