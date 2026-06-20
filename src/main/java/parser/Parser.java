package parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class Parser {
    public Parser(CSVFormat format) {
        InputStream is = getClass().getResourceAsStream("/movies.csv");

        if (is == null) {
            System.out.println("Failed to get csv");
            return;
        }

        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVParser.parse(reader, format)
        ) {
            headerNames = csvParser.getHeaderNames().toArray(new String[0]);
            retrieveCsv("title", csvParser);


        } catch (IOException e) {
            System.out.println("ERROR with resource: " + e.getMessage());
        }


    }

    private void retrieveCsv(String header, CSVParser parser) {
        for (CSVRecord r : parser) {
            System.out.println(r.get(header));
        }
        // set this to insert db
    }

    public String[] getHeaderNames() {
        return headerNames;
    }

    private String[] headerNames;
}