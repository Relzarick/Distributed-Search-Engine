package indexer.tokenizer;

import java.util.List;

/**
 * All tokenizers should implement this interface
 */
public interface TokenStrategy {
    List<String> toTokens(String input);
}
