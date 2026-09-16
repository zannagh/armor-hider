//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderStateImpl;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.AhArmProbe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Phase 2 boot+render smoke (fabric-client-gametest-api-v1).
 * <p>
 * Drives the client past the title screen into a fresh singleplayer world, equips the player
 * with a full diamond armor set + offhand shield (so every render scope this mod intercepts -
 * helmet / chest / legs / feet / cape / offhand - actually fires), switches to third-person
 * back camera so layer mixins run, and waits long enough for several render frames to
 * complete. FCGT exits the client cleanly when this method returns.
 * <p>
 * Pass = no exception thrown during world creation or rendering. Catches render-pipeline
 * crashes (mixin apply at runtime, NPE in interceptors, scope state-machine bugs) that the
 * boot-only Phase 1 test can't reach because the title screen never submits an entity.
 * <p>
 * Does <b>not</b> validate render correctness (transparency value, glint visibility, color
 * blend). For that we'd need screenshot diffing against a baseline - see
 * {@code scripts/README.md} for the rationale on why we don't.
 * <p>
 * Stonecutter-gated to the {@code fcgt} constant (Fabric variants with
 * {@code fabricapi.semver} pinned). Currently only fabric-26.2; replicate to other Fabric
 * 1.21+ variants by pinning {@code fabricapi.semver} in their stonecutter block and
 * verifying the FCGT API surface compiles against that fabric-api version.
 */
