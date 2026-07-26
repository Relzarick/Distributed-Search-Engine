package db;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RedisService implements Index {
    private final RedisClient client;
    private static final UUIDCodec CODEC = new UUIDCodec();

    private final StatefulRedisConnection<String, UUID> connection;
    private final RedisAsyncCommands<String, UUID> async;

    public RedisService(String host) {
        client = RedisClient.create(RedisURI.Builder.redis(host, 6379).build());
        connection = client.connect(CODEC);
        connection.setAutoFlushCommands(false);
        async = connection.async();
    }

    @Override
    public void set(String key, List<UUID> docs) {
        async.sadd(key, docs.toArray(UUID[]::new));
    }

    @Override
    public void flush() {
        RedisFuture<String> barrier = async.ping();
        connection.flushCommands();

        try {
            barrier.get(15, TimeUnit.SECONDS);
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
    public Set<UUID> retrieve(String key) throws ExecutionException, InterruptedException {
        RedisFuture<Set<UUID>> future = async.smembers(key);
        connection.flushCommands();

        return future.get();
    }

    @Override
    public void close() {
        connection.close();
        client.close();
    }

    private static class UUIDCodec implements RedisCodec<String, UUID> {
        private final StringCodec stringCodec = StringCodec.UTF8;

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return stringCodec.decodeKey(bytes);
        }

        @Override
        public UUID decodeValue(ByteBuffer bytes) {
            if (bytes == null || bytes.remaining() != 16)
                return null;

            long high = bytes.getLong();
            long low = bytes.getLong();

            return new UUID(high, low);
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return stringCodec.encodeKey(key);
        }

        @Override
        public ByteBuffer encodeValue(UUID value) {
            if (value == null)
                return null;

            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(value.getMostSignificantBits());
            buffer.putLong(value.getLeastSignificantBits());
            buffer.flip();

            return buffer;
        }

    }

}