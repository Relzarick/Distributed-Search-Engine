package db;

import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;

import java.util.List;

/**
 * All databases should implement this interface
 */
public interface Repository {
    BsonDocument fetch(String value);

    void insert(List<BsonDocument> batch);

    Boolean ifExists();

    MongoCollection<BsonDocument> getCollection();

    void close();
}