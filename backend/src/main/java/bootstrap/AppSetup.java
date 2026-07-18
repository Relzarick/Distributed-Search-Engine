package bootstrap;

import db.Repository;
import etl.CreateWorkers;
import etl.CsvParser;
import logging.StopWatch;

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
            StopWatch index = new StopWatch("CSV Index");
            CsvParser parser = new CsvParser();
            index.stop();

            CreateWorkers workers = new CreateWorkers();
            workers.run(parser, db);

            parse.stop();
        } catch (Exception e) {
            parse.stopOnFailure();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}