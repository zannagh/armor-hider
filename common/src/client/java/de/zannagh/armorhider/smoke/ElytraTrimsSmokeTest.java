// Verifies the whole point of the ElytraTrims compat branch: a worn elytra's ET decorations follow the
// configured elytra transparency. Gated to exactly the range where ETElytraTrimSubmitMixin exists.
//? if fcgt && >= 1.21.9 && < 26.3-0.snapshot.2 {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

import java.nio.file.Path;

/**
 * ElytraTrims transparency smoke (fabric-client-gametest-api-v1).
 * <p>
 * The reason this branch exists: with ElytraTrims (ET) present, ET's custom elytra decorations must
 * follow the player's configured elytra opacity, not just the coarse show/hide the mod falls back to
 * without the {@link de.zannagh.armorhider.client.mixin.compat.elytratrims.ETElytraTrimSubmitMixin}
 * hook. This test wears an elytra carrying a vanilla armor trim - ElytraTrims' core case, which ET
 * renders on the elytra through the submit helper our wrap targets - fades the elytra to 50%, and
 * asserts the version-appropriate behaviour.
 * <p>
 * Machine-checked, per this repo's smoke convention (assert the mixin fired, don't just "not crash"),
 * using two counters: {@link ArmorHiderRenderTypes#elytraTrimSeenCount()} (ET drew a trim through our
 * wrap) and {@link ArmorHiderRenderTypes#elytraTrimFadeCount()} (we alpha-scaled it). At 50% opacity
 * SEEN must climb on every version; FADE must climb on {@code >= 1.21.11} (ET draws translucent, so the
 * trim fades in lockstep) but must stay flat on {@code < 1.21.11} (ET draws cutout, which can't fade -
 * the policy there is untouched at 5-100%, hidden only at 0%). FADE must stay flat at 100% everywhere.
 * Screenshots are captured for human eyeballing.
 * <p>
 * Self-skips (no fail) when ET isn't present at runtime - detected via {@link CompatManager}, so run it
 * with {@code -Pcompat=elytratrims}. Pure vanilla API otherwise, so it doesn't need ET on the classpath.
 * Gated to {@code >= 1.21.9 && < 26.3-0.snapshot.2}, matching the mixin it exercises.
 */
public final class ElytraTrimsSmokeTest implements FabricClientGameTest {

