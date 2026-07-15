package bootstrap;

import db.Repository;
import etl.CreateWorkersForTask;
import etl.CsvParser;
import logging.StopWatch;
import tokenizer.StandardTokenizationV3;

import java.io.IOException;

public final class AppSetup {
    private AppSetup() {
    }

    /**
     * Handles setup logic including, parsing, tokenizing and ingestion to mongo and redis.
     *
     */
    public static void run(Repository db) throws IOException {
        StopWatch parse = new StopWatch("Parsing pipeline");

        try {
            StopWatch index = new StopWatch("Index");
            CsvParser parser = new CsvParser();
            index.stop();

            CreateWorkersForTask.run(parser, db, new StandardTokenizationV3());

            parse.stop();
        } catch (Exception e) {
            parse.stopOnFailure();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}