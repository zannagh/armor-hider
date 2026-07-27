package de.zannagh.armorhider.client.render.rendertype;
import net.minecraft.client.renderer.Sheets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//?if >= 1.21.11 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.resources.Identifier;
//? } elif >= 1.21.5 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
*///?} elif >= 1.21.2 {
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
*///?} else {
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
*///?}

//? if >= 26.1-0.snapshot.10 {
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.ColorTargetState;
//?}

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

    // Set of every translucent-no-depth-write render type this class hands out. These are the types
    // whose armor/entity draws must be deferred to the after-translucent-terrain render phase
    // (>= 26.2-1.pre) so translucent terrain (water, ice, stained glass) drawn afterwards cannot
    // overdraw the parts of a piece that aren't backed by an opaque body pixel — e.g. chestplate
    // shoulder pads silhouetted against a body of water. We store the exact memoized instances we
    // produce (and each carries a unique "armor_hider_*" name), so a plain contains-check is a
    // reliable membership test and the set stays tiny (one entry per texture actually used).
    private static final Set<Object> DEFERRED_TYPES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Whether the given render type is one of this mod's translucent-no-depth-write types that must
     * be routed into the after-terrain phase. Cross-version safe: takes {@code Object} so the
     * feature-dispatcher mixin can call it without importing the version-specific {@code RenderType}.
     *
     * @param renderType the render type a model submit is about to be enqueued with.
     * @return {@code true} when the draw should be deferred until after translucent terrain.
     */
    public static boolean isDeferredType(Object renderType) {
        return renderType != null && DEFERRED_TYPES.contains(renderType);
    }

    // Diagnostic counter of how many model submits have been deferred into the after-terrain phase.
    // The after-terrain redirect fails *silently* if the mixin target drifts between versions (the
    // piece just reverts to the old overdraw-by-water behaviour), so — matching this repo's
    // smoke-test convention of asserting a mixin actually fired rather than only "didn't crash" —
    // the water-scene game test asserts this climbs above zero.
    private static final java.util.concurrent.atomic.AtomicLong DEFERRED_SUBMIT_COUNT =
            new java.util.concurrent.atomic.AtomicLong();

    public static void recordDeferredSubmit() {
        DEFERRED_SUBMIT_COUNT.incrementAndGet();
    }

    public static long deferredSubmitCount() {
        return DEFERRED_SUBMIT_COUNT.get();
    }

    // Diagnostic counter: how many times the Female Gender Mod breast-armor render-type swap actually
    // ran and produced one of our translucent types. The swap is a @Pseudo @WrapOperation with
    // require=0, so it fails *silently* if it can't resolve its target — leaving the breast piece on
    // the alpha-tested armorCutoutNoCull type, where a faded (reduced-alpha) colour is discarded
    // wholesale and the piece vanishes instead of turning translucent. The gender smoke test fades
    // the breast and asserts this climbs, pinning down whether the swap fired.
    private static final java.util.concurrent.atomic.AtomicLong BREAST_ARMOR_TRANSLUCENT_SWAPS =
            new java.util.concurrent.atomic.AtomicLong();

    public static void recordBreastArmorTranslucentSwap() {
        BREAST_ARMOR_TRANSLUCENT_SWAPS.incrementAndGet();
    }

    public static long breastArmorTranslucentSwapCount() {
        return BREAST_ARMOR_TRANSLUCENT_SWAPS.get();
    }

    // Diagnostic counters for the Female Gender Mod breast-physics relaxation (GenderPhysicsMixin).
    // TICKS: the @ModifyReturnValue on PlayerConfig#getArmorPhysicsOverride fired at all (so the
    // injector resolved its target). RELAXED: it saw a fully-hidden chest and forced FGM's own
    // "Armor Physics Override" on — zeroing armor tightness and physics resistance, so the breasts
    // jiggle as if unarmored. The gender smoke asserts both climb while the chest is hidden.
    private static final java.util.concurrent.atomic.AtomicLong GENDER_PHYSICS_TICKS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong GENDER_PHYSICS_RELAXED =
            new java.util.concurrent.atomic.AtomicLong();

    public static void recordGenderPhysicsTick(boolean relaxed) {
        GENDER_PHYSICS_TICKS.incrementAndGet();
        if (relaxed) {
            GENDER_PHYSICS_RELAXED.incrementAndGet();
        }
    }

    public static long genderPhysicsTickCount() {
        return GENDER_PHYSICS_TICKS.get();
    }

    public static long genderPhysicsRelaxedCount() {
        return GENDER_PHYSICS_RELAXED.get();
    }

    // Test-only diagnostic switch. When flipped off, the after-terrain redirect is bypassed and the
    // translucent armor falls back to the pre-terrain phase — i.e. the pre-fix behaviour where water
    // overdraws the pads. The water-scene game test toggles this to capture a before/after and to
    // assert the redirect both fires when on and stays quiet when off. Always true in normal play.
    private static volatile boolean deferralEnabled = true;

    public static void setDeferralEnabled(boolean enabled) {
        deferralEnabled = enabled;
    }

    public static boolean isDeferralEnabled() {
        return deferralEnabled;
    }

    // --- Pipelines (>= 1.21.5) ---

    //? if >= 26.2-1.pre {
    private static RenderPipeline clonePipelineNoDepthWrite(RenderPipeline src, Identifier location) {
        var srcDss = src.getDepthStencilState();
        var noDss = new DepthStencilState(srcDss.depthTest(), false, srcDss.depthBiasScaleFactor(), srcDss.depthBiasConstant());
        // 26.3-snapshot-5 reworked RenderPipeline: the two shader accessors became a single
        // getShaders() map, and getColorTargetStates()/getVertexFormatBindings() now return
        // Lists (Snippet still takes arrays + an active-count int).
        //? if >= 26.3-0.snapshot.5 {
        /*var snippet = new RenderPipeline.Snippet(
                src.getShaders(),
                Optional.of(src.getShaderDefines()), Optional.of(src.getBindGroupLayouts()),
                src.getColorTargetStates().toArray(new ColorTargetState[0]), src.getColorTargetStates().size(),
                Optional.of(noDss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), src.getVertexFormatBindings().toArray(new com.mojang.renderpearl.api.vertex.VertexFormat[0]),
                Optional.of(src.getPrimitiveTopology()));
        *///?} else {
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getBindGroupLayouts()),
                src.getColorTargetStates(), src.getColorTargetStates().length,
                Optional.of(noDss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), src.getVertexFormatBindings(),
                Optional.of(src.getPrimitiveTopology()));
        //?}
        return RenderPipeline.builder(snippet).withLocation(location).build();
    }

    private static final RenderPipeline ARMOR_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            // 26.3 removed RenderPipelines.ARMOR_TRANSLUCENT; armor now renders through the
            // entity translucent pipeline, so we clone that as the depth-disabled armor base.
            //? if >= 26.3-0.snapshot.2 {
            /*RenderPipelines.ENTITY_TRANSLUCENT,
            *///?} else {
            RenderPipelines.ARMOR_TRANSLUCENT,
            //?}
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_translucent_no_depth"));

    private static final RenderPipeline ENTITY_TRANSLUCENT_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ENTITY_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/entity_translucent_no_depth"));

    private static final RenderPipeline ITEM_ENTITY_TRANSLUCENT_CULL_NO_DEPTH = clonePipelineNoDepthWrite(
            RenderPipelines.ITEM_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/item_translucent_no_depth"));
    //?} elif >= 26.2-0.snapshot {
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
    /*private static RenderPipeline clonePipelineNoDepthWrite(RenderPipeline src, Identifier location) {
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
    *///? } elif >= 1.21.5 {
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

    //? if >= 1.21.5 {
    public static RenderPipeline[] pipelines() {
        return new RenderPipeline[] {
                ARMOR_TRANSLUCENT_NO_DEPTH,
                ENTITY_TRANSLUCENT_NO_DEPTH,
                ITEM_ENTITY_TRANSLUCENT_CULL_NO_DEPTH
        };
    }
    //?}

    // --- Render types ---

    //? if >= 1.21.11 {
    private static final Function<Identifier, RenderType> TRANSLUCENT_ARMOR = memoize(
            texture -> RenderType.create("armor_hider_armor_translucent_no_depth",
                    RenderSetup.builder(ARMOR_TRANSLUCENT_NO_DEPTH)
                            // 26.3 OIT: a translucent type must carry an OIT set to fade under the
                            // "Improved Transparency" option (drawFromBufferOit throws without it). We do
                            // NOT set an opaque-parts pipeline: opaqueParts makes bothSolidAndTranslucent()
                            // true, which routes the model into the solid phase too — and since we reduce
                            // the whole model's alpha uniformly, that opaque copy just renders it fully
                            // opaque. OIT-only (no opaque parts) fades the entire piece.
                            //? if >= 26.3-0.snapshot.2 {
                            /*.setOitPipelines(RenderPipelines.OIT_ENTITY)
                            *///?}
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
                            //? if >= 26.3-0.snapshot.2 {
                            /*.setOitPipelines(RenderPipelines.OIT_ENTITY)
                            *///?}
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
    *///?} elif >= 1.21.2 {
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
                    //? if >= 26.3-0.snapshot.2 {
                    /*.setOitPipelines(RenderPipelines.OIT_ITEM)
                    *///?}
                    .withTexture("Sampler0", net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                    // 26.3 removed RenderSetupBuilder.setOutputTarget (and OutputTarget.ITEM_ENTITY_TARGET);
                    // item entities now draw to the default target, so the call is simply dropped.
                    //? if < 26.3-0.snapshot.2
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .useLightmap()
                    .useOverlay()
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
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true)
    );
    *///?} elif >= 1.21.2 {
    /*private static final RenderType TRANSLUCENT_ITEM_SHEET = RenderType.create(
            "armor_hider_item_translucent_cull_no_depth",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new TextureStateShard(
                            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
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
        RenderType renderType = TRANSLUCENT_ARMOR.apply(texture);
        DEFERRED_TYPES.add(renderType);
        return renderType;
    }

    public static RenderType translucentEntity(Identifier texture) {
        RenderType renderType = TRANSLUCENT_ENTITY.apply(texture);
        DEFERRED_TYPES.add(renderType);
        return renderType;
    }

    public static RenderType translucentArmorTrim() {
        // 26.3 removed the single Sheets.ARMOR_TRIMS_SHEET atlas — trims are now per-material
        // paletted textures (EquipmentLayerRenderer.TrimTextureKey / PalettedTextureManager).
        // This translucent-trim path is dormant on 26.3 (the trim-interception mixins target the
        // now-removed Sheets.armorTrimsSheet and no-op), so we return a valid translucent item
        // sheet as a compile-safe placeholder pending a paletted-trim redesign.
        //? if >= 26.3-0.snapshot.2 {
        /*return translucentItemSheet();
        *///?} else {
        return translucentArmor(Sheets.ARMOR_TRIMS_SHEET);
        //?}
    }

    public static RenderType translucentItemSheet() {
        return TRANSLUCENT_ITEM_SHEET;
    }
}