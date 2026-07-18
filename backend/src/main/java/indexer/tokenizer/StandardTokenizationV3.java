package indexer.tokenizer;

import java.util.ArrayList;
import java.util.List;

public class StandardTokenizationV3 extends BaseTokenization implements TokenStrategy {
    @Override
    public List<String> toTokens(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        List<String> tokens = new ArrayList<>();

        char[] chars = input.toCharArray();
        int length = chars.length;
        int i = 0;

        while (i < length) {
            while (i < length && Character.isWhitespace(chars[i]))
                i++;

            if (i >= length)
                break;

            int start = i;
            boolean isDigit = false;

            while (i < length && !Character.isWhitespace(chars[i])) {
                if (chars[i] >= '0' && chars[i] <= '9')
                    isDigit = true;

                i++;
            }
            int end = i;

            if (isDigit)
                continue;

            int wordStart = start;
            while (wordStart < end && isValid(chars[wordStart]))
                wordStart++;

            int wordEnd = end;
            while (wordEnd > wordStart && isValid(chars[wordEnd - 1]))
                wordEnd--;

            if (wordStart < wordEnd) {
                String token = new String(chars, wordStart, wordEnd - wordStart).toLowerCase();

                if (!STOP_WORDS.contains(token))
                    tokens.add(token);
            }
        }

        return tokens.isEmpty() ? null : tokens;
    }

    private boolean isValid(char c) {
        return (c < 'a' || c > 'z') && (c < 'A' || c > 'Z');
    }

}