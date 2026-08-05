package search;

import indexer.InvertedIndexer;
import mongo.Repository;
import redis.RedisQueryService;

import java.util.List;
import java.util.UUID;

public class SearchService {
    private final Repository mongo;
    private final RedisQueryService query = new RedisQueryService();
    private final InvertedIndexer indexer;

    private static final int ROWS_PER_PAGE = 50;

    public SearchService(Repository db, InvertedIndexer idx) {
        mongo = db;
        indexer = idx;
    }

    public String find(String input, int page) {
        List<String> cleanedInput = indexer.tokenizeKeyWords(input);

        if (cleanedInput.isEmpty())
            return "[]";

        int offset = page * ROWS_PER_PAGE;
        List<UUID> uuids = query.fetchFromRedis(cleanedInput, offset, ROWS_PER_PAGE);

        if (uuids.isEmpty())
            return "[]";

        return mongo.fetchMany(uuids).jsonify();
    }

}