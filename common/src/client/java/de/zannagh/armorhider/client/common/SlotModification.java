package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.configuration.items.ArmorOpacity;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the modifications that should be applied for a specific slot.
 * @param slot
 * @param shouldHide
 * @param shouldDisableGlint
 * @param transparency
 * @since 0.12.0
 */
public record SlotModification(
        EquipmentSlot slot,
        boolean needsModification,
        boolean shouldHide,
        boolean shouldDisableGlint,
        double transparency,
        String playerName,
        PlayerConfig config,
        ItemInfo itemInfo
) {

    /**
     * Returns the slot this modification applies to.
     * @return the slot this modification applies to, e.g. {@link EquipmentSlot#MAINHAND} or {@link EquipmentSlot#OFFHAND}
     */
    public EquipmentSlot slot() { return slot; }

    /**
     * Whether this piece is actually being made translucent (its opacity is below ~100%), as opposed
     * to {@link #needsModification()} which is also true when only the glint is toggled off at full
     * opacity. Only genuine translucency should route a piece onto the depth-write-disabled translucent
     * render pipeline: a fully-opaque piece with its glint merely disabled must stay on the normal,
     * depth-writing armor type, or it reads as see-through under shaders (Iris) against bright light or
     * water even though the user set 100% opacity. Glint suppression itself is handled separately via
     * {@link #shouldDisableGlint()} / {@code getHasFoil}, so decoupling the two here does not bring the
     * glint back.
     *
     * @return {@code true} when {@code transparency < ~1} (the piece is genuinely faded).
     */
    public boolean needsTranslucency() {
        return transparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2;
    }

    public static boolean shouldUseVanilla(PlayerConfig config){
        var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;

        // Server-wide force-off is the final guard - it overrides everything for every player, including the
        // local one (the server always has the last say).
        var serverConfig = manager.getServerConfig();
        if (serverConfig != null && serverConfig.serverWideSettings.forceArmorHiderOff.getValue()) {
            return true;
        }

        // Identify the local player by INSTANCE identity, not by name: resolveConfig() returns the exact
        // local config instance for the viewer and a distinct copy/override for everyone else, so identity is
        // drift-proof. A name-only check compares the local config's name (snapshotted at join) against the
        // live display name (getCurrentPlayerName()), which can diverge on servers that rewrite the display
        // name after join (rank prefixes, nicks - e.g. Hypixel) and would then vanilla-out the viewer's OWN
        // armor. The name check is kept as an OR so this can only ever exempt the local player, never add one.
        boolean isLocalPlayer = config == manager.getLocalPlayerConfig()
                || config.playerName.getValue().equals(ArmorHiderClient.getCurrentPlayerName());

        // "Disable Armor Hider" master switch. For the local player this reads the effective state - the
        // transient keybind override if one is active, otherwise the persisted setting - so the toggle key
        // takes effect without touching disk. For everyone else the flag on their own resolved config applies.
        // When set it trumps the opacity sliders and renders vanilla.
        boolean disableArmorHider = isLocalPlayer
                ? manager.isLocalArmorHiderDisabledEffective()
                : config.disableArmorHider.getValue();
        if (disableArmorHider || config.playerName.getValue().isBlank()) {
            return true;
        }

        // "Disable Armor Hider on Others" renders every other player vanilla, but only where client-side
        // other-player configuration is permitted (no mod server, or a mod server that allows it). It is a
        // viewer-local preference, so it is read from the local config rather than the rendered player's.
        if (!isLocalPlayer && manager.areOtherPlayerConfigsAllowed() && manager.isArmorHiderDisableForOthers()) {
            return true;
        }

        return false;
    }

    /**
     * Creates an empty slot modification.
     * @return An empty slot modification.
     */
    public static SlotModification empty(){
        return new SlotModification(EquipmentSlot.MAINHAND, false, false, false, 1.0, "", PlayerConfig.empty(), ItemInfo.empty());
    }

    public static SlotModification empty(EquipmentSlot slot){
        return new SlotModification(slot, false, false, false, 1.0, "", PlayerConfig.empty(), ItemInfo.empty());
    }

    public boolean isEmpty(){
        return playerName.isBlank();
    }

    public static boolean isEmpty(SlotModification modification) {
        return modification.playerName.isBlank();
    }

    public static SlotModification of(String playerName, EquipmentSlot slot, ItemStack itemStack) {
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);
        // Carry the *live* name through: combat events are registered under the player's current
        // display name, so anything that later uses this modification as a combat key must not fall
        // back to the config's snapshot. See the identity note in of(config, slot, liveName).
        return of(config, slot, playerName).addItemInformation(new ItemInfo(itemStack));
    }

    public static SlotModification of(PlayerConfig config, EquipmentSlot slot) {
        return of(config, slot, null);
    }

    /**
     * @param liveName the player's current display name if the caller knows it, else {@code null} to
     *                 fall back to the name stored on the config.
     */
    private static SlotModification of(PlayerConfig config, EquipmentSlot slot, @Nullable String liveName) {

        if (shouldUseVanilla(config)) {
            return empty(slot);
        }

        // Combat state is keyed by the display name the combat event was registered under
        // (AhCombatApiImpl -> PlayerNameUtil#getPlayerName), which is the player's *live* name. The
        // name persisted on the config is snapshotted at join and can drift on servers that rewrite
        // display names afterwards (rank prefixes, nicks - the same drift shouldUseVanilla guards
        // against). Keying combat off the stale snapshot would silently miss every combat event, so
        // prefer the live name whenever the caller has it. This name is also what the resulting
        // record carries, because downstream combat consumers (EquipmentRenderMixin's vanilla-model
        // check, VanillaArmorTextureManager) use playerName() as a combat key too.
        String resolvedName = liveName != null && !liveName.isBlank()
                ? liveName
                : config.playerName.getValue();

        var transparency = switch (slot) {
            case HEAD -> config.helmetOpacity.getValue();
            case CHEST -> config.chestOpacity.getValue();
            case LEGS -> config.legsOpacity.getValue();
            case FEET -> config.bootsOpacity.getValue();
            case OFFHAND -> config.offHandOpacity.getValue();
            default -> 1.0;
        };

        // Combat detection: a player who just took or dealt damage snaps to fully-visible armor and
        // then fades back to their configured opacity over the combat window. This is applied here,
        // at the single point every render path resolves its opacity through, so armor, elytra, hand
        // items and compat layers (Female Gender Mod breast armor, GeckoLib, ...) all follow it.
        //
        // Raising the transparency before shouldHide/needsModification are derived is deliberate:
        // an in-combat piece configured to 0% must stop being *hidden*, not merely become opaque.
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.shouldApplyCombatDetectionTo(config)) {
            transparency = CombatManager.transformTransparencyBasedOnCombat(resolvedName, transparency);
        }

        boolean disableGlint = switch (slot) {
            case HEAD -> !config.helmetGlint.getValue();
            case CHEST -> !config.chestGlint.getValue();
            case LEGS -> !config.legsGlint.getValue();
            case FEET -> !config.bootsGlint.getValue();
            default -> false;
        };


        boolean shouldHideEntirely = transparency < ArmorOpacity.TRANSPARENCY_STEP;

        boolean needsModification = (transparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2) || disableGlint;
        // itemInfo is filled in by addItemInformation(...) for the call sites that have
        // the stack on hand. Initialise to ItemInfo.empty() rather than null so callers
        // (e.g. RenderModifications.modifyRenderPriority -> itemInfo.isElytra()) that
        // read it before addItemInformation runs don't trip an NPE.
        return new SlotModification(slot, needsModification, shouldHideEntirely, disableGlint, transparency, resolvedName, config, ItemInfo.empty());
    }

    public SlotModification addItemInformation(ItemInfo itemInfo) {
        return addItemInformation(itemInfo.getStack());
    }

    /**
     * Adds additional item information to the slot modification in order to respect for example affecting skulls or elytra.
     * @param item The item to add information for
     * @return The modified slot modification
     */
    public SlotModification addItemInformation(@Nullable ItemStack item) {
        ItemStack resolvedItem = item != null ? item : ItemStack.EMPTY;
        ItemInfo resolvedItemInfo = new ItemInfo(resolvedItem);
        if (!resolvedItemInfo.isEmpty()) {
            var exclusionConfig = config.getExclusionItems();
            exclusionConfig.discoverItem(slot, resolvedItem.getItem(), resolvedItem.getHoverName().getString());

            if (exclusionConfig.shouldArmorHiderIgnore(slot, resolvedItem.getItem())) {
                return empty(slot);
            }
        }
        if (slot == EquipmentSlot.HEAD
                && resolvedItemInfo.isVanillaSkullItem()
                && !config.opacityAffectingHatOrSkull.getValue()) {
            return empty(slot);
        }

        if (slot == EquipmentSlot.CHEST
                && resolvedItemInfo.isElytra()
                && !resolvedItemInfo.isArmoredElytra()) {
            return elytraModification(resolvedItemInfo);
        }

        return new SlotModification(slot, needsModification, shouldHide, shouldDisableGlint, transparency, playerName, config, resolvedItemInfo);
    }

    /**
     * Builds the modification for an elytra worn in the chest slot. Since AH 0.12.14 the elytra is
     * decoupled from the chestplate: it follows its own {@link PlayerConfig#elytraOpacity} and
     * {@link PlayerConfig#elytraGlint} rather than the chest slider (the "chest opacity affects elytra"
     * toggle is gone). Combat detection is applied the same way {@link #of} applies it to every other
     * slot. Returns an empty (no-op) modification when nothing needs changing so the elytra render
     * scope is left untouched and the wings render vanilla.
     */
    private SlotModification elytraModification(ItemInfo elytraInfo) {
        double elytraTransparency = config.elytraOpacity.getValue();
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.shouldApplyCombatDetectionTo(config)) {
            elytraTransparency = CombatManager.transformTransparencyBasedOnCombat(playerName, elytraTransparency);
        }
        boolean disableGlint = !config.elytraGlint.getValue();
        boolean hideEntirely = elytraTransparency < ArmorOpacity.TRANSPARENCY_STEP;
        boolean needsMod = (elytraTransparency < 1 - ArmorOpacity.TRANSPARENCY_STEP / 2) || disableGlint;
        if (!needsMod) {
            return empty(slot);
        }
        return new SlotModification(slot, true, hideEntirely, disableGlint, elytraTransparency, playerName, config, elytraInfo);
    }

    public static boolean isSlotModified(@NotNull String playerName, @NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        var mod = of(playerName, slot, item);
        return mod.needsModification();
    }
}
