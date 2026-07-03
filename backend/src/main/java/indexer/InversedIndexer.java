package indexer;

import db.Cache;
import org.bson.Document;
import tokenizer.TokenStrategy;
import tokenizer.Tokenizer;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InversedIndexer {

    private final Cache cache;
    private final Tokenizer tk;

    public InversedIndexer(Cache redisClient, TokenStrategy tokenizer) {
        cache = redisClient;
        tk = new Tokenizer(tokenizer);
    }

    public void tokenizeToIndex(List<Document> batch) {

        for (Document doc : batch) {
            //  convert to list of string. numbers can ignore

            Set<Map.Entry<String, Object>> t = doc.entrySet();

            System.out.println(t);
        }

    }

}