package bootstrap;

import db.Cache;
import db.CacheClient;
import db.Repository;
import etl.CsvParser;
import indexer.InversedIndexer;
import org.bson.Document;
import timer.StopWatch;
import tokenizer.StandardTokenization;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class AppSetup {

    private AppSetup() {
    }

    public static void run(Repository db) throws AppSetupException {
        String pathName = "data";

        File dataDir = new File(pathName);
        File[] csv = dataDir.listFiles((_, file) -> file.endsWith(".csv"));

        if (csv == null)
            throw new AppSetupException(pathName + " dir was not found");

        if (csv.length == 0)
            throw new AppSetupException("No .csv file found in " + pathName);

        try (Cache cache = new CacheClient(); CsvParser parser = new CsvParser(csv[0])) {

            StopWatch timer = new StopWatch("parser");
            Iterator<List<Document>> docList = parser.returnTasks();
            InversedIndexer indexer = new InversedIndexer(cache, new StandardTokenization());

            createWorkersForTasks(docList, indexer, db);

            timer.stop();
        } catch (Exception e) {
            throw new AppSetupException(e.getMessage());
        }
    }

    private static void createWorkersForTasks(Iterator<List<Document>> tasks, InversedIndexer indexer, Repository db) {
        try (ExecutorService worker = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            while (tasks.hasNext()) {
                List<Document> batch = tasks.next();

                futures.add(worker.submit(() -> {
                    db.insert(batch);
                    indexer.tokenizeToIndex(batch);
                }));

            }

            for (Future<?> future : futures) {
                future.get();
            }

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public static class AppSetupException extends Exception {
        private AppSetupException(String message) {
            super(message);
        }
    }

}