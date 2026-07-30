package redis;

import etl.CommandQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.*;
import java.util.concurrent.*;

public class RedisShardRouter implements AutoCloseable {
    private final Writer[] shards;

    public RedisShardRouter() {
        shards = new Writer[]{
                new Writer(new RedisService("r1")),
                new Writer(new RedisService("r2")),
                new Writer(new RedisService("r3")),
                new Writer(new RedisService("r4"))
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
            int shardInstance = TokenHasher.hash(entry.getKey(), shards.length); // Hashing based on token

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