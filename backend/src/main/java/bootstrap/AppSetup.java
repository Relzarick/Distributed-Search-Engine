package bootstrap;

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

    /**
     * Handles setup logic including, parsing, tokenizing and ingestion to mongo and redis.
     *
     */
    public static void run(Repository db) throws AppSetupException {
        String pathName = "data";
        File csv = checkDirIfValid(pathName);

        try (CsvParser parser = new CsvParser(csv)) {
            StopWatch timer = new StopWatch("parser");

            Iterator<List<Document>> docList = parser.returnTasks();
            InversedIndexer indexer = new InversedIndexer(new CacheClient(), new StandardTokenization());

            createWorkersForTasks(docList, indexer, db);


            timer.stop();
        } catch (Exception e) {
            throw new AppSetupException(e.getMessage());
        }

    }

    /**
     * Create virtual threads to parallelize ingestion workload.
     *
     */
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

    /**
     * Should only provide ONE .csv file under specified folder
     *
     * @param path should be the data folder under backend directory but could be any folder specified.
     * @return a File only if a valid .csv was found in provided path.
     * @throws AppSetupException is a generic exception for the boot strap method.
     */
    private static File checkDirIfValid(String path) throws AppSetupException {
        File dir = new File(path);
        File[] csv = dir.listFiles((_, file) -> file.endsWith(".csv"));

        if (csv == null)
            throw new AppSetupException(path + " dir was not found");

        if (csv.length == 0)
            throw new AppSetupException("No .csv file found in " + path);

        if (csv.length > 1)
            throw new AppSetupException("Only accepts ONE .csv file " + path);

        return csv[0];
    }

    public static class AppSetupException extends Exception {
        private AppSetupException(String message) {
            super(message);
        }
    }

}