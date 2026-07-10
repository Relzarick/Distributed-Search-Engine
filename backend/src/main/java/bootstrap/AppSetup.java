package bootstrap;

import db.Repository;
import etl.CreateWorkersForTask;
import etl.CsvParser;
import indexer.InversedIndexer;
import logging.StopWatch;
import tokenizer.StandardTokenizationV2;

import java.io.IOException;
import java.nio.file.Path;

public final class AppSetup {
    private AppSetup() {
    }

    /**
     * Handles setup logic including, parsing, tokenizing and ingestion to mongo and redis.
     *
     */
    public static void run(Repository db) throws IOException {
        StopWatch total = new StopWatch("Total time");
        Path path = FileLoader.stageCsv();

        StopWatch parse = new StopWatch("Parsing pipeline");

        try {
            CsvParser parser = new CsvParser(path);
            InversedIndexer indexer = new InversedIndexer(null, new StandardTokenizationV2());

            CreateWorkersForTask.run(parser, indexer, db);

            parse.stop();
        } catch (Exception e) {
            parse.stopOnFailure();
            throw new RuntimeException(e.getMessage(), e);
        }

        total.stop();
    }

}