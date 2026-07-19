package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Compat glue that resolves each accessory provider's slot-type string and hands it to
 * {@link AhRenderManagementApi#shouldHideAccessory} — the provider-agnostic hide decision. Providers
 * expose no per-render alpha, so accessories are only ever hidden, not faded.
 * <p>
 * Each provider names its slot type differently:
 * Curios uses flat identifiers ({@code head}, {@code necklace}, {@code belt}, {@code feet})<br/>
 * Trinkets groups body regions ({@code head}, {@code chest}, {@code legs}, {@code feet})<br/>
 * Accessories uses data-driven, sometimes namespaced slot names ({@code hat}, {@code necklace}, {@code belt}, {@code shoes}, …)<br/>
 * {@link AhRenderManagementApi#mapAccessoryTypeToSlot} accepts all of them.
 * <p>
 * Provider slot values are read through {@link ReflectiveChain} (and the Accessories helper below), which
 * resolves and caches the reflective accessors once per concrete class — the per-frame render path only
 * pays a cached {@link Method#invoke}, never a fresh {@code getMethod} lookup.
 */
public final class AccessoryHidingCompat {

    private AccessoryHidingCompat() {
    }

    /** Curios {@code SlotContext.identifier()} → slot-type string. */
    private static final ReflectiveChain CURIOS_IDENTIFIER = new ReflectiveChain("identifier");

    /** Trinkets ({@code dev.emi.trinkets}) {@code SlotReference.inventory().getSlotType().getGroup()} → body region. */
    private static final ReflectiveChain TRINKETS_GROUP = new ReflectiveChain("inventory", "getSlotType", "getGroup");

    /** Accessories ({@code io.wispforest.accessories}) {@code SlotPath.slotName()} → slot name. */
    private static final ReflectiveChain ACCESSORIES_SLOT_NAME = new ReflectiveChain("slotName");

    /**
     * Curios entry point: resolves {@code SlotContext.identifier()} through the cached reflective chain
     * (Curios is an optional dependency not on the compile classpath) and delegates to
     * {@link AhRenderManagementApi#shouldHideAccessory}.
     */
    public static boolean shouldHideCurio(@Nullable Object slotContext, @Nullable Object carrier) {
        return AhRenderManagementApi.shouldHideAccessory(CURIOS_IDENTIFIER.resolve(slotContext), carrier);
    }

    /**
     * Trinkets ({@code dev.emi.trinkets}, the Trinkets/Trinkets-Canary mod) entry point: the slot's body
     * region is {@code slotReference.inventory().getSlotType().getGroup()} (a String like {@code head} /
     * {@code chest} / {@code legs} / {@code feet}), resolved through the cached reflective chain as
     * Trinkets is an optional dependency not on the compile classpath.
     */
    public static boolean shouldHideTrinket(@Nullable Object slotReference, @Nullable Object carrier) {
        return AhRenderManagementApi.shouldHideAccessory(TRINKETS_GROUP.resolve(slotReference), carrier);
    }

    /**
     * Accessories ({@code io.wispforest.accessories}) entry point: the accessory's slot name is
     * {@code accessoryState.getStateData(AccessoriesRenderStateKeys.SLOT_PATH).slotName()}. The
     * {@code SLOT_PATH} context key and the {@code getStateData} accessor are resolved and cached once
     * (Accessories is an optional dependency not on the compile classpath), then reused per frame.
     */
    public static boolean shouldHideAccessoriesAccessory(@Nullable Object accessoryState, @Nullable Object carrier) {
        return AhRenderManagementApi.shouldHideAccessory(ACCESSORIES_SLOT_NAME.resolve(accessoriesSlotPath(accessoryState)), carrier);
    }

    // --- Accessories SLOT_PATH lookup (context-key + getStateData), resolved once and cached ---

    private static volatile boolean accessoriesResolved;
    @Nullable
    private static volatile Object accessoriesSlotPathKey;
    @Nullable
    private static volatile Method accessoriesGetStateData;

    @Nullable
    private static Object accessoriesSlotPath(@Nullable Object accessoryState) {
        if (accessoryState == null) {
            return null;
        }
        if (!accessoriesResolved) {
            resolveAccessoriesAccessors(accessoryState.getClass().getClassLoader());
        }
        Object key = accessoriesSlotPathKey;
        Method getStateData = accessoriesGetStateData;
        if (key == null || getStateData == null) {
            return null;
        }
        try {
            return getStateData.invoke(accessoryState, key);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static synchronized void resolveAccessoriesAccessors(ClassLoader classLoader) {
        if (accessoriesResolved) {
            return;
        }
        try {
            Class<?> keys = Class.forName(
                    "io.wispforest.accessories.api.client.AccessoriesRenderStateKeys", false, classLoader);
            accessoriesSlotPathKey = keys.getField("SLOT_PATH").get(null);

            Class<?> stateClass = Class.forName(
                    "io.wispforest.accessories.api.client.AccessoryRenderState", false, classLoader);
            // Match by name + arity so we never need the (remapped) ContextKey parameter type.
            for (Method method : stateClass.getMethods()) {
                if (method.getName().equals("getStateData") && method.getParameterCount() == 1) {
                    method.setAccessible(true);
                    accessoriesGetStateData = method;
                    break;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // Leave the accessors null → shouldHideAccessoriesAccessory becomes a no-op (accessory renders).
        } finally {
            accessoriesResolved = true;
        }
    }
}
