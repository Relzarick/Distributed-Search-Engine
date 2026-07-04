package db;

import org.bson.Document;

import java.util.List;

public interface Cache extends AutoCloseable {
    void set(List<Document> batch);

    @Override
    default void close() {
    }
}
