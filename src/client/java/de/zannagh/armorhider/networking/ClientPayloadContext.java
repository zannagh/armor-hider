package de.zannagh.armorhider.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public record ClientPayloadContext(ClientPacketListener handler, Minecraft client) {
}
