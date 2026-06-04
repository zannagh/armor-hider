package de.zannagh.armorhider.client.api.render;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.NullLiteralExpression;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.ScopeHandover;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface AhRenderInterceptionApi {

    default RenderInterceptionResult interceptRenderCall(InterceptionContext context, @Nullable Object carrier, LocalRef<ScopeHandover> carrierRef) {
        var carrier1 = tryGetIdentityCarrierFromLocalRef(carrier, carrierRef);
        return interceptRenderCall(context, carrier1, null, null, carrierRef);
    }

    default RenderInterceptionResult interceptRenderCallAndResolveCarrier(InterceptionContext context, @Nullable Object carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack itemStack, LocalRef<ScopeHandover> carrierRef) {
        var carrier1 = tryGetIdentityCarrierFromLocalRef(carrier, carrierRef);
        return interceptRenderCall(context, carrier1, slot, itemStack, carrierRef);
    }

    RenderInterceptionResult interceptRenderCall(InterceptionContext context, @Nullable IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, LocalRef<ScopeHandover> modificationRef);

    void wrapAndCancelRenderCall(CallbackInfo ci);

    default void releaseContext() {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().clearActiveModification();
    }

    default IdentityCarrier tryGetIdentityCarrierFromLocalRef(Object context, LocalRef<ScopeHandover> scopeHandover){
        if (context instanceof IdentityCarrier carrier1) {
            scopeHandover.set(new ScopeHandover(carrier1, null));
            return carrier1;
        }
        if (scopeHandover.get() != null && scopeHandover.get().carrier() != null) {
            return scopeHandover.get().carrier();
        }

        return null;
    }

    default IdentityCarrier tryGetIdentityCarrier(Object context, LocalRef<IdentityCarrier> carrier){
        if (context instanceof IdentityCarrier carrier1) {
            carrier.set(carrier1);
            return carrier1;
        }
        if (carrier.get() instanceof IdentityCarrier localRefCarrier) {
            return localRefCarrier;
        }

        return null;
    }

    public enum InterceptionContext{
        PER_PLAYER_CAPTURE,
        PER_PIECE_LAYER,
        PER_PIECE_LAYER_WITHOUT_CONTEXT_SET
    }
}
