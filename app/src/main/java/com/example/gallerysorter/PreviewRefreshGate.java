package com.example.gallerysorter;

/** Successful, stable scans only. Unknown state always takes the normal scan path. */
final class PreviewRefreshGate {
    interface Environment {
        State readState() throws Exception;
        String snapshotRevision();
        String readCheckpoint();
        void writeCheckpoint(String value);
    }

    static final class State {
        final String sourceKey;
        final int itemCount;

        State(String sourceKey, int itemCount) {
            this.sourceKey = sourceKey;
            this.itemCount = itemCount;
        }

        String key() { return sourceKey + "|count=" + itemCount; }
    }

    private final Environment environment;
    private State pending;

    PreviewRefreshGate(Environment environment) { this.environment = environment; }

    boolean begin() {
        pending = null;
        try {
            State current = environment.readState();
            if (matchesCheckpoint(current)) {
                return true;
            }
            environment.writeCheckpoint("");
            pending = current;
        } catch (Exception ignored) {
            // Provider/permission failures must never become an unchanged result.
        }
        return false;
    }

    void complete(boolean snapshotSaved, int analyzedCount) {
        State started = pending;
        pending = null;
        if (!snapshotSaved || started == null || analyzedCount != started.itemCount) return;
        try {
            State finished = environment.readState();
            String revision = environment.snapshotRevision();
            if (finished != null && started.key().equals(finished.key()) && !revision.isEmpty()) {
                environment.writeCheckpoint(checkpoint(finished, revision));
            }
        } catch (Exception ignored) {
            // A cancelled, partial or concurrently changed scan establishes no baseline.
        }
    }

    boolean matchesCheckpoint() throws Exception {
        return matchesCheckpoint(environment.readState());
    }

    private boolean matchesCheckpoint(State current) {
        String revision = environment.snapshotRevision();
        return current != null && !revision.isEmpty()
                && checkpoint(current, revision).equals(environment.readCheckpoint());
    }

    private static String checkpoint(State state, String revision) {
        return "preview-refresh-v1|" + state.key() + "|snapshot=" + revision;
    }
}
