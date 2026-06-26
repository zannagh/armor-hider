package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.client.common.EquippableInformation;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
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
    public RenderType getTranslucentArmorTrimRenderType(boolean decal) {
        return customRenderTypeFactory != null
                ? customRenderTypeFactory.getTranslucentArmorTrimRenderType(decal)
                : ArmorHiderRenderTypes.translucentArmorTrim();
    }

    @Override
    public RenderType getTranslucentItemSheetRenderType() {
        return customRenderTypeFactory != null
                ? customRenderTypeFactory.getTranslucentItemSheetRenderType()
                : ArmorHiderRenderTypes.translucentItemSheet();
    }

    /**
     * Set the modification API to an empty modification.
     * @return An empty instance of {@link SlotModification}
     */
    protected SlotModification setEmptyModification() {
        var mod = SlotModification.empty();
        var modifications = AhRenderModificationApi.getInstance(mod);
        modificationApi.set(modifications);
        return mod;
    }

    /**
     * Build a {@link SlotModification} for the given carrier/slot/stack and update the per-thread
     * modification API so {@link #getRenderModificationApi()} reflects the latest interception.
     */
    protected SlotModification resolveModification(@NonNull IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack) {
        if (carrier.armorHider$playerName() == null) {
            return setEmptyModification();
        }
        var equipmentInfo = new EquippableInformation(carrier, slot, stack);
        if (!equipmentInfo.isValid()) {
            return setEmptyModification();
        }

        var mod = SlotModification.of(carrier.armorHider$playerName(), equipmentInfo.getSlot(), equipmentInfo.getStack());
        var modifications = AhRenderModificationApi.getInstance(mod);
        modificationApi.set(modifications);
        return mod;
    }

    /**
     * Default interception: empty modification → ignore; shouldHide → cancel CI and signal cancel;
     * otherwise → enter scope without cancelling.
     * <p>
     * Callers must null-check {@code carrier} before invoking — {@link #resolveModification}
     * dereferences the carrier on the first line. Every built-in renderer guards explicitly
     * (typically with {@code if (carrier == null) { setEmptyModification(); return ignore(); }})
     * before reaching this method.
     */
    protected RenderInterceptionResult standardIntercept(@NonNull IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
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
