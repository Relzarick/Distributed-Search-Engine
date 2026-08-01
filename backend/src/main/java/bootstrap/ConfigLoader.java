package bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigLoader {
    private static final Path PATH = Path.of("config.properties");
    private static final Properties CACHE = new Properties();

    static {
        try (InputStream in = Files.newInputStream(PATH)) {
            CACHE.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage());
        }
    }

    private ConfigLoader() {
    }

    public static int getInt(String property, String fallBack) {
        return Integer.parseInt(CACHE.getProperty(property, fallBack));
    }

    public static String getStr(String property, String fallBack) {
        return CACHE.getProperty(property, fallBack);
    }

}