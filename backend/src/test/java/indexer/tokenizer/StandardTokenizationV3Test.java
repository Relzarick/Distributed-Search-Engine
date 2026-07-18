package indexer.tokenizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StandardTokenizationV3Test {
    private TokenStrategy tokenizer;

    @BeforeEach
    void setup() {
        tokenizer = new StandardTokenizationV3();
    }

    @Test
    void normalSentenceTest() {
        assertInstanceOf(List.class, tokenizer.toTokens("This, is a test!"));
        assertEquals(List.of("test"), tokenizer.toTokens("This, is a test!"));
    }

    @Test
    void weirdSentenceTest() {
        assertEquals(List.of("test"), tokenizer.toTokens("This+, is the 3rd test!"));
        assertEquals(List.of("test"), tokenizer.toTokens("...test !!!"));
        assertNull(tokenizer.toTokens("don't"));
    }

    @Test
    void edgeCaseTest() {
        assertEquals(List.of("test"), tokenizer.toTokens("test   "));
        assertEquals(List.of("test"), tokenizer.toTokens("{_test'"));
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