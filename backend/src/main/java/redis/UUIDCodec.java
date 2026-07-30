package redis;

import io.lettuce.core.codec.RedisCodec;

import java.util.UUID;

public interface UUIDCodec extends RedisCodec<String, UUID> {
}
