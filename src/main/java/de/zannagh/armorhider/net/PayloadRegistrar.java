package de.zannagh.armorhider.net;

/**
 * Payload registration utility.
 * Now uses the custom PayloadRegistry instead of Fabric API's PayloadTypeRegistry.
 */
public final class PayloadRegistrar {

    /**
     * Register all custom payloads.
     * This must be called early during mod initialization,
     * before the codec system is used.
     */
    public static void registerPayloads() {
        PayloadRegistry.init();
    }
}
