package search;

import indexer.InvertedIndexer;
import mongo.Repository;
import redis.RedisQueryService;

import java.util.Set;
import java.util.UUID;

public class SearchService {
    private final Repository mongo;
    private final RedisQueryService query = new RedisQueryService();
    private final InvertedIndexer indexer;

    public SearchService(Repository db, InvertedIndexer idx) {
        mongo = db;
        indexer = idx;
    }

    public String find(String input) {
        Set<String> cleanedInput = indexer.tokenizeKeyWords(input);

        if (cleanedInput == null || cleanedInput.isEmpty())
            return input;

        Set<UUID> uuids = query.fetchFromRedis(cleanedInput);

        return mongo.fetchMany(uuids).jsonify();
    }

}