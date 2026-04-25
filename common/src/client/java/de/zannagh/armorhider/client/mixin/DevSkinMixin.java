package de.zannagh.armorhider.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
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
    @Unique
    //? if >= 1.21.9
    private static final String SKIN_METHOD = "get";
    //? if >= 1.21.4 && < 1.21.9
    //private static final String SKIN_METHOD = "getOrLoad";
    //? if < 1.21.4
    //private static final String SKIN_METHOD = "get";

    @WrapOperation(
            method = SKIN_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/authlib/minecraft/MinecraftSessionService;getPackedTextures(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/properties/Property;"
            )
    )
    private Property injectDevSkinTextures(MinecraftSessionService service, GameProfile profile, Operation<Property> original) {
        Property result = original.call(service, profile);
        if (getDevTextures() != null) {
            ArmorHider.LOGGER.debug("[DevSkin] Injecting dev skin textures for profile: {}", profile);
            return new Property("textures", getDevTextures(), getDevSignature());
        }
        return result;
    }
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
