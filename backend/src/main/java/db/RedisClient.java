package db;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RedisClient implements Index {
    private static final io.lettuce.core.RedisClient CLIENT =
            io.lettuce.core.RedisClient.create(RedisURI.Builder.redis("vermin", 6379).build());

    private final StatefulRedisConnection<String, String> connection;
    private final RedisAsyncCommands<String, String> async;

    public RedisClient() {
        connection = CLIENT.connect();
        connection.setAutoFlushCommands(false);
        async = connection.async();
    }

    @Override
    public void set(String key, String[] docs) {
        async.sadd(key, docs);
    }

    @Override
    public void flush() {
        RedisFuture<String> barrier = async.ping();
        connection.flushCommands();

        try {
            barrier.get(20, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException("Redis pipeline execution failed: " + e.getCause().getMessage(), e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Redis pipeline timed out. Redis is overloaded.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Redis pipeline interrupted.", e);
        }
    }


    @Override
    public Set<String> retrieve(String key) throws ExecutionException, InterruptedException {
        RedisFuture<Set<String>> future = async.smembers(key);
        connection.flushCommands();

        return future.get();
    }

    @Override
    public void close() {
        connection.close();
    }

}