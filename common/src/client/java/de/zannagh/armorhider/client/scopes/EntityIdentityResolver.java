package de.zannagh.armorhider.client.scopes;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.log.DebugTracer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//? if >= 1.21.9
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//? if >= 1.21.4 && < 1.21.9
//import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//? if < 1.21.4 {
/*import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
*///?}

/**
 * Resolves entity identity for the rendering pipeline.
 * Consolidates all version-dependent player detection and name resolution,
 * keeping Stonecutter conditionals out of scope objects and the ScopeProvider.
 */
public final class EntityIdentityResolver {

    private EntityIdentityResolver() {}

    public record Identity(@Nullable String playerName, boolean isPlayer) {}

    //? if >= 1.21.9 {
    public static Identity resolve(@Nullable LivingEntityRenderState renderState) {
        if (renderState == null) return new Identity(null, false);
        boolean isPlayer = renderState instanceof AvatarRenderState;
        if (!isPlayer) return new Identity(null, false);

        if (renderState.nameTag != null) {
            var result = new Identity(renderState.nameTag.getString(), true);
            DebugTracer.identityResolved("nameTag", result.playerName(), true);
            return result;
        }

        // Read player name from the render state itself (set during extractRenderState
        // via IdentityCarrier). This is per-entity and immune to ordering issues.
        if (renderState instanceof IdentityCarrier carrier) {
            String carriedName = carrier.armorHider$getPlayerName();
            if (carriedName != null) {
                var result = new Identity(carriedName, true);
                DebugTracer.identityResolved("carrier", result.playerName(), true);
                return result;
            }
        }

        DebugTracer.identityResolved("fallback-null", null, true);
        return new Identity(null, true);
    }
    //?}

    //? if >= 1.21.4 && < 1.21.9 {
    /*public static Identity resolve(@Nullable LivingEntityRenderState renderState) {
        if (renderState == null) return new Identity(null, false);
        boolean isPlayer = renderState instanceof PlayerRenderState;
        if (!isPlayer) return new Identity(null, false);

        if (renderState.nameTag != null) {
            var result = new Identity(renderState.nameTag.getString(), true);
            DebugTracer.identityResolved("nameTag", result.playerName(), true);
            return result;
        }

        if (renderState instanceof IdentityCarrier carrier) {
            String carriedName = carrier.armorHider$getPlayerName();
            if (carriedName != null) {
                var result = new Identity(carriedName, true);
                DebugTracer.identityResolved("carrier", result.playerName(), true);
                return result;
            }
        }

        DebugTracer.identityResolved("fallback-null", null, true);
        return new Identity(null, true);
    }
    *///?}

    //? if < 1.21.4 {
    /*public static Identity resolve(@Nullable LivingEntity entity) {
        if (entity == null) return new Identity(null, false);
        boolean isPlayer = entity instanceof Player;
        if (!isPlayer) return new Identity(null, false);

        String name = ((Player) entity).getName().getString();
        DebugTracer.identityResolved("entity-direct", name, true);
        return new Identity(name, true);
    }
    *///?}

    /**
     * Resolves identity from a GameProfile (used by mixins that have direct profile access,
     * such as OffHandRenderMixin and ItemEntityRendererMixin).
     */
    public static Identity resolveFromProfile(@NotNull GameProfile profile) {
        //? if >= 1.21.9
        String name = profile.name();
        //? if < 1.21.9
        //String name = profile.getName();
        return new Identity(name, true);
    }
}
