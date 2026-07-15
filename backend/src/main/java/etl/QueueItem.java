package etl;

import org.bson.Document;

import java.util.List;

public sealed interface QueueItem {
    record DocumentBatch(List<Document> documents) implements QueueItem {
    }

    record PoisonPill() implements QueueItem {
    }

}