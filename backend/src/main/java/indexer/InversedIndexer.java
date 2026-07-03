package indexer;

import db.Cache;
import org.bson.Document;
import tokenizer.TokenStrategy;
import tokenizer.Tokenizer;

import java.util.Iterator;
import java.util.List;

public final class InversedIndexer {

    private final Cache cache;
    private final Tokenizer tk;

    public InversedIndexer(Cache redisClient, TokenStrategy tokenizer) {
        cache = redisClient;
        tk = new Tokenizer(tokenizer);
    }

    public void insert(List<Document> batch) {
        Iterator<Document> t = batch.iterator();

        
    }

}