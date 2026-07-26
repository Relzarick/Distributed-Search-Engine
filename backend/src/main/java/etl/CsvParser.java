package etl;

import bootstrap.FileLoader;
import de.siegmar.fastcsv.reader.CsvIndex;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import io.github.robsonkades.uuidv7.UUIDv7;
import org.bson.BsonBinary;
import org.bson.BsonDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;

public final class CsvParser {
    private final static Path PATH = FileLoader.getSource();

    private final CsvIndex index;
    private final int totalPages;
    private final String[] headers;

    private static final int CAPACITY = 5000;

    /**
     *
     * @throws IOException if path does not exist
     */
    public CsvParser() throws IOException {
        try (IndexedCsvReader<CsvRecord> reader = IndexedCsvReader.builder().pageSize(CAPACITY).ofCsvRecord(PATH)) {
            index = reader.getIndex();
            totalPages = index.pages().size();

            List<CsvRecord> firstPage = reader.readPage(0);

            if (firstPage.isEmpty())
                throw new NoSuchElementException("CSV is empty.");

            headers = firstPage.getFirst().getFields().toArray(new String[0]);
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

    /**
     * This batches documents into chunks of 5k
     *
     * @param start page number for loop
     * @param end   page number for loop
     * @throws IOException          if path does not exist
     * @throws InterruptedException is from the queues
     */
    public void parseDataTo(BlockingQueue<QueueItem> queue1, BlockingQueue<QueueItem> queue2, int start, int end) throws IOException, InterruptedException {
        try (IndexedCsvReader<CsvRecord> reader = IndexedCsvReader.builder().index(index).pageSize(CAPACITY).ofCsvRecord(PATH)) {
            List<BsonDocument> batch = new ArrayList<>(CAPACITY); // A batch is a list of csv rows

            for (int i = start; i < end; i++) { // Page loop
                List<CsvRecord> page = reader.readPage(i);
                int rowStart = (i == 0) ? 1 : 0;

                for (int j = rowStart; j < page.size(); j++) { // Documents loop
                    batch.add(toDocument(page.get(j)));

                    if (batch.size() == CAPACITY) {
                        queue1.put(new QueueItem.DocumentBatch(batch));
                        queue2.put(new QueueItem.DocumentBatch(batch));
                        batch = new ArrayList<>(CAPACITY);
                    }
                }
            }

            if (!batch.isEmpty()) {
                queue1.put(new QueueItem.DocumentBatch(batch));
                queue2.put(new QueueItem.DocumentBatch(batch));
            }
        }
    }

    /**
     * This will also create a UUID7 for document _id
     *
     * @param records Is the row of values that will map to headers.
     * @return A Document for mongo.
     */
    private BsonDocument toDocument(CsvRecord records) {
        int capacity = (int) Math.ceil((headers.length + 1) / 0.75f);

        BsonDocument doc = new BsonDocument(capacity);
        doc.put("_id", new BsonBinary(UUIDv7.randomUUID()));

        for (int i = 0; i < headers.length; i++)
            doc.put(headers[i], TypeConverter.convert(records.getField(i)));

        return doc;
    }

}