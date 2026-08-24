package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Everything Armor Hider knows about the piece it is about to render, handed to the
 * {@code *Matching(...)} rule variants on {@link ArmorHiderRenderApi}.
 * <p>
 * Instances are short-lived and only valid for the duration of the predicate call - do not keep
 * a reference. A context is created on the render thread once per player, per slot, per
 * modification, and shared by every rule for that target, so predicates must be cheap and must not
 * allocate heavily or block.
 *
 * @since 0.13.0
 */
@ApiStatus.NonExtendable
@ApiStatus.Experimental
public interface AhHideContext {

    /**
     * The display name Armor Hider identifies the rendered player by. This is the player's
     * <em>live</em> display name (rank prefixes and nicks included), the same key the config and
     * combat lookups use - not necessarily the GameProfile name.
     *
     * @return the rendered player's name, never {@code null} and never blank.
     *
     * @since 0.13.0
     */
    @NonNull String playerName();

    /**
     * The player entity behind {@link #playerName()}, resolved lazily against the client level and
     * cached for roughly one tick. May be {@code null} when the entity is not loaded on the client
     * (out of render distance, already removed, or during early client startup).
     *
     * @return the resolved player, or {@code null} if it cannot be found.
     *
     * @since 0.13.0
     */
    @Nullable Player player();

    /**
     * The equipment slot being rendered. Elytra rules always report {@link EquipmentSlot#CHEST} -
     * use {@link #isElytra()} to tell them apart.
     *
     * @return the slot this decision applies to.
     *
     * @since 0.13.0
     */
    @NonNull EquipmentSlot slot();

    /**
     * The stack in {@link #slot()}. Rules are evaluated once, after the stack has been resolved,
     * so this is the real worn item in every render path. It is {@link ItemStack#EMPTY} only where
     * Armor Hider genuinely has no stack to offer - notably the accessory-slot query, which asks
     * "would this slot be hidden?" without a specific item. Never {@code null}.
     *
     * @return the rendered stack, never {@code null}.
     *
     * @since 0.13.0
     */
    @NonNull ItemStack stack();

    /**
     * Whether this decision is about elytra wings rather than a chest armor piece.
     *
     * @return {@code true} for the elytra render path.
     *
     * @since 0.13.0
     */
    boolean isElytra();

    /**
     * The Armor Hider config that resolved for this player - the viewer's own config for the local
     * player, the per-player override or the server-pushed config for everybody else. Read-only for
     * rule purposes; mutating it from a predicate is unsupported.
     *
     * @return the resolved player config, never {@code null}.
     *
     * @since 0.13.0
     */
    @NonNull PlayerConfig config();

    /**
     * The opacity Armor Hider derived from the user's config for this slot - including combat
     * detection - before any rule was applied, on the same {@code 0.0} (invisible) to {@code 1.0}
     * (opaque) scale used by the API. Rules are evaluated exactly once, so this is always the
     * config-derived value and never another rule's output.
     *
     * @return the pre-rule, config-derived opacity.
     *
     * @since 0.13.0
     */
    double baseOpacity();
}
