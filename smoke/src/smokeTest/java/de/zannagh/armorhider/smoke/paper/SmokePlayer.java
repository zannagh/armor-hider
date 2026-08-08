package de.zannagh.armorhider.smoke.paper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The fixed identity the test client connects with.
 *
 * <p>The server runs {@code online-mode=false}, so the UUID is the standard offline one. It is
 * computed rather than hardcoded, using the same expression as
 * {@code net.minecraft.core.UUIDUtil.createOfflinePlayerUUID}, so it cannot drift from what the
 * server derives for the same name.</p>
 *
 * <p>The player is seeded into {@code ops.json} at level 4 on purpose: the plugin then sends a
 * {@code PermissionPacket} with {@code permissionLevel = 4}, and 4 is a value the client can only
 * have obtained over the wire - its own default is 0.</p>
 */
public final class SmokePlayer {

    /** Username the test client authenticates as. */
    public static final String NAME = "ArmorHiderSmoke";

    /** Operator level granted to {@link #NAME}; the plugin's admin threshold is {@code >= 3}. */
    public static final int OP_LEVEL = 4;

    private static final UUID UUID_VALUE = offlineUuid(NAME);

    private SmokePlayer() {
    }

    /** The offline-mode UUID for {@link #NAME}. */
    public static UUID uuid() {
        return UUID_VALUE;
    }

    /** {@link #uuid()} in the dashed string form used as the {@code playerConfigs} map key. */
    public static String uuidString() {
        return UUID_VALUE.toString();
    }

    /** Mirrors {@code UUIDUtil.createOfflinePlayerUUID}. */
    public static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    /** The {@code ops.json} document granting {@link #NAME} operator level {@link #OP_LEVEL}. */
    public static String opsJson() {
        return "[{\"uuid\":" + Json.quote(uuidString())
                + ",\"name\":" + Json.quote(NAME)
                + ",\"level\":" + OP_LEVEL
                + ",\"bypassesPlayerLimit\":false}]\n";
    }
}
