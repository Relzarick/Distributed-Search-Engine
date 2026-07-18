package indexer.tokenizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardTokenizationV3Test {
    private TokenStrategy tokenizer;

    @BeforeEach
    void setup() {
        tokenizer = new StandardTokenizationV3();
    }

    private Set<String> getTokens(String input) {
        Set<String> tokens = new HashSet<>();
        tokenizer.toTokens(input, tokens);

        return tokens;
    }

    @Test
    void normalSentenceTest() {
        assertEquals(Set.of("test"), getTokens("This, is a test!"));
    }

    @Test
    void weirdSentenceTest() {
        assertEquals(Set.of("test"), getTokens("This+, is the 3rd test!"));
        assertEquals(Set.of("test"), getTokens("...test !!!"));

        assertTrue(getTokens("don't").isEmpty());
    }

    @Test
    void edgeCaseTest() {
        assertEquals(Set.of("test"), getTokens("test   "));
        assertEquals(Set.of("test"), getTokens("{_test'"));
    }

    @Test
    void numberTest() {
        assertTrue(getTokens("9").isEmpty());
        assertTrue(getTokens("-9").isEmpty());
        assertTrue(getTokens("+9").isEmpty());
    }

    @Test
    void InputEmptyTest() {
        assertTrue(getTokens("").isEmpty());
    }

}