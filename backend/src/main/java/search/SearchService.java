package search;

import indexer.InvertedIndexer;
import mongo.Repository;
import redis.RedisQueryService;

import java.util.List;

public class SearchService {
    private final Repository mongo;
    private final RedisQueryService query = new RedisQueryService();
    private final InvertedIndexer indexer;

    public SearchService(Repository db, InvertedIndexer idx) {
        mongo = db;
        indexer = idx;
    }

    public String find(String input, int offset, int size) {
        List<String> cleanedInput = indexer.tokenizeKeyWords(input);

        QueryResult queryResult = query.fetchFromRedis(cleanedInput, offset, size);

        if (queryResult.count() == 0)
            return "{count: 0, rows: []}";

        return mongo.fetchMany(queryResult.uuids()).jsonify(queryResult.count());
    }

}