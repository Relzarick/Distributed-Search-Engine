package etl;

import org.bson.*;

/**
 * Sorts data into propper types and returns
 * This accepts Infinity and NaN as floats
 */
public final class TypeConverter {
    private TypeConverter() {
    }

    public static BsonValue convert(String value) {
        if (value == null || value.isBlank()) {
            return BsonNull.VALUE;
        }

        int len = value.length();
        char firstChar = value.charAt(0);
        int start = 0;

        if (firstChar == '-' || firstChar == '+') {
            if (len == 1)
                return new BsonString(value);

            start = 1;
        }

        int dotCount = 0;
        boolean isDigit = false;

        for (int i = start; i < len; i++) {
            char c = value.charAt(i);

            if (c >= '0' && c <= '9')
                isDigit = true;
            else if (c == '.')
                dotCount++;
            else { // it's a String
                isDigit = false;
                break;
            }
        }

        if (!isDigit || dotCount > 1)
            return new BsonString(value);

        if (dotCount == 1)
            return new BsonDouble(Double.parseDouble(value));

        int digits = len - start;

        if (digits > 19)
            return new BsonString(value);

        try {
            long parsedValue = Long.parseLong(value);
            if (parsedValue >= Integer.MIN_VALUE && parsedValue <= Integer.MAX_VALUE)
                return new BsonInt32((int) parsedValue);

            return new BsonInt64(parsedValue);
        } catch (NumberFormatException e) { // exceeds Long limit (it's some weird ass number)
            return new BsonString(value);
        }

    }

}