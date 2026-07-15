package bootstrap;

import java.io.File;
import java.nio.file.Path;

public class FileLoader {
    private static final String PATH_NAME = "/app/data";

    private FileLoader() {
    }

    /**
     * Should only provide ONE CSV file under specified folder.
     *
     * @return A File only if a valid CSV was found in provided path.
     * @throws RuntimeException if rules were not followed
     */
    public static Path getSource() {
        File dir = new File(PATH_NAME);
        File[] csv = dir.listFiles((f, file) -> file.endsWith(".csv"));

        if (csv == null)
            throw new RuntimeException(PATH_NAME + " dir was not found");

        if (csv.length > 1)
            throw new RuntimeException("Only accepts ONE CSV file " + PATH_NAME);

        if (csv.length == 0)
            throw new RuntimeException("No CSV file found in " + PATH_NAME);

        return csv[0].toPath();
    }

}