package ch.sbb.polarion.extension.generic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ScopeUtilsTest {

    private static Stream<Arguments> testValuesForGetProjectByScope() {
        return Stream.of(
                Arguments.of("", null),
                Arguments.of("project/elibrary/", "elibrary"),
                Arguments.of("projects/test/", null),
                Arguments.of("invalid", null)
        );
    }

    private static Stream<Arguments> testValuesForGetScopeFromProject() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", ""),
                Arguments.of("     ", ""),
                Arguments.of("elibrary", "project/elibrary/")
                );
    }

    @ParameterizedTest
    @MethodSource("testValuesForGetProjectByScope")
    void getProjectByScope(String scope, String expected) {
        String projectFromScope = ScopeUtils.getProjectFromScope(scope);
        assertEquals(expected, projectFromScope);
    }

    @ParameterizedTest
    @MethodSource("testValuesForGetScopeFromProject")
    void getScopeFromProject(String scope, String expected) {
        assertEquals(expected, ScopeUtils.getScopeFromProject(scope));
    }
}