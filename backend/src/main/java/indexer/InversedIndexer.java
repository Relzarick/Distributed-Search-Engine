package indexer;

import db.Cache;
import org.bson.Document;
import tokenizer.TokenStrategy;
import tokenizer.Tokenizer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class InversedIndexer {
    private final Cache cache;
    private final Tokenizer tk;

    public InversedIndexer(Cache redisClient, TokenStrategy tokenizer) {
        cache = redisClient;
        tk = new Tokenizer(tokenizer);
    }

    public void tokenizeToIndex(List<Document> batch) {
        for (Document doc : batch) {
            Iterator<Map.Entry<String, Object>> iterator = doc.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Object> field = iterator.next();

                if ("_id".equals(field.getKey()))
                    continue;

                if (field.getValue() instanceof String value) {
                    List<String> tokens = tk.tokenize(value);

                    if (tokens == null)
                        iterator.remove();
                    else
                        field.setValue(tokens);
                } else
                    iterator.remove();
            }

        }

        // this part has to do the deciding
        cache.set(batch);
    }

}