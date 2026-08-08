package de.zannagh.armorhider.smoke;

import java.util.List;

/**
 * Shared reading of the {@code -Dsmoke.only} / {@code -Dsmoke.exclude} variant filters, so every
 * phase honours the same spelling of the same two properties.
 */
final class VariantFilter {

    private VariantFilter() {
    }

    /** Variants explicitly requested, or an empty list meaning "no restriction". */
    static List<String> only() {
        return split(System.getProperty("smoke.only", ""));
    }

    /** Variants explicitly skipped. */
    static List<String> exclude() {
        return split(System.getProperty("smoke.exclude", ""));
    }

    private static List<String> split(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return List.of(trimmed.split(",")).stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }
}
