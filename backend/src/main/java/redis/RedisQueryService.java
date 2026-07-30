package redis;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class RedisQueryService implements AutoCloseable {
    private final Index[] shards;

    @SuppressWarnings("resource")
    public RedisQueryService() {
        shards = new Index[]{
                new RedisService("r1"),
                new RedisService("r2"),
                new RedisService("r3"),
                new RedisService("r4")
        };
    }

    public Set<UUID> fetchFromRedis(Set<String> token) {
        int instance = TokenHasher.hash(token, shards.length); // this has to loop

        try {
            return shards[instance].retrieve(token);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        for (Index redis : shards)
            redis.close();
    }

}