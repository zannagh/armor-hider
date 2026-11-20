/** What [FetchCompatJars] does with a pinned version whose game versions do not include the target MC. */
internal enum class PinAutoCorrect {
    /** Fetch the pin anyway: it is itself the snapshot-fallback build of an MC-agnostic dep. */
    KEEP,

    /** Fetch the auto-resolved version instead of the pin. */
    REPLACE,

    /** Drop the pin to avoid an incompatible-mod boot failure. */
    SKIP;

    companion object {
        /**
         * Decides from the pinned [hash] and the version [resolved] for its project (null if none).
         * A pin that equals the snapshot-fallback build (e.g. a dep pinning the exact
         * fabric-language-kotlin build the fallback picks) only becomes recognisable as a fallback
         * after the lookup, so it must be kept rather than dropped as "resolved to itself".
         */
        fun decide(hash: String, resolved: String?, isSnapshotFallback: (String) -> Boolean): PinAutoCorrect = when {
            resolved == null -> SKIP
            resolved != hash -> REPLACE
            isSnapshotFallback(resolved) -> KEEP
            else -> SKIP
        }
    }
}
