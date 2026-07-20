package de.zannagh.armorhider.api.compat;

/**
 * The outcome of a {@link CompatInitializer#init()} call: whether it succeeded and a human-readable message.
 */
public record CompatInitializationResult(boolean success, String message) {

    private static final String MISSING_MESSAGE = "Compat requires initialization but it's missing.";

    public static final CompatInitializationResult SUCCESS =
            new CompatInitializationResult(true, "Compat initialization succeeded");

    public static final CompatInitializationResult MISSING =
            new CompatInitializationResult(false, MISSING_MESSAGE);

    public static final CompatInitializationResult FAILURE =
            new CompatInitializationResult(false, "Compat initialization failed");

    public static CompatInitializationResult failure(String message) {
        return new CompatInitializationResult(false, message);
    }

    public boolean isMissingInitializerResult() {
        return message.equals(MISSING_MESSAGE);
    }
}
