package de.zannagh.armorhider.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
//? if >= 26.3-0.snapshot.2 {
import com.mojang.authlib.minecraft.SessionService;
//?} else {
/*import com.mojang.authlib.minecraft.MinecraftSessionService;
*///?}
import com.mojang.authlib.properties.Property;
import de.zannagh.armorhider.ArmorHider;
import net.minecraft.client.resources.SkinManager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkinManager.class)
public class DevSkinMixin {
    
    //? if >= 1.21.1 {
    //~ if >= 1.21.4 && < 1.21.9 '"get"' -> '"getOrLoad"' {
    @WrapOperation(
            method = "get",
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3-0.snapshot.2 {
                    target = "Lcom/mojang/authlib/minecraft/SessionService;getPackedTextures(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/properties/Property;"
                    //?} else {
                    /*target = "Lcom/mojang/authlib/minecraft/MinecraftSessionService;getPackedTextures(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/properties/Property;"
                    *///?}
            )
    )
    //? if >= 26.3-0.snapshot.2 {
    private Property injectDevSkinTextures(SessionService service, GameProfile profile, Operation<Property> original) {
    //?} else {
    /*private Property injectDevSkinTextures(MinecraftSessionService service, GameProfile profile, Operation<Property> original) {
    *///?}
        String devTextures = getDevTextures();
        if (devTextures != null) {
            ArmorHider.LOGGER.debug("[DevSkin] Injecting dev skin textures for profile: {}", profile);
            return new Property("textures", devTextures, getDevSignature());
        }
        return original.call(service, profile);
    }
    //~}
    //?}
    
    @Unique
    @Nullable
    private static String getDevTextures(){
        return System.getProperty("armorhider.dev.skin.textures");
    }
    
    @Unique
    @Nullable
    private static String getDevSignature(){
        return System.getProperty("armorhider.dev.skin.signature");
    }
}
