package indexer;

import etl.QueueItem;
import indexer.tokenizer.TokenStrategy;
import indexer.tokenizer.Tokenizer;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public final class InversedIndexer {
    private final Tokenizer tk;

    public InversedIndexer(TokenStrategy strategy) {
        tk = new Tokenizer(strategy);
    }

    public void tokenizeToQueue(List<Document> from, BlockingQueue<QueueItem> to) throws InterruptedException {
        Map<String, List<UUID>> dict = new HashMap<>(131072);
        Set<String> uniqueTokensPerDoc = new HashSet<>(250);

        for (Document doc : from) {
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

        to.put(new QueueItem.IndexerBatch(dict));
    }

}