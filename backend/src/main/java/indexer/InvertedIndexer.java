package indexer;

import etl.QueueItem;
import indexer.tokenizer.TokenStrategy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public final class InvertedIndexer {
    private final TokenStrategy tk;

    public InvertedIndexer(TokenStrategy strategy) {
        tk = strategy;
    }

    public void tokenizeToQueue(QueueItem.DocumentBatch from, BlockingQueue<QueueItem> to) throws InterruptedException {
        Object2ObjectOpenHashMap<String, IntArrayList> uniqueTokens = new Object2ObjectOpenHashMap<>(262144);
        ObjectOpenHashSet<String> uniqueTokensPerDoc = new ObjectOpenHashSet<>(256);

        List<BsonDocument> docs = from.documents();
        int batchSize = docs.size();

        UUID[] docIds = new UUID[batchSize];
        int docIndex = 0;

        // This is the looping a batch of 5k documents
        for (BsonDocument doc : docs) {
            // Mapping the UUIDs to an array
            docIds[docIndex] = doc.getBinary("_id").asUuid();

            uniqueTokensPerDoc.clear(); // clear so next batch is clean

            // This is looping each field in the individual docuemnts
            for (Map.Entry<String, BsonValue> field : doc.entrySet()) {
                if (field.getKey().equals("_id"))
                    continue;

                if (field.getValue() instanceof BsonString str)
                    tk.toTokens(str.getValue(), uniqueTokensPerDoc);
            }

            // Maps ids to tokens from that document
            for (String token : uniqueTokensPerDoc)
                uniqueTokens.computeIfAbsent(token, k -> new IntArrayList()).add(docIndex);

            docIndex++;
        }

        to.put(new QueueItem.IndexerBatch(uniqueTokens, docIds));
    }

    public Set<String> tokenizeKeyWords(String input) {
        return tk.toTokens(input);
    }

}