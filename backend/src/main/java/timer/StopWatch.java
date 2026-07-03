package timer;

import java.time.Duration;
import java.time.Instant;

public class StopWatch {
    private final Instant start;

    public StopWatch(String id) {
        start = Instant.now();
        System.out.println("Timer started in " + id);
    }

    public void stop() {
        Instant end = Instant.now();
        Duration elapsed = Duration.between(start, end);

        long minutes = elapsed.toMinutes();
        long seconds = elapsed.toSecondsPart();

        if (minutes > 0) {
            System.out.printf("Time elapsed: %dm %ds%n", minutes, seconds);
        } else {
            System.out.printf("Time elapsed: %ds%n", seconds);
        }
    }

}