package indexer.tokenizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StandardTokenizationV2Test {
    private TokenStrategy tokenizer;

    @BeforeEach
    void setup() {
        tokenizer = new StandardTokenizationV2();
    }

    @Test
    void basicSentenceTest() {
        assertInstanceOf(List.class, tokenizer.toTokens("This, is a test!"));
        assertEquals(List.of("test"), tokenizer.toTokens("This, is a test!"));
    }

    @Test
    void numberTest() {
        assertNull(tokenizer.toTokens("9"));
        assertNull(tokenizer.toTokens("-9"));
        assertNull(tokenizer.toTokens("+9"));
    }

    @Test
    void InputEmptyTest() {
        assertNull(tokenizer.toTokens(""));
        assertNull(tokenizer.toTokens(null));
    }

}