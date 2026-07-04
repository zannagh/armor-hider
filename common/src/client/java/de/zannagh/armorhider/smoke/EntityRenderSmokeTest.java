//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.impl.AhRenderStateImpl;
import de.zannagh.armorhider.client.common.RenderScope;
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
 * with a full diamond armor set + offhand shield (so every render scope this mod intercepts —
 * helmet / chest / legs / feet / cape / offhand — actually fires), switches to third-person
 * back camera so layer mixins run, and waits long enough for several render frames to
 * complete. FCGT exits the client cleanly when this method returns.
 * <p>
 * Pass = no exception thrown during world creation or rendering. Catches render-pipeline
 * crashes (mixin apply at runtime, NPE in interceptors, scope state-machine bugs) that the
 * boot-only Phase 1 test can't reach because the title screen never submits an entity.
 * <p>
 * Does <b>not</b> validate render correctness (transparency value, glint visibility, color
 * blend). For that we'd need screenshot diffing against a baseline — see
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

        // Default `worldBuilder().create()` generates a survival world with normal terrain,
        // which spends 2+ minutes on spawn-chunk generation before the render window opens.
        // For smoke purposes we just need an entity in a world — turn on FCGT's consistent-
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
                // 0% chest opacity below this exercises the elytra *hide* path — the one that leaked
                // its scope and turned every later model submit (skull/offhand) invisible.
                player.setItemSlot(EquipmentSlot.CHEST,   new ItemStack(Items.ELYTRA));
                player.setItemSlot(EquipmentSlot.LEGS,    new ItemStack(Items.DIAMOND_LEGGINGS));
                player.setItemSlot(EquipmentSlot.FEET,    new ItemStack(Items.DIAMOND_BOOTS));
                player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

                // Third-person back so cape + body layer mixins fire (first-person skips most layers).
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .getConfigForPlayer(ArmorHiderClient.getCurrentPlayerName());
                // 50% helmet opacity → ARMOR_PIECE must enter with a real modification (asserted below).
                config.helmetOpacity.setValue(0.5);
                // 0% chest opacity → elytra is fully hidden, driving the cancel-at-HEAD path.
                config.chestOpacity.setValue(0.0);
            });

            // 20 ticks ≈ 1 s @ 20 TPS — enough for the render pipeline to draw several frames
            // covering every layer mixin. We're only checking "doesn't crash"; correctness
            // verification would need screenshot diffing (out of scope, see scripts/README.md).
            context.waitTicks(20);

            // The render hooks fail *silently* when injection targets drift between MC
            // versions (see NeoForge 1.21.4–1.21.8 pipeline regression) — assert the
            // interception actually fired instead of only checking "didn't crash".
            context.runOnClient(client -> {
                long entries = AhRenderStateImpl.modifiedScopeEnterCount(RenderScope.ARMOR_PIECE);
                if (entries == 0) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] ARMOR_PIECE scope never entered with a modification"
                                    + " — the render interception pipeline is dead on this version");
                }
                ArmorHider.LOGGER.info("[smoke/fcgt] ARMOR_PIECE modified scope entries: {}", entries);

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
                                    + " clear) — a cancelled render path entered a scope:" + leaks);
                }
            });

            ArmorHider.LOGGER.info("[smoke/fcgt] Render window elapsed without crash, returning");
        }
    }
}
//?}
