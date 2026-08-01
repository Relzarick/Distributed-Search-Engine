package bootstrap;

import etl.CreateWorkers;
import etl.parser.CsvParser;
import indexer.InvertedIndexer;
import logging.StopWatch;
import mongo.Repository;
import redis.RedisShardRouter;

import java.io.IOException;

public final class AppSetup {
    private AppSetup() {
    }

    /**
     * Handles setup logic including, parsing, tokenizing and ingestion to mongo and redis.
     *
     */
    public static void run(Repository db, InvertedIndexer indexer) throws IOException {
        StopWatch parse = new StopWatch("Parsing pipeline");

        try {
            StopWatch index = new StopWatch("CSV Index");
            CsvParser parser = new CsvParser();
            index.stop();

            CreateWorkers workers = new CreateWorkers();

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