package logging;

import java.time.Duration;

public class StopWatch {
    private final long startNanos;
    private final String id;

    /**
     * This will print: "Timer started for "
     */
    public StopWatch(String name) {
        startNanos = System.nanoTime();
        id = name;
    }

    public void stop() {
        printElapsed(id + " time elapsed: ");
    }

    public void stopOnFailure() {
        printElapsed(id + " failed after: ");
    }

    private void printElapsed(String label) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        long minutes = elapsed.toMinutes();
        long seconds = elapsed.toSecondsPart();
        long millis = elapsed.toMillisPart();
        long nanos = elapsed.toNanosPart() % 1_000_000;

        if (minutes > 0)
            System.out.printf("%s%dm %ds %dms %dns%n", label, minutes, seconds, millis, nanos);
        else if (seconds > 0)
            System.out.printf("%s%ds %dms %dns%n", label, seconds, millis, nanos);
        else if (millis > 0)
            System.out.printf("%s%dms %dns%n", label, millis, nanos);
        else
            System.out.printf("%s%dns%n", label, nanos);
    }

}