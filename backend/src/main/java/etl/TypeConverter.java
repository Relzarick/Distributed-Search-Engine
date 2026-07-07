package etl;

/**
 * Sorts data into propper types and returns
 * This accepts Infinity and NaN as floats
 */
public final class TypeConverter {
    private TypeConverter() {
    }

    public static Object convert(String value) {
        if (value == null || value.isBlank())
            return null;

        if (isInt(value)) {
            long parsedValue = Long.parseLong(value);

            if (parsedValue >= Integer.MIN_VALUE && parsedValue <= Integer.MAX_VALUE)
                return (int) parsedValue;

            return parsedValue;
        }

        if (isDouble(value))
            return Double.parseDouble(value);

        return value;
    }

    private static boolean isInt(String str) {
        int start = checkStart(str);

        if (start == -1)
            return false;

        for (int i = start; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i)))
                return false;
        }

        return true;
    }

    private static boolean isDouble(String str) {
        int start = checkStart(str);

        if (start == -1)
            return false;

        int dotCount = 0;
        boolean hasDigit = false;

        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);

            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (c == '.') {
                dotCount++;
            } else {
                return false;
            }
        }

        if (dotCount == 0 || !hasDigit)
            return false;

        return dotCount <= 1;
    }

    private static int checkStart(String str) {
        if (str.charAt(0) == '-') {
            if (str.length() == 1)
                return -1;

            return 1;
        }

        return 0;
    }

}