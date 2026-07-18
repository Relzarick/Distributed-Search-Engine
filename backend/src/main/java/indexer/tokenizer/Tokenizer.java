package indexer.tokenizer;

import java.util.Set;

public class Tokenizer {
    private final TokenStrategy strat;

    public Tokenizer(TokenStrategy strat) {
        this.strat = strat;
    }

    public void tokenizeInto(String inputs, Set<String> list) {
        strat.toTokens(inputs, list);
    }

}