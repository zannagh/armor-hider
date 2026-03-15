package de.zannagh.armorhider.client.net;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public record ClientPayloadContext(ClientPacketListener handler, Minecraft client) {
}
