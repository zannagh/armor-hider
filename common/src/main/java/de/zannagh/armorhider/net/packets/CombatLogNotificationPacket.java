//? if >= 1.20.5 {
package de.zannagh.armorhider.net.packets;

import de.zannagh.armorhider.net.CompressedJsonCodec;
import de.zannagh.armorhider.util.PlayerNameUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class CombatLogNotificationPacket implements CustomPacketPayload {

    public static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath("de.zannagh.armorhider", "combatlog_s2c_packet");
    public static final StreamCodec<ByteBuf, CombatLogNotificationPacket> STREAM_CODEC = CompressedJsonCodec.create(CombatLogNotificationPacket.class);

    public static final Type<CombatLogNotificationPacket> TYPE = new Type<>(PACKET_IDENTIFIER);

    public String playerName;
    
    public long timestamp;
    
    public UUID originator;

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

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//?}

//? if < 1.20.5 {
/*package de.zannagh.armorhider.net.packets;

import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

public class CombatLogNotificationPacket {

    public String playerName;
    
    public long timestamp;
    
    public UUID originator;

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
*///?}
