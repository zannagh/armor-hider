package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shared scaffolding for built-in renderers: stores the most recent {@link AhRenderModificationApi}
 * per thread (so {@link #getRenderModificationApi()} returns whatever the last interception produced) and
 * provides a {@code standardIntercept} that handles modification lookup, cancellation and result wrapping.
 * <p>
 * Scope-specific renderers override {@link #interceptFrom} (or {@link #intercept}) and call
 * {@code standardIntercept(...)} with their own carrier/slot/stack and any extra short-circuit rules.
 */
public abstract class AbstractArmorHiderRenderer implements AhRenderer {

    protected final ThreadLocal<AhRenderModificationApi> modificationApi = new ThreadLocal<>();

    @Nullable
    private AhRenderTypeFactory customRenderTypeFactory;

    @Override
    public AhRenderModificationApi getRenderModificationApi() {
        var api = modificationApi.get();
        return api != null ? api : RenderModifications.empty();
    }

    @Override
    public void registerRenderTypeFactory(AhRenderTypeFactory factory) {
        this.customRenderTypeFactory = factory;
    }

    @Override
    public RenderType getTranslucentArmorRenderType(Identifier texture) {
        return customRenderTypeFactory != null
                ? customRenderTypeFactory.getTranslucentArmorRenderType(texture)
                : ArmorHiderRenderTypes.translucentArmor(texture);
    }

    @Override
    public RenderType getTranslucentEntityRenderType(Identifier texture) {
        return customRenderTypeFactory != null
                ? customRenderTypeFactory.getTranslucentEntityRenderType(texture)
                : ArmorHiderRenderTypes.translucentEntity(texture);
    }

    @Override
    public RenderType getTranslucentArmorTrimRenderType() {
        return customRenderTypeFactory != null
                ? customRenderTypeFactory.getTranslucentArmorTrimRenderType()
                : ArmorHiderRenderTypes.translucentArmorTrim();
    }

    @Override
    public RenderType getTranslucentItemSheetRenderType() {
        return customRenderTypeFactory != null
                ? customRenderTypeFactory.getTranslucentItemSheetRenderType()
                : ArmorHiderRenderTypes.translucentItemSheet();
    }

    /**
     * Build a {@link SlotModification} for the given carrier/slot/stack and update the per-thread
     * modification API so {@link #getRenderModificationApi()} reflects the latest interception.
     */
    protected SlotModification resolveModification(@Nullable IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack) {
        SlotModification mod;
        if (carrier == null || carrier.armorHider$playerName() == null || slot == null) {
            mod = SlotModification.empty();
        } else {
            mod = SlotModification.of(carrier.armorHider$playerName(), slot, stack);
        }
        modificationApi.set(new RenderModifications(mod));
        return mod;
    }

    /**
     * Default interception: empty modification → ignore; shouldHide → cancel CI and signal cancel;
     * otherwise → enter scope without cancelling.
     */
    protected RenderInterceptionResult standardIntercept(@Nullable IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        var mod = resolveModification(carrier, slot, stack);
        if (mod.isEmpty()) {
            return RenderInterceptionResult.ignore();
        }
        if (mod.shouldHide()) {
            cancel(ci);
            return new RenderInterceptionResult(true, true, getTargetScope(), carrier, mod);
        }
        return new RenderInterceptionResult(true, false, getTargetScope(), carrier, mod);
    }

    protected static void cancel(@Nullable CallbackInfo ci) {
        if (ci != null && ci.isCancellable() && !ci.isCancelled()) {
            ci.cancel();
        }
    }
}
