package indexer;

import etl.QueueItem;
import indexer.tokenizer.TokenStrategy;
import indexer.tokenizer.Tokenizer;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public final class InvertedIndexer {
    private final Tokenizer tk;

    public InvertedIndexer(TokenStrategy strategy) {
        tk = new Tokenizer(strategy);
    }

    public void tokenizeToQueue(QueueItem.DocumentBatch from, BlockingQueue<QueueItem> to) throws InterruptedException {
        Map<String, List<UUID>> dict = new HashMap<>(262144);
        Set<String> uniqueTokensPerDoc = new HashSet<>(256);

        // This is the looping a batch of 5k documents
        for (BsonDocument doc : from.documents()) {
            UUID id = doc.getBinary("_id").asUuid();
            uniqueTokensPerDoc.clear();

            // This is looping each field in the individual docuemnts
            for (Map.Entry<String, BsonValue> field : doc.entrySet()) {
                if (field.getKey().equals("_id"))
                    continue;

                if (field.getValue() instanceof BsonString str)
                    tk.tokenizeInto(str.getValue(), uniqueTokensPerDoc);
            }

            // For each token, check if it already exists in dict else add it
            for (String token : uniqueTokensPerDoc)
                dict.computeIfAbsent(token, k -> new ArrayList<>(1)).add(id);
        }

        to.put(new QueueItem.IndexerBatch(dict));
    }

}