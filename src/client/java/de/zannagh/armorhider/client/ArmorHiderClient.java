package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.networking.ClientCommunicationManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import oshi.util.tuples.Pair;

public class ArmorHiderClient implements ClientModInitializer {

    public static Boolean isCurrentPlayerSinglePlayerHostOrAdmin = false;
    public static ClientConfigManager CLIENT_CONFIG_MANAGER;

    @Override
    public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientCommunicationManager.initClient();
        CLIENT_CONFIG_MANAGER = new ClientConfigManager();
    }
    
    public static @NonNull Boolean isClientConnectedToServer() {
        return Minecraft.getInstance().isLocalServer()
                || Minecraft.getInstance().getCurrentServer() != null
                || (Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().getConnection().getServerData() != null);
    }

    public static String getCurrentPlayerName() {
        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null) {
            return ClientConfigManager.DEFAULT_PLAYER_NAME;
        }
        Component displayText = clientPlayer.getDisplayName();
        if (displayText == null) {
            return ClientConfigManager.DEFAULT_PLAYER_NAME;
        }
        return displayText.getString();
    }

    @Contract("_ -> new")
    public static @NonNull Pair<Boolean, PlayerInfo> isPlayerRemotePlayer(String playerName) {
        ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
        if (networkHandler == null) {
            return new Pair<>(false, null);
        }
        //? if >= 1.21.9
        PlayerInfo entry = networkHandler.getPlayerInfoIgnoreCase(playerName);
        //? if < 1.21.9
        //PlayerInfo entry = networkHandler.getPlayerInfo(playerName);
        if (entry == null) {
            return new Pair<>(false, null);
        }
        //? if >= 1.21.9
        String profileName = entry.getProfile().name();
        //? if < 1.21.9
        //String profileName = entry.getProfile().getName();
        if (profileName == null) {
            return new Pair<>(false, null);
        }
        return new Pair<>(!profileName.equals(getCurrentPlayerName()), entry);
    }

    
}