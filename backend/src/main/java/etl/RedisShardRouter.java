package etl;

import db.Index;
import db.RedisService;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.*;
import java.util.concurrent.*;

public class RedisShardRouter implements AutoCloseable {
    private final Writer[] shards;

    private static final double JUMP_CONSTANT = 2147483648.0d; // (double) (1L << 31)

    // MurmurHash2 64-bit constants
    private static final long M = 0xc6a4a7935bd1e995L;
    private static final int R = 47;
    private static final long SEED = 0x1234567890ABCDEFL; // Arbitrary constant seed

    public RedisShardRouter() {
        shards = new Writer[]{
                new Writer(new RedisService("wench")),
                new Writer(new RedisService("wretch"))
        };
    }

    public void routeToRedis(Object2ObjectOpenHashMap<String, IntArrayList> UniqueTokens, UUID[] UUIDs) throws InterruptedException {
        int estimatedSize = (int) Math.ceil((UniqueTokens.size() / (double) shards.length) * 1.33);
        List<Map<String, List<UUID>>> batches = new ArrayList<>(shards.length);

        // Sizes the hashMap to the size of dict divided by n containers plus a little extra
        for (int i = 0; i < shards.length; i++)
            batches.add(new HashMap<>(estimatedSize));

        // Loops each token
        for (Map.Entry<String, IntArrayList> entry : UniqueTokens.entrySet()) {
            int shardInstance = hash(entry.getKey()); // Hashing based on token
            IntArrayList indices = entry.getValue(); // Get docIndex

            // Contains the UUIDs that matches the token
            List<UUID> mappedUUIDs = new ArrayList<>(indices.size());

            // Matches docIndex to UUIDs array
            for (int i = 0; i < indices.size(); i++)
                mappedUUIDs.add(UUIDs[indices.getInt(i)]);

            batches.get(shardInstance).put(entry.getKey(), mappedUUIDs);
        }

        UniqueTokens.clear();

        for (int i = 0; i < shards.length; i++) {
            Map<String, List<UUID>> subBatch = batches.get(i);

            if (!subBatch.isEmpty())
                shards[i].queueBatch(subBatch);
        }
    }

    // AI magic algo
    private int hash(String key) {
        long keyHash = murmur2_64(key);
        return jumpConsistentHash(keyHash, shards.length);
    }

    private static int jumpConsistentHash(long key, int numBuckets) {
        long b = -1;
        long j = 0;

        while (j < numBuckets) {
            b = j;
            key = key * 2862933555777941757L + 1L;
            j = (long) ((b + 1) * JUMP_CONSTANT / (double) ((key >>> 33) + 1));
        }
        return (int) b;
    }

    private static long murmur2_64(String text) {
        int len = text.length();
        long h = SEED ^ (len * M);

        int i = 0;
        // Process 4 characters (64 bits) at a time
        for (; i <= len - 4; i += 4) {
            long k = ((long) text.charAt(i) << 48) |
                    ((long) text.charAt(i + 1) << 32) |
                    ((long) text.charAt(i + 2) << 16) |
                    ((long) text.charAt(i + 3));

            k *= M;
            k ^= k >>> R;
            k *= M;

            h ^= k;
            h *= M;
        }

        // Handle the remaining 1 to 3 characters using a fall-through switch
        switch (len - i) {
            case 3:
                h ^= ((long) text.charAt(i + 2)) << 16;
                // fall through
            case 2:
                h ^= ((long) text.charAt(i + 1)) << 32;
                // fall through
            case 1:
                h ^= ((long) text.charAt(i)) << 48;
                h *= M;
        }

        // Final avalanche to ensure the last bits are heavily scrambled
        h ^= h >>> R;
        h *= M;
        h ^= h >>> R;

        return h;
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