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

    // FNV-1a 64-bit constants
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    public RedisShardRouter() {
        shards = new Writer[]{
                new Writer(new RedisService("r1")),
                new Writer(new RedisService("r2")),
                new Writer(new RedisService("r3")),
                new Writer(new RedisService("r4")),
        };
    }

    public void routeToRedis(Object2ObjectOpenHashMap<String, IntArrayList> UniqueTokens, UUID[] UUIDs) throws InterruptedException {
        int estimatedSize = (int) Math.ceil((UniqueTokens.size() / (double) shards.length) * 1.33);
        List<Map<String, UUID[]>> batches = new ArrayList<>(shards.length);

        // Sizes the hashMap to the size of dict divided by n containers plus a little extra
        for (int i = 0; i < shards.length; i++)
            batches.add(new HashMap<>(estimatedSize));

        // Loops each token
        for (Map.Entry<String, IntArrayList> entry : UniqueTokens.entrySet()) {
            int shardInstance = hash(entry.getKey()); // Hashing based on token

            IntArrayList docIndexes = entry.getValue(); // Get docIndex
            UUID[] mappedUUIDs = new UUID[docIndexes.size()];  // Documents that contains the token

            // Matches docIndex to UUIDs array
            for (int i = 0; i < docIndexes.size(); i++)
                mappedUUIDs[i] = UUIDs[docIndexes.getInt(i)];

            batches.get(shardInstance).put(entry.getKey(), mappedUUIDs);
        }

        UniqueTokens.clear(); // Clearing because its already copied to batches

        // Routes the hashed token into their respective shards
        for (int i = 0; i < shards.length; i++) {
            Map<String, UUID[]> subBatch = batches.get(i);

            if (!subBatch.isEmpty())
                shards[i].queueBatch(subBatch);
        }
    }

    // AI magic algo
    private int hash(String key) {
        long keyHash = fastHash64(key);
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

    private static long fastHash64(String text) {
        long hash = FNV_OFFSET_BASIS;

        for (int i = 0, len = text.length(); i < len; i++) {
            hash ^= text.charAt(i);
            hash *= FNV_PRIME;
        }

        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >>> 33;

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

                    for (Map.Entry<String, UUID[]> entry : cmd.batch().entrySet()) {
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

        public void queueBatch(Map<String, UUID[]> batch) throws InterruptedException {
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