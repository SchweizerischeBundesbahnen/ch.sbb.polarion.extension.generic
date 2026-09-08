package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.util.AdministrationMenuOrderRestorer.Outcome;
import com.polarion.alm.administration.web.server.AdministrationPageExtender;
import com.polarion.alm.administration.web.server.AdministrationPageExtenderProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministrationMenuOrderRestorerTest {

    /**
     * Stands in for {@code AdministrationPageExtender}, which is what makes the reordering necessary:
     * it implements neither {@code equals} nor {@code hashCode}, so identity is all there is.
     */
    @SuppressWarnings("ClassCanBeRecord")
    private static final class Entry {
        private final String name;

        Entry(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static List<Object> entries(String... names) {
        List<Object> result = new ArrayList<>();
        for (String name : names) {
            result.add(new Entry(name));
        }
        return result;
    }

    private static List<String> names(List<?> entries) {
        return entries.stream().map(String::valueOf).toList();
    }

    private static List<Object> shuffled(List<Object> source, int... indices) {
        List<Object> result = new ArrayList<>();
        for (int index : indices) {
            result.add(source.get(index));
        }
        return result;
    }

    @Test
    void appliesTheDeclaredOrder() {
        List<Object> declared = entries("about", "user-guide", "css", "webhooks");
        List<Object> live = shuffled(declared, 2, 0, 3, 1);

        assertEquals(Outcome.RESTORED, AdministrationMenuOrderRestorer.restore(live, declared));
        assertEquals(List.of("about", "user-guide", "css", "webhooks"), names(live));
    }

    @Test
    void keepsTheSameInstances() {
        List<Object> declared = entries("about", "css");
        List<Object> live = shuffled(declared, 1, 0);

        AdministrationMenuOrderRestorer.restore(live, declared);

        assertSame(declared.get(0), live.get(0));
        assertSame(declared.get(1), live.get(1));
    }

    @Test
    void neverChangesTheSize() {
        List<Object> declared = entries("a", "b", "c");
        List<Object> live = shuffled(declared, 2, 1, 0);

        AdministrationMenuOrderRestorer.restore(live, declared);

        assertEquals(3, live.size());
    }

    /**
     * Every extension bundle runs this, so only the first one may do the work. A later bundle must
     * report ALREADY_ORDERED, which is what keeps one log line per server start instead of one per
     * extension.
     */
    @Test
    void reportsAlreadyOrderedOnASecondCall() {
        List<Object> declared = entries("about", "user-guide", "css");
        List<Object> live = shuffled(declared, 1, 2, 0);

        assertEquals(Outcome.RESTORED, AdministrationMenuOrderRestorer.restore(live, declared));
        List<String> once = names(live);
        assertEquals(Outcome.ALREADY_ORDERED, AdministrationMenuOrderRestorer.restore(live, declared));

        assertEquals(once, names(live));
    }

    @Test
    void leavesAnAlreadyOrderedListUntouched() {
        List<Object> declared = entries("about", "css");
        List<Object> live = new ArrayList<>(declared);

        assertEquals(Outcome.ALREADY_ORDERED, AdministrationMenuOrderRestorer.restore(live, declared));
        assertEquals(names(declared), names(live));
    }

    @Test
    void rejectsListsOfDifferentSize() {
        List<Object> declared = entries("about", "css", "webhooks");
        List<Object> live = shuffled(declared, 1, 0);

        assertEquals(Outcome.MISMATCH, AdministrationMenuOrderRestorer.restore(live, declared));
        assertEquals(List.of("css", "about"), names(live));
    }

    @Test
    void rejectsListsHoldingDifferentEntries() {
        List<Object> declared = entries("about", "css");
        List<Object> live = entries("css", "about");
        Object firstBefore = live.getFirst();

        // Equal-looking but distinct instances: nothing may be replaced.
        assertEquals(Outcome.MISMATCH, AdministrationMenuOrderRestorer.restore(live, declared));
        assertSame(firstBefore, live.getFirst());
        assertEquals(List.of("css", "about"), names(live));
    }

    @Test
    void rejectsARepeatedInstance() {
        List<Object> declared = entries("about", "css");
        List<Object> live = new ArrayList<>(List.of(declared.getFirst(), declared.getFirst()));

        assertEquals(Outcome.MISMATCH, AdministrationMenuOrderRestorer.restore(live, declared));
    }

    @Test
    void handlesEmptyLists() {
        List<Object> live = new ArrayList<>();

        assertEquals(Outcome.ALREADY_ORDERED, AdministrationMenuOrderRestorer.restore(live, List.of()));
        assertTrue(live.isEmpty());
    }

    @Test
    void reordersEntriesOfEveryExtensionAtOnce() {
        // Two extensions plus a Polarion-provided entry, declared in this order.
        List<Object> declared = entries("ext-a/about", "ext-a/css", "ext-b/about", "ext-b/settings", "core/properties");
        List<Object> live = shuffled(declared, 3, 0, 4, 2, 1);

        assertEquals(Outcome.RESTORED, AdministrationMenuOrderRestorer.restore(live, declared));
        assertEquals(names(declared), names(live));
    }

    @Test
    void describesRealEntriesByParentAndId() {
        AdministrationPageExtender extender = new AdministrationPageExtender();
        extender.setId("style-package");
        extender.setParentNodeId("pdf-export");

        assertEquals("pdf-export/style-package", AdministrationMenuOrderRestorer.describe(List.of(extender)));
    }

    @Test
    void describesEntriesInOrder() {
        assertEquals("a, b", AdministrationMenuOrderRestorer.describe(entries("a", "b")));
    }

    /**
     * Pins the one reflective name that can silently break this class against Polarion's real type. If a
     * Polarion upgrade renames or retypes the field, this fails at build time instead of degrading to a
     * WARN on a production server, where the symptom is the randomized menu this class exists to fix.
     */
    @Test
    @SuppressWarnings("java:S3011")
    void providerStillDeclaresTheExtendersListField() throws Exception {
        Field field = AdministrationPageExtenderProvider.class.getDeclaredField("extenders");
        field.setAccessible(true);

        assertInstanceOf(List.class, field.get(new AdministrationPageExtenderProvider()));
    }

    /**
     * The provider must keep taking entries through this setter and keep them in a mutable list, which is
     * what lets the order be corrected in place.
     */
    @Test
    @SuppressWarnings("unchecked")
    void providerKeepsItsEntriesInAMutableList() throws Exception {
        AdministrationPageExtender extender = new AdministrationPageExtender();
        extender.setId("about");
        AdministrationPageExtenderProvider provider = new AdministrationPageExtenderProvider();
        provider.setAdministrationPageExtenders(new LinkedHashSet<>(List.of(extender)));

        Field field = AdministrationPageExtenderProvider.class.getDeclaredField("extenders");
        field.setAccessible(true);
        List<Object> entries = (List<Object>) field.get(provider);

        assertEquals(1, entries.size());
        assertSame(extender, entries.get(0));
        assertDoesNotThrow(() -> entries.set(0, extender), "the entry list must be writable in place");
    }
}
