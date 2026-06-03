package de.zannagh.armorhider.client.api.render;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public abstract class RenderInterceptor<THost, TEntity, TModel> {

    THost host;
    TEntity entity;
    TModel model;
    IdentityCarrier carrier;
    SlotModification modification;

    public boolean shouldIntercept(THost context, @Nullable IdentityCarrier carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack itemStack, @Nullable TEntity entity, @Nullable TModel model){
        this.host = context;
        this.entity = entity;
        this.model = model;
        this.carrier = carrier;
        var playerName = carrier != null ? carrier.armorHider$playerName() : null;
        if (playerName != null && slot != null) {
            var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(carrier.armorHider$playerName());
            if (config != null) {
                modification = SlotModification.of(config, slot);
                if (itemStack != null) {
                    modification = modification.addItemInformation(itemStack);
                }
            }
        }


        return true;
    }

    public abstract boolean shouldCancelRender(THost context);

    protected void cancelRenderAndResetContext(CallbackInfo ci) {
        if (ci.isCancellable()) {
            ci.cancel();
        }
        close();
    }

    protected THost getHost() throws RenderInterceptException {
        if (host == null) {
            throw new RenderInterceptException("Context not setup.");
        }
        return host;
    }

    protected void close() {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().clearActiveModification();
    }

    public abstract void interceptRender(THost context, CallbackInfo ci);
}
