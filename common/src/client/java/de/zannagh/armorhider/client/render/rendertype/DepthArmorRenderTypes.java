package de.zannagh.armorhider.client.render.rendertype;

//? if >= 26.3-0.snapshot.2 {
/*import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 26.3 replacement for the removed RenderTypes.armorTranslucent(Identifier): the depth-WRITING
// translucent armor type handed out under an active shaderpack (see
// ArmorHiderRenderTypes#translucentArmor).
//
// 26.3 dropped the dedicated ARMOR_TRANSLUCENT pipeline and its render type; vanilla armor now only
// has the cutout types, and translucent entities go through ENTITY_TRANSLUCENT. This rebuilds the
// 26.2 "armor_translucent" setup on that vanilla pipeline: same texture/lightmap/overlay bindings,
// VIEW_OFFSET_Z_LAYERING (so the piece doesn't z-fight the body it is layered over), crumbling,
// upload sorting and outline behaviour, plus the OIT_ENTITY set that every 26.3 translucent type
// needs to draw under the "Improved Transparency" option. It deliberately uses Minecraft's own
// pipeline instead of a clone so Iris' shadow-pass override map still knows it.
// (Line comments on purpose: the class body sits inside a stonecutter block comment on < 26.3.)
final class DepthArmorRenderTypes {

    private DepthArmorRenderTypes() {}

    private static final Map<Identifier, RenderType> ARMOR_TRANSLUCENT_DEPTH = new ConcurrentHashMap<>();

    static RenderType armorTranslucent(Identifier texture) {
        return ARMOR_TRANSLUCENT_DEPTH.computeIfAbsent(texture, DepthArmorRenderTypes::create);
    }

    private static RenderType create(Identifier texture) {
        return RenderType.create("armor_hider_armor_translucent_depth",
                RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                        .setOitPipelines(RenderPipelines.OIT_ENTITY)
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .useOverlay()
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                        .createRenderSetup());
    }
}
*///?}
