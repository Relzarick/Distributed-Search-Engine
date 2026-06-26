package bootstrap;

import db.Repository;
import etl.CsvParser;

import java.io.File;

public final class AppSetup {
    public AppSetup(Repository db) throws AppSetupException {
        String pathName = "data";

        File dataDir = new File(pathName);
        File[] csvfile = dataDir.listFiles((dir, name) -> name.endsWith(".csv"));

        if (csvfile == null)
            throw new AppSetupException("Data dir was not found");

        if (csvfile.length == 0)
            throw new AppSetupException("No .csv file found in " + pathName);

        CsvParser CsvParser = new CsvParser(csvfile[0]);
        CsvParser.parse(db::insert);
    }

    public static class AppSetupException extends Exception {
        private AppSetupException(String message) {
            super(message);
        }
    }

}

// TODO need shutdown hook to close db on JVM exit (return after fully built)