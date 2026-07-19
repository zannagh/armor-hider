package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Shared decision logic for hiding accessories rendered by accessory providers (Curios / Trinkets /
 * Artifacts) — issue #246. Providers expose no per-render alpha, so accessories can only be hidden,
 * not faded: an accessory disappears when the armor slot its accessory-slot type maps to is fully
 * hidden, gated by the master {@code affectAccessories} toggle and the per-region toggle.
 * <p>
 * Slot-type vocabulary is normalised across providers here: Curios uses flat identifiers
 * ({@code head}, {@code necklace}, {@code belt}, {@code feet}); Trinkets groups body regions
 * ({@code head}, {@code chest}, {@code legs}, {@code feet}). Both are accepted.
 */
public final class AccessoryHidingCompat {

    private AccessoryHidingCompat() {
    }

    private static volatile Method curiosIdentifierMethod;

    /**
     * Map an accessory slot-type key to the armor slot that governs it, or {@code null} when the type
     * is not one of the four regions Armor Hider tracks (rings, gloves, capes, charms, … are left alone).
     */
    @Nullable
    public static EquipmentSlot mapAccessoryTypeToSlot(@Nullable String typeKey) {
        if (typeKey == null) {
            return null;
        }
        return switch (typeKey.toLowerCase(Locale.ROOT)) {
            case "head" -> EquipmentSlot.HEAD;
            case "necklace", "chest" -> EquipmentSlot.CHEST;
            case "belt", "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    /**
     * @param typeKey the provider's accessory slot-type / group key
     * @param carrier the render-state (or entity) the accessory is drawn on; must be an {@link IdentityCarrier}
     * @return whether this accessory should be skipped (hidden) for the rendered player
     */
    public static boolean shouldHideAccessory(@Nullable String typeKey, @Nullable Object carrier) {
        EquipmentSlot slot = mapAccessoryTypeToSlot(typeKey);
        if (slot == null) {
            return false;
        }
        if (!(carrier instanceof IdentityCarrier identityCarrier)) {
            return false;
        }
        String playerName = identityCarrier.armorHider$playerName();
        if (playerName == null) {
            return false;
        }
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);
        if (!config.affectAccessories.getValue()) {
            return false;
        }
        boolean regionEnabled = switch (slot) {
            case HEAD -> config.affectHeadAccessory.getValue();
            case CHEST -> config.affectChestAccessory.getValue();
            case LEGS -> config.affectLegsAccessory.getValue();
            case FEET -> config.affectFeetAccessory.getValue();
            default -> false;
        };
        if (!regionEnabled) {
            return false;
        }
        // SlotModification.of already yields an empty (non-hiding) modification when Armor Hider is
        // disabled / force-off for this player, so those guards are respected without repeating them.
        return SlotModification.of(config, slot).shouldHide();
    }

    /**
     * Curios entry point: resolves {@code SlotContext.identifier()} reflectively (Curios is an optional
     * dependency not on the compile classpath) and delegates to {@link #shouldHideAccessory}.
     */
    public static boolean shouldHideCurio(@Nullable Object slotContext, @Nullable Object carrier) {
        return shouldHideAccessory(curiosIdentifier(slotContext), carrier);
    }

    /**
     * Trinkets ({@code dev.emi.trinkets}, the Trinkets/Trinkets-Canary mod) entry point: the slot's body
     * region is {@code slotReference.inventory().getSlotType().getGroup()} (a String like {@code head} /
     * {@code chest} / {@code legs} / {@code feet}), resolved reflectively as Trinkets is an optional
     * dependency not on the compile classpath.
     */
    public static boolean shouldHideTrinket(@Nullable Object slotReference, @Nullable Object carrier) {
        return shouldHideAccessory(reflectiveNoArgChain(slotReference, "inventory", "getSlotType", "getGroup"), carrier);
    }

    /** Walk a chain of no-arg methods (e.g. inventory→slotType→group) and return the final String, or null. */
    @Nullable
    private static String reflectiveNoArgChain(@Nullable Object target, String... methods) {
        Object current = target;
        try {
            for (String methodName : methods) {
                if (current == null) {
                    return null;
                }
                current = current.getClass().getMethod(methodName).invoke(current);
            }
            return current instanceof String result ? result : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static String curiosIdentifier(@Nullable Object slotContext) {
        if (slotContext == null) {
            return null;
        }
        try {
            Method method = curiosIdentifierMethod;
            if (method == null || !method.getDeclaringClass().isInstance(slotContext)) {
                method = slotContext.getClass().getMethod("identifier");
                curiosIdentifierMethod = method;
            }
            return method.invoke(slotContext) instanceof String identifier ? identifier : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
