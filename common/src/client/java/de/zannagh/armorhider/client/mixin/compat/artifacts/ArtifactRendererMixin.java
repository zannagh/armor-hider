//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.compat.artifacts;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compat for the <a href="https://modrinth.com/mod/artifacts">Artifacts</a> mod (issue #246).
 * <p>
 * Artifacts renders body accessories through its own {@code ArtifactRenderer}, submitting a custom
 * model straight to the {@link SubmitNodeCollector} — it never goes through vanilla
 * {@code EquipmentLayerRenderer}, so armor-hider's normal armor/elytra interception never sees it.
 * Every accessory provider (Curios on NeoForge, Trinkets/Accessories on Fabric) funnels its body
 * draw through {@code ArtifactRenderer.renderModelWithFoil}, so a single hook there covers all
 * providers and both loaders.
 * <p>
 * Accessories are not tied to a vanilla armor slot, so there is no per-slot opacity — this is a plain
 * "hide accessories" visibility toggle ({@link PlayerConfig#hideAccessories}). Elytra-like items are
 * never rendered through {@code ArtifactRenderer} (an elytra in any slot follows the chest slider via
 * the elytra path instead), so no elytra carve-out is needed here.
 * <p>
 * {@code @Pseudo} + {@code require = 0}: Artifacts is an optional third-party mod, so the target class
 * is absent unless it is installed, and the hook degrades to a no-op if a future Artifacts version
 * renames the method rather than crashing the game.
 *
 * @since 0.12.x
 */
@Pseudo
@Mixin(targets = "artifacts.client.item.renderer.ArtifactRenderer", remap = false)
public class ArtifactRendererMixin {

    // Bind by bare method name (renderModelWithFoil is unique on ArtifactRenderer) so no
    // Minecraft-typed descriptor is emitted into the selector — a remap=false @Pseudo mixin does
    // not remap the selector, and a mapped descriptor would fail to match in production.
    @Inject(method = "renderModelWithFoil", at = @At("HEAD"), cancellable = true, require = 0)
    private static void armorHider$hideAccessory(Model<?> model,
                                                 HumanoidRenderState renderState,
                                                 PoseStack poseStack,
                                                 SubmitNodeCollector submitNodeCollector,
                                                 Identifier texture,
                                                 int packedLight,
                                                 boolean hasFoil,
                                                 CallbackInfo ci) {
        if (!(renderState instanceof IdentityCarrier carrier)) {
            return;
        }
        String playerName = carrier.armorHider$playerName();
        if (playerName == null) {
            return;
        }
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);
        // shouldUseVanilla covers the master "disable armor hider" switch (incl. the local keybind
        // override), server force-off and the "disable for others" preference — an accessory hide
        // must yield to all of them, exactly like a slot opacity would.
        if (SlotModification.shouldUseVanilla(config)) {
            return;
        }
        if (config.hideAccessories.getValue()) {
            ci.cancel();
        }
    }
}
//?}
