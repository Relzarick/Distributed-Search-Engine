package etl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public final class CsvParser implements AutoCloseable {
    File file;
    CSVFormat format;

    /**
     * Ingests .csv files and parses it for database
     */
    public CsvParser(File file) {
        this.file = file;
        this.format = CSVFormat.EXCEL.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .get();
    }

    public void parse(Consumer<List<Document>> consumer) throws IOException {
        try (CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, format)
        ) {
            String[] headerNames = parser.getHeaderNames().toArray(new String[0]);
            List<Document> batch = new ArrayList<>();

            for (CSVRecord record : parser) {
                Document doc = new Document();

                for (String header : headerNames) {
                    doc.append(header, TypeConverter.convert(record.get(header)));
                }

                batch.add(doc);

                if (batch.size() == 1500) {
                    consumer.accept(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty())
                consumer.accept(batch);

        } catch (IOException e) {
            throw new IOException("IO ERROR with parsing: ", e);
        }

    }

    @Override
    public void close() {
    }
}