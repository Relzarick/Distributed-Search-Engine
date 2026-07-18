package indexer.tokenizer;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Removes basic stop words
 */
public class StandardTokenization extends BaseTokenization implements TokenStrategy {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("^[^a-z']+|[^a-z']+$");

    @Override
    public List<String> toTokens(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String lowerCaseInput = input.toLowerCase();

        return WHITESPACE.splitAsStream(lowerCaseInput)
                .map(token -> PUNCTUATION.matcher(token).replaceAll(""))
                .filter(token -> !STOP_WORDS.contains(token))
                .toList();
    }

}