package db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import org.bson.Document;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Database implements Repository {
    private final MongoClient client;
    private final MongoDatabase db;

    private static final ConnectionString CONNECTION_STRING = new ConnectionString("mongodb://mongrel:27017");
    private static final InsertManyOptions UNORDERED = new InsertManyOptions().ordered(false);
    private static final String DATABASENAME = "mongrel-db";
    private static final String COLLECTION = "col";

    public Database() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(CONNECTION_STRING)
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(200)
                        .minSize(50)
                        .maxConnecting(10)
                        .maxWaitTime(3, TimeUnit.SECONDS)
                )
                .build();

        client = MongoClients.create(settings);
        db = client.getDatabase(DATABASENAME);
    }

    @Override
    public Document fetch(String id) {
        MongoCollection<Document> col = docCollection();
        return col.find(Filters.eq("_id", id)).first();
        // should make this fetch a bunch at once
    }

    @Override
    public void insert(List<Document> batch) {
        MongoCollection<Document> col = docCollection();
        col.insertMany(batch, UNORDERED);
    }

    @Override
    public Boolean ifExists() {
        MongoCollection<Document> col = docCollection();
        return col.find().first() != null;
    }

    @Override
    public void close() {
        if (client != null) client.close();
    }

    private MongoCollection<Document> docCollection() {
        return db.getCollection(COLLECTION);
    }

}