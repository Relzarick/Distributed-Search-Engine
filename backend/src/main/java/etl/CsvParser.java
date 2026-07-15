package etl;

import bootstrap.FileLoader;
import de.siegmar.fastcsv.reader.CsvIndex;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import logging.StopWatch;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;

public final class CsvParser {
    private final static Path PATH = FileLoader.getSource();

    private final CsvIndex index;
    private final int totalPages;
    private String[] headers;

    private static final int CAPACITY = 5000;
    public static final List<Document> POISON_PILL = Collections.emptyList();

    public CsvParser() throws IOException {
        StopWatch timer = new StopWatch("Index");
        try (IndexedCsvReader<CsvRecord> reader = IndexedCsvReader.builder().pageSize(CAPACITY).ofCsvRecord(PATH)) {
            index = reader.getIndex();
            totalPages = index.pages().size();

            getHeaders();
        }

        timer.stop();
    }

    /**
     * @throws NoSuchElementException Crashes app if CSV is empty.
     */
    private void getHeaders() throws NoSuchElementException {
        try (IndexedCsvReader<CsvRecord> r = IndexedCsvReader.builder().index(index).ofCsvRecord(PATH)) {
            List<CsvRecord> firstPage = r.readPage(0);

            headers = firstPage.getFirst().getFields().toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @param threadIndex in the current thread.
     * @param threadCount is threads assigned to parsing.
     * @return the page range for reader.
     */
    public int[] getPageRange(int threadIndex, int threadCount) {
        int base = totalPages / threadCount;

        int start = threadIndex * base;
        int end = (threadIndex == threadCount - 1) ? totalPages : start + base;

        return new int[]{start, end};
    }

    public void parseDataTo(BlockingQueue<List<Document>> queue, int start, int end) throws IOException, InterruptedException {
        try (IndexedCsvReader<CsvRecord> reader = IndexedCsvReader.builder().index(index).pageSize(CAPACITY).ofCsvRecord(PATH)) {
            List<Document> batch = new ArrayList<>(CAPACITY); // A batch is a list of csv rows

            for (int i = start; i < end; i++) { // Page loop
                List<CsvRecord> page = reader.readPage(i);
                int rowStart = (i == 0) ? 1 : 0;

                for (int j = rowStart; j < page.size(); j++) { // Documents loop
                    batch.add(toDocument(page.get(j)));

                    if (batch.size() == CAPACITY) {
                        queue.put(batch);
                        batch = new ArrayList<>(CAPACITY);
                    }
                }

            }

            if (!batch.isEmpty())
                queue.put(batch);
        }

    }

    /**
     * @param records Is the row of values that will map to headers.
     * @return A Document for mongo.
     */
    private Document toDocument(CsvRecord records) {
        Document doc = new Document();

        for (int i = 0; i < headers.length; i++)
            doc.put(headers[i], TypeConverter.convert(records.getField(i)));

        return doc;
    }

}