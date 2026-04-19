package de.zannagh.armorhider.client.rendering;

import net.minecraft.client.renderer.Sheets;
import java.util.concurrent.ConcurrentHashMap;

//?if >= 1.21.11 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.resources.Identifier;
//? } elif >= 1.21.5 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
*///?} elif >= 1.21.4 {
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
*///?} else {
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
*///?}

//? if >= 26.1-0.snapshot.10 {
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.ColorTargetState;
//?}
//? if >= 26.2-0.snapshot {
/*import com.mojang.blaze3d.pipeline.BindGroupLayout;
*///?}

import java.util.Optional;
import java.util.function.Function;

/**
 * Custom render types identical to vanilla translucent types but with depth writing disabled.
 * Prevents semi-transparent armor/items from occluding translucent terrain (water, ice) behind them.
 */
//? if < 1.21.11 {
/*public final class ArmorHiderRenderTypes extends RenderStateShard {
    private ArmorHiderRenderTypes() { super("armor_hider_dummy", () -> {}, () -> {}); }
*///?} else {
public final class ArmorHiderRenderTypes {
    private ArmorHiderRenderTypes() {}
//?}

    private static <T, R> Function<T, R> memoize(Function<T, R> fn) {
        var cache = new ConcurrentHashMap<T, R>();
        return t -> cache.computeIfAbsent(t, fn);
    }

    // --- Pipelines (>= 1.21.5) ---

