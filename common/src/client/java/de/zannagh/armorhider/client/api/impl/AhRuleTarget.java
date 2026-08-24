package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.net.packets.SharedRuleTarget;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Internal registry key for {@link de.zannagh.armorhider.client.api.AhRenderRule}s.
 * <p>
 * {@link de.zannagh.armorhider.client.common.RenderScope} is deliberately not reused here: it
 * collapses HEAD/CHEST/LEGS/FEET armor into a single {@code ARMOR_PIECE} scope, while rules are
 * addressed per slot. {@link #ELYTRA} is the one target that is not an equipment slot - the elytra
 * is worn in {@link EquipmentSlot#CHEST} but follows its own opacity and glint settings.
 */
@ApiStatus.Internal
public enum AhRuleTarget {
    HEAD,
    CHEST,
    LEGS,
    FEET,
    OFFHAND,
    ELYTRA;

    /**
     * Maps an equipment slot to its rule target, or {@code null} for a slot Armor Hider has no
     * render path for: {@code BODY}, {@code SADDLE} and - less obviously - {@code MAINHAND}.
     * <p>
     * {@code MAINHAND} is excluded deliberately. It looks targetable (it is a real hand slot, and
     * {@code SlotModification.empty()} even uses it as its placeholder), but there is no main-hand
     * interceptor - {@code ArmorHiderOffhandRenderer} covers the off hand only - and no main-hand
     * opacity in the config, so {@code baseTransparencyFor} returns 1.0 for it. A rule registered
     * against it would be accepted, hand back a valid handle, and then never do anything. Returning
     * {@code null} turns that silent no-op into an immediate registration-time throw.
     */
    /**
     * @return the equipment slot this target is worn in. {@link #ELYTRA} reports {@code CHEST}, which
     *         is where the wings actually live - the distinction between the two is in which config
     *         values apply, not in where the item sits.
     */
    public EquipmentSlot slot() {
        return switch (this) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST, ELYTRA -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            case OFFHAND -> EquipmentSlot.OFFHAND;
        };
    }

    /** @return the wire-side twin of this target, for shared rules. Mapped by name, never by ordinal. */
    public SharedRuleTarget toWire() {
        // Both enums are exhaustive over the same constant names, so this cannot fail; valueOf makes
        // adding a target to one enum and forgetting the other a loud failure rather than a silent
        // mis-mapping.
        return SharedRuleTarget.valueOf(name());
    }

    /** @return the client-side twin of a wire target, or {@code null} for one this version does not know. */
    public static @Nullable AhRuleTarget fromWire(@Nullable SharedRuleTarget target) {
        if (target == null) {
            return null;
        }
        for (AhRuleTarget candidate : values()) {
            if (candidate.name().equals(target.name())) {
                return candidate;
            }
        }
        return null;
    }

    public static @Nullable AhRuleTarget of(@Nullable EquipmentSlot slot) {
        if (slot == null) {
            return null;
        }
        return switch (slot) {
            case HEAD -> HEAD;
            case CHEST -> CHEST;
            case LEGS -> LEGS;
            case FEET -> FEET;
            case OFFHAND -> OFFHAND;
            default -> null;
        };
    }
}
