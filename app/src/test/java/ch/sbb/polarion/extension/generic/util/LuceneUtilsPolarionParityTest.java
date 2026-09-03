package ch.sbb.polarion.extension.generic.util;

import com.polarion.alm.shared.tracker.query.LuceneQueryPart;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins {@link LuceneUtils#escapeValue(String)} to Polarion's own escaping instead of leaving the character
 * list to be reviewed by eye. A Polarion upgrade that changes the rules fails here.
 * <p>
 * The two wildcards are left out of the comparison on purpose: Polarion protects them from escaping so that
 * a wildcard typed into its query builder survives, which is the one place this class deliberately differs.
 * {@link LuceneUtilsTest} covers that difference.
 */
class LuceneUtilsPolarionParityTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "T123",
            "",
            "T-123",
            "+A",
            "A(B)",
            "A{B}",
            "A[B]",
            "A^B",
            "A~B",
            "A!B",
            "a:b",
            "a\"b",
            "a&&b",
            "a||b",
            "a\\b",
            "ABC 123",
            "+A -B",
            "a\\-b"
    })
    void testEscapingMatchesPolarion(String value) {
        assertEquals(LuceneQueryPart.escape(value), LuceneUtils.escapeValue(value));
    }
}
