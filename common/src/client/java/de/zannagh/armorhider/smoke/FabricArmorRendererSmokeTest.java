// Regression test for issue #348: armor drawn through Fabric API's ArmorRenderer (fabric-rendering-v1)
// ignored every Armor Hider setting. Fabric API cancels HumanoidArmorLayer.renderArmorPiece at HEAD to
// draw the mod's own model, and two same-priority HEAD injections have no guaranteed order - so Armor
// Hider's own hook could be skipped entirely. The compat mixins pin that ordering with an explicit mixin
// priority; this asserts they actually take ownership of such a piece, hide it, and fade it.
//
// Mod-agnostic on purpose: it searches the item registry for whatever item currently has a custom
// ArmorRenderer registered (Nycto supplies one on the rows that pin nycto.version) and self-skips when
// none is present, so it stays green on compat=none rows and on versions Nycto has no build for.
//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderStateImpl;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.compat.FabricArmorRendererCompat;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Fabric API {@code ArmorRenderer} compat smoke.
 * <p>
 * Three phases against the same worn piece:
 * <ol>
 *   <li><b>Opaque control</b> - every slot at full opacity. The compat must still recognise the piece as
 *       Fabric-rendered (the bracket counter climbs) while cancelling nothing.</li>
 *   <li><b>Hidden</b> - every slot at zero opacity. The hide counter must climb, which can only happen if
 *       the compat's HEAD callback ran <i>ahead of</i> Fabric API's dispatch. Before the fix it was a
 *       coin flip decided by mixin application order, and in the reported instance Fabric API won: at the
 *       first attempted priority this phase measured a flat 0 because the worn slot never reached the
 *       hook at all.</li>
 *   <li><b>Faded</b> - half opacity. The mod's own draw must reach a translucent armor type; nothing else
 *       is worn, so the only possible source of that swap is the compat firing inside the mod's
 *       renderer.</li>
 * </ol>
 * Finally the armor-piece scope must not have leaked once: the bracket Fabric API's cancel skips out of
 * has to be closed elsewhere, or a hide-scope bleeds alpha 0 onto later submits.
 */
public final class FabricArmorRendererSmokeTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Fabric ArmorRenderer compat smoke starting");
        // Guard before touching anything Fabric-API-shaped: rows built with compat=none have no
        // fabric-rendering-v1 at all.
        if (!CompatManager.requiresCompatTo(CompatFlags.FABRIC_ARMOR_RENDERER)) {
            ArmorHider.LOGGER.info("[smoke/fcgt] Fabric ArmorRenderer smoke skipped: fabric-rendering-v1 absent");
            return;
        }
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    state.setGenerateStructures(false);
                })
                .create()) {

            String worn = context.computeOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                return equipCustomRenderedArmor(player::setItemSlot);
            });
            if (worn.isEmpty()) {
                ArmorHider.LOGGER.info("[smoke/fcgt] Fabric ArmorRenderer smoke skipped: no mod in this run "
                        + "registers an ArmorRenderer for a wearable item (pin nycto.version for this variant "
                        + "to cover it)");
                return;
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] Fabric-rendered armor under test: {}", worn);

            // ── Phase 1: full opacity. Seen, but nothing cancelled. ────────────────────────────────
            context.runOnClient(client -> setAllSlotOpacities(1.0));
            context.waitTicks(20);
            long[] opaque = counterDelta(context);
            context.takeScreenshot("armorhider_fabric_armorrenderer_1_opaque");
            ArmorHider.LOGGER.info("[smoke/fcgt] OPAQUE: bracketed delta = {}, hidden delta = {}",
                    opaque[0], opaque[1]);

            if (opaque[0] <= 0) {
                throw new IllegalStateException("[smoke/fcgt] the compat never saw the Fabric-rendered piece ("
                        + worn + ", bracketed delta " + opaque[0] + ") - FabricArmorRendererLayerMixin's HEAD"
                        + " callback did not run, so nothing downstream can hide or fade it");
            }
            if (opaque[1] != 0) {
                throw new IllegalStateException("[smoke/fcgt] the compat cancelled a piece at full opacity ("
                        + worn + ", hidden delta " + opaque[1] + ") - it must only cancel when the slot is"
                        + " actually configured hidden");
            }

            // ── Phase 2: hidden. The dispatch must be cancelled before Fabric API draws. ───────────
            context.runOnClient(client -> setAllSlotOpacities(0.0));
            context.waitTicks(20);
            long[] hidden = counterDelta(context);
            context.takeScreenshot("armorhider_fabric_armorrenderer_2_hidden");
            ArmorHider.LOGGER.info("[smoke/fcgt] HIDDEN: bracketed delta = {}, hidden delta = {}",
                    hidden[0], hidden[1]);

            if (hidden[1] <= 0) {
                throw new IllegalStateException("[smoke/fcgt] the Fabric-rendered piece (" + worn + ") was not"
                        + " hidden (hidden delta " + hidden[1] + ") while every slot is at zero opacity - Fabric"
                        + " API's dispatch is winning the renderArmorPiece HEAD race again; check the mixin"
                        + " priority on FabricArmorRendererLayerMixin");
            }

            // ── Phase 3: half opacity. The mod's own draw must land on a translucent armor type. ──
            //    Nothing but this piece is worn, so any climb in the translucent-armor path counters can
            //    only come from the compat's armorCutoutNoCull swap firing inside the mod's renderer.
            long translucentBefore = context.computeOnClient(client -> translucentArmorTypes());
            context.runOnClient(client -> setAllSlotOpacities(0.5));
            context.waitTicks(20);
            long translucent = context.computeOnClient(client -> translucentArmorTypes()) - translucentBefore;
            context.takeScreenshot("armorhider_fabric_armorrenderer_3_faded");
            ArmorHider.LOGGER.info("[smoke/fcgt] FADED: translucent armor-type delta = {}", translucent);

            if (translucent <= 0) {
                throw new IllegalStateException("[smoke/fcgt] the Fabric-rendered piece (" + worn + ") never"
                        + " reached a translucent armor type at 50% opacity (delta " + translucent + ") - the"
                        + " FabricArmorRendererTypeMixin swap on armorCutoutNoCull is not firing, so the piece"
                        + " stays fully opaque on the alpha-tested cutout type");
            }

            long leaks = context.computeOnClient(client ->
                    AhRenderStateImpl.leakedScopeClears(RenderScope.ARMOR_PIECE));
            if (leaks != 0) {
                throw new IllegalStateException("[smoke/fcgt] armor-piece scope leaked " + leaks + " time(s) -"
                        + " the compat bracket is not closing on Fabric API's cancelling return, and a hide"
                        + " scope left open bleeds alpha 0 onto later submits");
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] Fabric ArmorRenderer compat smoke passed (item={}, opaque={}/{},"
                    + " hidden={}/{}, translucent={})", worn, opaque[0], opaque[1], hidden[0], hidden[1], translucent);
        }
    }

    /** Accepts an equip action so the search can stay free of a direct player reference. */
    private interface Equipper {
        void equip(EquipmentSlot slot, ItemStack stack);
    }

    /**
     * Find any item that currently has a Fabric {@code ArmorRenderer} registered and can be worn, equip it,
     * and return its id (empty when the run has no such mod).
     */
    private static String equipCustomRenderedArmor(Equipper equipper) {
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!FabricArmorRendererCompat.hasCustomRenderer(stack)) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            EquipmentSlot slot = id == null ? null : armorSlotFor(id.getPath());
            if (slot == null) {
                continue;
            }
            equipper.equip(slot, stack);
            return id + " (" + slot + ")";
        }
        return "";
    }

    /**
     * Slot from the item id's suffix. A deliberate heuristic rather than an equipment-component lookup:
     * that component moved twice across the versions this test compiles on, while the naming convention
     * for armor items has not.
     */
    private static @Nullable EquipmentSlot armorSlotFor(String path) {
        if (path.endsWith("helmet")) {
            return EquipmentSlot.HEAD;
        }
        if (path.endsWith("chestplate")) {
            return EquipmentSlot.CHEST;
        }
        if (path.endsWith("leggings")) {
            return EquipmentSlot.LEGS;
        }
        if (path.endsWith("boots")) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    private static void setAllSlotOpacities(double opacity) {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
        // A persisted disableArmorHider (from a prior run or a keybind) renders everything vanilla.
        config.disableArmorHider.setValue(false);
        config.helmetOpacity.setValue(opacity);
        config.chestOpacity.setValue(opacity);
        config.legsOpacity.setValue(opacity);
        config.bootsOpacity.setValue(opacity);
    }

    /** Every route translucentArmor can take, so the assertion holds with or without a shaderpack. */
    private static long translucentArmorTypes() {
        return ArmorHiderRenderTypes.armorNoDepthPathCount()
                + ArmorHiderRenderTypes.armorDepthPathCount()
                + ArmorHiderRenderTypes.armorDitherPathCount();
    }

    /** @return {@code [bracketed, hidden]} deltas measured across ten rendered ticks. */
    private static long[] counterDelta(ClientGameTestContext context) {
        long bracketedBefore = context.computeOnClient(client -> FabricArmorRendererCompat.bracketedPieceCount());
        long hiddenBefore = context.computeOnClient(client -> FabricArmorRendererCompat.hiddenPieceCount());
        context.waitTicks(10);
        long bracketedAfter = context.computeOnClient(client -> FabricArmorRendererCompat.bracketedPieceCount());
        long hiddenAfter = context.computeOnClient(client -> FabricArmorRendererCompat.hiddenPieceCount());
        return new long[]{bracketedAfter - bracketedBefore, hiddenAfter - hiddenBefore};
    }
}
//?}
