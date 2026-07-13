package de.zannagh.armorhider.client.mixin.lang;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.ArmorHider;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mixin to inject mod translations into the language system without Fabric API.
 * This loads lang files from the mod's resources and adds them to the translation map.
 * <p>
 * This class is claude generated :)
 */
@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {

    @Unique
    private static final Gson armorHider$GSON = new Gson();

    @Unique
    private static final ThreadLocal<List<String>> armorHider$currentLanguages = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<ResourceManager> armorHider$currentResourceManager = new ThreadLocal<>();

    @WrapOperation(
            method = "loadFrom",
            at = @At(
                    value = "INVOKE",
                    //? if > 1.21.1
                    target = "Ljava/util/Map;copyOf(Ljava/util/Map;)Ljava/util/Map;"
                    //? if <= 1.21.1
                    //target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"
            )
    )
    //? if > 1.21.1
    private static <K, V> Map<K, V> injectTranslationsBeforeCopy(Map<K, V> map, Operation<Map<K, V>> original) {    
    //? if <= 1.21.1
    //private static <K, V> ImmutableMap<K, V> injectTranslationsBeforeCopy(Map<K, V> map, Operation<ImmutableMap<K, V>> original) {
        // Load translation map.
        @SuppressWarnings("unchecked")
        Map<String, String> stringMap = (Map<String, String>) map;

        List<String> languages = armorHider$currentLanguages.get();
        if (languages != null) {
            // Load translations for each requested language
            for (String langCode : languages) {
                armorHider$loadModTranslations(langCode, stringMap);
                armorHider$loadTranslationsFromResourcePacks(langCode, stringMap);
            }

            // Always load en_us as fallback if not already in the list
            if (!languages.contains("en_us")) {
                armorHider$loadModTranslations("en_us", stringMap);
            }

            armorHider$currentLanguages.remove();
            armorHider$currentResourceManager.remove();
        }
        return original.call(map);
    }

    /**
     * Capture the language codes being loaded so we can use them in the wrap operation.
     * Targets: Map<String, String> map = new HashMap();
     */
    @WrapOperation(
            method = "loadFrom",
            at = @At(
                    //? if > 1.21.1
                    value = "NEW",
                    //? if > 1.21.1
                    target = "java/util/HashMap"
                    //? if <= 1.21.1
                    //value = "INVOKE",
                    //? if <= 1.21.1
                    //target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"
            )
    )
    private static <K, V> java.util.HashMap<K, V> captureLanguages(
            Operation<java.util.HashMap<K, V>> original,
            ResourceManager resourceManager,
            List<String> languageCodes,
            boolean rightToLeft
    ) {
        armorHider$currentLanguages.set(languageCodes);
        armorHider$currentResourceManager.set(resourceManager);
        return original.call();
    }

    @Unique
    private static void armorHider$loadModTranslations(String langCode, Map<String, String> map) {
        String resourcePath = String.format(Locale.ROOT, "/assets/armor-hider/lang/%s.json", langCode);
        try (InputStream stream = ClientLanguageMixin.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                armorHider$parseTranslations(stream, resourcePath, map);
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to load translations from {}: {}", resourcePath, e.getMessage());
        }
    }

    @Unique
    private static void armorHider$loadTranslationsFromResourcePacks(String langCode, Map<String, String> map) {
        // Load from non-activated packs on the filesystem first (lowest priority)
        armorHider$loadFromResourcePacksDirectory(langCode, map);

        // Activated resource packs override via ResourceManager (highest priority)
        ResourceManager resourceManager = armorHider$currentResourceManager.get();
        if (resourceManager != null) {
            armorHider$loadFromResourceManager(resourceManager, langCode, map);
        }
    }

    @Unique
    private static void armorHider$loadFromResourceManager(ResourceManager resourceManager, String langCode, Map<String, String> map) {
        //? if >= 1.20.5 {
        var location = ResourceLocation.parse("armor-hider:lang/" + langCode + ".json");
        //?} else {
        /*var location = new ResourceLocation("armor-hider", "lang/" + langCode + ".json");
        *///?}
        resourceManager.getResource(location).ifPresent(resource -> {
            try (InputStream stream = resource.open()) {
                armorHider$parseTranslations(stream, "resource pack [" + langCode + "]", map);
            } catch (Exception e) {
                ArmorHider.LOGGER.warn("Failed to load translations from resource pack for {}: {}", langCode, e.getMessage());
            }
        });
    }

    @Unique
    private static void armorHider$loadFromResourcePacksDirectory(String langCode, Map<String, String> map) {
        Path resourcePacksDir = Path.of("resourcepacks");
        if (!Files.isDirectory(resourcePacksDir)) {
            return;
        }
        try (var packEntries = Files.list(resourcePacksDir)) {
            packEntries.filter(Files::isDirectory).forEach(packDir -> {
                Path langFile = packDir.resolve("assets/armor-hider/lang/" + langCode + ".json");
                if (Files.isRegularFile(langFile)) {
                    try (InputStream stream = Files.newInputStream(langFile)) {
                        armorHider$parseTranslations(stream, langFile.toString(), map);
                    } catch (Exception e) {
                        ArmorHider.LOGGER.warn("Failed to load translations from {}: {}", langFile, e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            ArmorHider.LOGGER.warn("Failed to scan resource packs for translations: {}", e.getMessage());
        }
    }

    @Unique
    private static void armorHider$parseTranslations(InputStream stream, String source, Map<String, String> map) {
        JsonObject json = armorHider$GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
        if (json == null) {
            return;
        }
        int count = 0;
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
                count++;
            }
        }
        ArmorHider.LOGGER.debug("Loaded {} translations from {}", count, source);
    }
}