    //? if >= 26.2-0.snapshot {
    /*private static RenderPipeline clonePipelineNoDepthWrite(RenderPipeline src, Identifier location) {
        var srcDss = src.getDepthStencilState();
        var noDss = new DepthStencilState(srcDss.depthTest(), false, srcDss.depthBiasScaleFactor(), srcDss.depthBiasConstant());
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getBindGroupLayouts()),
                Optional.of(src.getColorTargetState()),
                Optional.of(noDss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), Optional.of(src.getVertexFormat()),
                Optional.of(src.getVertexFormatMode()));
        return RenderPipeline.builder(snippet).withLocation(location).build();
    }

    private static final RenderPipeline ARMOR_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ARMOR_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_translucent_no_depth"));

    private static final RenderPipeline ENTITY_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ENTITY_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/entity_translucent_no_depth"));

    private static final RenderPipeline ITEM_ENTITY_TRANSLUCENT_CULL_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ITEM_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/item_translucent_no_depth"));
    *///?} elif >= 26.1-0.snapshot.10 {
    private static RenderPipeline clonePipelineNoDepthWrite(RenderPipeline src, Identifier location) {
        var srcDss = src.getDepthStencilState();
        var noDss = new DepthStencilState(srcDss.depthTest(), false, srcDss.depthBiasScaleFactor(), srcDss.depthBiasConstant());
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getSamplers()),
                Optional.of(src.getUniforms()), Optional.of(src.getColorTargetState()),
                Optional.of(noDss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), Optional.of(src.getVertexFormat()),
                Optional.of(src.getVertexFormatMode()));
        return RenderPipeline.builder(snippet).withLocation(location).build();
    }

    private static final RenderPipeline ARMOR_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ARMOR_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_translucent_no_depth"));

    private static final RenderPipeline ENTITY_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ENTITY_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/entity_translucent_no_depth"));

    private static final RenderPipeline ITEM_ENTITY_TRANSLUCENT_CULL_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ITEM_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/item_translucent_no_depth"));
    //? } elif >= 1.21.5 {
    /*private static RenderPipeline clonePipelineNoDepthWrite(RenderPipeline src, Identifier location) {
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getSamplers()),
                Optional.of(src.getUniforms()), src.getBlendFunction(),
                Optional.of(src.getDepthTestFunction()), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), Optional.of(src.isWriteColor()),
                Optional.of(src.isWriteAlpha()), Optional.of(false),
                Optional.of(src.getColorLogic()), Optional.of(src.getVertexFormat()),
                Optional.of(src.getVertexFormatMode()));
        return RenderPipeline.builder(snippet)
                .withLocation(location)
                .withDepthBias(src.getDepthBiasScaleFactor(), src.getDepthBiasConstant())
                .build();
    }

    private static final RenderPipeline ARMOR_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ARMOR_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_translucent_no_depth"));

    private static final RenderPipeline ENTITY_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ENTITY_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/entity_translucent_no_depth"));

    private static final RenderPipeline ITEM_ENTITY_TRANSLUCENT_CULL_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ITEM_ENTITY_TRANSLUCENT_CULL,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/item_entity_translucent_cull_no_depth"));
    *///?}

    // --- Render types ---

    //? if >= 1.21.11 {
    private static final Function<Identifier, RenderType> TRANSLUCENT_ARMOR = memoize(
            texture -> RenderType.create("armor_hider_armor_translucent_no_depth",
                    RenderSetup.builder(ARMOR_TRANSLUCENT_NO_DEPTH)
                            .withTexture("Sampler0", texture)
                            .useLightmap()
                            .useOverlay()
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .affectsCrumbling()
                            .sortOnUpload()
                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup())
    );

    private static final Function<Identifier, RenderType> TRANSLUCENT_ENTITY = memoize(
            texture -> RenderType.create("armor_hider_entity_translucent_no_depth",
                    RenderSetup.builder(ENTITY_TRANSLUCENT_NO_DEPTH)
                            .withTexture("Sampler0", texture)
                            .useLightmap()
                            .useOverlay()
                            .affectsCrumbling()
                            .sortOnUpload()
                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup())
    );
    //? } elif >= 1.21.5 {
    /*private static final Function<Identifier, RenderType> TRANSLUCENT_ARMOR = memoize(
            texture -> RenderType.create("armor_hider_armor_translucent_no_depth", 1536, true, true,
                    ARMOR_TRANSLUCENT_NO_DEPTH,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false))
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .createCompositeState(true))
    );

    private static final Function<Identifier, RenderType> TRANSLUCENT_ENTITY = memoize(
            texture -> RenderType.create("armor_hider_entity_translucent_no_depth", 1536, true, true,
                    ENTITY_TRANSLUCENT_NO_DEPTH,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false))
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(true))
    );
    *///?} elif >= 1.21.4 {
    /*private static final Function<Identifier, RenderType> TRANSLUCENT_ARMOR = memoize(
            texture -> RenderType.create("armor_hider_armor_translucent_no_depth",
                    DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ARMOR_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true))
    );

    private static final Function<Identifier, RenderType> TRANSLUCENT_ENTITY = memoize(
            texture -> RenderType.create("armor_hider_entity_translucent_no_depth",
                    DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true))
    );
    *///?} else {
    /*private static final Function<Identifier, RenderType> TRANSLUCENT_ARMOR = memoize(
            texture -> RenderType.create("armor_hider_armor_translucent_no_depth",
                    DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true))
    );

    private static final Function<Identifier, RenderType> TRANSLUCENT_ENTITY = TRANSLUCENT_ARMOR;
    *///?}

    // --- Item sheet types ---

    //? if >= 1.21.11 {
    private static final RenderType TRANSLUCENT_ITEM_SHEET = RenderType.create(
            "armor_hider_item_translucent_cull_no_depth",
            RenderSetup.builder(ITEM_ENTITY_TRANSLUCENT_CULL_NO_DEPTH)
                    .withTexture("Sampler0", net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                    .useLightmap()
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
    );
    //? } elif >= 1.21.5 {
    /*private static final RenderType TRANSLUCENT_ITEM_SHEET = RenderType.create(
            "armor_hider_item_translucent_cull_no_depth", 1536, true, true,
            ITEM_ENTITY_TRANSLUCENT_CULL_NO_DEPTH,
            RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, false))
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(true)
    );
    *///?} elif >= 1.21.4 {
    /*private static final RenderType TRANSLUCENT_ITEM_SHEET = RenderType.create(
            "armor_hider_item_translucent_cull_no_depth",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new TextureStateShard(
                            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );
    *///?} else {
    /*private static final RenderType TRANSLUCENT_ITEM_SHEET = RenderType.create(
            "armor_hider_item_translucent_cull_no_depth",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new TextureStateShard(
                            net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );
    *///?}

    // --- Public API ---

    public static RenderType translucentArmor(Identifier texture) {
        return TRANSLUCENT_ARMOR.apply(texture);
    }

    public static RenderType translucentEntity(Identifier texture) {
        return TRANSLUCENT_ENTITY.apply(texture);
    }

    public static RenderType translucentArmorTrim() {
        return translucentArmor(Sheets.ARMOR_TRIMS_SHEET);
    }

    public static RenderType translucentItemSheet() {
        return TRANSLUCENT_ITEM_SHEET;
    }
}
