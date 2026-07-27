package etl.parser;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import org.bson.*;

/**
 * Sorts data into propper types and returns
 * This accepts Infinity and NaN as floats
 */
public final class TypeConverter {
    private TypeConverter() {
    }

    /**
     *
     * @param buf This is the internal buffer from fastCSV
     */
    public static BsonValue convert(char[] buf, int offset, int length) {
        if (length == 0 || isBlank(buf, offset, length))
            return BsonNull.VALUE;

        int i = offset;
        int end = offset + length;

        // Check for signs
        boolean negative = false;
        if (buf[i] == '-' || buf[i] == '+') {
            if (length == 1)
                return new BsonString(new String(buf, offset, length));

            negative = (buf[i] == '-');
            i++;
        }

        long accumulator = 0;
        int dotCount = 0;
        boolean hasDigits = false;
        boolean overflow = false;

        for (; i < end; i++) {
            char c = buf[i];

            if (c >= '0' && c <= '9') {
                hasDigits = true;
                if (!overflow) { // Check signed 64-bit long overflow threshold
                    int digit = c - '0';

                    if (accumulator > 922337203685477580L || (accumulator == 922337203685477580L && digit > (negative ? 8 : 7)))
                        overflow = true;
                    else
                        accumulator = accumulator * 10 + digit;
                }
            } else if (c == '.')
                dotCount++;
            else // Not a number
                return new BsonString(new String(buf, offset, length));
        }

        if (!hasDigits || dotCount > 1) // Random garbage
            return new BsonString(new String(buf, offset, length));

        if (dotCount == 1) // Its a float
            return new BsonDouble(JavaDoubleParser.parseDouble(buf, offset, length));

        if (overflow) // Weird ass number that is greater than a long
            return new BsonString(new String(buf, offset, length));

        long finalVal = negative ? -accumulator : accumulator;

        if (finalVal >= Integer.MIN_VALUE && finalVal <= Integer.MAX_VALUE)
            return new BsonInt32((int) finalVal);

        return new BsonInt64(finalVal);
    }

    private static boolean isBlank(char[] buf, int offset, int length) {
        for (int i = 0; i < offset + length; i++) {
            if (buf[i] > ' ')
                return false;
        }

        return true;
    }

}