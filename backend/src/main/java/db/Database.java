package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.List;

/**
 * Use localhost client if you are only running docker db
 * If just running compose up, leave as default
 */
public final class Database implements Repository {
    private final MongoClient client;
    private final MongoDatabase db;

    public Database() {
        client = MongoClients.create("mongodb://mongrel:27017");
//        client = MongoClients.create("mongodb://localhost:27017");
        db = client.getDatabase("mongrel-db");

        System.out.println("Database initialized");
    }

    @Override
    public Document fetch(String header, int value) {
        MongoCollection<Document> col = db.getCollection("col");
        return col.find(Filters.eq(header, value)).first();

        // change the param when built
    }

    @Override
    public void insert(List<Document> batch) {
        MongoCollection<Document> col = db.getCollection("col");
        col.insertMany(batch);
    }

    @Override
    public Boolean ifExists() {
        MongoCollection<Document> col = db.getCollection("col");
        return col.find().first() != null;
    }

    @Override
    public void close() {
        if (client != null)
            client.close();
    }

}