    private static final float CAMERA_PITCH = 2.0F;
    private static final double PLAYER_Y = 100.0;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] ElytraTrims transparency smoke starting");
        context.waitForScreen(TitleScreen.class);

        boolean etPresent = CompatManager.requiresCompatTo(CompatFlags.ELYTRA_TRIMS);
        if (!etPresent) {
            // ET not fetched into run/mods - nothing to exercise. Skip loudly rather than fail, and do
            // NOT touch ETItemFlag below (keeps its class off the loader when ET is absent).
            ArmorHider.LOGGER.warn("[smoke/fcgt] ElytraTrims not present - skipping (run with -Pcompat=elytratrims)");
            return;
        }

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] World created; equipping ET-decorated elytra");

            var server = singleplayer.getServer();
            server.runOnServer(mcServer -> {
                var sp = mcServer.getPlayerList().getPlayers().get(0);
                var abilities = sp.getAbilities();
                abilities.mayfly = true;
                abilities.flying = true;
                sp.onUpdateAbilities();
                sp.setNoGravity(true);
                sp.connection.teleport(0.5, PLAYER_Y, 0.5, 0.0F, CAMERA_PITCH);
            });

            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                ItemStack elytra = new ItemStack(Items.ELYTRA);
                // A vanilla armor trim is ElytraTrims' core case: ET renders the trim on the elytra through
                // its submit helper (the one our wrap targets). Emissive decorators like GLOW bypass that
                // helper, so a trim is what actually exercises the fade path. Pure vanilla API - the test
                // needs ET only at runtime (guarded above), not on the classpath.
                var reg = player.registryAccess();
                var material = reg.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.NETHERITE);
                var pattern = reg.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY);
                elytra.set(DataComponents.TRIM, new ArmorTrim(material, pattern));
                player.setItemSlot(EquipmentSlot.CHEST, elytra);
                player.setYRot(0.0F);
                player.setXRot(CAMERA_PITCH);
                player.setYHeadRot(0.0F);
                player.getAbilities().flying = true;
                player.setNoGravity(true);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.opacityAffectingElytra.setValue(true);
                // The elytra flight short-circuit (ArmorHiderElytraRenderer) keys off creative-fly too, so
                // turn it off here or the hovering player would keep the elytra fully visible in "flight".
                config.elytraInFlight.setValue(false);
                config.elytraOpacity.setValue(0.5);
            });

            // Plain tick-wait for chunks/render to settle - portable across every FCGT API version in
            // this range (the getClientLevel().waitForChunksRender() convenience is 26.2+ only).
            context.waitTicks(60);

            // FADED: 50% elytra opacity. Measure both signals over the window: SEEN (ET drew a trim
            // through our wrap at all) and FADE (we alpha-scaled it). What we assert depends on how ET
            // draws on this version.
            context.runOnClient(ElytraTrimsSmokeTest::snapPose);
            context.waitTicks(3);
            long seenBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimSeenCount());
            long fadeBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimFadeCount());
            context.waitTicks(6);
            Path fadedShot = context.takeScreenshot("armorhider_et_faded_50");
            long seenDelta = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimSeenCount()) - seenBefore;
            long fadeDelta = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimFadeCount()) - fadeBefore;
            ArmorHider.LOGGER.info("[smoke/fcgt] ET faded window: seenDelta={} fadeDelta={} shot {}",
                    seenDelta, fadeDelta, fadedShot);
            if (seenDelta <= 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] ET drew no trim through our wrap (seenDelta " + seenDelta + ") - "
                                + "ETElytraTrimSubmitMixin isn't bound, or the trimmed elytra didn't render "
                                + "(ET absent, or the wrap target drifted)");
            }
            //? if >= 1.21.11 {
            // Translucent era: partial opacity MUST fade ET's trim in lockstep with the wing.
            if (fadeDelta <= 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] ET trim was not faded at 50% opacity (fadeDelta " + fadeDelta
                                + ") - the ELYTRA scope fade path didn't run");
            }
            //? } else {
            /*// Cutout era (1.21.9/1.21.10): ET's trims can't be alpha-blended, so partial opacity must
            // leave the elytra untouched (full show; only 0% hides). We must NOT have faded it.
            if (fadeDelta != 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] ET trim was faded at 50% on a cutout version (fadeDelta " + fadeDelta
                                + ") - partial transparency must be a no-op there, not a fade");
            }
            *///?}

            // CONTROL: full opacity - the wrap sees the trim but must never fade it, on any version.
            context.runOnClient(client -> {
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.elytraOpacity.setValue(1.0);
                snapPose(client);
            });
            context.waitTicks(3);
            long fullFadeBefore = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimFadeCount());
            context.waitTicks(6);
            Path fullShot = context.takeScreenshot("armorhider_et_full_100");
            long fullFadeDelta = context.computeOnClient(client -> ArmorHiderRenderTypes.elytraTrimFadeCount()) - fullFadeBefore;
            ArmorHider.LOGGER.info("[smoke/fcgt] ET full-opacity window: fadeDelta={} shot {}", fullFadeDelta, fullShot);
            if (fullFadeDelta != 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] ET trim was faded at 100% opacity (fadeDelta " + fullFadeDelta
                                + ") - the trim must render untouched at full opacity");
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] ElytraTrims transparency smoke complete");
        }
    }

    private static void snapPose(Minecraft client) {
        var player = client.player;
        if (player == null) {
            return;
        }
        player.setPos(0.5, PLAYER_Y, 0.5);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.setYRot(0.0F);
        player.setXRot(CAMERA_PITCH);
        player.setYHeadRot(0.0F);
        player.setYBodyRot(0.0F);
        player.getAbilities().flying = true;
        player.setNoGravity(true);
    }
}
//?}
