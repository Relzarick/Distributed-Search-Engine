package etl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TypeConverterTest {
    @Test
    void convertNullOrEmpty() {
        assertNull(TypeConverter.convert(null));
        assertNull(TypeConverter.convert(""));
    }

    @Test
    void convertToInt() {
        assertEquals(100, TypeConverter.convert("100"));
        assertEquals(-100, TypeConverter.convert("-100"));
    }

    @Test
    void convertToLong() {
        assertEquals(2147483648L, TypeConverter.convert("2147483648"));
        assertEquals(-2147483649L, TypeConverter.convert("-2147483649"));
    }

    @Test
    void convertToDouble() {
        assertEquals(100.99, TypeConverter.convert("100.99"));
        assertEquals(-100.99, TypeConverter.convert("-100.99"));
    }

    @Test
    void convertToString() {
        assertEquals("This is a test!", TypeConverter.convert("This is a test!"));
    }

}