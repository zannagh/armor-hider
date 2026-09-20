package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.client.api.AhHideContext;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A single {@link AhHideContext} evaluation. One instance is built per evaluation and shared by
 * every rule registered for that target, so the {@link Player} lookup is paid at most once even
 * when a dozen rules ask for it - and not at all when none of them do. Not thread-safe and not
 * reusable: it is confined to the thread that built it and is dead once evaluation returns.
 */
@ApiStatus.Internal
public final class AhHideContextImpl implements AhHideContext {

    private final String playerName;
    private final EquipmentSlot slot;
    private final ItemStack stack;
    private final boolean elytra;
    private final PlayerConfig config;
    private final double baseOpacity;

    private boolean playerResolved;
    private @Nullable Player player;

    public AhHideContextImpl(String playerName,
                             EquipmentSlot slot,
                             @Nullable ItemStack stack,
                             boolean elytra,
                             PlayerConfig config,
                             double baseOpacity) {
        this.playerName = playerName != null ? playerName : "";
        this.slot = slot;
        this.stack = stack != null ? stack : ItemStack.EMPTY;
        this.elytra = elytra;
        this.config = config;
        this.baseOpacity = baseOpacity;
    }

    @Override
    public @NonNull String playerName() {
        return playerName;
    }

    @Override
    public @Nullable Player player() {
        if (!playerResolved) {
            playerResolved = true;
            player = AhPlayerLookupCache.resolve(playerName);
        }
        return player;
    }

    @Override
    public @NonNull EquipmentSlot slot() {
        return slot;
    }

    @Override
    public @NonNull ItemStack stack() {
        return stack;
    }

    @Override
    public boolean isElytra() {
        return elytra;
    }

    @Override
    public @NonNull PlayerConfig config() {
        return config;
    }

    @Override
    public double baseOpacity() {
        return baseOpacity;
    }
}
