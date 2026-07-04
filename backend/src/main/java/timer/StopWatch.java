package timer;

import java.time.Duration;

public class StopWatch {
    private final long startNanos;

    public StopWatch(String id) {
        this.startNanos = System.nanoTime();
        System.out.println("Timer started in " + id);
    }

    public void stop() {
        long endNanos = System.nanoTime();

        Duration elapsed = Duration.ofNanos(endNanos - startNanos);

        long minutes = elapsed.toMinutes();
        long seconds = elapsed.toSecondsPart();
        long millis = elapsed.toMillisPart();

        long nanos = elapsed.toNanosPart() % 1_000_000;

        System.out.print("Time elapsed: ");

        if (minutes > 0) {
            System.out.printf("%dm %ds %dms %dns%n", minutes, seconds, millis, nanos);
        } else if (seconds > 0) {
            System.out.printf("%ds %dms %dns%n", seconds, millis, nanos);
        } else if (millis > 0) {
            System.out.printf("%dms %dns%n", millis, nanos);
        } else {
            // For operations that finish in under a single millisecond
            System.out.printf("%dns%n", nanos);
        }
    }
    
}