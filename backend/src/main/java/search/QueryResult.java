package search;

import java.util.List;
import java.util.UUID;

public record QueryResult(List<UUID> uuids, int count) {
}
