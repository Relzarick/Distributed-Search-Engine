package etl;

import java.util.Map;
import java.util.UUID;

public interface CommandQueue {
    record Commands(Map<String, UUID[]> batch) implements CommandQueue {
    }

    record PoisonPill() implements CommandQueue {
    }

}