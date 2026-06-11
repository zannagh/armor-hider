package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Per-scope renderer that owns the interception decision for one {@link RenderScope}.
 * <p>
 * Mixins delegate the bulk of their work to a renderer: identity-carrier detection, slot/stack
 * resolution, modification lookup, scope-specific suppression rules (elytra-while-flying,
 * mainhand-vs-offhand, custom head slot, etc.) and cancelling the underlying callback all live here.
 * <p>
 * <b>Implementing a custom renderer</b> — extend
 * {@link de.zannagh.armorhider.client.render.interceptors.AbstractArmorHiderRenderer} (handles the
 * boilerplate: modification storage, render-type factory plumbing) and override
 * {@link #intercept} or {@link #interceptFrom}. Pick the right {@link RenderScope} via
 * {@link RenderScopeProvider#getTargetScope()}, then register with
 * {@link AhRenderInterceptionRegistryApi#register(AhRenderer, int)} at a lower priority value
 * than the built-in ({@value de.zannagh.armorhider.client.api.impl.AhRendererRegistryImpl#DEFAULT_PRIORITY})
 * to take over.
 *
 * @since 0.12.0
 */
public interface AhRenderer extends RenderScopeProvider, AhRenderTypeFactory {

    /**
     * Intercept a render call when the caller already has the slot and stack (e.g.
     * {@code EquipmentLayerRenderer} receives them as method arguments).
     *
     * @param identityCarrier opaque carrier — usually an entity render state or a {@link Player};
     *                        the renderer casts to {@link IdentityCarrier} if it can.
     *                        May be {@code null} when no entity context is available.
     * @param slot            equipment slot, or {@code null} if the renderer should derive it
     *                        from the stack.
     * @param stack           item stack being rendered, or {@code null} if not available.
     * @param ci              the mixin callback — the renderer may call {@link CallbackInfo#cancel()}
     *                        to short-circuit the underlying method. May be {@code null} for
     *                        context-recovery calls outside a cancellable injection frame.
     * @return a {@link RenderInterceptionResult} describing whether the caller should enter the
     *         scope ({@code shouldIntercept}) and whether the underlying render was cancelled
     *         ({@code shouldCancel}). Never {@code null} — return {@link RenderInterceptionResult#ignore()}
     *         to opt out.
     */
    RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, @Nullable CallbackInfo ci);

    /**
     * Intercept a render call when only the identity carrier is available — the renderer derives
     * slot and stack itself (cape: chest equipment; elytra: synthetic elytra stack;
     * head: custom head item; …).
     * <p>
     * The default implementation delegates to {@link #intercept} with {@code null} slot and stack,
     * which is fine for renderers that always receive a stack from the caller. Renderers that need
     * to derive their state from the carrier should override this method.
     *
     * @param carrier identity carrier (entity render state, player, …), or {@code null}.
     * @param ci      callback for cancellation, or {@code null} for context-recovery calls (e.g.
     *                re-resolving the head scope from {@code resolveSkullRenderType}).
     */
    default RenderInterceptionResult interceptFrom(@Nullable IdentityCarrier carrier, @Nullable CallbackInfo ci) {
        return intercept(carrier, null, null, ci);
    }

    /**
     * Returns the {@link AhRenderModificationApi} the renderer last produced from
     * {@link #intercept(Object, EquipmentSlot, ItemStack, CallbackInfo) intercept}/
     * {@link #interceptFrom(IdentityCarrier, CallbackInfo) interceptFrom} on the current thread.
     * <p>
     * Useful when downstream code needs the modification API immediately (before a scope has been
     * entered via {@link AhRenderManagementApi#enterScope}) — for example when a mixin queries
     * the modification API while still deciding whether to enter the scope. After a scope is
     * entered, prefer {@link AhRenderManagementApi#getActiveScope(RenderScope)} which is the
     * authoritative source.
     *
     * @return the last-resolved modification API, or an empty pass-through API if no interception
     * has happened yet on this thread.
     */
    AhRenderModificationApi getRenderModificationApi();

    /**
     * Installs a custom {@link AhRenderTypeFactory} for this renderer. The factory will be consulted
     * whenever the renderer needs a translucent render type — overriding the built-in defaults from
     * {@link de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes}.
     * <p>
     * Useful when a compat layer needs a custom render pipeline (e.g. a shader-friendly blend mode).
     * Passing {@code null} (or never calling this method) leaves the renderer using its built-in
     * defaults.
     */
    void registerRenderTypeFactory(AhRenderTypeFactory factory);
}
