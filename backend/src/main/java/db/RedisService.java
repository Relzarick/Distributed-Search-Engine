package db;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.ToByteBufEncoder;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RedisService implements Index {
    private final RedisClient client;
    private static final UUIDCodecV2 CODEC = new UUIDCodecV2();

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
        final int CHUNK_SIZE = 1000;

        // If it is small enough, send it directly to avoid chunking overhead
        if (docs.length <= CHUNK_SIZE) {
            CommandArgs<String, UUID> args = new CommandArgs<>(CODEC).addKey(key).addValues(docs);
            async.dispatch(CommandType.SADD, new IntegerOutput<>(CODEC), args);
            return;
        }

        for (int i = 0; i < docs.length; i += CHUNK_SIZE) {
            int end = Math.min(docs.length, i + CHUNK_SIZE);

            UUID[] chunk = Arrays.copyOfRange(docs, i, end);

            CommandArgs<String, UUID> args = new CommandArgs<>(CODEC).addKey(key).addValues(chunk);
            async.dispatch(CommandType.SADD, new IntegerOutput<>(CODEC), args);
        }
    }

    @Override
    public void flush() { // this ping is not the main concern for now
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

    private static class UUIDCodecV2 implements ToByteBufEncoder<String, UUID>, RedisCodec<String, UUID> {
        private final StringCodec stringCodec = StringCodec.UTF8;

        @Override
        public void encodeKey(String key, ByteBuf target) {
            target.writeCharSequence(key, StandardCharsets.UTF_8);
        }

        @Override
        public void encodeValue(UUID value, ByteBuf target) {
            if (value != null) {
                target.writeLong(value.getMostSignificantBits());
                target.writeLong(value.getLeastSignificantBits());
            }
        }

        @Override
        public int estimateSize(Object keyOrValue) {
            if (keyOrValue instanceof UUID)
                return 16;

            if (keyOrValue instanceof String)
                return ((String) keyOrValue).length();

            return 0;
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

        @Override
        public ByteBuffer encodeKey(String key) {
            return null;
        }

        @Override
        public ByteBuffer encodeValue(UUID value) {
            return null;
        }

    }

}