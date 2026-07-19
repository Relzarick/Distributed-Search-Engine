package etl;

import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public sealed interface QueueItem {
    record DocumentBatch(List<Document> documents) implements QueueItem {
    }

    record IndexerBatch(Map<String, List<UUID>> dict) implements QueueItem {
    }

    record PoisonPill() implements QueueItem {
    }

}