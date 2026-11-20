package de.zannagh.armorhider.client.render.rendertype;

//? if >= 26.4-0.snapshot.1 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.pipeline.variant.EntityRenderPipelineVariant;
import net.minecraft.client.renderer.pipeline.variant.ItemRenderPipelineVariant;

import java.util.function.Function;

// 26.4 replaced the static translucent pipelines (RenderPipelines.ENTITY_TRANSLUCENT / ITEM_TRANSLUCENT
// plus the separate OIT_ENTITY / OIT_ITEM sets handed to RenderSetupBuilder.setOitPipelines) with dynamic
// pipelines built from a variant and an OitStage. A RenderSetup now takes one Function<OitStage,
// RenderPipeline>: RenderType asks it with a null stage for the regular (non-OIT) pipeline and with each
// OitStage for the "Improved Transparency" passes.
//
// These providers keep the 26.3 behaviour: the non-OIT stage uses our own depth-write-free clone, every
// OIT stage uses the vanilla translucent variant (the 26.3 OIT_ENTITY / OIT_ITEM equivalent).
// (Line comments on purpose: the class body sits inside a stonecutter block comment before 26.4.)
final class RenderPipelineProviders {

    private RenderPipelineProviders() {}

    // Vanilla's entity_translucent pipeline (non-OIT), the base the no-depth armor/entity clones copy.
    static RenderPipeline entityTranslucent() {
        return RenderPipelines.entity(new RenderPipelines.EntityParameters(EntityRenderPipelineVariant.TRANSLUCENT, null));
    }

    // Vanilla's item_translucent pipeline (non-OIT), the base of the no-depth item sheet clone.
    static RenderPipeline itemTranslucent() {
        return RenderPipelines.item(new RenderPipelines.ItemParameters(ItemRenderPipelineVariant.TRANSLUCENT, null));
    }

    // Vanilla entity translucent for every stage: the depth-WRITING path (DepthArmorRenderTypes).
    static Function<OitStage, RenderPipeline> entityTranslucentAllStages() {
        return oit -> RenderPipelines.entity(new RenderPipelines.EntityParameters(EntityRenderPipelineVariant.TRANSLUCENT, oit));
    }

    // nonOit for the regular pass, vanilla entity translucent for the OIT passes.
    static Function<OitStage, RenderPipeline> entityWithNonOit(RenderPipeline nonOit) {
        return oit -> oit == null
                ? nonOit
                : RenderPipelines.entity(new RenderPipelines.EntityParameters(EntityRenderPipelineVariant.TRANSLUCENT, oit));
    }

    // nonOit for the regular pass, vanilla item translucent for the OIT passes.
    static Function<OitStage, RenderPipeline> itemWithNonOit(RenderPipeline nonOit) {
        return oit -> oit == null
                ? nonOit
                : RenderPipelines.item(new RenderPipelines.ItemParameters(ItemRenderPipelineVariant.TRANSLUCENT, oit));
    }
}
*///?}
