package de.zannagh.armorhider;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.paper.config.ServerConfigurationState;
import de.zannagh.armorhider.paper.config.ServerWideSettingsDefaults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the Paper plugin's hand-written view of the wire schema in step with the mod's real one.
 *
 * <p><b>Why this exists.</b> {@code :paper} is deliberately a schema-agnostic relay - it stores and
 * forwards every {@code PlayerConfig} as an opaque {@code JsonObject} and never inspects it, so
 * adding fields there needs no plugin change at all. But it is not schema-<em>free</em>: it has to
 * understand the {@code serverWideSettings} block (to apply admin toggles and to migrate an
 * on-disk config), the {@code playerId}/{@code playerName} keys (for the name-collision index) and
 * the channel names. Those are re-declared in {@code ServerWideSettingsDefaults},
 * {@code ServerConfigurationState} and {@code Channels}.</p>
 *
 * <p>{@code :paper} cannot simply depend on the real classes: {@code ConfigurationSource} extends
 * {@code CustomPacketPayload} and the payload classes import {@code Identifier} /
 * {@code StreamCodec}, none of which exist on a Bukkit server. Until that is untangled, this test
 * is what stops the two halves drifting - add a fifth server-wide setting, change a default, bump
 * the schema version or move a channel, and this fails immediately instead of the plugin silently
 * ignoring it months later.</p>
 *
 * <p>Runs from the <em>active</em> stonecutter variant only ({@code enabled = sc.current.isActive}),
 * so the channel assertions verify that variant's namespace. Switching variants re-checks the
 * other era, which is the point: the settings channels change namespace at 1.21.11.</p>
 */
@DisplayName("Paper plugin mirrors the mod's wire schema")
class PaperSchemaContractTests {

    /**
     * The whole {@code serverWideSettings} block in one assertion: field names, shipped defaults and
     * {@code configVersion} all at once.
     *
     * <p>Serialising a default-constructed {@link ServerWideSettings} through the real
     * {@code ArmorHider.GSON} is exactly what a first-run server writes, and
     * {@code ServerWideSettingsDefaults.create()} is the plugin's hand-rolled equivalent. They must
     * be byte-for-byte the same document.</p>
     */
    @Test
    @DisplayName("the plugin's default serverWideSettings block equals the mod's")
    void defaultServerWideSettingsBlocksMatch() {
        JsonObject fromMod = ArmorHider.GSON.toJsonTree(new ServerWideSettings()).getAsJsonObject();
        // The no-arg constructor leaves configVersion at 0 - only the four-arg one and migrate()
        // stamp CURRENT_CONFIG_VERSION - so normalise it here rather than assert a quirk of object
        // construction. The version itself is covered by schemaVersionsMatch().
        fromMod.addProperty(ServerWideSettingsDefaults.CONFIG_VERSION,
                ServerWideSettings.CURRENT_CONFIG_VERSION);
        JsonObject fromPaper = ServerWideSettingsDefaults.create();

        assertEquals(fromMod, fromPaper,
                "The Paper plugin's ServerWideSettingsDefaults.create() no longer matches what the"
                        + " mod actually serialises. If you added or renamed a server-wide setting,"
                        + " or changed a default, mirror it in"
                        + " paper/src/main/java/de/zannagh/armorhider/paper/config/"
                        + "ServerWideSettingsDefaults.java - the plugin cannot relay or persist a"
                        + " field it does not know about.");
    }

    @Test
    @DisplayName("the plugin's schema version matches the mod's")
    void schemaVersionsMatch() {
        assertEquals(ServerWideSettings.CURRENT_CONFIG_VERSION,
                ServerWideSettingsDefaults.CURRENT_CONFIG_VERSION,
                "ServerWideSettings.CURRENT_CONFIG_VERSION was bumped without updating the Paper"
                        + " plugin, so the plugin will keep stamping the old version onto configs it"
                        + " writes and the mod's migration will not run.");
    }

    /**
     * {@code fillMissing} is the plugin's migration for a config written by an older mod release.
     * It can only backfill fields it knows, so its output must carry the full current key set.
     */
    @Test
    @DisplayName("the plugin backfills every current setting when migrating an old config")
    void migrationBackfillsEveryCurrentSetting() {
        JsonObject legacy = new JsonObject();
        legacy.addProperty(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION, false);

        JsonObject migrated = ServerWideSettingsDefaults.fillMissing(legacy);

        assertEquals(serializedNames(ServerWideSettings.class), migrated.keySet(),
                "fillMissing() did not produce the mod's current field set, so a server upgrading"
                        + " from an older release keeps a config the mod considers incomplete.");
        assertEquals(false,
                migrated.get(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION).getAsBoolean(),
                "fillMissing() must preserve values that were already present");
    }

