package de.zannagh.armorhider.net.packets;

import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * C2S report that a client detected a local combat-log event. A plain POJO carried on eunomia's
 * {@code de.zannagh.armorhider:combatlog_c2s_packet} channel.
 */
public class CombatLogEventPacket {

    public String playerName;

    public long timestamp;

    public UUID originator;

    public CombatLogEventPacket() {
    }

    public CombatLogEventPacket(Player player, UUID originator) {
        this.playerName = PlayerNameUtil.getPlayerName(player);
        this.timestamp = System.currentTimeMillis();
        this.originator = originator;
    }
}
