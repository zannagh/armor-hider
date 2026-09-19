package de.zannagh.armorhider.net.packets;

import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * S2C re-broadcast of a combat-log event as a notification. A plain POJO carried on eunomia's
 * {@code de.zannagh.armorhider:combatlog_s2c_packet} channel.
 */
public class CombatLogNotificationPacket {

    public String playerName;

    public long timestamp;

    public UUID originator;

    public CombatLogNotificationPacket() {
    }

    public CombatLogNotificationPacket(String playerName, UUID originator) {
        this.playerName = playerName;
        this.timestamp = System.currentTimeMillis();
        this.originator = originator;
    }

    public CombatLogNotificationPacket(String playerName, UUID originator, long timestamp) {
        this.playerName = playerName;
        this.timestamp = timestamp;
        this.originator = originator;
    }

    public CombatLogNotificationPacket(Player player, UUID originator) {
        this.playerName = PlayerNameUtil.getPlayerName(player);
        this.timestamp = System.currentTimeMillis();
        this.originator = originator;
    }
}
