package bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigLoader {
    private static final Path PATH = Path.of("config.properties");

    private ConfigLoader() {
    }

    public static int getInt(String property, String fallBack) {
        return Integer.parseInt(load().getProperty(property, fallBack));
    }

    public static String getStr(String property, String fallBack) {
        return load().getProperty(property, fallBack);
    }

    private static Properties load() {
        Properties props = new Properties();

        try (InputStream in = Files.newInputStream(PATH)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return props;
    }

}