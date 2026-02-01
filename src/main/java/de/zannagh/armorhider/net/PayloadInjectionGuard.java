//? if >= 1.20.5 {
package de.zannagh.armorhider.net;

/**
 * Guard against re-entrancy when other mods (like Carpet) also mixin to the same codec method.
 * This prevents infinite recursion when their mixins call back into the codec method.
 */
public final class PayloadInjectionGuard {
    private static final ThreadLocal<Boolean> IS_INJECTING = ThreadLocal.withInitial(() -> false);

    private PayloadInjectionGuard() {}

    public static boolean isInjecting() {
        return IS_INJECTING.get();
    }

    public static void setInjecting(boolean value) {
        IS_INJECTING.set(value);
    }
}
//?}
