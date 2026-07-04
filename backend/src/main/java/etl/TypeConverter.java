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

        try {
            long parsedValue = Long.parseLong(value);

            if (parsedValue >= Integer.MIN_VALUE && parsedValue <= Integer.MAX_VALUE)
                return (int) parsedValue;

            return parsedValue;

        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                return value;
            }
        }

    }

}