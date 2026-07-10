package bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileLoader {
    /**
     * The following settings were what I tested to be fastest for my setup, adjust accordingly.
     */
    private static final String PATH_NAME = "/app/bind";
    private static final int BUFFER_SIZE = 8 * 1024 * 1024;
    private static final int THREAD_COUNT = 4;

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

            long sourceSize = sourceAttrs.size();
            long targetSize = targetAttrs.size();

            FileTime sourceLMT = sourceAttrs.lastModifiedTime();
            FileTime targetLMT = targetAttrs.lastModifiedTime();

            if (targetSize == sourceSize && targetLMT.equals(sourceLMT))
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
                    try (RandomAccessFile in = new RandomAccessFile(source.toFile(), "r");
                         RandomAccessFile out = new RandomAccessFile(target.toFile(), "rw")) {

                        long remaining = range[1] - range[0];
                        byte[] buffer = new byte[BUFFER_SIZE];

                        in.seek(range[0]);
                        out.seek(range[0]);

                        while (remaining > 0) {
                            int toRead = (int) Math.min(buffer.length, remaining);
                            int bytesRead = in.read(buffer, 0, toRead);
                            out.write(buffer, 0, bytesRead);

                            remaining -= bytesRead;
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            }

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
        File[] csv = dir.listFiles((_, file) -> file.endsWith(".csv"));

        if (csv == null)
            throw new RuntimeException(PATH_NAME + " dir was not found");

        if (csv.length > 1)
            throw new RuntimeException("Only accepts ONE CSV file " + PATH_NAME);

        if (csv.length == 0)
            throw new RuntimeException("No CSV file found in " + PATH_NAME);

        return csv[0].toPath();
    }

}