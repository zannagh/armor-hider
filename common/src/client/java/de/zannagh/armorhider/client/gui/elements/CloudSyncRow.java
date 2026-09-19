package de.zannagh.armorhider.client.gui.elements;

import com.mojang.datafixers.util.Pair;
import de.zannagh.eunomia.client.gui.CompoundButtonWidget;
import de.zannagh.eunomia.client.gui.ElementSpacingOptions;
import de.zannagh.eunomia.client.gui.screens.EunomiaSettingsScreen;
import de.zannagh.eunomia.client.settings.ServerSettingsClient;
import de.zannagh.eunomia.client.settings.ServerSettingsView;
import de.zannagh.eunomia.client.settings.SyncSettingSource;
import de.zannagh.eunomia.configuration.EunomiaSyncSettings;
import de.zannagh.eunomia.configuration.SyncSetting;
import de.zannagh.eunomia.ui.UiSizes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * The "Cloud Synchronisation" row: a left-hand status readout of the effective eunomia relay state and a
 * right-hand button opening eunomia's own cloud/relay settings screen.
 */
public final class CloudSyncRow {
    /** Width of the right-hand "Cloud Settings" button; the label takes whatever is left. */
    private static final int BUTTON_WIDTH = 100;

    private CloudSyncRow() {
    }

    /**
     * Builds the row for the given host screen. The returned widget is a single row of the options list,
     * so the caller must not set its position or size itself.
     */
    public static AbstractWidget create(Screen hostScreen, Options gameOptions, int rowWidth) {
        var label = statusLabel();

        // Two very different kinds of "the server has an opinion here", and only one of them may grey this
        // button out. Getting either direction wrong is a real bug, so they are spelled out:
        //
        //  - ServerSettingsView#editable() -> NEVER disable. It is false whenever the joined server manages
        //    its own SERVER-WIDE sync settings, which says nothing about this player: in eunomia's precedence
        //    chain PLAYER still outranks SERVER, so they can change their own three sync settings. editable()
        //    is a rendering hint for the server-wide admin controls only, and eunomia's own
        //    ServerSettingsSection greys exactly those out inside the screen. Disabling here would lock
        //    players out of settings they are entitled to change; that state is surfaced in the tooltip only.
        //  - EunomiaSyncSettings#isLockedByServer(EXTERNAL_FALLBACK) -> DO disable. True only when the server
        //    actively enforces the setting, in which case SERVER_ENFORCED outranks PLAYER: the player's own
        //    override is ignored (though kept, and it applies again on a server that does not enforce), so
        //    the screen behind this button holds nothing they could act on for cloud sync.
        boolean lockedByServer = EunomiaSyncSettings.isLockedByServer(SyncSetting.EXTERNAL_FALLBACK);
        var button = Button.builder(
                Component.translatable("armorhider.options.cloud_sync.button"),
                btn -> Minecraft.getInstance().setScreenAndShow(new EunomiaSettingsScreen(hostScreen, gameOptions))
        ).tooltip(Tooltip.create(Component.translatable(lockedByServer
                ? "armorhider.options.cloud_sync.button.tooltip.enforced"
                : "armorhider.options.cloud_sync.button.tooltip"))).build();
        button.active = !lockedByServer;

        return new CompoundButtonWidget(new AbstractWidget[]{label, button}, rowWidth, UiSizes.DEFAULT_BUTTON_HEIGHT, spacing(rowWidth));
    }

    /** The left column: "Cloud Synchronisation: ON/OFF" plus a tooltip naming where that value came from. */
    private static AbstractWidget statusLabel() {
        boolean relayUsable = EunomiaSyncSettings.externalRelayUsable();
        var state = Component.translatable(relayUsable
                ? "armorhider.options.toggle.on"
                : "armorhider.options.toggle.off");
        int rowHeight = UiSizes.DEFAULT_BUTTON_HEIGHT;
        var label = new MultiLineTextWidget(
                Component.translatable("armorhider.options.cloud_sync.status", state),
                Minecraft.getInstance().font) {
            @Override
            public void setY(int y) {
                // Same nudge as the "Compatibilities" row: the parent sets the row's top Y, so offset on
                // top of it to vertically centre the single line of text against the button beside it.
                super.setY(y + Math.max(0, (rowHeight - Minecraft.getInstance().font.lineHeight) / 2));
            }
        };
        label.setTooltip(Tooltip.create(statusTooltip(ServerSettingsClient.lastKnown())));
        refreshServerStateInto(label);
        return label;
    }

    /**
     * Asks the server for its current settings and re-tooltips the label when the answer lands.
     *
     * <p>{@link ServerSettingsClient#lastKnown()} is only a cache of the last answer received on this
     * connection, and it starts out null - so without this round trip the server-managed note would never
     * show on a freshly joined server. The callback fires on the network thread, hence the hop to the
     * client thread; if the screen has closed by then the label is simply orphaned and the update is inert.
     */
    private static void refreshServerStateInto(AbstractWidget label) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        ServerSettingsClient.request(view -> Minecraft.getInstance().execute(
                () -> label.setTooltip(Tooltip.create(statusTooltip(view)))));
    }

    /**
     * Names the precedence rung the effective value comes from, and whether the server manages its own.
     *
     * <p>Enforcement needs no extra line here: when the server locks cloud sync,
     * {@link SyncSettingSource#forExternalFallback()} already resolves to {@code SERVER_ENFORCED}, whose
     * label reads "Locked by server", so the source line states it. The appended note below is about the
     * unrelated, weaker case of a server merely managing its own server-wide settings.</p>
     */
    private static MutableComponent statusTooltip(@Nullable ServerSettingsView serverView) {
        var tooltip = Component.translatable(
                "armorhider.options.cloud_sync.tooltip.source",
                SyncSettingSource.forExternalFallback().label());
        if (serverView != null && !serverView.editable()) {
            tooltip.append(Component.literal("\n"))
                    .append(Component.translatable("armorhider.options.cloud_sync.tooltip.server_locked"));
        }
        return tooltip;
    }

    /** Two columns: the label left-bound in the remaining width, the button right-bound at a fixed width. */
    private static ElementSpacingOptions spacing(int rowWidth) {
        int gap = UiSizes.DEFAULT_BUTTON_SPACING / 2;
        int buttonWidth = Math.min(BUTTON_WIDTH, Math.max(UiSizes.SQUARE_BUTTON_WIDTH, rowWidth / 2));
        int labelWidth = Math.max(UiSizes.SQUARE_BUTTON_WIDTH, rowWidth - buttonWidth - gap);

        var groups = new ArrayList<Pair<Integer, Integer>>();
        groups.add(new Pair<>(0, 0));
        groups.add(new Pair<>(1, 1));

        return new ElementSpacingOptions(rowWidth)
                .forEvenElements(UiSizes.SQUARE_BUTTON_WIDTH, 2)
                .withGroups(groups)
                .withMinSizesForGroups(new int[]{UiSizes.SQUARE_BUTTON_WIDTH, UiSizes.SQUARE_BUTTON_WIDTH})
                .withSizesForGroups(new int[]{labelWidth, buttonWidth})
                .withLeftAlignmentForGroup(0)
                .withRightAlignmentForGroup(1);
    }
}