    /**
     * The two keys the plugin genuinely reads out of an otherwise opaque {@code PlayerConfig}. Every
     * other field is relayed untouched, which is why adding one needs no plugin change.
     */
    @Test
    @DisplayName("the plugin reads the same playerId/playerName keys the mod writes")
    void playerIdentityKeysMatch() {
        Set<String> playerConfigFields = serializedNames(
                de.zannagh.armorhider.net.packets.PlayerConfig.class);

        assertTrue(playerConfigFields.contains(ServerConfigurationState.PLAYER_ID),
                "The Paper plugin keys its config store on \"" + ServerConfigurationState.PLAYER_ID
                        + "\", which PlayerConfig no longer serialises.");
        assertTrue(playerConfigFields.contains(ServerConfigurationState.PLAYER_NAME),
                "The Paper plugin builds its playerNameConfigs index on \""
                        + ServerConfigurationState.PLAYER_NAME
                        + "\", which PlayerConfig no longer serialises. Name lookups would silently"
                        + " return null on the client.");
    }

    //? if >= 1.20.5 {
    /**
     * Channel names, for the stonecutter variant under test. The settings family switches namespace
     * at 1.21.11 while the permission and combat-log families do not, so the plugin registers the
     * union of both - but each real identifier must still appear in the matching alias list.
     */
    @Test
    @DisplayName("every packet identifier is a channel the plugin registers")
    void channelsCoverEveryPacketIdentifier() {
        assertChannel(de.zannagh.armorhider.net.packets.PlayerConfig.PACKET_IDENTIFIER.toString(),
                de.zannagh.armorhider.paper.net.Channels.PLAYER_CONFIG_C2S, "PlayerConfig");
        assertChannel(ServerWideSettings.PACKET_IDENTIFIER.toString(),
                de.zannagh.armorhider.paper.net.Channels.SERVER_WIDE_SETTINGS_C2S,
                "ServerWideSettings");
        assertChannel(de.zannagh.armorhider.server.ServerConfiguration.PACKET_IDENTIFIER.toString(),
                de.zannagh.armorhider.paper.net.Channels.SERVER_CONFIGURATION_S2C,
                "ServerConfiguration");
        assertChannel(de.zannagh.armorhider.net.packets.PermissionPacket.PACKET_IDENTIFIER.toString(),
                de.zannagh.armorhider.paper.net.Channels.PERMISSIONS_S2C, "PermissionPacket");
        assertChannel(
                de.zannagh.armorhider.net.packets.CombatLogEventPacket.PACKET_IDENTIFIER.toString(),
                de.zannagh.armorhider.paper.net.Channels.COMBAT_LOG_C2S, "CombatLogEventPacket");
        assertChannel(
                de.zannagh.armorhider.net.packets.CombatLogNotificationPacket.PACKET_IDENTIFIER
                        .toString(),
                de.zannagh.armorhider.paper.net.Channels.COMBAT_LOG_S2C,
                "CombatLogNotificationPacket");
    }

    private static void assertChannel(String identifier, java.util.List<String> aliases,
                                      String payload) {
        assertTrue(aliases.contains(identifier),
                () -> payload + " travels on \"" + identifier + "\" on this game version, but the"
                        + " Paper plugin only registers " + aliases + ". Bukkit drops any plugin"
                        + " message on an unregistered channel silently, so this would look like the"
                        + " mod connecting and doing nothing. Update Channels.java.");
    }
    //?}

    /**
     * The names Gson will actually emit for a config class, in declaration order.
     *
     * <p>Mirrors Gson's own rule: {@code @SerializedName} when present, otherwise the plain field
     * name - {@code PlayerConfig.playerId} and {@code playerName} carry no annotation. Static and
     * synthetic fields are skipped because Gson does not serialise them either.</p>
     */
    private static Set<String> serializedNames(Class<?> type) {
        Set<String> names = new LinkedHashSet<>();
        for (Field field : type.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    || java.lang.reflect.Modifier.isTransient(field.getModifiers())
                    || field.isSynthetic()) {
                continue;
            }
            SerializedName annotation = field.getAnnotation(SerializedName.class);
            names.add(annotation != null ? annotation.value() : field.getName());
        }
        return names;
    }
}
