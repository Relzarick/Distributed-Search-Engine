package indexer;

import db.Index;
import indexer.tokenizer.TokenStrategy;
import indexer.tokenizer.Tokenizer;
import org.bson.Document;

import java.util.*;

public final class InversedIndexer implements AutoCloseable {
    private final Index redis;
    private final Tokenizer tk;

    public InversedIndexer(Index redisClient, TokenStrategy strategy) {
        redis = redisClient;
        tk = new Tokenizer(strategy);
    }

    /**
     *
     * @param batch contains a list of Bson documents
     */
    public void tokenizeToIndex(List<Document> batch) {
        Map<String, List<UUID>> dict = new HashMap<>(2500);
        Set<String> uniqueTokensPerDoc = new HashSet<>(250);

        for (Document doc : batch) {
            UUID id = (UUID) doc.get("_id");
            uniqueTokensPerDoc.clear();

            for (Map.Entry<String, Object> field : doc.entrySet()) {
                if (field.getKey().equals("_id"))
                    continue;

                if (field.getValue() instanceof String value)
                    tk.tokenizeInto(value, uniqueTokensPerDoc);
            }

            for (String token : uniqueTokensPerDoc)
                dict.computeIfAbsent(token, k -> new ArrayList<>(1)).add(id);
        }

        if (!dict.isEmpty())
            pushAndFlush(dict);
    }

    private void pushAndFlush(Map<String, List<UUID>> dict) {
        for (Map.Entry<String, List<UUID>> entry : dict.entrySet())
            redis.set(entry.getKey(), entry.getValue().toArray(new UUID[0]));

        redis.flush();
    }

    @Override
    public void close() {
        redis.close();
    }

}