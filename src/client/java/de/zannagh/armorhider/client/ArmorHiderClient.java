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
        return Minecraft.getInstance().player instanceof Player clientPlayer
                && clientPlayer.getDisplayName() instanceof Component displayText
                ? displayText.getString()
                : ClientConfigManager.DEFAULT_PLAYER_NAME;
    }

    @Contract("_ -> new")
    public static @NonNull Pair<Boolean, PlayerInfo> isPlayerRemotePlayer(String playerName) {
        if (Minecraft.getInstance().getConnection() instanceof ClientPacketListener networkHandler
                && networkHandler.getPlayerInfoIgnoreCase(playerName) instanceof PlayerInfo entry
                && entry.getProfile().name() != null) {
            return new Pair<>(!entry.getProfile().name().equals(getCurrentPlayerName()), entry);
        }
        return new Pair<>(false, null);
    }

    
}