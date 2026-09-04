package ch.sbb.polarion.extension.generic.util;

import com.polarion.alm.shared.tracker.query.LuceneQueryPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pins {@link LuceneUtils#escapeValue(String)} to Polarion's own escaping instead of leaving the character
 * list to be reviewed by eye. A Polarion upgrade that changes the rules fails here.
 * <p>
 * The character range is generated rather than listed, so the test also covers the direction a hand-written
 * list cannot: a character Polarion escapes and this class does not. Both intentional divergences are pinned
 * by their own tests below, so neither can be introduced silently and neither reads as an omission.
 */
class LuceneUtilsPolarionParityTest {

    private static final char WILDCARD_ANY = '*';
    private static final char WILDCARD_ONE = '?';

    @ParameterizedTest
    @MethodSource("everyPrintableCharacter")
    void testEscapingMatchesPolarionForEveryPrintableCharacter(String value) {
        assertEquals(LuceneQueryPart.escape(value), LuceneUtils.escapeValue(value));
    }

    static Stream<String> everyPrintableCharacter() {
        // The wildcards are a documented divergence and have their own test.
        return IntStream.rangeClosed(0x20, 0x7E)
                .filter(character -> character != WILDCARD_ANY && character != WILDCARD_ONE)
                .mapToObj(character -> "a" + (char) character + "b");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "T123",
            "a\\b",
            "a\\-b",
            "a&&b",
            "a||b",
            "ABC 123",
            "+A -B"
    })
    void testEscapingMatchesPolarionForValuesTheRangeCannotExpress(String value) {
        assertEquals(LuceneQueryPart.escape(value), LuceneUtils.escapeValue(value));
    }

    @Test
    void testWildcardsAreTheFirstIntentionalDivergence() {
        // Polarion protects these so a wildcard typed into its query builder survives. A value arriving from
        // a request or an uploaded file has to match literally, so this class escapes them.
        assertEquals("a*b", LuceneQueryPart.escape("a*b"));
        assertEquals("a\\*b", LuceneUtils.escapeValue("a*b"));
        assertEquals("a?b", LuceneQueryPart.escape("a?b"));
        assertEquals("a\\?b", LuceneUtils.escapeValue("a?b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a\tb", "a\nb", "a\rb", "a　b"})
    void testNonSpaceWhitespaceIsTheSecondIntentionalDivergence(String value) {
        // Polarion quotes on the plain space alone, which leaves the other characters its parser skips free to
        // end the term. This class quotes on all of them, so the two disagree here by design.
        assertNotEquals(LuceneQueryPart.escape(value), LuceneUtils.escapeValue(value));
        assertEquals("\"" + value + "\"", LuceneUtils.escapeValue(value));
    }

    @Test
    void testTheSpaceItselfStillAgreesWithPolarion() {
        assertEquals(LuceneQueryPart.escape("a b"), LuceneUtils.escapeValue("a b"));
        assertEquals("\"a b\"", LuceneUtils.escapeValue("a b"));
    }
}
