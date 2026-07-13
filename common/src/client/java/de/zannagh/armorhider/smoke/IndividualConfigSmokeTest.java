//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.elements.ArmorHiderOptionsPanelWidget;
import de.zannagh.armorhider.client.gui.elements.PlayerHeadBarWidget;
import de.zannagh.armorhider.client.gui.elements.PlayerPreviewWidget;
import de.zannagh.armorhider.client.gui.screens.IndividualPlayerConfigurationsScreen;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.gui.util.PlayerFaceTextures;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.server.ServerConfiguration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * Phase 2 smoke for the client-side per-player configuration feature (issue #259).
 * <p>
 * Boots into a singleplayer world, then runs three checks entirely on the client thread:
 * <ol>
 *   <li><b>Resolver</b> — with a mod-aware server config that allows individual configs, a stored override
 *       for a player name is what {@code getConfigForPlayer(name)} returns; when the server disallows it, the
 *       override is ignored; and the local player is never overridden.</li>
 *   <li><b>Screen layout</b> — {@link IndividualPlayerConfigurationsScreen} initializes without crashing and
 *       always produces a functional layout (at minimum a Done button; a notice when no peers are online).</li>
 *   <li><b>Widget render</b> — the version-gated {@link PlayerHeadBarWidget} (face blit + scissor),
 *       {@link PlayerPreviewWidget} (arbitrary entity) and the per-player {@link ArmorHiderOptionsPanelWidget}
 *       are laid out and rendered for several frames without throwing.</li>
 * </ol>
 * Runtime rendering can't be validated for correctness (no screenshot baselines — see scripts/README.md);
 * this catches init/layout crashes and dead render paths that compilation can't.
 */
public final class IndividualConfigSmokeTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Individual-config smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (var singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] World created, running individual-config checks");

            context.runOnClient(IndividualConfigSmokeTest::assertResolverAppliesOverrides);
            context.runOnClient(IndividualConfigSmokeTest::assertGlobalOverrideResolution);

            // Main options screen: builds the compound button row (combat/vanilla/invisibility + the
            // individual-settings entry + presets). Opening it exercises createCompoundButtonWidget, which
            // is otherwise not touched by the per-player screen's toggles-only row.
            context.runOnClient(client -> {
                var optionsScreen = new de.zannagh.armorhider.client.gui.screens.ArmorHiderOptionsScreen(
                        null, client.options);
                Minecraft.getInstance().setScreenAndShow(optionsScreen);
                assertScreenHasDoneButton(optionsScreen);
            });
            context.waitTicks(5);

            // Screen: open the real per-player screen and assert its layout initialized.
            // (setScreenAndShow is the version-portable navigation call used everywhere in the mod; it
            // downgrades to setScreen below 1.21.9. Its init() runs synchronously so children() is ready.)
            context.runOnClient(client -> {
                var screen = new IndividualPlayerConfigurationsScreen(
                        null, client.options, Component.translatable("armorhider.individual.title"));
                Minecraft.getInstance().setScreenAndShow(screen);
                assertScreenHasDoneButton(screen);
            });
            context.waitTicks(10);

            // Global tab under gated states: "Others: Vanilla" collapses rows B/C to disabled buttons, and a
            // server force-off collapses all three — render both so the gated path is exercised, not just the
            // normal two-button choice.
            context.runOnClient(client -> {
                var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;
                boolean prior = manager.isArmorHiderDisableForOthers();
                manager.setArmorHiderDisabledForOthersTo(true, Optional.of(true));
                var screen = new IndividualPlayerConfigurationsScreen(
                        null, client.options, Component.translatable("armorhider.individual.title"));
                Minecraft.getInstance().setScreenAndShow(screen);
                assertScreenHasDoneButton(screen);
                manager.setArmorHiderDisabledForOthersTo(prior, Optional.of(true));
            });
            context.waitTicks(5);

            context.runOnClient(client -> {
                var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;
                var serverConfig = new ServerConfiguration();
                serverConfig.serverWideSettings.forceArmorHiderOff.setValue(true);
                manager.setServerConfig(serverConfig);
                var screen = new IndividualPlayerConfigurationsScreen(
                        null, client.options, Component.translatable("armorhider.individual.title"));
                Minecraft.getInstance().setScreenAndShow(screen);
                assertScreenHasDoneButton(screen);
                manager.clearServerConfig();
            });
            context.waitTicks(5);

            // Widgets: render the version-gated head bar / preview / per-player panel for several frames.
            context.runOnClient(client -> {
                var player = client.player;
                if (player == null || client.getConnection() == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player/connection unavailable for render probe");
                }
                var localInfo = client.getConnection().getPlayerInfo(player.getUUID());
                if (localInfo == null) {
                    throw new IllegalStateException("[smoke/fcgt] Local PlayerInfo unavailable for face render probe");
                }
                Minecraft.getInstance().setScreenAndShow(new WidgetProbeScreen(localInfo, player.getUUID()));
            });
            context.waitTicks(10);

            ArmorHider.LOGGER.info("[smoke/fcgt] Individual-config checks passed");
        }
    }

    private static void assertResolverAppliesOverrides(Minecraft client) {
        var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;
        PlayerConfig localConfig = manager.getLocalPlayerConfig();

        // Deterministic mod-aware server config so the test doesn't depend on the integrated server timing.
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.setValue(true);
        manager.setServerConfig(serverConfig);

        String serverKey = manager.getServerKey();
        String targetName = "AhSmokeTarget";
        PlayerConfig override = PlayerConfig.defaults(UUID.randomUUID(), targetName);
        override.helmetOpacity.setValue(0.25);
        localConfig.individualConfigurations.putOverride(serverKey, targetName, override);

        try {
            PlayerConfig whenAllowed = manager.resolveConfig(targetName);
            if (whenAllowed != override) {
                throw new IllegalStateException(
                        "[smoke/fcgt] override not applied when the server allows individual configs");
            }

            serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.setValue(false);
            PlayerConfig whenDisallowed = manager.resolveConfig(targetName);
            if (whenDisallowed == override) {
                throw new IllegalStateException(
                        "[smoke/fcgt] override applied even though the server disallows individual configs");
            }
            serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.setValue(true);

            // The local player must never be resolved to an override (always their own live config).
            String localName = ArmorHiderClient.getCurrentPlayerName();
            if (localName != null) {
                localConfig.individualConfigurations.putOverride(serverKey, localName,
                        PlayerConfig.defaults(UUID.randomUUID(), localName));
                if (manager.resolveConfig(localName) != localConfig) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] the local player was resolved to an override — must use their own config");
                }
                localConfig.individualConfigurations.removeOverride(serverKey, localName);
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] resolver override precedence verified");
        } finally {
            localConfig.individualConfigurations.removeOverride(serverKey, targetName);
            manager.clearServerConfig();
        }
    }

    private static void assertGlobalOverrideResolution(Minecraft client) {
        var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;
        PlayerConfig localConfig = manager.getLocalPlayerConfig();

        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.setValue(true);
        serverConfig.serverWideSettings.forceArmorHiderOff.setValue(false);
        manager.setServerConfig(serverConfig);

        boolean priorUseGlobalAll = manager.shouldUseGlobalOverrideForAllPlayers();
        boolean priorUseOwn = manager.shouldUseLocalSettingsForUnknowns();
        boolean priorDisableOthers = manager.isArmorHiderDisableForOthers();
        try {
            PlayerConfig globalOverride = manager.ensureGlobalOverride();

            // Row B set to "global for unknown": an unknown (non-mod) player resolves to the global config.
            manager.setUseOwnSettingsForUnknowns(false, Optional.of(true));
            manager.setUseGlobalOverrideForAllPlayersTo(false, Optional.of(true));
            manager.setArmorHiderDisabledForOthersTo(false, Optional.of(true));
            if (manager.resolveConfig("AhUnknownPlayer") != globalOverride) {
                throw new IllegalStateException("[smoke/fcgt] global config was not applied to an unknown player (Row B)");
            }

            // Row B set to "own settings": unknown player resolves to the viewer's own config, not the global.
            manager.setUseOwnSettingsForUnknowns(true, Optional.of(true));
            if (manager.resolveConfig("AhUnknownPlayer") == globalOverride) {
                throw new IllegalStateException("[smoke/fcgt] 'use my settings for unknown' must not use the global config");
            }

            // Row C "global for all": overrides even a server-broadcast config for a known modded player.
            PlayerConfig broadcast = PlayerConfig.defaults(UUID.randomUUID(), "AhModPlayer");
            serverConfig.put("AhModPlayer", UUID.randomUUID(), broadcast);
            if (manager.resolveConfig("AhModPlayer") != broadcast) {
                throw new IllegalStateException("[smoke/fcgt] a server-broadcast config should be used when Row C is off");
            }
            manager.setUseGlobalOverrideForAllPlayersTo(true, Optional.of(true));
            if (manager.resolveConfig("AhModPlayer") != globalOverride) {
                throw new IllegalStateException("[smoke/fcgt] Row C must apply the global config to all other players");
            }

            // Individual overrides still win over the global-for-all setting.
            String serverKey = manager.getServerKey();
            PlayerConfig individual = PlayerConfig.defaults(UUID.randomUUID(), "AhModPlayer");
            localConfig.individualConfigurations.putOverride(serverKey, "AhModPlayer", individual);
            if (manager.resolveConfig("AhModPlayer") != individual) {
                throw new IllegalStateException("[smoke/fcgt] individual override must win over the global override");
            }
            localConfig.individualConfigurations.removeOverride(serverKey, "AhModPlayer");

            // Server force-off is the final guard — even the global config renders vanilla under it.
            serverConfig.serverWideSettings.forceArmorHiderOff.setValue(true);
            if (!SlotModification.shouldUseVanilla(globalOverride)) {
                throw new IllegalStateException("[smoke/fcgt] server force-off must still guard the global config");
            }
            serverConfig.serverWideSettings.forceArmorHiderOff.setValue(false);

            // Row A "Others: vanilla" renders other players vanilla when other-player config is allowed.
            manager.setArmorHiderDisabledForOthersTo(true, Optional.of(true));
            if (!SlotModification.shouldUseVanilla(globalOverride)) {
                throw new IllegalStateException("[smoke/fcgt] 'disable armor hider on others' must render others vanilla when allowed");
            }
            // ...but is inert when the server disallows individual configs.
            serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.setValue(false);
            if (SlotModification.shouldUseVanilla(globalOverride)) {
                throw new IllegalStateException("[smoke/fcgt] 'disable armor hider on others' must be inert when the server disallows it");
            }

            ArmorHider.LOGGER.info("[smoke/fcgt] global override resolution + precedence verified");
        } finally {
            manager.setArmorHiderDisabledForOthersTo(priorDisableOthers, Optional.of(true));
            manager.setUseOwnSettingsForUnknowns(priorUseOwn, Optional.of(true));
            manager.setUseGlobalOverrideForAllPlayersTo(priorUseGlobalAll, Optional.of(true));
            manager.clearServerConfig();
        }
    }

    private static void assertScreenHasDoneButton(Screen screen) {
        boolean hasDone = false;
        int widgetCount = 0;
        for (var child : screen.children()) {
            widgetCount++;
            if (child instanceof Button button
                    && button.getMessage().getString().equals(net.minecraft.network.chat.CommonComponents.GUI_DONE.getString())) {
                hasDone = true;
            }
        }
        if (!hasDone) {
            throw new IllegalStateException(
                    "[smoke/fcgt] IndividualPlayerConfigurationsScreen produced no Done button (widgets="
                            + widgetCount + ") — init/layout failed");
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] per-player screen layout ok ({} widgets)", widgetCount);
    }

    /** Minimal screen that lays out the new version-gated widgets so a render pass exercises their code. */
    private static final class WidgetProbeScreen extends Screen {
        private final PlayerInfo info;
        private final UUID id;

        WidgetProbeScreen(PlayerInfo info, UUID id) {
            super(Component.literal("AH widget render probe"));
            this.info = info;
            this.id = id;
        }

        @Override
        protected void init() {
            var entry = new PlayerHeadBarWidget.Entry(id, "AhSmokeTarget", () -> PlayerFaceTextures.face(info));
            addRenderableWidget(new PlayerHeadBarWidget(10, 10, this.width - 20, 28, List.of(entry), e -> {}));

            int panelWidth = this.width * 6 / 10;
            int panelTop = 50;
            int panelHeight = Math.max(40, this.height - panelTop - 40);
            var override = PlayerConfig.defaults(id, "AhSmokeTarget");
            addRenderableWidget(new ArmorHiderOptionsPanelWidget(
                    0, panelTop, panelWidth, panelHeight, this, Minecraft.getInstance().options,
                    () -> {}, ArmorHiderClient.PRESET_MANAGER, override, false));

            addRenderableWidget(new PlayerPreviewWidget(
                    panelWidth + 10, panelTop, 100, 100, () -> Minecraft.getInstance().player));
        }
    }
}
//?}
