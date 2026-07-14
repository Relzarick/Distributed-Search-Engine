package db;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Interface for redis client.
 */
public interface Index {
    void set(String key, String[] doc);

    void flush();

    Set<String> retrieve(String key) throws ExecutionException, InterruptedException;

    void close();
}
