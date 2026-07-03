package etl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CsvParser implements AutoCloseable {

    CSVParser records;

    /**
     * Ingests .csv files and parses it for database
     */
    public CsvParser(File file) throws IOException {
        CSVFormat format = CSVFormat.EXCEL.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .get();

        records = CSVParser.parse(file, StandardCharsets.UTF_8, format);
    }

    public Iterator<List<Document>> returnTasks() throws IOException {
        String[] headerNames = records.getHeaderNames().toArray(new String[0]);

        return new BatchIterator(headerNames, records.iterator());
    }

    @Override
    public void close() throws Exception {

    }

    private record BatchIterator(
            String[] headers,
            Iterator<CSVRecord> records
    ) implements Iterator<List<Document>> {

        @Override
        public boolean hasNext() {
            return records.hasNext();
        }

        @Override
        public List<Document> next() {
            List<Document> batch = new ArrayList<>();

            while (batch.size() < 5000 && records.hasNext()) {
                CSVRecord record = records.next();
                Document doc = new Document();

                for (String header : headers) {
                    doc.append(header, TypeConverter.convert(record.get(header)));
                }

                batch.add(doc);
            }

            return batch;
        }
    }

}