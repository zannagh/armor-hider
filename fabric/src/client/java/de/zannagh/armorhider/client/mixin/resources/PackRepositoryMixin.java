//? if >= 1.21 {
package de.zannagh.armorhider.client.mixin.resources;

import de.zannagh.armorhider.client.resources.ArmorHiderModPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

/**
 * When fabric-resource-loader-v0 is absent, vanilla Fabric Loader does not mount mod jars
 * as resource packs — so the armor-hider namespace is invisible to the ResourceManager and
 * its gui sprites render as the missing-texture checkerboard. We append our own
 * {@link ArmorHiderModPackSource} to the varargs passed into the {@link PackRepository}
 * constructor (which stores them as an {@code ImmutableSet}, so modifying the field after
 * construction would throw — we have to inject before {@code ImmutableSet.copyOf}).
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {

    @ModifyArg(
            method = "<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V",
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableSet;copyOf([Ljava/lang/Object;)Lcom/google/common/collect/ImmutableSet;")
    )
    private static Object[] armorHider$appendModPackSource(Object[] sources) {
        if (de.zannagh.armorhider.CompatManager.requiresCompatTo(de.zannagh.armorhider.api.CompatFlags.FABRIC_API_RESOURCE_LOADER)) {
            return sources;
        }
        var combined = Arrays.copyOf(sources, sources.length + 1, RepositorySource[].class);
        combined[sources.length] = new ArmorHiderModPackSource();
        return combined;
    }
}
//?}
