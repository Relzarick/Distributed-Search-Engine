package db;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Interface for redis client.
 */
public interface Index {
    void set(String key, UUID[] doc);

    void flush();

    Set<UUID> retrieve(String key) throws ExecutionException, InterruptedException;

    void close();
}
