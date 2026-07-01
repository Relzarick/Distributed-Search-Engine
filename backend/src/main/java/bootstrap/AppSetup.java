package bootstrap;

import db.lettuce.Cache;
import db.lettuce.CacheClient;
import db.mongo.Repository;
import etl.CsvParser;
import indexer.InversedIndexer;
import tokenizer.StandardTokenization;

import java.io.File;
import java.io.IOException;

public final class AppSetup {
    public AppSetup(Repository db) throws AppSetupException {
        String pathName = "data";

        File dataDir = new File(pathName);
        File[] csvfile = dataDir.listFiles((_, name) -> name.endsWith(".csv"));

        if (csvfile == null)
            throw new AppSetupException(pathName + " dir was not found");

        if (csvfile.length == 0)
            throw new AppSetupException("No .csv file found in " + pathName);

        try (CsvParser CsvParser = new CsvParser(csvfile[0]);
             Cache cache = new CacheClient()) {

            CsvParser.parse(db::insert);
            InversedIndexer indexer = new InversedIndexer(cache, db, new StandardTokenization());

        } catch (IOException | RuntimeException e) {
            throw new AppSetupException(e.getMessage());
        }

    }

    public static class AppSetupException extends Exception {
        private AppSetupException(String message) {
            super(message);
        }
    }

}

// TODO shutdown hook to close redis? (return after fully built)
// TODO virtual threads