package bootstrap;

import db.Repository;
import etl.CreateWorkersForTask;
import etl.CsvParser;
import indexer.InversedIndexer;
import logging.StopWatch;
import tokenizer.StandardTokenizationV2;

import java.io.File;

public final class AppSetup {
    private static final String PATH_NAME = "data";

    private AppSetup() {
    }

    /**
     * Handles setup logic including, parsing, tokenizing and ingestion to mongo and redis.
     *
     */
    public static void run(Repository db) {
        StopWatch timer = new StopWatch("Parsing pipeline");

        try {
            CsvParser parser = new CsvParser(checkDirIfValid());
            InversedIndexer indexer = new InversedIndexer(null, new StandardTokenizationV2());

            CreateWorkersForTask.run(parser, indexer, db);

            timer.stop();
        } catch (Exception e) {
            timer.stopOnFailure();
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    public static void copyToTemp() {
        String t = ConfigLoader.getStr("TempFileLocation", PATH_NAME);
        // for downloading to temp folder on the container.
        // conducting as a test.
    }

    /**
     * Should only provide ONE CSV file under specified folder.
     *
     * @return A File only if a valid CSV was found in provided path.
     * @throws RuntimeException if rules were not followed
     */
    private static File checkDirIfValid() {
        File dir = new File(PATH_NAME);
        File[] csv = dir.listFiles((_, file) -> file.endsWith(".csv"));

        if (csv == null)
            throw new RuntimeException(PATH_NAME + " dir was not found");

        if (csv.length > 1)
            throw new RuntimeException("Only accepts ONE CSV file " + PATH_NAME);

        if (csv.length == 0)
            throw new RuntimeException("No CSV file found in " + PATH_NAME);

        return csv[0];
    }

}