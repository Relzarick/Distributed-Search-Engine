package db.mongo;

import org.bson.Document;

import java.util.List;

/**
 * All databases should implement this interface
 */
public interface Repository {
    void fetch();

    void insert(List<Document> batch);

    Boolean ifExists();

    void close();
}
