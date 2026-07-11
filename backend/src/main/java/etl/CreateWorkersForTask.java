package etl;

import bootstrap.ConfigLoader;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import db.Repository;
import indexer.InversedIndexer;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Used to insert parsed data into the databases
 */
public final class CreateWorkersForTask {
    private static int consumerTc;

    public static void run(CsvParser parser, InversedIndexer indexer, Repository db) throws InterruptedException, ExecutionException {
        BlockingQueue<List<Document>> tasks = new ArrayBlockingQueue<>(150);
        consumerTc = ConfigLoader.getInt("consumer.threadCount", "6");

        Future<?> error = runProducers(parser, tasks);
        runConsumers(tasks, db);

        error.get();
    }

    private static Future<?> runProducers(CsvParser parser, BlockingQueue<List<Document>> tasks) {
        int tc = ConfigLoader.getInt("producer.threadCount", "2");

        ExecutorService producer = Executors.newFixedThreadPool(tc);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < tc; i++) {
            int[] range = parser.getPageRange(i, tc);

            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    parser.parseDataTo(tasks, range[0], range[1]);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, producer));

        }

        CompletableFuture<Void> done = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, throwable) -> {
                    try {
                        for (int i = 0; i < consumerTc; i++)
                            tasks.put(CsvParser.POISON_PILL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

        producer.shutdown();
        return done;
    }

    private static void runConsumers(BlockingQueue<List<Document>> queue, Repository db) throws InterruptedException {
        try (ExecutorService consumer = Executors.newFixedThreadPool(consumerTc)) {
            List<Future<?>> cFutures = new ArrayList<>();
            List<Document> batch;

            while ((batch = queue.take()) != CsvParser.POISON_PILL) {
                List<Document> finalBatch = batch;

                cFutures.add(consumer.submit(() -> {
                            db.insert(finalBatch);
//                            indexer.tokenizeToIndex(finalBatch);
                        })
                );
            }

            checkForBulkWriteEx(cFutures);
        }
    }

    private static void checkForBulkWriteEx(List<Future<?>> list) {
        for (Future<?> future : list) {
            try {
                future.get();
            } catch (Exception e) {
                Throwable cause = e.getCause();

                if (cause instanceof MongoBulkWriteException bulkEx) {
                    for (BulkWriteError err : bulkEx.getWriteErrors())
                        System.err.println("Index " + err.getIndex() + ": " + err.getMessage());

                    return;
                }

                throw new RuntimeException(cause);
            }
        }

    }

}