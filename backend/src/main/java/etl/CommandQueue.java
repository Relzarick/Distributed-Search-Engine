package etl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CommandQueue {
    record Commands(Map<String, List<UUID>> batch) implements CommandQueue {
    }

    record PoisonPill() implements CommandQueue {
    }

}