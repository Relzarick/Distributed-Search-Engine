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
import org.bson.UuidRepresentation;

import java.util.List;

public final class Database implements Repository {
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    private static final ConnectionString CONNECTION_STRING = new ConnectionString("mongodb://mongrel:27017");
    private static final InsertManyOptions UNORDERED = new InsertManyOptions().ordered(false);
    private static final String DATABASENAME = "mongrel-db";
    private static final String COLLECTION = "col";

    public Database() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(CONNECTION_STRING)
                .build();

        client = MongoClients.create(settings);

        MongoDatabase db = client.getDatabase(DATABASENAME);
        collection = db.getCollection(COLLECTION);
    }

    @Override
    public Document fetch(String id) {
        return collection.find(Filters.eq("_id", id)).first();
//         should make this fetch a bunch at once?
    }

    @Override
    public void insert(List<Document> batch) {
        collection.insertMany(batch, UNORDERED);
    }

    @Override
    public Boolean ifExists() {
        return collection.find().first() != null;
    }

    @Override
    public MongoCollection<Document> getCollection() {
        return collection;
    }

    @Override
    public void close() {
        if (client != null)
            client.close();
    }

}