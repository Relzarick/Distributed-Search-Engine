package etl;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import db.Repository;
import indexer.InversedIndexer;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Used to insert parsed data into the databases
 */
public final class CreateWorkersForTask {
    public static void run(CsvParser parser, InversedIndexer indexer, Repository db) throws InterruptedException {
        BlockingQueue<List<Document>> taskQueue = new ArrayBlockingQueue<>(150);

        //noinspection resource
        ExecutorService producer = Executors.newSingleThreadExecutor();
        Future<Void> producerFutures = producer.submit(() -> {
            parser.parseDataTo(taskQueue);
            return null;
        });

        producer.shutdown();

        try (ExecutorService service = Executors.newFixedThreadPool(12)) {
            List<Future<?>> futures = new ArrayList<>();
            List<Document> batch;

            while ((batch = taskQueue.take()) != CsvParser.POISON_PILL) {
                List<Document> finalBatch = batch;

                futures.add(service.submit(() -> {
                            db.insert(finalBatch);
//                            indexer.tokenizeToIndex(finalBatch);
                        })
                );
            }

            for (Future<?> future : futures)
                checkWorkerFutures(future);
        }

        try { // This should crash if it can't parse
            producerFutures.get();
        } catch (ExecutionException | InterruptedException e) {
            taskQueue.put(CsvParser.POISON_PILL);
            throw new RuntimeException(e);
        }

    }

    private static void checkWorkerFutures(Future<?> future) throws InterruptedException {
        try {
            future.get();
        } catch (ExecutionException e) {
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