package db;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

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

    // need to expose commands
    // a setter for now

    public void set(String key, String value) {
        commands.set(key, value);

        // value should be generic
        // need to take a batch of values
    }

    @Override
    public void close() {
        connection.close();
        client.shutdown();
    }
}