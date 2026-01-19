package de.zannagh.armorhider.net;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record ServerPayloadContext(ServerPlayer player, MinecraftServer server) {
}
