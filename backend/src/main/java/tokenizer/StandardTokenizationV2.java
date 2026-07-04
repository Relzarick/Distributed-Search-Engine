package tokenizer;

import java.util.List;
import java.util.regex.Pattern;

public final class StandardTokenizationV2 extends BaseTokenization implements TokenStrategy {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("^[^a-z']+|[^a-z']+$");
    private static final Pattern NUMBERS = Pattern.compile("[0-9]");

    @Override
    public List<String> toTokens(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String lowerCaseInput = input.toLowerCase();

        List<String> tokens = WHITESPACE.splitAsStream(lowerCaseInput)
                .filter(token -> !NUMBERS.asPredicate().test(token))
                .map(token -> PUNCTUATION.matcher(token).replaceAll(""))
                .filter(token -> !STOP_WORDS.contains(token))
                .toList();

        return tokens.isEmpty() ? null : tokens;
    }

}