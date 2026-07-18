package indexer.tokenizer;

import java.util.List;

public class Tokenizer {
    private final TokenStrategy strat;

    public Tokenizer(TokenStrategy strat) {
        this.strat = strat;
    }

    public List<String> tokenize(String inputs) {
        return strat.toTokens(inputs);
    }

}