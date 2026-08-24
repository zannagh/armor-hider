package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Compatibility for Fabric API's {@code ArmorRenderer} (fabric-rendering-v1), the loader-level API through
 * which a mod registers a custom armor renderer for one of its items. Nycto's vampire/hunter armor uses it,
 * and so does every other Fabric mod that draws armor from its own model instead of an equipment asset.
 *
 * <p>Fabric API dispatches those renderers from its own {@code @Inject(at = HEAD, cancellable = true)} on
 * {@code HumanoidArmorLayer.renderArmorPiece}: when the worn item has a registered renderer it draws the
 * mod's model and cancels the vanilla body. Armor Hider hooks the very same method at HEAD, and the
 * relative order of two same-priority HEAD injections from different mods is not guaranteed - when Fabric
 * API wins the race it cancels before Armor Hider's hook ever runs, and the armor stays visible. The
 * compat mixins therefore pin that ordering with an explicit mixin priority so their scope is opened
 * before Fabric API's dispatch and closed after it.
 *
 * <p>This helper supplies the two pieces of state those mixins need:
 * <ul>
 *   <li>{@link #hasCustomRenderer(ItemStack)} - whether Fabric API will take over this piece. It asks
 *       Fabric API's own registry, so the answer is exactly the one Fabric API is about to act on. The
 *       lookup is reflective: {@code ArmorRendererRegistryImpl} is a Fabric-internal class, and Armor
 *       Hider must build and run without fabric-rendering-v1 on the classpath at all (NeoForge, or a
 *       Fabric instance without Fabric API).</li>
 *   <li>{@link #isDrawingCustomArmor()} - whether a Fabric custom armor renderer is drawing right now.
 *       The geometry hooks sit on shared Minecraft entry points ({@code armorCutoutNoCull}, the model
 *       submit / draw) and gate on this, so they can never touch anything but the piece Armor Hider
 *       bracketed.</li>
 * </ul>
 *
 * <p>Everything here degrades to "inert" rather than failing: an unresolvable registry (Fabric API absent,
 * or a future rename) leaves {@link #hasCustomRenderer(ItemStack)} permanently {@code false}, and the
 * compat mixins then never act.
 *
 * @since 0.12.20
 */
public final class FabricArmorRendererCompat {

    private static final String REGISTRY_CLASS = "net.fabricmc.fabric.impl.client.rendering.ArmorRendererRegistryImpl";

    /**
     * {@code (Item) -> Object}: Fabric API's {@code ArmorRendererRegistryImpl.get(Item)}, or {@code null}
     * when fabric-rendering-v1 is absent or the method could not be bound. Resolved once - a
     * {@link MethodHandle} rather than a {@link Method} so the per-piece call on the render path costs no
     * varargs array and no boxing.
     */
    private static final @Nullable MethodHandle GET_RENDERER = resolveRegistryLookup();

    /**
     * Set for the duration of one {@code renderArmorPiece} call that Fabric API is handling. A
     * {@link ThreadLocal} rather than a plain field because Armor Hider's own render state is thread-local
     * too and entity rendering is not contractually single-threaded; reads are guarded by the
     * {@link #GET_RENDERER} null check, which is a constant {@code null} whenever Fabric API is absent, so
     * the shared Minecraft entry points that consult this pay nothing outside a Fabric-API instance.
     */
    private static final ThreadLocal<Boolean> DRAWING_CUSTOM_ARMOR = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Diagnostic counters. Like the render pipeline's own {@code modifiedScopeEnterCount}, these exist so
     * a smoke test can tell "the compat ran and decided" apart from "the injection silently stopped
     * binding" - the failure mode a version bump in Fabric API would produce, and one that is otherwise
     * invisible because a missed hook simply leaves the mod's armor drawn as before.
     */
    private static final AtomicLong BRACKETED_PIECES = new AtomicLong();

    private static final AtomicLong HIDDEN_PIECES = new AtomicLong();

    private FabricArmorRendererCompat() {}

    private static @Nullable MethodHandle resolveRegistryLookup() {
        if (!CompatManager.requiresCompatTo(CompatFlags.FABRIC_ARMOR_RENDERER)) {
            return null;
        }
        try {
            Class<?> registry = Class.forName(REGISTRY_CLASS, false,
                    FabricArmorRendererCompat.class.getClassLoader());
            Method get = registry.getMethod("get", Item.class);
            get.setAccessible(true);
            // Erase the mod-typed return to Object: ArmorRenderer is not on our compile classpath, and all
            // we ever ask is "is there one".
            return MethodHandles.lookup().unreflect(get)
                    .asType(MethodType.methodType(Object.class, Item.class));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /**
     * @return whether Fabric API's armor-renderer registry could be bound - i.e. whether any of this
     * compat can do anything at all in the current instance.
     */
    public static boolean isAvailable() {
        return GET_RENDERER != null;
    }

    /**
     * @param stack the item worn in the armor slot about to be rendered.
     * @return {@code true} when Fabric API has a custom {@code ArmorRenderer} registered for the item, and
     * will therefore cancel the vanilla armor piece and draw its own model.
     */
    public static boolean hasCustomRenderer(@Nullable ItemStack stack) {
        MethodHandle lookup = GET_RENDERER;
        if (lookup == null || stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            Object renderer = lookup.invokeExact(stack.getItem());
            return renderer != null;
        } catch (Throwable t) {
            // A registry that throws once will throw every frame - never let it break rendering.
            return false;
        }
    }

    /**
     * Open the "a Fabric custom armor renderer is drawing" window. Paired with
     * {@link #endCustomArmorRender()} by the compat layer mixin.
     */
    public static void beginCustomArmorRender() {
        DRAWING_CUSTOM_ARMOR.set(Boolean.TRUE);
    }

    /** Records that the compat took ownership of one Fabric-rendered armor piece. */
    public static void recordBracketedPiece(boolean hidden) {
        BRACKETED_PIECES.incrementAndGet();
        if (hidden) {
            HIDDEN_PIECES.incrementAndGet();
        }
    }

    /**
     * @return how many Fabric-rendered armor pieces the compat has taken ownership of - i.e. how often
     * {@link #hasCustomRenderer(ItemStack)} was true at the top of {@code renderArmorPiece}.
     */
    public static long bracketedPieceCount() {
        return BRACKETED_PIECES.get();
    }

    /** @return how many of those pieces were cancelled outright because the slot is fully hidden. */
    public static long hiddenPieceCount() {
        return HIDDEN_PIECES.get();
    }

    /** Close the window opened by {@link #beginCustomArmorRender()}. Safe to call when none is open. */
    public static void endCustomArmorRender() {
        DRAWING_CUSTOM_ARMOR.remove();
    }

    /**
     * @return whether a Fabric custom armor renderer is drawing on this thread right now. Always
     * {@code false} without fabric-rendering-v1.
     */
    public static boolean isDrawingCustomArmor() {
        return GET_RENDERER != null && DRAWING_CUSTOM_ARMOR.get();
    }
}
