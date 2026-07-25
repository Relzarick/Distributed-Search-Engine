package indexer.tokenizer;

import org.tartarus.snowball.ext.englishStemmer;

import java.util.Set;

public class StemTokenization extends BaseTokenization implements TokenStrategy {
    private static final ThreadLocal<englishStemmer> STEMMER = ThreadLocal.withInitial(englishStemmer::new);

    @Override
    public void toTokens(String input, Set<String> list) {
        if (input == null || input.isBlank())
            return;

        englishStemmer stemmer = STEMMER.get();

        char[] chars = input.toLowerCase().toCharArray();
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

            if (isDigit)
                continue;

            int end = i;

            while (start < end && isNotValid(chars[start]))
                start++;

            while (end > start && isNotValid(chars[end - 1]))
                end--;

            if (start < end) {
                String token = new String(chars, start, end - start);

                if (!STOP_WORDS.contains(token)) {
                    stemmer.setCurrent(token);
                    stemmer.stem();
                    list.add(stemmer.getCurrent());
                }
            }
        }
    }

    private boolean isNotValid(char c) {
        return (c < 'a' || c > 'z');
    }

}