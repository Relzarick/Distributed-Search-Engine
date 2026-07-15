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
        StopWatch total = new StopWatch("total pipeline");
        StopWatch parse = new StopWatch("Parsing pipeline");

        try {
            CsvParser parser = new CsvParser();
            CreateWorkersForTask.run(parser, db, new StandardTokenizationV3());

            parse.stop();
        } catch (Exception e) {
            parse.stopOnFailure();
            throw new RuntimeException(e.getMessage(), e);
        }

        total.stop();
    }

}