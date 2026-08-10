package dev.frostguard.update;

import dev.frostguard.api.runtime.RuntimeChannel;

public record RunningBuild(
        SemanticVersion version,
        SemanticVersion updaterVersion,
        RuntimeChannel channel,
        UpdatePlatform platform,
        boolean pullRequestBuild,
        String authenticodePublisher) {

    public RunningBuild {
        if (version == null || updaterVersion == null || channel == null || platform == null) {
            throw new IllegalArgumentException("Running build identity is incomplete");
        }
        authenticodePublisher = authenticodePublisher == null ? "" : authenticodePublisher.trim();
    }

    public boolean permitsAutomaticUpdates() {
        return channel != RuntimeChannel.DEVELOPMENT && !pullRequestBuild && !authenticodePublisher.isBlank();
    }
}
