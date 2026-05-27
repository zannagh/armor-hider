//? if >= 1.21 {
package de.zannagh.armorhider.client.mixin.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.VanillaRootAccessor;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.parts.EMFModelPart;

@Pseudo
@Mixin(value = EMFModelPart.class, remap = false)
public abstract class EmfModelPartMixin {

    @Unique
    private static int armorHider$logCounter = 0;

    @Unique
    private static final java.util.Set<Integer> armorHider$renderedVanillaRoots = new java.util.HashSet<>();

    @Unique
    private static long armorHider$lastFrameTime = 0;

    @Unique
    private static boolean armorHider$shouldForceVanillaByPlayer() {
        String playerName = ArmorHiderClient.RENDER_CONTEXT.currentPlayerName();
        if (playerName == null) return false;
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) return false;
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
        if (!armorHider$shouldApplyCombatDetection(config)) return false;
        if (!CombatManager.isInCombat(playerName)) return false;
        return config.inCombatUseDefaultModel.getValue();
    }

    @Unique
    private static boolean armorHider$shouldApplyCombatDetection(PlayerConfig config) {
        if (config.enableCombatDetection.getValue()) return true;
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        return serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void armorHider$renderVanillaWhenForced(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int k, CallbackInfo ci) {
        boolean emfForced = EMFAnimationEntityContext.isEntityForcedToVanillaModel();
        boolean playerForced = armorHider$shouldForceVanillaByPlayer();
        if (!emfForced && !playerForced) return;

        if (this instanceof VanillaRootAccessor accessor) {
            ModelPart vanilla = accessor.armorHider$getVanillaRoot();
            if (vanilla != null) {
                long now = System.nanoTime() / 1_000_000;
                if (now != armorHider$lastFrameTime) {
                    armorHider$renderedVanillaRoots.clear();
                    armorHider$lastFrameTime = now;
                }
                int id = System.identityHashCode(vanilla);
                if (armorHider$renderedVanillaRoots.add(id)) {
                    if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
                        DebugLogger.log("[EMF mixin] REDIRECT to vanillaRoot | id={} | player={} | class={}", id, ArmorHiderClient.RENDER_CONTEXT.currentPlayerName(), this.getClass().getSimpleName());
                    }
                    vanilla.render(matrices, vertices, light, overlay, k);
                } else if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
                    DebugLogger.log("[EMF mixin] DEDUP vanillaRoot | id={} | player={} | class={}", id, ArmorHiderClient.RENDER_CONTEXT.currentPlayerName(), this.getClass().getSimpleName());
                }
                ci.cancel();
                return;
            }
        }

        if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
            DebugLogger.log("[EMF mixin] CANCEL (non-root) | player={} | class={}", ArmorHiderClient.RENDER_CONTEXT.currentPlayerName(), this.getClass().getSimpleName());
        }
        ci.cancel();
    }
}
//?}

//? if < 1.21 {
/*package de.zannagh.armorhider.client.mixin.compat.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.VanillaRootAccessor;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.parts.EMFModelPart;

@Pseudo
@Mixin(value = EMFModelPart.class, remap = false)
public abstract class EmfModelPartMixin {

    @Unique
    private static int armorHider$logCounter = 0;

    @Unique
    private static boolean armorHider$shouldForceVanillaByPlayer() {
        String playerName = ArmorHiderClient.RENDER_CONTEXT.currentPlayerName();
        if (playerName == null) return false;
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) return false;
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
        if (!armorHider$shouldApplyCombatDetection(config)) return false;
        if (!CombatManager.isInCombat(playerName)) return false;
        return config.inCombatUseDefaultModel.getValue();
    }

    @Unique
    private static boolean armorHider$shouldApplyCombatDetection(PlayerConfig config) {
        if (config.enableCombatDetection.getValue()) return true;
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        return serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void armorHider$renderVanillaWhenForced(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        boolean emfForced = EMFAnimationEntityContext.isEntityForcedToVanillaModel();
        boolean playerForced = armorHider$shouldForceVanillaByPlayer();
        if (!emfForced && !playerForced) return;

        if (this instanceof VanillaRootAccessor accessor) {
            ModelPart vanilla = accessor.armorHider$getVanillaRoot();
            if (vanilla != null) {
                if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
                    DebugLogger.log("[EMF mixin] REDIRECT to vanillaRoot | player={} | class={}", ArmorHiderClient.RENDER_CONTEXT.currentPlayerName(), this.getClass().getSimpleName());
                }
                vanilla.render(matrices, vertices, light, overlay, red, green, blue, alpha);
                ci.cancel();
                return;
            }
        }

        if (DebugLogger.isEnabled() && armorHider$logCounter++ % 600 == 0) {
            DebugLogger.log("[EMF mixin] CANCEL (non-root) | player={} | class={}", ArmorHiderClient.RENDER_CONTEXT.currentPlayerName(), this.getClass().getSimpleName());
        }
        ci.cancel();
    }
}
*///?}
