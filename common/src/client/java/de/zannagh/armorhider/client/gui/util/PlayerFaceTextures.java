package de.zannagh.armorhider.client.gui.util;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolves the face (head) skin {@link ResourceLocation} for a player across Minecraft versions. The accessor
 * changed shape twice: {@code PlayerSkin.body().texturePath()} from 1.21.10 (ClientAsset), plain
 * {@code PlayerSkin.texture()} for 1.21–1.21.9, and {@code PlayerInfo.getSkinLocation()} before 1.21.
 */
public final class PlayerFaceTextures {

    private PlayerFaceTextures() {
    }

    public static ResourceLocation face(PlayerInfo info) {
        //? if >= 1.21.10 {
        /*return info.getSkin().body().texturePath();
        *///?}
        //? if >= 1.21 && < 1.21.10 {
        return info.getSkin().texture();
        //?}
        //? if < 1.21 {
        /*return info.getSkinLocation();
        *///?}
    }
}
