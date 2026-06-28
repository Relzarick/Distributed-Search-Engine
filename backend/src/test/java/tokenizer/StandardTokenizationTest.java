package tokenizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


class StandardTokenizationTest {
    @Test
    void basicSentenceTest() {
        var tokenizer = new StandardTokenization();

        assertInstanceOf(List.class, tokenizer.toTokens("This, is a test!"));
        assertEquals(List.of("test"), tokenizer.toTokens("This, is a test!"));

    }

    @Test
    void InputEmptyTest() {
        var tokenizer = new StandardTokenization();
        assertEquals(List.of(), tokenizer.toTokens(""));
        assertEquals(List.of(), tokenizer.toTokens(null));
    }

}