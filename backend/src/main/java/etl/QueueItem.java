package etl;

import org.bson.BsonDocument;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public sealed interface QueueItem {
    record DocumentBatch(List<BsonDocument> documents) implements QueueItem {
    }

    record IndexerBatch(Map<String, List<UUID>> dict) implements QueueItem {
    }

    record PoisonPill() implements QueueItem {
    }

}