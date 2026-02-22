package de.zannagh.armorhider.mixin.client.lang;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.ArmorHider;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.core.appender.rolling.action.IfAccumulatedFileCount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
            }

            // Always load en_us as fallback if not already in the list
            if (!languages.contains("en_us")) {
                armorHider$loadModTranslations("en_us", stringMap);
            }

            // Clean up thread local
            armorHider$currentLanguages.remove();
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
        return original.call();
    }

    @Unique
    private static void armorHider$loadModTranslations(String langCode, Map<String, String> map) {
        String resourcePath = String.format(Locale.ROOT, "/assets/armor-hider/lang/%s.json", langCode);

        try (InputStream stream = ClientLanguageMixin.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                JsonObject json = armorHider$GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
                if (json != null) {
                    int count = 0;
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        String key = entry.getKey();
                        if (entry.getValue().isJsonPrimitive()) {
                            String value = entry.getValue().getAsString();
                            map.put(key, value);
                            count++;
                        }
                    }
                    ArmorHider.LOGGER.debug("Loaded {} translations from {}", count, resourcePath);
                }
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to load translations from {}: {}", resourcePath, e.getMessage());
        }
    }
}
