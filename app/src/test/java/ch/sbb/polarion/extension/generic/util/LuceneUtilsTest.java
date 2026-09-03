package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LuceneUtilsTest {

    @Test
    void testValueWithoutSpecialCharactersStaysAsItIs() {
        assertEquals("T123", LuceneUtils.escapeValue("T123"));
        assertEquals("", LuceneUtils.escapeValue(""));
    }

    @Test
    void testSpaceTurnsTheValueIntoAPhrase() {
        assertEquals("\"ABC 123\"", LuceneUtils.escapeValue("ABC 123"));
    }

    @Test
    void testMetaCharactersAreEscaped() {
        assertEquals("T\\-123", LuceneUtils.escapeValue("T-123"));
        assertEquals("\\+A", LuceneUtils.escapeValue("+A"));
        assertEquals("A\\(B\\)", LuceneUtils.escapeValue("A(B)"));
        assertEquals("A\\{B\\}", LuceneUtils.escapeValue("A{B}"));
        assertEquals("A\\[B\\]", LuceneUtils.escapeValue("A[B]"));
        assertEquals("A\\^B", LuceneUtils.escapeValue("A^B"));
        assertEquals("A\\~B", LuceneUtils.escapeValue("A~B"));
        assertEquals("A\\!B", LuceneUtils.escapeValue("A!B"));
        assertEquals("a\\:b", LuceneUtils.escapeValue("a:b"));
        assertEquals("a\\\"b", LuceneUtils.escapeValue("a\"b"));
    }

    @Test
    void testBooleanOperatorsAreEscaped() {
        assertEquals("a\\&&b", LuceneUtils.escapeValue("a&&b"));
        assertEquals("a\\||b", LuceneUtils.escapeValue("a||b"));
    }

    @Test
    void testBackslashIsEscapedBeforeTheEscapesAdded() {
        // A single backslash comes out doubled. Escaping it last would also double the backslashes this
        // method introduced for the other characters.
        assertEquals("a\\\\b", LuceneUtils.escapeValue("a\\b"));
        assertEquals("a\\\\\\-b", LuceneUtils.escapeValue("a\\-b"));
    }

    @Test
    void testWildcardsAreEscaped() {
        assertEquals("AB\\*", LuceneUtils.escapeValue("AB*"));
        assertEquals("A\\?B", LuceneUtils.escapeValue("A?B"));
    }

    @Test
    void testMetaCharactersInsideAPhrase() {
        assertEquals("\"\\+A \\-B\"", LuceneUtils.escapeValue("+A -B"));
    }

    @Test
    void testTerm() {
        assertEquals("title:\"a title\"", LuceneUtils.term("title", "a title"));
        assertEquals("id:T\\-1", LuceneUtils.term("id", "T-1"));
    }

    @Test
    void testProjectTerm() {
        assertEquals("project.id:elibrary", LuceneUtils.projectTerm("elibrary"));
        // The project id reaches this from a request in several places, so it is escaped like any value.
        assertEquals("project.id:\"x\\\" OR project.id\\:other\"", LuceneUtils.projectTerm("x\" OR project.id:other"));
    }

    @Test
    void testAnyOf() {
        assertEquals("id:T\\-1", LuceneUtils.anyOf("id", List.of("T-1")));
        // Not parenthesized here: and(...) brackets every part it combines.
        assertEquals("id:T\\-1 OR id:T\\-2", LuceneUtils.anyOf("id", List.of("T-1", "T-2")));
    }

    @Test
    void testAnyOfWithoutUsableValues() {
        assertNull(LuceneUtils.anyOf("id", null));
        assertNull(LuceneUtils.anyOf("id", List.of()));
        assertNull(LuceneUtils.anyOf("id", Arrays.asList(null, "", "  ")));
    }

    @Test
    void testAnyOfSkipsUnusableValues() {
        assertEquals("id:T\\-1", LuceneUtils.anyOf("id", Arrays.asList(null, "T-1", "")));
    }

    @Test
    void testAnd() {
        assertEquals("project.id:elibrary", LuceneUtils.and("project.id:elibrary"));
        assertEquals("(project.id:elibrary) AND (type:testcase)", LuceneUtils.and("project.id:elibrary", "type:testcase"));
    }

    @Test
    void testAndParenthesizesEveryPart() {
        // The second part is a free-form query the caller did not build. Combined unparenthesized it would
        // read as "project.id:elibrary AND a OR b", which is no longer restricted to the project.
        assertEquals("(project.id:elibrary) AND (a OR b)", LuceneUtils.and("project.id:elibrary", "a OR b"));
    }

    @Test
    void testAndWithoutUsableParts() {
        assertEquals("", LuceneUtils.and());
        assertEquals("", LuceneUtils.and((String[]) null));
        assertEquals("", LuceneUtils.and(null, "", "  "));
    }

    @Test
    void testAndSkipsUnusableParts() {
        assertEquals("project.id:elibrary", LuceneUtils.and(null, "project.id:elibrary", ""));
    }

    @Test
    void testAnyOfCombinesWithAnd() {
        String query = LuceneUtils.and(LuceneUtils.projectTerm("elibrary"), LuceneUtils.anyOf("customField", Set.of("A B")));
        assertEquals("(project.id:elibrary) AND (customField:\"A B\")", query);
    }

    @Test
    void testSeveralValuesCombineWithAndWithoutDoubleBrackets() {
        String query = LuceneUtils.and(LuceneUtils.projectTerm("elibrary"), LuceneUtils.anyOf("id", List.of("T-1", "T-2")));
        assertEquals("(project.id:elibrary) AND (id:T\\-1 OR id:T\\-2)", query);
    }
}
