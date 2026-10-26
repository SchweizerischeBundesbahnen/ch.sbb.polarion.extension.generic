package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class BundleCacheKeyTest {

    // Three builds of one module: the first, a rebuild that changed nothing, and one that changed a chunk it
    // imports, which renames the chunk in the entry
    private static final String FIRST = "bundle-cache-key/first";
    private static final String REBUILT = "bundle-cache-key/rebuilt";
    private static final String CHANGED = "bundle-cache-key/changed";

    /** The jar, with the content of each module in memory. */
    private static final ClassLoader BUNDLE = new ClassLoader(null) {
        private final Map<String, String> modules = Map.of(
                FIRST, "import('./shared-AAAA');",
                REBUILT, "import('./shared-AAAA');",
                CHANGED, "import('./shared-BBBB');");

        @Override
        public InputStream getResourceAsStream(String name) {
            String module = modules.get(name);
            return module == null ? null : new ByteArrayInputStream(module.getBytes(StandardCharsets.UTF_8));
        }
    };

    private static String keyFor(Version version, String modulePath) {
        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(VersionUtils::getVersion).thenReturn(version);
            return BundleCacheKey.forModule(modulePath, BUNDLE);
        }
    }

    private static String keyFor(String modulePath) {
        return keyFor(Version.builder().bundleVersion("13.7.1").build(), modulePath);
    }

    @Test
    void isTheVersionFollowedByAHashOfTheModule() {
        assertTrue(keyFor(FIRST).matches("13\\.7\\.1-[0-9a-f]{12}"), keyFor(FIRST));
    }

    @Test
    void changesWithTheModuleWithinOneVersion() {
        // Two builds of one SNAPSHOT, or a rebuilt release: the version alone would give both the same URL, and
        // the build timestamp only changes by the minute
        assertNotEquals(keyFor(FIRST), keyFor(CHANGED));
    }

    @Test
    void staysTheSameForARebuildThatChangedNothing() {
        // The browser may keep its copy: it is the same module
        assertEquals(keyFor(FIRST), keyFor(REBUILT));
    }

    @Test
    void isTheVersionAloneWhereTheModuleIsNotInTheJar() {
        assertEquals("13.7.1", keyFor("bundle-cache-key/no-such-module"));
    }

    @Test
    void fallsBackToZeroWhereTheManifestCarriesNoVersion() {
        // A unit test, or a deployment that lost its metadata. The key must still be something: left out, the
        // placeholder would be requested literally
        assertEquals("0", keyFor(Version.builder().build(), "bundle-cache-key/no-such-module"));
        assertTrue(keyFor(Version.builder().build(), FIRST).matches("0-[0-9a-f]{12}"));
    }
}
