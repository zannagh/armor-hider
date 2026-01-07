package de.zannagh.armorhider;

import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

final class ServerConfigProviderMock {
    @Contract("_ -> new")
    static @NonNull StringServerConfigProvider createServerConfigWithPlayers(int playerCount) {
        ServerConfiguration configuration = new ServerConfiguration();

        for (int i = 0; i < playerCount; i++) {
            UUID playerId = UUID.randomUUID();
            String playerName = "Player" + i;
            double helmetOpacity = Math.random();
            double chestOpacity = Math.random();
            double legsOpacity = Math.random();
            double bootsOpacity = Math.random();
            boolean combatDetection = Math.random() > 0.5;
            configuration.put(playerName, playerId, new PlayerConfig(helmetOpacity, chestOpacity, legsOpacity, bootsOpacity, combatDetection, playerId.toString(), playerName));
        }
        return new StringServerConfigProvider(configuration.toJson());
    }
}
