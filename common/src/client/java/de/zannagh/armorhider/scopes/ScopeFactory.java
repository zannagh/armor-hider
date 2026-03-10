package de.zannagh.armorhider.scopes;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//? if < 1.21.4
//import net.minecraft.world.entity.LivingEntity;

/**
 * Creates scope objects from mixin parameters.
 * Handles identity resolution, config lookup, and slot resolution (e.g. elytra → CHEST).
 * Separates scope creation (this class) from scope storage ({@link ScopeProvider}).
 */
public final class ScopeFactory {

    private ScopeFactory() {}

    //? if >= 1.21.4 {
    /**
     * Creates an ItemRenderScope for layer mixins that have a HumanoidRenderState.
     * Also enriches the entity scope if not already resolved.
     *
     * @return the created scope, or null if no valid context could be established
     */
    public static @Nullable ItemRenderScope createItemScope(
            @NotNull ScopeProvider provider,
            @Nullable ItemStack itemStack,
            @NotNull EquipmentSlot slot,
            @NotNull HumanoidRenderState renderState) {

        // Enrich entity scope if still sentinel or absent
        var entityScope = provider.entityScope();
        entityScope = ScopeUtils.enrichIfNullOrSentinel(provider, entityScope, renderState);

        return buildItemScope(entityScope, itemStack, slot);
    }
    //?}

    //? if < 1.21.4 {
    /*public static @Nullable ItemRenderScope createItemScope(
            @NotNull ScopeProvider provider,
            @Nullable ItemStack itemStack,
            @NotNull EquipmentSlot slot,
            @NotNull LivingEntity entity) {

        var entityScope = provider.entityScope();
        entityScope = ScopeUtils.enrichIfNullOrSentinel(provider, entityScope, entity);

        return buildItemScope(entityScope, itemStack, slot);
    }
    *///?}

    /**
     * Creates an ItemRenderScope using the already-enriched entity scope.
     * Used when the render state or entity is not directly available in the mixin
     * (e.g. 1.21.4-1.21.8 renderArmorPiece which doesn't receive the render state).
     */
    public static @Nullable ItemRenderScope createItemScope(
            @NotNull ScopeProvider provider,
            @Nullable ItemStack itemStack,
            @NotNull EquipmentSlot slot) {
        return buildItemScope(provider.entityScope(), itemStack, slot);
    }

    /**
     * Creates an ItemRenderScope for mixins that have a GameProfile
     * (e.g. OffHandRenderMixin, ItemEntityRendererMixin).
     */
    public static @Nullable ItemRenderScope createItemScope(
            @NotNull ScopeProvider provider,
            @Nullable ItemStack itemStack,
            @NotNull EquipmentSlot slot,
            @NotNull GameProfile profile) {

        var identity = EntityIdentityResolver.resolveFromProfile(profile);
        if (identity.playerName() == null) {
            return null;
        }

        EquipmentSlot resolvedSlot = resolveSlot(slot, itemStack);
        ItemStack resolvedItem = itemStack != null ? itemStack : ItemStack.EMPTY;

        ArmorModificationInfo modification = new ArmorModificationInfo(
                resolvedSlot,
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(identity.playerName())
        );

        return new ItemRenderScope(resolvedSlot, resolvedItem, modification);
    }

    private static @Nullable ItemRenderScope buildItemScope(
            @Nullable EntityRenderScope entityScope,
            @Nullable ItemStack itemStack,
            @NotNull EquipmentSlot slot) {

        if (entityScope == null || !entityScope.isPlayerEntity()) {
            return null;
        }

        EquipmentSlot resolvedSlot = resolveSlot(slot, itemStack);
        ItemStack resolvedItem = itemStack != null ? itemStack : ItemStack.EMPTY;

        ArmorModificationInfo modification = new ArmorModificationInfo(
                resolvedSlot,
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(entityScope.resolvedPlayerName())
        );

        return new ItemRenderScope(resolvedSlot, resolvedItem, modification);
    }

    private static EquipmentSlot resolveSlot(@NotNull EquipmentSlot slot, @Nullable ItemStack itemStack) {
        if (ItemsUtil.itemStackContainsElytra(itemStack)) {
            return EquipmentSlot.CHEST;
        }
        return slot;
    }
}
