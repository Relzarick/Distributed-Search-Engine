package etl;

import etl.parser.TypeConverter;
import org.bson.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TypeConverterTest {
    private BsonValue convert(String str) {
        if (str == null)
            return TypeConverter.convert(null, 0, 0);

        return TypeConverter.convert(str.toCharArray(), 0, str.length());
    }

    @Test
    void convertNullOrEmpty() {
        assertEquals(BsonNull.VALUE, convert(null));
        assertEquals(BsonNull.VALUE, convert(""));
        assertEquals(BsonNull.VALUE, convert(" "));
    }

    @Test
    void convertToInt() {
        assertEquals(new BsonInt32(100), convert("100"));
        assertEquals(new BsonInt32(-100), convert("-100"));

        assertInstanceOf(BsonInt32.class, convert("2147483647"));
        assertEquals(new BsonInt32(2147483647), convert("2147483647"));

        assertInstanceOf(BsonInt32.class, convert("-2147483648"));
        assertEquals(new BsonInt32(-2147483648), convert("-2147483648"));
    }

    @Test
    void convertToLong() {
        assertInstanceOf(BsonInt64.class, convert("2147483648"));
        assertEquals(new BsonInt64(2147483648L), convert("2147483648"));

        assertInstanceOf(BsonInt64.class, convert("-2147483649"));
        assertEquals(new BsonInt64(-2147483649L), convert("-2147483649"));
    }

    @Test
    void convertToDouble() {
        assertInstanceOf(BsonDouble.class, convert("100.99"));

        assertEquals(new BsonDouble(100.99), convert("100.99"));
        assertEquals(new BsonDouble(-100.99), convert("-100.99"));
    }

    @Test
    void convertToString() {
        assertEquals(new BsonString("This is a test!"), convert("This is a test!"));
    }

}