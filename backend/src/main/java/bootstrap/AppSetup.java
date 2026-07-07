package bootstrap;

import db.Repository;
import etl.CreateWorkersForTask;
import etl.CsvParser;
import indexer.InversedIndexer;
import logging.StopWatch;
import tokenizer.StandardTokenizationV2;

import java.io.File;

public final class AppSetup {
    private AppSetup() {
    }

    /**
     * Handles setup logic including, parsing, tokenizing and ingestion to mongo and redis.
     *
     */
    public static void run(Repository db) {
        String pathName = "data";

        StopWatch timer = new StopWatch("parser");

        try (CsvParser parser = new CsvParser(checkDirIfValid(pathName))) {
            InversedIndexer indexer = new InversedIndexer(null, new StandardTokenizationV2());
            CreateWorkersForTask.run(parser, indexer, db);

            timer.stop();
        } catch (Exception e) {
            timer.stopOnFailure();
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * Should only provide ONE .csv file under specified folder.
     *
     * @param path should be the data folder under backend directory but could be any folder specified.
     * @return a File only if a valid .csv was found in provided path.
     * @throws RuntimeException if rules were not followed
     */
    private static File checkDirIfValid(String path) {
        File dir = new File(path);
        File[] csv = dir.listFiles((_, file) -> file.endsWith(".csv"));

        if (csv == null)
            throw new RuntimeException(path + " dir was not found");

        if (csv.length > 1)
            throw new RuntimeException("Only accepts ONE .csv file " + path);

        if (csv.length == 0)
            throw new RuntimeException("No .csv file found in " + path);

        return csv[0];
    }

}