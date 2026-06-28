package etl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeConverterTest {
    @Test
    void convertNullOrEmpty() {
        assertNull(TypeConverter.convert(null));
        assertNull(TypeConverter.convert(""));
        assertNull(TypeConverter.convert(" "));
    }

    @Test
    void convertToInt() {
        assertEquals(100, TypeConverter.convert("100"));
        assertEquals(-100, TypeConverter.convert("-100"));

        assertInstanceOf(Integer.class, TypeConverter.convert("2147483647"));
        assertEquals(2147483647, TypeConverter.convert("2147483647"));

        assertInstanceOf(Integer.class, TypeConverter.convert("-2147483648"));
        assertEquals(-2147483648, TypeConverter.convert("-2147483648"));
    }

    @Test
    void convertToLong() {
        assertInstanceOf(Long.class, TypeConverter.convert("2147483648"));
        assertEquals(2147483648L, TypeConverter.convert("2147483648"));

        assertInstanceOf(Long.class, TypeConverter.convert("-2147483649"));
        assertEquals(-2147483649L, TypeConverter.convert("-2147483649"));
    }

    @Test
    void convertToDouble() {
        assertInstanceOf(Double.class, TypeConverter.convert("100.99"));

        assertEquals(100.99, TypeConverter.convert("100.99"));
        assertEquals(-100.99, TypeConverter.convert("-100.99"));
    }

    @Test
    void convertToString() {
        assertEquals("This is a test!", TypeConverter.convert("This is a test!"));
    }

}