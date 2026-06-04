package de.zannagh.armorhider.client.api.render;

import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Array;

public interface AhRenderInterceptionApi {

    RenderInterceptionResult interceptRenderCall(InterceptionContext context, Object... additionalContext);

    void wrapAndCancelRenderCall(CallbackInfo ci);

    default void releaseContext() {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().clearActiveModification();
    }

    public enum InterceptionContext{
        PER_PLAYER_CAPTURE,
        PER_PIECE_LAYER,
        PER_PIECE_LAYER_WITHOUT_CONTEXT_SET
    }
}
