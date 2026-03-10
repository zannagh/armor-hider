package de.zannagh.armorhider.scopes;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.client.ArmorHiderClient;
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

    /**
     * Identity hint captured from the entity during {@code extractRenderState},
     * where we still have access to the actual {@link net.minecraft.world.entity.player.Player}
     * entity. Used as a reliable fallback when {@code nameTag} is null (which happens
     * for sneaking players, invisible players, hidden nametags, etc. — not just the local player).
     */
    private static final ThreadLocal<Identity> identityHint = new ThreadLocal<>();

    public static void setIdentityHint(Identity hint) {
        identityHint.set(hint);
    }

    public static void clearIdentityHint() {
        identityHint.remove();
    }

    public record Identity(@Nullable String playerName, boolean isPlayer) {}

    //? if >= 1.21.9 {
    public static Identity resolve(@Nullable LivingEntityRenderState renderState) {
        if (renderState == null) return new Identity(null, false);
        boolean isPlayer = renderState instanceof AvatarRenderState;
        if (!isPlayer) return new Identity(null, false);

        if (renderState.nameTag != null) {
            return new Identity(renderState.nameTag.getString(), true);
        }

        // nameTag is null — use the identity hint captured from extractRenderState
        // where we had access to the actual Player entity
        Identity hint = identityHint.get();
        if (hint != null) {
            return hint;
        }

        // No hint available — can't reliably identify this player
        return new Identity(null, true);
    }
    //?}

    //? if >= 1.21.4 && < 1.21.9 {
    /*public static Identity resolve(@Nullable LivingEntityRenderState renderState) {
        if (renderState == null) return new Identity(null, false);
        boolean isPlayer = renderState instanceof PlayerRenderState;
        if (!isPlayer) return new Identity(null, false);

        if (renderState.nameTag != null) {
            return new Identity(renderState.nameTag.getString(), true);
        }

        // nameTag is null — use the identity hint captured from extractRenderState
        // where we had access to the actual Player entity
        Identity hint = identityHint.get();
        if (hint != null) {
            return hint;
        }

        // No hint available — can't reliably identify this player
        return new Identity(null, true);
    }
    *///?}

    //? if < 1.21.4 {
    /*public static Identity resolve(@Nullable LivingEntity entity) {
        if (entity == null) return new Identity(null, false);
        boolean isPlayer = entity instanceof Player;
        if (!isPlayer) return new Identity(null, false);

        String name = ((Player) entity).getName().getString();
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
