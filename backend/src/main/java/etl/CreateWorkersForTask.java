package etl;

import bootstrap.ConfigLoader;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import db.Repository;
import indexer.InversedIndexer;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Used to insert parsed data into the databases
 */
public final class CreateWorkersForTask {
    public static void run(CsvParser parser, InversedIndexer indexer, Repository db) throws InterruptedException, ExecutionException {
        BlockingQueue<List<Document>> tasks = new ArrayBlockingQueue<>(150);
        AtomicReference<Exception> errors = new AtomicReference<>();

        Future<?> err = runProducers(parser, errors, tasks);
        runConsumers(tasks, db);

        err.get();

        if (errors.get() != null)
            throw new RuntimeException(errors.get());
    }

    private static Future<?> runProducers(CsvParser parser, AtomicReference<Exception> errors, BlockingQueue<List<Document>> tasks) {
        int tc = ConfigLoader.getInt("consumer.threadCount", "2");

        //noinspection resource
        ExecutorService producer = Executors.newFixedThreadPool(tc);
        List<Future<Void>> pFutures = new ArrayList<>();

        for (int i = 0; i < tc; i++) {
            int[] range = parser.getPageRange(i, tc);

            pFutures.add(producer.submit(() -> {
                        parser.parseDataTo(tasks, range[0], range[1]);
                        return null;
                    })
            );
        }

        //noinspection resource
        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<?> err = service.submit(() -> {
            for (Future<?> future : pFutures) {
                try {
                    future.get();
                } catch (ExecutionException | InterruptedException e) {
                    errors.set(e);
                    break;
                }
            }

            try {
                tasks.put(CsvParser.POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        });

        producer.shutdown();
        service.shutdown();

        return err;
    }

    private static void runConsumers(BlockingQueue<List<Document>> queue, Repository db) throws InterruptedException {
        int tc = ConfigLoader.getInt("consumer.threadCount", "4");

        try (ExecutorService consumer = Executors.newFixedThreadPool(tc)) {
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