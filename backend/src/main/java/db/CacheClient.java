package db;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.bson.Document;

import java.util.List;

public final class CacheClient implements Cache {
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    public CacheClient() {
        RedisURI uri = RedisURI.Builder
                .redis("localhost", 6379)
                .build();

        client = RedisClient.create(uri);
        connection = client.connect();
        commands = connection.sync();
    }

    @Override
    public void set(List<Document> batch) {
        connection.setAutoFlushCommands(false);

        connection.setAutoFlushCommands(true);
    }

    @Override
    public void retrieve() {

    }

    @Override
    public void close() {
        connection.close();
        client.shutdown();
    }

    // this is coupled to mongo now
}