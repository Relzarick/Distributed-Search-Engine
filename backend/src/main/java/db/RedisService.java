package db;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RedisService implements Index {
    private final RedisClient client;
    private static final UUIDCodec CODEC = new UUIDCodecV3();

    private final StatefulRedisConnection<String, UUID> connection;
    private final RedisAsyncCommands<String, UUID> async;


    public RedisService(String host) {
        client = RedisClient.create(RedisURI.Builder.redis(host, 6379).build());
        connection = client.connect(CODEC);
        connection.setAutoFlushCommands(false);
        async = connection.async();
    }

    @Override
    public void set(String key, UUID[] docs) {
        final int CHUNK_SIZE = 512;
        int len = docs.length;

        // If it is small enough, send it directly to avoid chunking
        if (len <= CHUNK_SIZE) {
            CommandArgs<String, UUID> args = new CommandArgs<>(CODEC).addKey(key).addValues(docs);
            async.dispatch(CommandType.SADD, new IntegerOutput<>(CODEC), args);

            return;
        }

        for (int i = 0; i < len; i += CHUNK_SIZE) {
            int end = Math.min(len, i + CHUNK_SIZE);

            CommandArgs<String, UUID> args = new CommandArgs<>(CODEC).addKey(key);

            for (int j = i; j < end; j++)
                args.addValues(docs[j]);

            async.dispatch(CommandType.SADD, new IntegerOutput<>(CODEC), args);
        }
    }

    @Override
    public void flush() {
        RedisFuture<String> barrier = async.ping();
        connection.flushCommands();

        try {
            barrier.get(10, TimeUnit.SECONDS);
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

    private static class UUIDCodecV3 implements UUIDCodec {
        private final StringCodec stringCodec = StringCodec.UTF8;

        @Override
        public ByteBuffer encodeKey(String key) {
            return stringCodec.encodeKey(key);
        }

        @Override
        public ByteBuffer encodeValue(UUID value) {
            if (value == null)
                return ByteBuffer.allocate(0);

            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(value.getMostSignificantBits());
            buffer.putLong(value.getLeastSignificantBits());
            buffer.flip();

            return buffer;
        }

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return stringCodec.decodeKey(bytes);
        }

        @Override
        public UUID decodeValue(ByteBuffer bytes) {
            if (bytes == null || bytes.remaining() != 16)
                return null;

            return new UUID(bytes.getLong(), bytes.getLong());
        }
    }

}