public final class EntityRenderSmokeTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Entry render smoke starting");
        // Hold here until the title screen is ready to take input.
        context.waitForScreen(TitleScreen.class);
        AhArmProbe.enable();

        // Default `worldBuilder().create()` generates a survival world with normal terrain,
        // which spends 2+ minutes on spawn-chunk generation before the render window opens.
        // For smoke purposes we just need an entity in a world - turn on FCGT's consistent-
        // settings flag (skips the random-seed dance), force creative, drop structure gen.
        // Cuts world creation from ~2 minutes to a handful of seconds.
        try (var singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] Singleplayer world created, equipping player");
            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                // Drive every slot through the interceptors. Diamond chosen for trim-capable + iconic glint.
                player.setItemSlot(EquipmentSlot.HEAD,    new ItemStack(Items.DIAMOND_HELMET));
                // Elytra (not a chestplate) so the WingsLayer interceptor fires; combined with a
                // 0% chest opacity below this exercises the elytra *hide* path - the one that leaked
                // its scope and turned every later model submit (skull/offhand) invisible.
                player.setItemSlot(EquipmentSlot.CHEST,   new ItemStack(Items.ELYTRA));
                player.setItemSlot(EquipmentSlot.LEGS,    new ItemStack(Items.DIAMOND_LEGGINGS));
                player.setItemSlot(EquipmentSlot.FEET,    new ItemStack(Items.DIAMOND_BOOTS));
                player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

                // Third-person back so cape + body layer mixins fire (first-person skips most layers).
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                // 50% helmet opacity → ARMOR_PIECE must enter with a real modification (asserted below).
                config.helmetOpacity.setValue(0.5);
                // Elytra has its own slider. Capture it fully visible first (important for EMF/FA
                // custom wing models), then hide and restore it below.
                config.elytraOpacity.setValue(1.0);
            });

            context.waitTicks(10);
            context.takeScreenshot("armorhider_elytra_visible_100");
            context.runOnClient(client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName())
                    .elytraOpacity.setValue(0.0));
            context.waitTicks(10);
            context.runOnClient(client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName())
                    .elytraOpacity.setValue(0.5));
            context.waitTicks(10);
            context.takeScreenshot("armorhider_elytra_restored_50");

            // The render hooks fail *silently* when injection targets drift between MC
            // versions (see NeoForge 1.21.4–1.21.8 pipeline regression) - assert the
            // interception actually fired instead of only checking "didn't crash".
            context.runOnClient(client -> {
                long entries = AhRenderStateImpl.modifiedScopeEnterCount(RenderScope.ARMOR_PIECE);
                if (entries == 0) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] ARMOR_PIECE scope never entered with a modification"
                                    + " - the render interception pipeline is dead on this version");
                }
                ArmorHider.LOGGER.info("[smoke/fcgt] ARMOR_PIECE modified scope entries: {}", entries);

                long elytraEntries = AhRenderStateImpl.modifiedScopeEnterCount(RenderScope.ELYTRA);
                // The elytra scope is only entered when *we* drive the wings render. Two compat mods
                // legitimately take that over, so a zero count with either present is correct behaviour,
                // not a dead pipeline:
                //   - Armored Elytra replaces the vanilla wings submit outright (its own smoke owns it);
                //   - ElytraTrims drives the elytra's appearance through its own render pipeline, so
                //     ArmorHiderElytraRenderer deliberately does NOT enter the scope for a non-hidden
                //     elytra (entering it would leak our modification into ET's submissions and
                //     reintroduce the trim regressions the ET branch was added to fix).
                // This assertion therefore targets the vanilla/EMF wings path only. Verified on a real
                // GPU: with ElytraTrims active the scope is never entered on 1.21.8+, exactly as the ET
                // branch intends, while the compat=none rows enter it on every version.
                boolean elytraOwnedByCompat = CompatManager.requiresCompatTo(CompatFlags.ARMORED_ELYTRA)
                        || CompatManager.requiresCompatTo(CompatFlags.ELYTRA_TRIMS);
                if (elytraEntries == 0 && !elytraOwnedByCompat) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] ELYTRA scope never resumed after being restored");
                }
                ArmorHider.LOGGER.info("[smoke/fcgt] ELYTRA modified scope entries: {}", elytraEntries);

                if (CompatManager.requiresCompatTo(CompatFlags.ENTITY_MODEL_FEATURES)) {
                    // Since PR #362 (issue #360) EquipmentRenderMixin.armorHider$vanillaEquipmentModel KEEPS
                    // EMF's custom humanoid/elytra armor model on a translucent piece - swapping in vanilla
                    // geometry while the pack's custom-UV texture is bound produced offset texels - so the
                    // vanilla-geometry fallback must never fire with EMF loaded.
                    long fallbacks = AhArmProbe.equipmentFallbackCount();
                    if (fallbacks > 0) {
                        throw new IllegalStateException("[smoke/fcgt] EMF custom armor model must be kept"
                                + " (#360/#362), but the vanilla-geometry fallback fired " + fallbacks + " times");
                    }
                    // The positive counterpart (EMF's model actually reached the translucent submit and was
                    // kept) needs an EMF-wrapped armor model, i.e. a custom CEM armor pack: with EMF 3.3 and
                    // no pack the armor roots stay vanilla and the branch is never reached even on a real GPU
                    // (measured: 0 here on hardware). EmfCustomArmorTranslucencySmokeTest stages the Glowing
                    // 3D Armor pack and asserts that signal; here it is informational only.
                    ArmorHider.LOGGER.info("[smoke/fcgt] EMF present: vanilla-geometry fallbacks {} (must be 0),"
                            + " EMF armor model kept {} times", fallbacks, AhArmProbe.emfModelKeptCount());
                }

                // No scope may be left active for a bulk clear to sweep up: that means it was entered
                // on a render path cancelled before its exit hook ran (e.g. elytra hidden at 0%), and
                // a leaked hide-scope bleeds alpha 0 onto later model submits (invisible skull/offhand).
                // The elytra above is hidden, so pre-fix this counter climbs every entity render.
                StringBuilder leaks = new StringBuilder();
                for (RenderScope scope : RenderScope.values()) {
                    long leaked = AhRenderStateImpl.leakedScopeClears(scope);
                    if (leaked > 0) {
                        leaks.append(' ').append(scope).append('=').append(leaked);
                    }
                }
                if (leaks.length() > 0) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] render scope(s) leaked (entered but never exited, swept by a bulk"
                                    + " clear) - a cancelled render path entered a scope:" + leaks);
                }
            });

            ArmorHider.LOGGER.info("[smoke/fcgt] Render window elapsed without crash, returning");
        }
    }
}
//?}
