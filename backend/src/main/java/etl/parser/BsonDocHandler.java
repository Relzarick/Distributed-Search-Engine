package etl.parser;

import de.siegmar.fastcsv.reader.AbstractBaseCsvCallbackHandler;
import de.siegmar.fastcsv.reader.RecordType;
import io.github.robsonkades.uuidv7.UUIDv7;
import org.bson.BsonBinary;
import org.bson.BsonDocument;

public class BsonDocHandler extends AbstractBaseCsvCallbackHandler<BsonDocument> {
    private BsonDocument currentDoc;
    private final String[] headers;

    public BsonDocHandler(String[] headers) {
        this.headers = headers;
    }

    @Override
    protected void handleBegin(long startingLineNumber) {
        int capacity = (int) Math.ceil((headers.length + 1) / 0.75f);

        currentDoc = new BsonDocument(capacity);
        currentDoc.put("_id", new BsonBinary(UUIDv7.randomUUID()));
    }

    @Override
    protected void handleField(int fieldIdx, char[] buf, int offset, int len, boolean quoted) {
        if (fieldIdx < headers.length) {
            String headerName = headers[fieldIdx];
            currentDoc.put(headerName, TypeConverter.convert(buf, offset, len));
        }
    }

    @Override
    protected BsonDocument buildRecord() {
        if (getRecordType() == RecordType.DATA)
            return currentDoc;

        return null;
    }

}