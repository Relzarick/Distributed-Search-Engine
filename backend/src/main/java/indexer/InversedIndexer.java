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
        Map<String, Set<UUID>> dict = new HashMap<>(1000);

        for (Document doc : batch) {
            UUID id = (UUID) doc.get("_id");

            for (Map.Entry<String, Object> field : doc.entrySet()) {
                if (field.getKey().equals("_id"))
                    continue;

                if (field.getValue() instanceof String value) {
                    List<String> tokens = tk.tokenize(value);

                    if (tokens != null)
                        for (String key : tokens)
                            dict.computeIfAbsent(key, k -> new HashSet<>()).add(id);
                }
            }
        }

        if (!dict.isEmpty())
            pushAndFlush(dict);
    }

    private void pushAndFlush(Map<String, Set<UUID>> dict) {
        for (Map.Entry<String, Set<UUID>> entry : dict.entrySet())
            redis.set(entry.getKey(), entry.getValue().toArray(new UUID[0]));

        redis.flush();
    }

    @Override
    public void close() {
        redis.close();
    }

}