package de.zannagh.armorhider.client.gui.screens;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.gui.elements.ArmorHiderOptionsPanelWidget;
import de.zannagh.armorhider.client.gui.elements.ElementSpacingOptions;
import de.zannagh.armorhider.client.gui.elements.PlayerHeadBarWidget;
import de.zannagh.armorhider.client.gui.elements.PlayerPreviewWidget;
import de.zannagh.armorhider.client.gui.util.PlayerFaceTextures;
import de.zannagh.armorhider.client.utils.McClientUtils;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lets the user configure how other players' armor looks to them (a purely client-side override). A
 * horizontally scrollable bar shows a "Global configuration" entry (always available, even offline — it
 * spans all servers) followed by the online players' head icons (when connected to a server that allows
 * individual configs). Picking an entry reveals an enable/disable control, the ArmorHider options bound to
 * that entry's override config, and a live preview.
 *
 * <p>The screen is rebuilt (via {@link #rebuildWidgets()}) whenever the selection or override state changes;
 * {@link #selectedId}/{@link #globalSelected}/{@link #savedScroll} persist the UI state across rebuilds.
 */
public class IndividualPlayerConfigurationsScreen extends ArmorHiderConfigurationScreen {

    private static final UUID GLOBAL_ENTRY_ID = new UUID(0L, 0L);

    private @Nullable UUID selectedId;
    private boolean globalSelected = true;
    private int savedScroll;
    private @Nullable PlayerHeadBarWidget headBar;

    public IndividualPlayerConfigurationsScreen(Screen parent, Options gameOptions, Component title) {
        super(parent, gameOptions, title);
        this.gameOptions = gameOptions;
    }

    @Override
    protected void init() {
        Minecraft minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;

        boolean connected = McClientUtils.isClientConnectedToServer() && connection != null;
        boolean perPlayerAllowed = connected && manager.areIndividualConfigsAllowedByServer();

        // The global entry is always first; player entries follow when per-player configs are available.
        List<PlayerHeadBarWidget.Entry> entries = new ArrayList<>();
        entries.add(PlayerHeadBarWidget.Entry.global(GLOBAL_ENTRY_ID,
                Component.translatable("armorhider.individual.global.title").getString(),
                IndividualPlayerConfigurationsScreen::globalIconTexture));
        if (perPlayerAllowed) {
            entries.addAll(collectPlayerEntries(connection));
        }
        // Dev/UI testing: seed N fake players into the bar so the horizontal scroll can be exercised
        // without spawning real clients. Enable with a run-config VM arg, e.g. -Darmorhider.demo.players=30.
        int demoCount = Integer.getInteger("armorhider.demo.players", 0);
        for (int i = 1; i <= demoCount; i++) {
            entries.add(new PlayerHeadBarWidget.Entry(new UUID(0L, i), "DemoPlayer" + i, this::demoFaceTexture));
        }

        int margin = 12;
        int barHeight = 28;
        headBar = new PlayerHeadBarWidget(margin, topMargin, this.width - margin * 2, barHeight, entries, this::onEntrySelected);

        int selectedIndex = resolveSelectedIndex(entries);
        headBar.setSelectedIndex(selectedIndex);
        headBar.setScrollOffset(savedScroll);
        addRenderableWidget(headBar);

        int controlsY = topMargin + barHeight + 10;
        PlayerHeadBarWidget.Entry selected = selectedIndex >= 0 ? entries.get(selectedIndex) : null;
        if (selected == null) {
            addCenteredNotice(Component.translatable("armorhider.individual.hint.select"), controlsY + 20);
        } else if (selected.global) {
            buildGlobalSection(controlsY);
        } else {
            buildPlayerSection(selected, controlsY);
        }

        addDoneButton();
    }

    private List<PlayerHeadBarWidget.Entry> collectPlayerEntries(net.minecraft.client.multiplayer.ClientPacketListener connection) {
        String localName = ArmorHiderClient.getCurrentPlayerName();
        List<PlayerHeadBarWidget.Entry> players = new ArrayList<>();
        for (PlayerInfo info : connection.getListedOnlinePlayers()) {
            //? if >= 1.21.9 {
            /*String name = info.getProfile().name();
            UUID id = info.getProfile().id();
            *///?}
            //? if < 1.21.9 {
            String name = info.getProfile().getName();
            UUID id = info.getProfile().getId();
            //?}
            if (name == null || name.equals(localName)) {
                continue;
            }
            players.add(new PlayerHeadBarWidget.Entry(id, name, () -> PlayerFaceTextures.face(info)));
        }
        players.sort(Comparator.comparing(entry -> entry.name.toLowerCase(Locale.ROOT)));
        return players;
    }

    private int resolveSelectedIndex(List<PlayerHeadBarWidget.Entry> entries) {
        if (globalSelected) {
            return 0; // global entry is always first
        }
        if (selectedId != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (!entries.get(i).global && entries.get(i).id.equals(selectedId)) {
                    return i;
                }
            }
        }
        // Selected player is gone (left the server) — fall back to the always-present global entry.
        selectedId = null;
        globalSelected = true;
        return 0;
    }

    private void buildPlayerSection(PlayerHeadBarWidget.Entry selected, int controlsY) {
        var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;
        final String playerName = selected.name;
        final UUID playerId = selected.id;
        boolean hasOverride = manager.hasIndividualOverride(playerName);

        Component previewNote = livingEntityFor(playerId) == null
                ? Component.translatable("armorhider.individual.preview.out_of_range", playerName)
                : null;

        layoutOverrideEditor(controlsY,
                null,
                () -> manager.putIndividualConfigOverride(playerName, PlayerConfig.defaults(playerId, playerName), Optional.of(true)),
                () -> manager.removeIndividualOverride(playerName, Optional.of(true)),
                Component.translatable("armorhider.individual.enable.tooltip", playerName),
                Component.translatable("armorhider.individual.disable.tooltip", playerName),
                hasOverride,
                Component.translatable(hasOverride ? "armorhider.individual.override_active" : "armorhider.individual.override_inactive", playerName),
                hasOverride ? manager.getIndividualConfigOverride(playerName) : null,
                () -> previewEntityFor(playerId),
                Component.translatable("armorhider.individual.hint.enable", playerName),
                previewNote);
    }

    private void buildGlobalSection(int controlsY) {
        var manager = ArmorHiderClient.CLIENT_CONFIG_MANAGER;
        var serverConfig = manager.getServerConfig();
        boolean forceOff = serverConfig != null && serverConfig.serverWideSettings.forceArmorHiderOff.getValue();
        boolean disallowed = serverConfig != null
                && !serverConfig.serverWideSettings.allowIndividualPlayerConfigurations.getValue();
        boolean othersVanilla = manager.isArmorHiderDisableForOthers();

        // A row is "gated" (shown as a disabled "Vanilla" state) when the server forces off / disallows,
        // or — for the unknown/all rows — when "Others: Vanilla" makes them irrelevant.
        Component serverReason = forceOff
                ? Component.translatable("armorhider.individual.global.gated.forceoff")
                : (disallowed ? Component.translatable("armorhider.individual.global.gated.disallowed") : null);
        Component granularReason = serverReason != null
                ? serverReason
                : (othersVanilla ? Component.translatable("armorhider.individual.global.gated.others_vanilla") : null);

        addCenteredNotice(Component.translatable("armorhider.individual.global.title"), controlsY);
        int y = controlsY + 13;

        // Row A — Armor Hider on other players: Adjust (apply) or Vanilla (no-op). A single toggle whose
        // label reflects the current state; gated to a disabled "Vanilla" state only by the server.
        Component othersTooltip = Component.translatable("armorhider.individual.global.others.tooltip");
        addToggleRow(y, serverReason, Component.translatable("armorhider.individual.global.others.gated"),
                Component.translatable(othersVanilla
                        ? "armorhider.individual.global.others.off"
                        : "armorhider.individual.global.others.on"),
                othersTooltip,
                () -> manager.setArmorHiderDisabledForOthersTo(!othersVanilla, Optional.of(true)));
        y += 22;

        // Row B — unknown players use the viewer's own settings, or the global configuration. A single toggle
        // between the two; gated to a disabled "Vanilla" state by the server or when "Others: Vanilla".
        boolean ownForUnknown = manager.shouldUseLocalSettingsForUnknowns();
        Component unknownTooltip = Component.translatable("armorhider.individual.global.unknown.tooltip");
        addToggleRow(y, granularReason, Component.translatable("armorhider.individual.global.unknown.gated"),
                Component.translatable(ownForUnknown
                        ? "armorhider.individual.global.unknown.own"
                        : "armorhider.individual.global.unknown.global"),
                unknownTooltip,
                () -> manager.setUseOwnSettingsForUnknowns(!ownForUnknown, Optional.of(true)));
        y += 22;

        // Row C — apply the global configuration to every other player. A single ON/OFF toggle.
        boolean globalForAll = manager.shouldUseGlobalOverrideForAllPlayers();
        Component allTooltip = Component.translatable("armorhider.individual.global.all.tooltip");
        addToggleRow(y, granularReason, Component.translatable("armorhider.individual.global.all.gated"),
                Component.translatable(globalForAll
                        ? "armorhider.individual.global.all.on"
                        : "armorhider.individual.global.all.off"),
                allTooltip,
                () -> manager.setUseGlobalOverrideForAllPlayersTo(!globalForAll, Optional.of(true)));
        y += 24;

        // The global configuration panel + preview are always shown.
        addPanelAndPreview(y, manager.ensureGlobalOverride(), () -> Minecraft.getInstance().player, null, null);
    }

    /** Lays out an optional title, the enable/disable row + status, and (when enabled) the options panel + preview. */
    private void layoutOverrideEditor(int controlsY, @Nullable Component titleNote, Runnable onEnable, Runnable onDisable,
                                      Component enableTooltip, Component disableTooltip, boolean enabled, Component statusNote,
                                      @Nullable PlayerConfig boundConfig, Supplier<LivingEntity> previewEntity,
                                      @Nullable Component enableHint, @Nullable Component previewNote) {
        int y = controlsY;
        if (titleNote != null) {
            addCenteredNotice(titleNote, y);
            y += 13;
        }

        int btnWidth = rowButtonWidth(90);
        int btnGap = 8;
        int rowX = this.width / 2 - (btnWidth * 2 + btnGap) / 2;
        Button enableButton = Button.builder(Component.translatable("armorhider.individual.enable"),
                        btn -> {
                            onEnable.run();
                            this.settingsChanged = true;
                            this.rebuildWidgets();
                        })
                .tooltip(Tooltip.create(enableTooltip)).bounds(rowX, y, btnWidth, 20).build();
        enableButton.active = !enabled;
        Button disableButton = Button.builder(Component.translatable("armorhider.individual.disable"),
                        btn -> {
                            onDisable.run();
                            this.settingsChanged = true;
                            this.rebuildWidgets();
                        })
                .tooltip(Tooltip.create(disableTooltip)).bounds(rowX + btnWidth + btnGap, y, btnWidth, 20).build();
        disableButton.active = enabled;
        addRenderableWidget(enableButton);
        addRenderableWidget(disableButton);
        addCenteredNotice(statusNote, y + 24);

        addPanelAndPreview(y + 24 + 14, enabled ? boundConfig : null, previewEntity, previewNote, enableHint);
    }

    /**
     * A single full-width toggle button whose {@code label} reflects the current state and whose click runs
     * {@code onToggle} (which flips the underlying setting) then rebuilds. When {@code gateReason} is non-null
     * it collapses to a disabled button showing {@code gatedLabel} with the reason as its tooltip — used when
     * the server forces off / disallows, or when "Others: Vanilla" makes the finer controls irrelevant.
     */
    private void addToggleRow(int y, @Nullable Component gateReason, Component gatedLabel,
                              Component label, Component tooltip, Runnable onToggle) {
        int width = rowButtonWidth(120) * 2 + 8;
        int rowX = this.width / 2 - width / 2;
        if (gateReason != null) {
            Button disabled = Button.builder(gatedLabel, btn -> {})
                    .tooltip(Tooltip.create(gateReason)).bounds(rowX, y, width, 20).build();
            disabled.active = false;
            addRenderableWidget(disabled);
            return;
        }
        Button toggle = Button.builder(label, btn -> {
                    onToggle.run();
                    this.settingsChanged = true;
                    this.rebuildWidgets();
                })
                .tooltip(Tooltip.create(tooltip)).bounds(rowX, y, width, 20).build();
        addRenderableWidget(toggle);
    }

    /** Adds the left-hand options panel (bound to {@code boundConfig}, or a hint when null) and the preview. */
    private void addPanelAndPreview(int panelTop, @Nullable PlayerConfig boundConfig, Supplier<LivingEntity> previewEntity,
                                    @Nullable Component previewNote, @Nullable Component enableHint) {
        int panelBottom = this.height - 27 - 6;
        var spacing = new ElementSpacingOptions(this.width)
                .forVaryingElements(1, 1)
                .withPercentageWidthForPrimaryElement(60)
                .withGap(0);
        int panelWidth = spacing.getWidth(0);
        int panelHeight = Math.max(0, panelBottom - panelTop);

        if (boundConfig != null) {
            addRenderableWidget(new ArmorHiderOptionsPanelWidget(
                    spacing.getX(0), panelTop, panelWidth, panelHeight, this, this.gameOptions,
                    () -> {
                        this.settingsChanged = true;
                        ArmorHiderClient.CLIENT_CONFIG_MANAGER.markLocalDirty();
                    },
                    ArmorHiderClient.PRESET_MANAGER, boundConfig, false));
        } else if (enableHint != null) {
            addCenteredNotice(enableHint, panelTop + 20);
        }

        // The preview is square and its internal panel is roughly as tall as it is wide, so cap it by the
        // column width, the available vertical space AND 150px, then centre it in its column and clamp it to
        // the screen so it never leaves the visible area at large GUI scales / small windows.
        int columnX = spacing.getX(1);
        int columnWidth = spacing.getWidth(1);
        int previewWidth = Math.min(Math.min(150, columnWidth - previewMargin), panelHeight - previewMargin);
        if (previewWidth > 24) {
            int previewX = columnX + (columnWidth - previewWidth) / 2;
            previewX = Math.max(previewMargin, Math.min(previewX, this.width - previewMargin - previewWidth));
            int previewY = panelTop + previewMargin / 2;
            addRenderableWidget(new PlayerPreviewWidget(previewX, previewY, previewWidth, previewWidth, previewEntity));
            if (previewNote != null) {
                addCenteredNoteAt(previewNote, columnX, previewY + previewWidth + 4, columnWidth);
            }
        }
    }

    /** Width for one button in a centered two-button row, shrinking to fit narrow screens. */
    private int rowButtonWidth(int preferred) {
        int available = this.width - 24; // screen margins
        return Math.max(40, Math.min(preferred, (available - 8) / 2));
    }

    private void onEntrySelected(PlayerHeadBarWidget.Entry entry) {
        if (this.headBar != null) {
            this.savedScroll = this.headBar.getScrollOffset();
        }
        if (entry.global) {
            this.globalSelected = true;
            this.selectedId = null;
        } else {
            this.globalSelected = false;
            this.selectedId = entry.id;
        }
        this.rebuildWidgets();
    }

    private @Nullable LivingEntity livingEntityFor(@Nullable UUID id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (id == null || minecraft.level == null) {
            return null;
        }
        for (var player : minecraft.level.players()) {
            if (player.getUUID().equals(id)) {
                return player;
            }
        }
        return null;
    }

    private @Nullable LivingEntity previewEntityFor(@Nullable UUID id) {
        LivingEntity live = livingEntityFor(id);
        return live != null ? live : Minecraft.getInstance().player;
    }

    /** Face texture for seeded demo entries: the local player's skin when available, else the global icon. */
    private ResourceLocation demoFaceTexture() {
        Minecraft mc = Minecraft.getInstance();
        var connection = mc.getConnection();
        if (mc.player != null && connection != null) {
            var info = connection.getPlayerInfo(mc.player.getUUID());
            if (info != null) {
                return PlayerFaceTextures.face(info);
            }
        }
        return globalIconTexture();
    }

    private static ResourceLocation globalIconTexture() {
        //? if >= 1.21 {
        return ResourceLocation.fromNamespaceAndPath("armor-hider", "textures/gui/sprites/global_settings_override.png");
        //?}
        //? if < 1.21 {
        /*return new ResourceLocation("armor-hider", "textures/gui/sprites/global_settings_override.png");
        *///?}
    }

    private void addDoneButton() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    @Override
    protected void addOptions() {
        // Layout is built entirely in init(); nothing to add to the base widget list.
    }

    @Override
    protected void saveSettingsOnClose() {
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.saveCurrent();
    }
}
