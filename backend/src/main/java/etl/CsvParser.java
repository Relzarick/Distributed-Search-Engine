package etl;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public final class CsvParser implements AutoCloseable {
    private final CsvReader<NamedCsvRecord> parser;

    public static final List<Document> POISON_PILL = Collections.emptyList();

    public CsvParser(File file) throws IOException {
        parser = CsvReader.builder().ofNamedCsvRecord(file.toPath());
    }

    public void parseDataTo(BlockingQueue<List<Document>> queue) throws InterruptedException {
        Iterator<NamedCsvRecord> records = parser.iterator();

        if (!records.hasNext()) {
            queue.put(POISON_PILL);
            System.out.println("Empty CSV");
            return;
        }

        NamedCsvRecord firstRecord = records.next();
        String[] headers = firstRecord.getHeader().toArray(new String[0]);

        List<Document> batch = new ArrayList<>();
        batch.add(toDocument(firstRecord, headers));

        while (records.hasNext()) {
            batch.add(toDocument(records.next(), headers));

            if (batch.size() == 5000) {
                queue.put(batch);
                batch = new ArrayList<>();
            }
        }

        if (!batch.isEmpty())
            queue.put(batch);

        queue.put(POISON_PILL);
    }

    private Document toDocument(NamedCsvRecord record, String[] headers) {
        Document doc = new Document();

        for (String header : headers)
            doc.append(header, TypeConverter.convert(record.getField(header)));

        return doc;
    }

    @Override
    public void close() throws Exception {
        parser.close();
    }

}