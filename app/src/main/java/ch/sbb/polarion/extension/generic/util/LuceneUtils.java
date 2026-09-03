package ch.sbb.polarion.extension.generic.util;

import com.polarion.alm.projects.model.IUniqueObject;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Assembles Lucene queries for Polarion's search APIs.
 * <p>
 * Every value placed into a query goes through {@link #escapeValue(String)}. An unescaped value fails in
 * one of three ways: it silently stops matching what the caller meant, it makes the query unparseable, or,
 * when it carries a wildcard, it matches a whole family of entities instead of one.
 */
@UtilityClass
public class LuceneUtils {

    public static final String PROJECT_ID_FIELD = IUniqueObject.KEY_PROJECT + ".id";

    /**
     * The characters Polarion escapes in {@code com.polarion.alm.shared.tracker.query.LuceneQueryPart.escape},
     * plus the two wildcards. Polarion leaves {@code *} and {@code ?} alone so that a user can type a wildcard
     * on purpose in its query builder; a value arriving from a request or an uploaded file has to match
     * literally, so both are escaped here.
     * <p>
     * The backslash comes first: it has to be doubled before the escapes added below introduce their own.
     */
    private static final String CHARACTERS_TO_ESCAPE = "\\+-!(){}[]^\"~:*?";

    /**
     * Escapes a value for use as a term in a Lucene query. A value containing a space becomes a quoted
     * phrase, which is how Polarion itself passes such a value to the index.
     *
     * @param value the value to escape
     * @return the escaped value, ready to be concatenated after a field name and a colon
     */
    public static @NotNull String escapeValue(@NotNull String value) {
        String escaped = value;
        for (char character : CHARACTERS_TO_ESCAPE.toCharArray()) {
            escaped = escaped.replace(String.valueOf(character), "\\" + character);
        }
        escaped = escaped.replace("&&", "\\&&").replace("||", "\\||");
        return escaped.contains(" ") ? "\"" + escaped + "\"" : escaped;
    }

    /**
     * A single {@code field:value} term with the value escaped. The field is a field name rather than a
     * value, so it is used as it is.
     *
     * @param field the field name
     * @param value the value to match
     * @return the term
     */
    public static @NotNull String term(@NotNull String field, @NotNull String value) {
        return field + ":" + escapeValue(value);
    }

    /**
     * The {@code project.id} term restricting a query to one project. Several Polarion search methods are
     * repository wide and are restricted this way rather than by their arguments.
     *
     * @param projectId the project to restrict to
     * @return the term
     */
    public static @NotNull String projectTerm(@NotNull String projectId) {
        return term(PROJECT_ID_FIELD, projectId);
    }

    /**
     * One field matched against any of several values, each escaped. Null and blank values are left out.
     * <p>
     * The terms are joined but not parenthesized, because {@link #and(String...)} parenthesizes every part
     * it combines: grouping here as well would only produce a second pair of brackets.
     *
     * @param field  the field name
     * @param values the values to match, any of which may be null
     * @return the alternative, or null when no usable value was given
     */
    public static @Nullable String anyOf(@NotNull String field, @Nullable Collection<String> values) {
        if (values == null) {
            return null;
        }
        List<String> terms = values.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .map(value -> term(field, value))
                .toList();
        return terms.isEmpty() ? null : String.join(" OR ", terms);
    }

    /**
     * Combines query parts into a conjunction, leaving out the parts that are null or blank. Each part is
     * parenthesized whenever there is more than one, because a part may be a free-form query the caller did
     * not build: {@code a OR b} combined unparenthesized would no longer be a restriction.
     *
     * @param parts the parts to combine, any of which may be null
     * @return the combined query, or an empty string when no usable part was given
     */
    public static @NotNull String and(@Nullable String... parts) {
        if (parts == null) {
            return "";
        }
        List<String> usableParts = Arrays.stream(parts)
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .toList();
        if (usableParts.isEmpty()) {
            return "";
        }
        if (usableParts.size() == 1) {
            return usableParts.get(0);
        }
        return usableParts.stream().collect(Collectors.joining(") AND (", "(", ")"));
    }
}
