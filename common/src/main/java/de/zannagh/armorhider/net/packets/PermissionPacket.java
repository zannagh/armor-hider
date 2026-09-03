package de.zannagh.armorhider.net.packets;

/**
 * Server -&gt; client announcement of the receiving player's op/permission level (0-4). A plain POJO
 * carried on eunomia's {@code de.zannagh.armorhider:permissions_s2c_packet} channel; server-side
 * permission resolution lives in {@code ArmorHiderServerNet} (via {@code ServerUtil}), not here.
 */
public class PermissionPacket {

    public int permissionLevel;

    public PermissionPacket(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public PermissionPacket() {
        permissionLevel = 0;
    }
}
