package bootstrap;

import db.Repository;
import etl.CreateWorkers;
import etl.RedisShardRouter;
import etl.parser.CsvParser;
import indexer.InvertedIndexer;
import indexer.tokenizer.StemTokenization;
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
            InvertedIndexer indexer = new InvertedIndexer(new StemTokenization());

            try (RedisShardRouter router = new RedisShardRouter()) {
                workers.run(parser, indexer, db, router);
            }

            parse.stop();
        } catch (Exception e) {
            parse.stopOnFailure();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}