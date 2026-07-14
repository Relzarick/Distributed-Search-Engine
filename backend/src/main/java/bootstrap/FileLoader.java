package bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileLoader {
    /**
     * The following settings were what I tested to be fastest for my setup, adjust accordingly.
     */
    private static final String PATH_NAME = "/app/bind";
    private static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static final int THREAD_COUNT = 8;

    private FileLoader() {
    }

    /**
     * Copies the csv onto a Named Volume.
     *
     * @return The path to the staged CSV on the named volume.
     * @throws IOException If file is not found.
     */
    public static Path stageCsv() throws IOException {
        Path source = getSource();
        Path target = Paths.get("/app/data/file.csv");

        if (Files.exists(target)) {
            BasicFileAttributes sourceAttrs = Files.readAttributes(source, BasicFileAttributes.class);
            BasicFileAttributes targetAttrs = Files.readAttributes(target, BasicFileAttributes.class);

            if (targetAttrs.size() == sourceAttrs.size() &&
                    targetAttrs.lastModifiedTime().equals(sourceAttrs.lastModifiedTime()))
                return target;
        }

        long size = Files.size(source);

        try (RandomAccessFile out = new RandomAccessFile(target.toFile(), "rw")) {
            out.setLength(size);
        }

        try (ExecutorService service = Executors.newFixedThreadPool(THREAD_COUNT)) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                long[] range = copyWithRange(i, size);

                service.submit(() -> {
                    try (FileChannel in = FileChannel.open(source, StandardOpenOption.READ);
                         FileChannel out = FileChannel.open(target, StandardOpenOption.WRITE)) {

                        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                        int capacity = buffer.capacity();
                        long remaining = range[1] - range[0];

                        in.position(range[0]);
                        out.position(range[0]);

                        while (remaining > 0) {
                            buffer.clear();

                            if (remaining < capacity)
                                buffer.limit((int) remaining);

                            int bytesRead = in.read(buffer);

                            if (bytesRead == -1)
                                break;

                            buffer.flip();
                            out.write(buffer);
                            remaining -= bytesRead;
                        }

                    } catch (IOException e) {
                        throw new RuntimeException("Thread failed during copy", e);
                    }
                });

            }

            service.shutdown();
        }

        Files.setLastModifiedTime(target, Files.getLastModifiedTime(source));

        return target;
    }

    private static long[] copyWithRange(int index, long size) {
        long base = size / THREAD_COUNT;

        long start = index * base;
        long end = (index == THREAD_COUNT - 1) ? size : start + base;

        return new long[]{start, end};
    }

    /**
     * Should only provide ONE CSV file under specified folder.
     *
     * @return A File only if a valid CSV was found in provided path.
     * @throws RuntimeException if rules were not followed
     */
    private static Path getSource() {
        File dir = new File(PATH_NAME);
        File[] csv = dir.listFiles((f, file) -> file.endsWith(".csv"));

        if (csv == null)
            throw new RuntimeException(PATH_NAME + " dir was not found");

        if (csv.length > 1)
            throw new RuntimeException("Only accepts ONE CSV file " + PATH_NAME);

        if (csv.length == 0)
            throw new RuntimeException("No CSV file found in " + PATH_NAME);

        return csv[0].toPath();
    }

}