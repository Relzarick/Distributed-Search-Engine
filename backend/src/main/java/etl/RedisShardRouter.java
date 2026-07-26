package etl;

import db.Index;
import db.RedisService;

import java.util.*;
import java.util.concurrent.*;

public class RedisShardRouter implements AutoCloseable {
    private final Writer[] shards;

    public RedisShardRouter() {
        shards = new Writer[]{
                new Writer(new RedisService("wench")),
                new Writer(new RedisService("wretch"))
        };
    }

    public void routeToRedis(Map<String, List<UUID>> dict) throws InterruptedException {
        List<Map<String, List<UUID>>> batches = new ArrayList<>(shards.length);
        int estimatedSize = (int) Math.ceil((dict.size() / (double) shards.length) * 1.33);

        for (int i = 0; i < shards.length; i++)
            batches.add(new HashMap<>(estimatedSize));

        for (Map.Entry<String, List<UUID>> entry : dict.entrySet()) {
            int index = hash(entry.getKey());
            batches.get(index).put(entry.getKey(), entry.getValue());
        }

        for (int i = 0; i < shards.length; i++) {
            Map<String, List<UUID>> subBatch = batches.get(i);

            if (!subBatch.isEmpty())
                shards[i].queueBatch(subBatch);
        }
    }

    private int hash(String key) {
        long keyHash = fnv1a64(key);
        return jumpConsistentHash(keyHash, shards.length);
    }

    private static int jumpConsistentHash(long key, int numBuckets) {
        long b = -1;
        long j = 0;
        while (j < numBuckets) {
            b = j;
            key = key * 2862933555777941757L + 1L;
            j = (long) ((b + 1) * (double) (1L << 31) / (double) ((key >>> 33) + 1));
        }
        return (int) b;
    }

    private static long fnv1a64(String text) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < text.length(); i++) {
            hash ^= text.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    @Override
    public void close() {
        for (Writer w : shards)
            w.closeThreads();
    }

    private static class Writer {
        private final Index redis;
        private static final int FLUSH_THRESHOLD = 1000;

        private final BlockingQueue<CommandQueue> queue = new ArrayBlockingQueue<>(10);
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        public Writer(Index instance) {
            redis = instance;
            executor.submit(this::loop);
        }

        private void loop() {
            int count = 0;

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    CommandQueue item = queue.take();

                    if (item instanceof CommandQueue.PoisonPill) {
                        if (count > 0)
                            redis.flush();

                        break;
                    }

                    CommandQueue.Commands cmd = (CommandQueue.Commands) item;

                    for (Map.Entry<String, List<UUID>> entry : cmd.batch().entrySet()) {
                        redis.set(entry.getKey(), entry.getValue());
                        count++;

                        if (count >= FLUSH_THRESHOLD) {
                            redis.flush();
                            count = 0;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                redis.close();
            }
        }

        public void queueBatch(Map<String, List<UUID>> batch) throws InterruptedException {
            queue.put(new CommandQueue.Commands(batch));
        }

        public void closeThreads() {
            try {
                queue.put(new CommandQueue.PoisonPill());
                executor.shutdown();

                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

    }

}