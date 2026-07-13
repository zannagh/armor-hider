package de.zannagh.armorhider.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

//? if >= 1.21.4 {
/*import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
*///?}

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaArmorTextureManager {

    private static final ConcurrentHashMap<ResourceLocation, ResourceLocation> fallbackCache = new ConcurrentHashMap<>();
    private static final Object ABSENT = new Object();
    private static final ConcurrentHashMap<ResourceLocation, Object> negativeLookupCache = new ConcurrentHashMap<>();
    private static ResourceManager lastResourceManager;

    private static int logCounter = 0;

    private VanillaArmorTextureManager() {}

    public static ResourceLocation resolveArmorTexture(SlotModification mod, ResourceLocation texture) {
        if (mod.isEmpty() || mod.playerName().isBlank()) {
            return texture;
        }
        if (!shouldUseCombatVanillaTexture(mod.playerName())) {
            return texture;
        }

        ResourceLocation fallback = getVanillaFallback(texture);

        if (DebugLogger.isEnabled() && logCounter++ % 60 == 0) {
            DebugLogger.log("[VanillaTexture] player={} | texture={} | fallback={} | hasMod={}",
                    mod.playerName(), texture, fallback, !mod.isEmpty());
        }

        return fallback != null ? fallback : texture;
    }

    private static @Nullable ResourceLocation getVanillaFallback(ResourceLocation original) {
        invalidateCacheIfNeeded();

        ResourceLocation cached = fallbackCache.get(original);
        if (cached != null) {
            return cached;
        }
        if (negativeLookupCache.containsKey(original)) {
            return null;
        }

        return loadVanillaFallback(original);
    }

    private static synchronized @Nullable ResourceLocation loadVanillaFallback(ResourceLocation original) {
        ResourceLocation cached = fallbackCache.get(original);
        if (cached != null) {
            return cached;
        }
        if (negativeLookupCache.containsKey(original)) {
            return null;
        }

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        List<Resource> stack = resourceManager.getResourceStack(original);

        if (DebugLogger.isEnabled()) {
            DebugLogger.log("[VanillaTexture] Loading fallback for {} | resource itemStack size={} | sources={}",
                    original, stack.size(),
                    stack.stream().map(Resource::sourcePackId).toList());
        }

        if (stack.size() <= 1) {
            negativeLookupCache.put(original, ABSENT);
            if (DebugLogger.isEnabled()) {
                DebugLogger.log("[VanillaTexture] No override detected for {} — skipping", original);
            }
            return null;
        }

        Resource vanillaResource = stack.get(0);

        //? if >= 1.21 {
        ResourceLocation fallbackId = ResourceLocation.fromNamespaceAndPath("armor_hider",
                "vanilla_fallback/" + original.getNamespace() + "/" + original.getPath());
        //?} else {
        /*ResourceLocation fallbackId = new ResourceLocation("armor_hider",
                "vanilla_fallback/" + original.getNamespace() + "/" + original.getPath());
        *///?}

        try (InputStream in = vanillaResource.open()) {
            NativeImage image = NativeImage.read(in);
            //? if >= 1.21.8 {
            /*DynamicTexture dynamicTexture = new DynamicTexture(() -> "armor_hider_vanilla_" + original.getPath(), image);
            *///?} else {
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            //?}
            Minecraft.getInstance().getTextureManager().register(fallbackId, dynamicTexture);
            fallbackCache.put(original, fallbackId);
            ArmorHider.LOGGER.debug("Loaded vanilla fallback texture for {} (overridden by {} packs)", original, stack.size() - 1);
            if (DebugLogger.isEnabled()) {
                DebugLogger.log("[VanillaTexture] Registered fallback {} -> {} (vanilla source: {})",
                        original, fallbackId, vanillaResource.sourcePackId());
            }
            return fallbackId;
        } catch (IOException e) {
            ArmorHider.LOGGER.warn("Failed to load vanilla fallback texture for {}", original, e);
            if (DebugLogger.isEnabled()) {
                DebugLogger.log("[VanillaTexture] FAILED to load fallback for {}: {}", original, e.getMessage());
            }
            negativeLookupCache.put(original, ABSENT);
            return null;
        }
    }

    private static void invalidateCacheIfNeeded() {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if (current != lastResourceManager) {
            if (lastResourceManager != null) {
                var textureManager = Minecraft.getInstance().getTextureManager();
                for (ResourceLocation fallbackId : fallbackCache.values()) {
                    textureManager.release(fallbackId);
                }
                if (DebugLogger.isEnabled()) {
                    DebugLogger.log("[VanillaTexture] ResourceManager changed — cleared {} cached fallback textures",
                            fallbackCache.size());
                }
            }
            fallbackCache.clear();
            negativeLookupCache.clear();
            lastResourceManager = current;
        }
    }

    //? if >= 1.21.4 {
    /*public static @Nullable ResourceLocation resolveVanillaEquipmentTexture(
            ResourceKey<EquipmentAsset> assetKey,
            EquipmentClientInfo.LayerType layerType
    ) {
        invalidateCacheIfNeeded();

        //? if >= 1.21.11 {
        /^ResourceLocation assetLocation = assetKey.identifier();
        ^///?} else {
        ResourceLocation assetLocation = assetKey.location();
        //?}
        //? if >= 1.21 {
        ResourceLocation vanillaTexturePath = ResourceLocation.fromNamespaceAndPath(
                assetLocation.getNamespace(),
                "textures/entity/equipment/" + layerType.getSerializedName() + "/" + assetLocation.getPath() + ".png"
        );
        //?} else {
        /^ResourceLocation vanillaTexturePath = new ResourceLocation(
                assetLocation.getNamespace(),
                "textures/entity/equipment/" + layerType.getSerializedName() + "/" + assetLocation.getPath() + ".png"
        );
        ^///?}

        ResourceLocation cached = fallbackCache.get(vanillaTexturePath);
        if (cached != null) return cached;

        if (negativeLookupCache.containsKey(vanillaTexturePath)) return vanillaTexturePath;

        return loadVanillaEquipmentTexture(vanillaTexturePath);
    }

    private static synchronized @Nullable ResourceLocation loadVanillaEquipmentTexture(ResourceLocation vanillaTexturePath) {
        ResourceLocation cached = fallbackCache.get(vanillaTexturePath);
        if (cached != null) return cached;
        if (negativeLookupCache.containsKey(vanillaTexturePath)) return vanillaTexturePath;

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        List<Resource> stack = resourceManager.getResourceStack(vanillaTexturePath);

        if (DebugLogger.isEnabled()) {
            DebugLogger.log("[VanillaTexture] Resolving vanilla equipment texture {} | itemStack size={} | sources={}",
                    vanillaTexturePath, stack.size(),
                    stack.stream().map(Resource::sourcePackId).toList());
        }

        if (stack.isEmpty()) {
            negativeLookupCache.put(vanillaTexturePath, ABSENT);
            return null;
        }

        if (stack.size() == 1) {
            negativeLookupCache.put(vanillaTexturePath, ABSENT);
            if (DebugLogger.isEnabled()) {
                DebugLogger.log("[VanillaTexture] Only vanilla provides {} — using directly", vanillaTexturePath);
            }
            return vanillaTexturePath;
        }

        Resource vanillaResource = stack.get(0);

        //? if >= 1.21 {
        ResourceLocation fallbackId = ResourceLocation.fromNamespaceAndPath("armor_hider",
                "vanilla_fallback/" + vanillaTexturePath.getNamespace() + "/" + vanillaTexturePath.getPath());
        //?} else {
        /^ResourceLocation fallbackId = new ResourceLocation("armor_hider",
                "vanilla_fallback/" + vanillaTexturePath.getNamespace() + "/" + vanillaTexturePath.getPath());
        ^///?}

        try (InputStream in = vanillaResource.open()) {
            NativeImage image = NativeImage.read(in);
            //? if >= 1.21.8 {
            /^DynamicTexture dynamicTexture = new DynamicTexture(() -> "armor_hider_vanilla_" + vanillaTexturePath.getPath(), image);
            ^///?} else {
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            //?}
            Minecraft.getInstance().getTextureManager().register(fallbackId, dynamicTexture);
            fallbackCache.put(vanillaTexturePath, fallbackId);
            if (DebugLogger.isEnabled()) {
                DebugLogger.log("[VanillaTexture] Registered vanilla equipment fallback {} -> {} (vanilla source: {})",
                        vanillaTexturePath, fallbackId, vanillaResource.sourcePackId());
            }
            return fallbackId;
        } catch (IOException e) {
            ArmorHider.LOGGER.warn("Failed to load vanilla equipment texture for {}", vanillaTexturePath, e);
            negativeLookupCache.put(vanillaTexturePath, ABSENT);
            return null;
        }
    }
    *///?}

    private static boolean shouldUseCombatVanillaTexture(String playerName) {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderGloballyDisabled()) {
            return false;
        }
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);
        if (!config.inCombatUseDefaultModel.getValue()) {
            return false;
        }
        if (!config.enableCombatDetection.getValue()) {
            var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
            if (serverConfig == null || !serverConfig.serverWideSettings.enableCombatDetection.getValue()) {
                return false;
            }
        }
        return ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerName);
    }
}
