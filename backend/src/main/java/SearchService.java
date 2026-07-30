import indexer.InvertedIndexer;
import indexer.tokenizer.StemTokenization;
import mongo.Repository;
import org.bson.BsonDocument;
import redis.RedisQueryService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SearchService {
    private final Repository mongo;
    private final RedisQueryService query = new RedisQueryService();
    private final InvertedIndexer indexer = new InvertedIndexer(new StemTokenization());

    public SearchService(Repository db) {
        mongo = db;
    }

    public void search(String input) {
        Set<String> cleanedInput = indexer.tokenizeKeyWords(input);

        Set<UUID> uuids = query.fetchFromRedis(cleanedInput);
        List<BsonDocument> docs = mongo.fetchMany(uuids);

        // return as json
    }

}
