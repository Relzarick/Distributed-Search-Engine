package redis;

import search.QueryResult;

import java.util.*;
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

    public QueryResult fetchFromRedis(List<String> tokens, int offset, int size) {
        Set<UUID> result = null;

        for (String token : tokens) {
            int instance = TokenHasher.hash(token, shards.length);
            Set<UUID> tokenUuids;

            try {
                tokenUuids = new HashSet<>(shards[instance].retrieve(token));
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to retrieve token: " + token, e);
            }

            if (result == null)
                result = tokenUuids;
            else result.retainAll(tokenUuids);

            if (result.isEmpty())
                break;
        }

        List<UUID> sorted = new ArrayList<>(result == null ? Set.of() : result);
        sorted.sort(Comparator.naturalOrder());

        int end = Math.min(offset + size, sorted.size()); // So doesn't go out of bound

        if (offset >= sorted.size()) // return if start is already out of bound
            return new QueryResult(List.of(), 0);

        return new QueryResult(sorted.subList(offset, end), sorted.size());
    }

    @Override
    public void close() {
        for (Index redis : shards)
            redis.close();
    }

}