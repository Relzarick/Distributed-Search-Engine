package setup;

import org.apache.commons.csv.CSVFormat;
import parser.Parser;

import java.util.Arrays;

public final class Setup {
    static void main(String[] args) {
        CSVFormat format = CSVFormat.EXCEL.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .get();

        Parser parser = new Parser(format);

    }

    private void insertToDb() {

    }

}