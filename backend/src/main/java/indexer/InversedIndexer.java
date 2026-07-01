package indexer;

import db.lettuce.Cache;
import db.mongo.Repository;
import tokenizer.TokenStrategy;
import tokenizer.Tokenizer;

public final class InversedIndexer {
    private final Cache cache;
    private final Repository db;
    private final Tokenizer tk;

    public InversedIndexer(Cache redisClient, Repository database, TokenStrategy tokenizer) {
        cache = redisClient;
        db = database;
        tk = new Tokenizer(tokenizer);
    }

}