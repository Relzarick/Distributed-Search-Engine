package db;

import org.bson.Document;

import java.util.List;

/**
 * All databases should implement this interface
 */
public interface Repository {
    Document fetch(String value);

    void insert(List<Document> batch);

    Boolean ifExists();

    void close();
}
