package indexer;

import db.Index;
import org.bson.Document;
import tokenizer.TokenStrategy;
import tokenizer.Tokenizer;

import java.util.*;

public final class InversedIndexer implements AutoCloseable {
    private final Index redis;
    private final Tokenizer tk;

    public InversedIndexer(Index redisClient, TokenStrategy strategy) {
        redis = redisClient;
        tk = new Tokenizer(strategy);
    }

    public void tokenizeToIndex(List<Document> batch) {
        Map<String, Set<String>> dict = new HashMap<>(1000);

        for (Document doc : batch) {
            String id = doc.getObjectId("_id").toHexString();

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

    private void pushAndFlush(Map<String, Set<String>> dict) {
        for (Map.Entry<String, Set<String>> entry : dict.entrySet())
            redis.set(entry.getKey(), entry.getValue().toArray(new String[0]));

        redis.flush();
    }

    @Override
    public void close() {
        redis.close();
    }

}