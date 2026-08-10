package dev.frostguard.app;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildMetadataTest {
    @Test
    void readsFilteredPrBuildIdentity() {
        BuildMetadata release = BuildMetadata.read(stream(
                "pullRequestBuild=false\nauthenticodePublisher=CN=Frostguard Project, O=Frostguard"));
        assertFalse(release.pullRequestBuild());
        assertEquals("CN=Frostguard Project, O=Frostguard", release.authenticodePublisher());
        assertTrue(BuildMetadata.read(stream("pullRequestBuild=true")).pullRequestBuild());
    }

    @Test
    void missingOrInvalidIdentityDisablesAutomaticUpdates() {
        assertTrue(BuildMetadata.read(null).pullRequestBuild());
        assertTrue(BuildMetadata.read(stream("pullRequestBuild=maybe")).pullRequestBuild());
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
