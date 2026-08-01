package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Compat glue that resolves each accessory provider's slot-type string and hands it to
 * {@link AhRenderManagementApi#shouldHideAccessory} - the provider-agnostic hide decision. Providers
 * expose no per-render alpha, so accessories are only ever hidden, not faded.
 * <p>
 * Each provider names its slot type differently:
 * Curios uses flat identifiers ({@code head}, {@code necklace}, {@code belt}, {@code feet})<br/>
 * Trinkets groups body regions ({@code head}, {@code chest}, {@code legs}, {@code feet})<br/>
 * Accessories uses data-driven, sometimes namespaced slot names ({@code hat}, {@code necklace}, {@code belt}, {@code shoes}, …)<br/>
 * {@link AhRenderManagementApi#mapAccessoryTypeToSlot} accepts all of them.
 * <p>
 * Provider slot values are read through {@link ReflectiveChain} (and the Accessories helper below), which
 * resolves and caches the reflective accessors once per concrete class - the per-frame render path only
 * pays a cached {@link Method#invoke}, never a fresh {@code getMethod} lookup.
 */
public final class AccessoryHidingCompat {

    private AccessoryHidingCompat() {
    }

    /** Curios {@code SlotContext.identifier()} → slot-type string. */
    private static final ReflectiveChain CURIOS_IDENTIFIER = new ReflectiveChain("identifier");

    /** Curios {@code SlotContext.entity()} → the wearer. Used on the pre-render-state (1.21.1) path,
     *  where {@code ICurioRenderer.render} is handed no entity and the wearer lives on the SlotContext. */
    private static final ReflectiveChain CURIOS_ENTITY = new ReflectiveChain("entity");

    /** Trinkets ({@code dev.emi.trinkets}) {@code SlotReference.inventory().getSlotType().getGroup()} → body region. */
    private static final ReflectiveChain TRINKETS_GROUP = new ReflectiveChain("inventory", "getSlotType", "getGroup");

    /** Accessories ({@code io.wispforest.accessories}) {@code SlotPath.slotName()} / {@code SlotReference.slotName()}
     *  → slot name (both provider slot objects expose {@code slotName()}). */
    private static final ReflectiveChain ACCESSORIES_SLOT_NAME = new ReflectiveChain("slotName");

    /** Accessories {@code SlotReference.entity()} → the wearer. The pre-1.21.8 slot object (SlotReference) is
     *  entity-bearing; the 1.21.8+ SlotPath is not, so that era passes the render state as the carrier instead. */
    private static final ReflectiveChain ACCESSORIES_SLOT_REFERENCE_ENTITY = new ReflectiveChain("entity");

    /**
     * Curios entry point: resolves {@code SlotContext.identifier()} through the cached reflective chain
     * (Curios is an optional dependency not on the compile classpath) and delegates to
     * {@link AhRenderManagementApi#shouldHideAccessory}.
     */
    public static boolean shouldHideCurio(@Nullable Object slotContext, @Nullable Object carrier) {
        return AhRenderManagementApi.shouldHideAccessory(CURIOS_IDENTIFIER.resolve(slotContext), carrier);
    }

    /**
     * Curios pre-render-state (1.21.1) entry point: {@code CuriosLayer.lambda$render$0} passes no wearer to
     * {@code ICurioRenderer.render}, so both the slot type and the wearer are read off the {@code SlotContext}
     * ({@code identifier()} / {@code entity()}). The entity is the live {@code LivingEntity} (a Player is an
     * {@code IdentityCarrier}).
     */
    public static boolean shouldHideCurioByContext(@Nullable Object slotContext) {
        // resolveObject, not resolve: entity() returns the wearer LivingEntity, not a String.
        return AhRenderManagementApi.shouldHideAccessory(CURIOS_IDENTIFIER.resolve(slotContext), CURIOS_ENTITY.resolveObject(slotContext));
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

    /**
     * Accessories pre-1.21.8 entry point: the per-accessory {@code AccessoryRenderer.render} is handed a
     * {@code SlotReference}, which is entity-bearing - both the slot name ({@code slotName()}) and the wearer
     * ({@code entity()}) come off it, so this covers the direct-entity era (1.20.1/1.21.1) and the transitional
     * render-state era (1.21.4) uniformly.
     */
    public static boolean shouldHideAccessoriesBySlotReference(@Nullable Object slotReference) {
        // resolveObject for the entity: SlotReference.entity() returns the wearer LivingEntity, not a String.
        return AhRenderManagementApi.shouldHideAccessory(
                ACCESSORIES_SLOT_NAME.resolve(slotReference), ACCESSORIES_SLOT_REFERENCE_ENTITY.resolveObject(slotReference));
    }

    /**
     * Accessories 1.21.8 entry point: the slot object is a {@code SlotPath} (entity-free), so the slot name is
     * read off it while the wearer is the render state passed to {@code AccessoryRenderer.render} (an
     * {@code IdentityCarrier}).
     */
    public static boolean shouldHideAccessoriesBySlotPath(@Nullable Object slotPath, @Nullable Object carrier) {
        return AhRenderManagementApi.shouldHideAccessory(ACCESSORIES_SLOT_NAME.resolve(slotPath), carrier);
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
            // AccessoryRenderState is public, so no setAccessible is needed to invoke its methods.
            for (Method method : stateClass.getMethods()) {
                if (method.getName().equals("getStateData") && method.getParameterCount() == 1) {
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
