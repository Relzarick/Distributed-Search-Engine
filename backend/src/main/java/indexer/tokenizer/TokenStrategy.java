package indexer.tokenizer;

import java.util.List;
import java.util.Set;

/**
 * All tokenizers should implement this interface
 */
public interface TokenStrategy {
    void toTokens(String input, Set<String> list);

    List<String> toTokens(String input);
}