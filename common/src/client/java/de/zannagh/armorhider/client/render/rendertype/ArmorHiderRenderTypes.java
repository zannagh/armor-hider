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
    // overdraw the parts of a piece that aren't backed by an opaque body pixel - e.g. chestplate
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
    // piece just reverts to the old overdraw-by-water behaviour), so - matching this repo's
    // smoke-test convention of asserting a mixin actually fired rather than only "didn't crash" -
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
    // require=0, so it fails *silently* if it can't resolve its target - leaving the breast piece on
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
    // "Armor Physics Override" on - zeroing armor tightness and physics resistance, so the breasts
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

    // Diagnostic counter for the First Person Model layer guards (FirstPersonCompat). Counts how often
    // we recognised FPM's first-person body and declined to enter a render scope for a layer submit FPM
    // is about to cancel at its HEAD. Without the guard the scope is entered and never exited (the
    // cancelled submit skips our @At("RETURN") release), leaking it into the rest of the frame. The
    // first-person smoke asserts this climbs and that no scope is left active afterwards.
    private static final java.util.concurrent.atomic.AtomicLong FIRST_PERSON_LAYER_GUARDS =
            new java.util.concurrent.atomic.AtomicLong();

    public static void recordFirstPersonLayerGuard() {
        FIRST_PERSON_LAYER_GUARDS.incrementAndGet();
    }

    public static long firstPersonLayerGuardCount() {
        return FIRST_PERSON_LAYER_GUARDS.get();
    }

    // Diagnostic counter: how many times a faded enchanted armor piece had its glint pass swapped onto
    // our translucent glint render type (issue #324). Vanilla draws the armor glint as a separate
    // additive pass that depth-tests EQUAL against the base armor's depth; our translucent base
    // disables depth writes, so that EQUAL test fails and the glint vanishes. The swap re-issues the
    // glint on a pipeline sharing the translucent base's depth state so it co-draws. The glint smoke
    // asserts this climbs while a faded enchanted chest is on screen, pinning that the wrap fired.
    private static final java.util.concurrent.atomic.AtomicLong ARMOR_GLINT_SWAPS =
            new java.util.concurrent.atomic.AtomicLong();

    public static void recordArmorGlintSwap() {
        ARMOR_GLINT_SWAPS.incrementAndGet();
    }

    public static long armorGlintSwapCount() {
        return ARMOR_GLINT_SWAPS.get();
    }

    // Diagnostic counters for the ElytraTrims 4.x compat (ETElytraTrimSubmitMixin, >= 1.21.9). ET draws
    // its custom elytra decorators through one shared submit helper we wrap. SEEN = our wrap fired at all
    // (ET drew a trim through the helper) - proves the wrap is bound and ET is decorating. FADE = we
    // scaled the trim's alpha to match a faded wing (the real "transparency works" path, only where ET
    // draws translucent, i.e. >= 1.21.11). On < 1.21.11 ET draws cutout, which can't be faded, so the
    // elytra is left untouched at partial opacity (only 0% full-hides, handled by ArmorHiderElytraRenderer
    // cancelling the whole wing before ET even draws) - there SEEN climbs but FADE does not. The wrap is
    // @Pseudo/require=0 and no-ops silently if ET's helper drifts, so - per this repo's convention - the
    // ElytraTrims smoke asserts these while a trimmed elytra is worn.
    private static final java.util.concurrent.atomic.AtomicLong ELYTRA_TRIM_SEEN =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong ELYTRA_TRIM_FADES =
            new java.util.concurrent.atomic.AtomicLong();

    public static void recordElytraTrimSeen() {
        ELYTRA_TRIM_SEEN.incrementAndGet();
    }

    public static void recordElytraTrimFade() {
        ELYTRA_TRIM_FADES.incrementAndGet();
    }

    public static long elytraTrimSeenCount() {
        return ELYTRA_TRIM_SEEN.get();
    }

    public static long elytraTrimFadeCount() {
        return ELYTRA_TRIM_FADES.get();
    }

    // Test-only diagnostic switch, mirroring deferralEnabled. Flipped off, the glint pass keeps the
    // vanilla armorEntityGlint type (the pre-fix behaviour where the glint's EQUAL depth test fails
    // against our no-depth-write base and the glint vanishes on faded armor). The glint smoke toggles
    // this to capture a before/after pair with identical framing. Always true in normal play.
    private static volatile boolean glintSwapEnabled = true;

    public static void setGlintSwapEnabled(boolean enabled) {
        glintSwapEnabled = enabled;
    }

    public static boolean isGlintSwapEnabled() {
        return glintSwapEnabled;
    }

    // Test-only diagnostic switch, mirroring deferralEnabled below. Flipped off, FirstPersonCompat's
    // predicates all report false, restoring the unguarded behaviour so the first-person smoke can
    // observe the scope leak and its absence in a single run. Always true in normal play.
    private static volatile boolean firstPersonGuardsEnabled = true;

    public static void setFirstPersonGuardsEnabled(boolean enabled) {
        firstPersonGuardsEnabled = enabled;
    }

    public static boolean areFirstPersonGuardsEnabled() {
        return firstPersonGuardsEnabled;
    }

    // Test-only diagnostic switch. When flipped off, the after-terrain redirect is bypassed and the
    // translucent armor falls back to the pre-terrain phase - i.e. the pre-fix behaviour where water
    // overdraws the pads. The water-scene game test toggles this to capture a before/after and to
    // assert the redirect both fires when on and stays quiet when off. Always true in normal play.
    private static volatile boolean deferralEnabled = true;

    public static void setDeferralEnabled(boolean enabled) {
        deferralEnabled = enabled;
    }

    public static boolean isDeferralEnabled() {
        return deferralEnabled;
    }

    // Whether an Iris shaderpack is currently active. Installed by IrisCompat at init when Iris is
    // present (a plain BooleanSupplier so this render-path check never hard-references the Iris API -
    // class-loading IrisCompat when Iris is absent would NoClassDefFoundError). Default: no shaderpack.
    // Used to write depth on translucent armor ONLY under shaders: shaders composite depth-less
    // translucent geometry badly at grazing angles (the body under faded armor reads see-through),
    // while in vanilla the no-depth-write is what stops faded armor occluding water behind it. Where
    // the after-terrain deferral exists (>= 26.2-1.pre) that occlusion is already handled by draw
    // order, so writing depth under shaders is safe there.
    private static volatile java.util.function.BooleanSupplier shaderPackActiveCheck = () -> false;

    public static void setShaderPackActiveCheck(java.util.function.BooleanSupplier check) {
        shaderPackActiveCheck = check != null ? check : () -> false;
    }

    public static boolean isShaderPackActive() {
        try {
            return shaderPackActiveCheck.getAsBoolean();
        } catch (Throwable t) {
            return false;
        }
    }

    // Test hook: force the "shaderpack active" state so the depth-write armor path can be exercised
    // headlessly (no real Iris on the test box). Null clears the override.
    private static volatile Boolean shaderPackActiveOverride = null;

    public static void setShaderPackActiveOverride(Boolean value) {
        shaderPackActiveOverride = value;
    }

    public static boolean armorShouldWriteDepth() {
        Boolean override = shaderPackActiveOverride;
        return override != null ? override : isShaderPackActive();
    }

    // --- Pipelines (>= 1.21.5) ---

    //? if >= 26.2-1.pre {
    private static RenderPipeline clonePipelineNoDepthWrite(RenderPipeline src, Identifier location) {
        var srcDss = src.getDepthStencilState();
        var noDss = new DepthStencilState(srcDss.depthTest(), false, srcDss.depthBiasScaleFactor(), srcDss.depthBiasConstant());
        // 26.3-snapshot-5 reworked RenderPipeline: the two shader accessors became a single
        // getShaders() map, and getColorTargetStates()/getVertexFormatBindings() now return
        // Lists (Snippet takes arrays + an active-count int). 26.3-snapshot-8 added a trailing
        // pushConstantSize int to the Snippet canonical constructor.
        //? if >= 26.3-0.snapshot.5 {
        /*var snippet = new RenderPipeline.Snippet(
                src.getShaders(),
                Optional.of(src.getShaderDefines()), Optional.of(src.getBindGroupLayouts()),
                src.getColorTargetStates().toArray(new ColorTargetState[0]), src.getColorTargetStates().size(),
                Optional.of(noDss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), src.getVertexFormatBindings().toArray(new com.mojang.renderpearl.api.vertex.VertexFormat[0]),
                Optional.of(src.getPrimitiveTopology()), src.pushConstantSize());
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

    // Depth-writing translucent armor pipelines to also register with Iris (empty on eras that don't
    // use the under-shaders depth-write path). See armorShouldWriteDepth() / shaderPackActiveCheck.
    public static RenderPipeline[] shaderDepthPipelines() {
        //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
        return new RenderPipeline[] { ARMOR_TRANSLUCENT_DEPTH };
        //?} else {
        /*return new RenderPipeline[0];
        *///?}
    }
    //?}

    // --- Depth-writing translucent armor for shaderpacks (fixes the body reading see-through under
    // Iris at grazing angles). Only where the after-terrain deferral already handles water occlusion by
    // draw order (>= 26.2-1.pre) is writing depth on faded armor safe; older eras rely on no-depth. ---
    //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
    private static RenderPipeline clonePipelineKeepDepth(RenderPipeline src, Identifier location) {
        // Same as clonePipelineNoDepthWrite but keeps the source depth state (i.e. depth writing on).
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getBindGroupLayouts()),
                src.getColorTargetStates(), src.getColorTargetStates().length,
                Optional.of(src.getDepthStencilState()), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), src.getVertexFormatBindings(),
                Optional.of(src.getPrimitiveTopology()));
        return RenderPipeline.builder(snippet).withLocation(location).build();
    }

    private static final RenderPipeline ARMOR_TRANSLUCENT_DEPTH = clonePipelineKeepDepth(
            RenderPipelines.ARMOR_TRANSLUCENT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_translucent_depth"));

    private static final Function<Identifier, RenderType> TRANSLUCENT_ARMOR_DEPTH = memoize(
            texture -> RenderType.create("armor_hider_armor_translucent_depth",
                    RenderSetup.builder(ARMOR_TRANSLUCENT_DEPTH)
                            .withTexture("Sampler0", texture)
                            .useLightmap()
                            .useOverlay()
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .affectsCrumbling()
                            .sortOnUpload()
                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup()));

    private static RenderType translucentArmorDepth(Identifier texture) {
        // Deliberately NOT added to DEFERRED_TYPES: under a shaderpack we want the faded armor to render
        // as an ordinary translucent entity (depth-write, in the normal entity pass), which Iris
        // composites correctly over the solid body. Deferring it to the after-terrain phase is what made
        // the body read see-through under shaders. Water occlusion is instead handled by the depth write
        // (the pad still occludes water behind it), at the cost of the pad blending over terrain rather
        // than water at its protruding edges - unnoticeable under shaders and far better than a
        // see-through torso.
        return TRANSLUCENT_ARMOR_DEPTH.apply(texture);
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
                            // true, which routes the model into the solid phase too - and since we reduce
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

    // Diagnostic: how many times translucentArmor returned the depth-writing vs the no-depth type.
    private static final java.util.concurrent.atomic.AtomicLong ARMOR_DEPTH_PATH =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong ARMOR_NODEPTH_PATH =
            new java.util.concurrent.atomic.AtomicLong();

    public static long armorDepthPathCount() {
        return ARMOR_DEPTH_PATH.get();
    }

    public static long armorNoDepthPathCount() {
        return ARMOR_NODEPTH_PATH.get();
    }

    /**
     * Opaque cutout armor type backed by a dithered (screen-door) copy of {@code base} approximating
     * {@code opacity}, for the under-shaders path. Returns {@code null} where unavailable (caller falls
     * back to the translucent type). See {@link de.zannagh.armorhider.client.render.ShaderDitheredArmorTextures}.
     */
    public static RenderType ditheredArmorCutout(Identifier base, float opacity, de.zannagh.armorhider.net.packets.PlayerConfig config) {
        //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
        // Count the decision to take the dither path (the swap is wired) up front, independent of
        // whether the texture upload succeeds - the smoke tests assert on this like the other paths.
        ARMOR_DITHER_PATH.incrementAndGet();
        Identifier derived = de.zannagh.armorhider.client.render.ShaderDitheredArmorTextures
                .ditheredTexture(base, opacity, config);
        if (derived == null) {
            return null;
        }
        return net.minecraft.client.renderer.rendertype.RenderTypes.armorCutoutNoCull(derived);
        //?} else {
        /*return null;
        *///?}
    }

    // Diagnostic: how many times a faded armor piece was routed onto the under-shaders dithered
    // opaque-cutout type (the fix for the Iris see-through-body bug). Asserted by the smoke tests,
    // mirroring the depth/no-depth path counters, since the swap fails silently on a target drift.
    private static final java.util.concurrent.atomic.AtomicLong ARMOR_DITHER_PATH =
            new java.util.concurrent.atomic.AtomicLong();

    public static long armorDitherPathCount() {
        return ARMOR_DITHER_PATH.get();
    }

    public static RenderType translucentArmor(Identifier texture) {
        // Under an active shaderpack, hand back the depth-writing armor type so the body under faded
        // armor stops reading see-through at grazing angles. Safe only where the deferral covers water.
        //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
        if (armorShouldWriteDepth()) {
            ARMOR_DEPTH_PATH.incrementAndGet();
            return translucentArmorDepth(texture);
        }
        //?}
        ARMOR_NODEPTH_PATH.incrementAndGet();
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
        // 26.3 removed the single Sheets.ARMOR_TRIMS_SHEET atlas - trims are now per-material
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

    // --- Enchantment glint on translucent armor (issue #324) ---
    // On 1.21.5..<26.3 the armor glint is a SEPARATE additive pass (RenderPipelines.GLINT) that
    // depth-tests EQUAL against the depth the base armor wrote. Our translucent base disables depth
    // writes, so the glint's EQUAL test fails everywhere and the glint vanishes on faded armor. We
    // re-issue the glint on a clone of the GLINT pipeline that carries the translucent base's depth
    // state (GREATER_THAN_OR_EQUAL, no depth write) so it co-draws wherever the faded armor draws.
    // (< 1.21.5 is the RenderStateShard/CompositeState era, handled in the final else below.)
    //? if >= 1.21.5 && < 26.1-0.snapshot.10 {
    /*// 1.21.5..<26.1-snapshot-10: the RenderPipeline exposes its depth state as a plain
    // getDepthTestFunction()/isWrite* pair (no DepthStencilState). Clone vanilla's GLINT pipeline,
    // forcing the depth test to match our translucent armor base (ARMOR_TRANSLUCENT) and disabling
    // depth writes, so the glint co-draws wherever the faded armor draws instead of failing the
    // vanilla glint's EQUAL test against a depth value our no-depth-write base never wrote.
    private static RenderPipeline cloneGlintCoDraw(RenderPipeline src, Identifier location) {
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getSamplers()),
                Optional.of(src.getUniforms()), src.getBlendFunction(),
                Optional.of(RenderPipelines.ARMOR_TRANSLUCENT.getDepthTestFunction()), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), Optional.of(src.isWriteColor()),
                Optional.of(src.isWriteAlpha()), Optional.of(false),
                Optional.of(src.getColorLogic()), Optional.of(src.getVertexFormat()),
                Optional.of(src.getVertexFormatMode()));
        return RenderPipeline.builder(snippet)
                .withLocation(location)
                .withDepthBias(src.getDepthBiasScaleFactor(), src.getDepthBiasConstant())
                .build();
    }

    private static final RenderPipeline ARMOR_GLINT_TRANSLUCENT_CODRAW = cloneGlintCoDraw(
            RenderPipelines.GLINT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_glint_translucent_codraw"));

    //? if >= 1.21.11 {
    private static final RenderType TRANSLUCENT_ARMOR_GLINT = RenderType.create(
            "armor_hider_armor_glint_translucent",
            RenderSetup.builder(ARMOR_GLINT_TRANSLUCENT_CODRAW)
                    .withTexture("Sampler0", net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ARMOR)
                    .setTextureTransform(net.minecraft.client.renderer.rendertype.TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());
    //? } else {
    /^private static final RenderType TRANSLUCENT_ARMOR_GLINT = RenderType.create(
            "armor_hider_armor_glint_translucent", 1536, true, true,
            ARMOR_GLINT_TRANSLUCENT_CODRAW,
            RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ARMOR, false))
                    .setTexturingState(ARMOR_ENTITY_GLINT_TEXTURING)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false));
    ^///?}

    /^*
     * The translucent-armor glint render type for the reported case: a faded enchanted piece. Deferred
     * with the base so it draws in the same after-terrain phase and, sharing the base's depth state,
     * appears wherever the faded armor does instead of failing the vanilla glint's EQUAL depth test.
     * Returns {@code original} unchanged when the glint swap is toggled off (test only).
     ^/
    public static RenderType translucentArmorGlint(RenderType original) {
        if (!glintSwapEnabled) {
            return original;
        }
        DEFERRED_TYPES.add(TRANSLUCENT_ARMOR_GLINT);
        recordArmorGlintSwap();
        return TRANSLUCENT_ARMOR_GLINT;
    }
    *///? } elif >= 26.1-0.snapshot.10 && < 26.2-1.pre {
    /*// 26.1-snapshot-10..<26.2: the RenderPipeline exposes a DepthStencilState. Borrow our translucent
    // armor base's DepthStencilState (depth test matching ARMOR_TRANSLUCENT, depth writes disabled) so
    // the cloned GLINT pipeline co-draws wherever the faded armor draws.
    private static RenderPipeline cloneGlintCoDraw(RenderPipeline src, Identifier location) {
        var dss = ARMOR_TRANSLUCENT_NO_DEPTH.getDepthStencilState();
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getSamplers()),
                Optional.of(src.getUniforms()), Optional.of(src.getColorTargetState()),
                Optional.of(dss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), Optional.of(src.getVertexFormat()),
                Optional.of(src.getVertexFormatMode()));
        return RenderPipeline.builder(snippet).withLocation(location).build();
    }

    private static final RenderPipeline ARMOR_GLINT_TRANSLUCENT_CODRAW = cloneGlintCoDraw(
            RenderPipelines.GLINT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_glint_translucent_codraw"));

    private static final RenderType TRANSLUCENT_ARMOR_GLINT = RenderType.create(
            "armor_hider_armor_glint_translucent",
            RenderSetup.builder(ARMOR_GLINT_TRANSLUCENT_CODRAW)
                    .withTexture("Sampler0", net.minecraft.client.renderer.feature.ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
                    .setTextureTransform(net.minecraft.client.renderer.rendertype.TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());

    public static RenderType translucentArmorGlint(RenderType original) {
        if (!glintSwapEnabled) {
            return original;
        }
        DEFERRED_TYPES.add(TRANSLUCENT_ARMOR_GLINT);
        recordArmorGlintSwap();
        return TRANSLUCENT_ARMOR_GLINT;
    }
    *///? } elif >= 26.2-1.pre && < 26.3-0.snapshot.2 {
    private static RenderPipeline cloneGlintCoDraw(RenderPipeline src, Identifier location) {
        // Borrow the exact depth state of our translucent armor base so the glint draws under the same
        // depth rules (and, like the base, writes no depth).
        var dss = ARMOR_TRANSLUCENT_NO_DEPTH.getDepthStencilState();
        var snippet = new RenderPipeline.Snippet(
                Optional.of(src.getVertexShader()), Optional.of(src.getFragmentShader()),
                Optional.of(src.getShaderDefines()), Optional.of(src.getBindGroupLayouts()),
                src.getColorTargetStates(), src.getColorTargetStates().length,
                Optional.of(dss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), src.getVertexFormatBindings(),
                Optional.of(src.getPrimitiveTopology()));
        return RenderPipeline.builder(snippet).withLocation(location).build();
    }

    private static final RenderPipeline ARMOR_GLINT_TRANSLUCENT_CODRAW = cloneGlintCoDraw(
            RenderPipelines.GLINT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_glint_translucent_codraw"));

    private static final RenderType TRANSLUCENT_ARMOR_GLINT = RenderType.create(
            "armor_hider_armor_glint_translucent",
            RenderSetup.builder(ARMOR_GLINT_TRANSLUCENT_CODRAW)
                    .withTexture("Sampler0", net.minecraft.client.renderer.feature.ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
                    .setTextureTransform(net.minecraft.client.renderer.rendertype.TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());

    /**
     * The translucent-armor glint render type for the reported case: a faded enchanted piece. Deferred
     * with the base so it draws in the same after-terrain phase and, sharing the base's depth state,
     * appears wherever the faded armor does instead of failing the vanilla glint's EQUAL depth test.
     * On version eras this fix does not yet cover, returns {@code original} unchanged (glint stays
     * vanilla).
     */
    public static RenderType translucentArmorGlint(RenderType original) {
        if (!glintSwapEnabled) {
            return original;
        }
        DEFERRED_TYPES.add(TRANSLUCENT_ARMOR_GLINT);
        recordArmorGlintSwap();
        return TRANSLUCENT_ARMOR_GLINT;
    }
    //?} elif >= 26.3-0.snapshot.2 {
    /*// 26.3 fuses the glint into the entity shader: armorCutoutNoCullGlint(texture) is a single
    // combined armor+glint draw (RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT, opaque). We clone it to a
    // translucent, depth-write-disabled variant so a faded enchanted piece keeps its glint and fades
    // with it (the ENTITY vertex format carries colour here, so the glint fades too). Keyed by armor
    // texture, since the glint is composited with the piece's own texture in one pass.
    private static RenderPipeline cloneGlintTranslucent(RenderPipeline src, Identifier location) {
        var dss = ARMOR_TRANSLUCENT_NO_DEPTH.getDepthStencilState();
        var translucent = new ColorTargetState[]{
                new ColorTargetState(com.mojang.renderpearl.api.pipeline.BlendFunction.TRANSLUCENT)
        };
        var snippet = new RenderPipeline.Snippet(
                src.getShaders(),
                Optional.of(src.getShaderDefines()), Optional.of(src.getBindGroupLayouts()),
                translucent, translucent.length,
                Optional.of(dss), Optional.of(src.getPolygonMode()),
                Optional.of(src.isCull()), src.getVertexFormatBindings().toArray(new com.mojang.renderpearl.api.vertex.VertexFormat[0]),
                Optional.of(src.getPrimitiveTopology()), src.pushConstantSize());
        return RenderPipeline.builder(snippet).withLocation(location).build();
    }

    private static final RenderPipeline ARMOR_GLINT_TRANSLUCENT_NO_DEPTH = cloneGlintTranslucent(
            RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT,
            Identifier.fromNamespaceAndPath("armor_hider", "pipeline/armor_glint_translucent_no_depth"));

    private static final Function<Identifier, RenderType> TRANSLUCENT_ARMOR_GLINT = memoize(
            texture -> RenderType.create("armor_hider_armor_glint_translucent",
                    RenderSetup.builder(ARMOR_GLINT_TRANSLUCENT_NO_DEPTH)
                            .setOitPipelines(RenderPipelines.OIT_ENTITY)
                            .withTexture("Sampler0", texture)
                            .withTexture("GlintSampler", net.minecraft.client.renderer.feature.ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
                            .setTextureTransform(net.minecraft.client.renderer.rendertype.TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                            .useLightmap()
                            .useOverlay()
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .affectsCrumbling()
                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup()));

    /^*
     * The 26.3 combined (armor + shader glint) translucent render type for a faded enchanted piece,
     * keyed by the piece's armor texture. Deferred with the base so it draws in the same after-terrain
     * phase. Returns null when the glint-swap toggle is off (test-only), so the caller keeps vanilla.
     ^/
    public static RenderType translucentArmorGlint(Identifier texture) {
        if (!glintSwapEnabled) {
            return null;
        }
        RenderType renderType = TRANSLUCENT_ARMOR_GLINT.apply(texture);
        DEFERRED_TYPES.add(renderType);
        recordArmorGlintSwap();
        return renderType;
    }

    // Cross-version no-op overload (the 26.2-family separate-glint path calls this; unused on 26.3).
    public static RenderType translucentArmorGlint(RenderType original) {
        return original;
    }
    *///? } else {
    /*// < 1.21.5 (1.20.1..1.21.4): the RenderStateShard/CompositeState era. Vanilla armorEntityGlint is a
    // separate additive POSITION_TEX pass depth-tested EQUAL against the depth the base armor wrote
    // (issue #324). Our translucent base disables depth writes, so the EQUAL test fails and the glint
    // vanishes on faded armor. Rebuild the same glint type but with the base's depth test (LEQUAL -
    // the translucent armor base sets no depth test, so it uses the default LEQUAL) so it co-draws
    // wherever the faded armor draws. Depth writes stay disabled (COLOR_WRITE), exactly like vanilla.
    private static final RenderType TRANSLUCENT_ARMOR_GLINT = RenderType.create(
            "armor_hider_armor_glint_translucent",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER)
                    //? if >= 1.21.2 {
                    .setTextureState(new TextureStateShard(
                            net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ENTITY, TriState.DEFAULT, false))
                    //?} else {
                    /^.setTextureState(new TextureStateShard(
                            net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
                    ^///?}
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(ENTITY_GLINT_TEXTURING)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false));

    /^*
     * The translucent-armor glint render type for a faded enchanted piece (issue #324). Shares the
     * translucent base's depth test (LEQUAL) with depth writes disabled, so it co-draws wherever the
     * faded armor draws instead of failing the vanilla glint's EQUAL depth test. Returns
     * {@code original} unchanged when the glint swap is toggled off (test only).
     ^/
    public static RenderType translucentArmorGlint(RenderType original) {
        if (!glintSwapEnabled) {
            return original;
        }
        DEFERRED_TYPES.add(TRANSLUCENT_ARMOR_GLINT);
        recordArmorGlintSwap();
        return TRANSLUCENT_ARMOR_GLINT;
    }
    *///?}
}
