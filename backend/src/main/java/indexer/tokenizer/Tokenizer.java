package indexer.tokenizer;

import java.util.Set;

public class Tokenizer {
    private final TokenStrategy strat;

    public Tokenizer(TokenStrategy strat) {
        this.strat = strat;
    }

    /**
     * Tokenizes the String and adds unique words into the list.
     *
     * @param inputs Is the string that will be tokenized
     * @param list   Is the list that the tokens will be added to
     */
    public void tokenizeInto(String inputs, Set<String> list) {
        strat.toTokens(inputs, list);
    }

}