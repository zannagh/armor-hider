package de.zannagh.armorhider.client.render;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.api.AhColorTransformer;
import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import de.zannagh.armorhider.client.render.rendertype.RenderTypeFactory;
import de.zannagh.armorhider.client.render.utils.DefaultColorTransformer;
import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies visual modifications (transparency, render type swaps, color changes)
 * based on the active modification in the render context.<br>
 * <br>
 * All methods are "pass-through safe": if no active modification exists,
 * they return the original values unchanged.
 */
public class RenderModifications implements AhRenderModificationApi {

    public static final int ELYTRA_RENDER_PRIORITY = 100;
    public static final int SKULL_RENDER_PRIORITY = 99;

    private final SlotModification slotModification;
    private final ItemInfo itemInfo;

    /**
     * Set only on the shared {@link #EMPTY} instance. It makes the two setters below no-ops, so the one
     * instance handed out for every scope miss cannot be repurposed by a compat layer and leak a custom
     * render type or color transformer into unrelated render paths.
     */
    private final boolean immutable;

    public RenderModifications(SlotModification slotModification) {
        this(slotModification, false);
    }

    private RenderModifications(SlotModification slotModification, boolean immutable) {
        this.slotModification = slotModification;
        this.itemInfo = slotModification.itemInfo();
        this.immutable = immutable;
    }

    /**
     * The shared pass-through instance. {@code empty()} is on the render hot path (it is what
     * {@code RenderScopeContext.empty(...)} builds on every scope miss - per model part, per baked quad),
     * and every method on an empty modification short-circuits to the original value, so one instance is
     * enough. It deliberately does NOT go through {@code AhRenderModificationApi.getInstance(...)}: the
     * previous {@code new RenderModifications(...)} here did not either, so registered factories and
     * transformers were never applied to an empty context and still are not.
     */
    private static final RenderModifications EMPTY = buildEmpty();

    /**
     * Null if the chain is not ready yet - see the note on {@code RenderScopeContext.buildEmpties()} and the
     * precedent at {@code ItemInfo.java:40-48} (issue #260). Letting a transient early-bootstrap failure
     * escape this {@code <clinit>} would latch it for the session and kill the render path.
     */
    private static RenderModifications buildEmpty() {
        try {
            return new RenderModifications(SlotModification.empty(), true);
        } catch (Throwable t) {
            ArmorHider.LOGGER.warn(
                    "Could not pre-build the shared empty RenderModifications; falling back to allocating per call",
                    t);
            return null;
        }
    }

    public static RenderModifications empty() {
        RenderModifications cached = EMPTY;
        if (cached != null) {
            return cached;
        }
        // Degraded path - see buildEmpty(). Allocates, but keeps rendering alive.
        return new RenderModifications(SlotModification.empty(), true);
    }

    public AhRenderTypeFactory renderTypes() {
        return customRenderTypeFactory == null ? RenderTypeFactory.getInstance() : customRenderTypeFactory;
    }

    // --- Render type modifications ---
    // These swap a piece onto the depth-write-disabled translucent pipeline, so they gate on
    // needsTranslucency() (opacity < ~100%), NOT needsModification(): a fully-opaque piece with only
    // its glint toggled off must stay on the normal depth-writing armor type, or it reads as
    // see-through under shaders (Iris) against bright light/water. Glint suppression is handled
    // separately by getHasFoil(), which keeps gating on needsModification().

    public RenderType getSkullRenderLayer(Identifier texture, RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsTranslucency()) {
            return originalLayer;
        }
        return getTranslucentEntityRenderType(texture);
    }

    public RenderType getTranslucentArmorRenderType(Identifier texture, RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsTranslucency()) {
            return originalLayer;
        }
        // Under an active Iris shaderpack, an alpha-blended (translucent) armor piece is routed into the
        // shaderpack's translucent-entity pass, where deferred packs (Complementary, …) composite it so
        // the OPAQUE body behind the piece reads see-through - the terrain/sky draws through the torso
        // (issue #342 follow-up). Render an opaque ordered-dither ("screen-door") cutout copy instead:
        // shader-safe partial opacity that never enters the translucent pass and so never lets the body
        // read through. Falls back to the translucent type if the dithered texture can't be built.
        // The Iris mode/scale/phases/resolution cap are THIS client's render preferences (the "local
        // settings" group of AdvancedArmorHiderSettingsScreen, saved to the local config only), not a
        // property of the player being drawn - so read them from the local viewer's config, never from
        // the rendered player's synced one, or a remote player's settings would decide how this GPU dithers.
        var localConfig = armorHider$localViewerConfig();
        if (ArmorHiderRenderTypes.armorShouldWriteDepth() && localConfig.irisPartialTransparencyMode.getValue() != de.zannagh.armorhider.configuration.IrisPartialTransparencyMode.NONE) {
            RenderType dithered = ArmorHiderRenderTypes.ditheredArmorCutout(
                    texture,
                    (float) slotModification.transparency(),
                    localConfig);
            if (dithered != null) {
                return dithered;
            }
        }
        return getTranslucentArmorRenderType(texture);
    }

    public RenderType getTrimRenderLayer(boolean decal, RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsTranslucency()) {
            return originalLayer;
        }
        return getTranslucentArmorTrimRenderType(decal);
    }

    public RenderType getTranslucentItemRenderType(RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsTranslucency()) {
            return originalLayer;
        }
        //? if <= 26.1.2
        //if (originalLayer == Sheets.cutoutBlockSheet()) {
        //? if > 26.1.2
        if (originalLayer == Sheets.cutoutBlockItemSheet()) {
            return getTranslucentItemSheetRenderType();
        }
        return originalLayer;
    }

    // --- Color modifications ---

    public int applyArmorTransparency(int originalColor) {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            return originalColor;
        }
        return colors().applyTransparency(originalColor, (float) slotModification.transparency());
    }

    public int applyTransparencyFromWhite() {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            // Opaque white is the no-op tint for a white-based render (vanilla renders these
            // pieces with color -1). Returning a caller-supplied fallback here is a footgun:
            // passing anything else (e.g. packedOverlay) tints the piece invisible when the
            // modification is inert. Keeping the fallback in one place is what lets both
            // loaders' mixins call this identically without drifting.
            return 0xFFFFFFFF;
        }
        return colors().whiteWithTransparency((float) slotModification.transparency());
    }

    public float getTransparencyAlpha() {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            return 1.0f;
        }
        return (float) slotModification.transparency();
    }

    public boolean getHasFoil(boolean original) {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            return original;
        }
        return !slotModification.shouldDisableGlint();
    }

    // --- API bridge methods (Object-typed for version independence) ---

    @Override
    public Object getTranslucentArmorRenderType(Object textureIdentifier, Object originalRenderType) {
        if (textureIdentifier instanceof Identifier texture && originalRenderType instanceof RenderType original) {
            return getTranslucentArmorRenderType(texture, original);
        }
        return originalRenderType;
    }

    @Override
    public Object getTrimRenderLayer(boolean decal, Object originalRenderType) {
        if (originalRenderType instanceof RenderType original) {
            return getTrimRenderLayer(decal, original);
        }
        return originalRenderType;
    }

    @Override
    public Object getTranslucentItemRenderType(Object originalRenderType) {
        if (originalRenderType instanceof RenderType original) {
            return getTranslucentItemRenderType(original);
        }
        return originalRenderType;
    }

    @Override
    public Object getSkullRenderLayer(Object textureIdentifier, Object originalRenderType) {
        if (textureIdentifier instanceof Identifier texture && originalRenderType instanceof RenderType original) {
            return getSkullRenderLayer(texture, original);
        }
        return originalRenderType;
    }

    // --- Priority modifications ---

    public int modifyRenderPriority(int originalPriority) {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            return originalPriority;
        }
        if (itemInfo.isElytra()) {
            return ELYTRA_RENDER_PRIORITY;
        }
        if (itemInfo.isVanillaSkullItem()) {
            return SKULL_RENDER_PRIORITY;
        }
        return originalPriority;
    }

    private AhRenderTypeFactory customRenderTypeFactory;
    private AhColorTransformer customColorTransformer;

    public void setRenderTypeFactory(AhRenderTypeFactory renderTypeFactory) {
        if (immutable) {
            warnImmutable("setRenderTypeFactory");
            return;
        }
        customRenderTypeFactory = renderTypeFactory;
    }

    @Override
    public void setColorTransformer(AhColorTransformer colorTransformer) {
        if (immutable) {
            warnImmutable("setColorTransformer");
            return;
        }
        customColorTransformer = colorTransformer;
    }

    /** Guards {@link #warnImmutable(String)} so a per-quad misuse cannot flood the log. */
    private static final AtomicBoolean IMMUTABLE_WARNING_LOGGED = new AtomicBoolean();

    /**
     * Reports an attempt to customise the shared pass-through instance. Logged rather than thrown: this
     * object reaches third parties through public API ({@code ArmorHiderEmptyRenderer#getRenderModificationApi},
     * {@code AbstractArmorHiderRenderer}), and throwing would break existing consumers.
     */
    private static void warnImmutable(String setter) {
        if (IMMUTABLE_WARNING_LOGGED.compareAndSet(false, true)) {
            ArmorHider.LOGGER.warn(
                    "Ignoring {} on the shared empty RenderModifications instance: it is handed out for every"
                            + " scope miss and cannot be customised. Obtain a modification API bound to an actual"
                            + " render scope instead. This is logged once per session.", setter);
        }
    }

    @Override
    public AhColorTransformer colors() {
        return customColorTransformer == null ? DefaultColorTransformer.getInstance() : customColorTransformer;
    }

    public RenderType getTranslucentArmorRenderType(Identifier texture) {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentArmorRenderType(texture);
        }
        // Same under-shaders dither path as the two-arg overload, so callers that reach the render type
        // through this entry (e.g. the GeckoLib armor hook) also avoid the translucent-pass see-through.
        var localConfig = armorHider$localViewerConfig();
        if (!slotModification.isEmpty()
                && slotModification.needsTranslucency()
                && ArmorHiderRenderTypes.armorShouldWriteDepth()
                && localConfig.irisPartialTransparencyMode.getValue() != de.zannagh.armorhider.configuration.IrisPartialTransparencyMode.NONE) {
            RenderType dithered = ArmorHiderRenderTypes.ditheredArmorCutout(
                    texture, (float) slotModification.transparency(), localConfig);
            if (dithered != null) {
                return dithered;
            }
        }
        return ArmorHiderRenderTypes.translucentArmor(texture);
    }

    public RenderType getTranslucentEntityRenderType(Identifier texture){
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentEntityRenderType(texture);
        }
        return ArmorHiderRenderTypes.translucentEntity(texture);
    }

    public RenderType getTranslucentArmorTrimRenderType(boolean decal) {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentArmorTrimRenderType(decal);
        }
        return ArmorHiderRenderTypes.translucentArmorTrim();
    }

    public RenderType getTranslucentItemSheetRenderType() {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentItemSheetRenderType();
        }
        return ArmorHiderRenderTypes.translucentItemSheet();
    }

    // Buffer wrapping for < 1.21.9 (used by ItemInHandLayerMixin and OffHandRenderMixin)

    //? if < 1.21.9 {

    /*// Cache: maps entitySolid/entityCutout Identifiers to entityTranslucent equivalents.
    private static final java.util.Map<net.minecraft.client.renderer.rendertype.RenderType, net.minecraft.client.renderer.rendertype.RenderType> solidToTranslucent
            = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        //? if >= 1.21 {
        solidToTranslucent.put(
                net.minecraft.client.renderer.rendertype.RenderType.entitySolid(Sheets.SHIELD_SHEET),
                ArmorHiderRenderTypes.translucentEntity(Sheets.SHIELD_SHEET));
        solidToTranslucent.put(
                net.minecraft.client.renderer.rendertype.RenderType.entitySolid(Sheets.BANNER_SHEET),
                ArmorHiderRenderTypes.translucentEntity(Sheets.BANNER_SHEET));
        //? } else {
        /^solidToTranslucent.put(
                net.minecraft.client.renderer.rendertype.RenderType.entitySolid(Sheets.SHIELD_SHEET),
                net.minecraft.client.renderer.rendertype.RenderType.entityTranslucent(Sheets.SHIELD_SHEET));
        solidToTranslucent.put(
                net.minecraft.client.renderer.rendertype.RenderType.entitySolid(Sheets.BANNER_SHEET),
                net.minecraft.client.renderer.rendertype.RenderType.entityTranslucent(Sheets.BANNER_SHEET));
        ^///?}
        //? if < 1.21 {
        /^solidToTranslucent.put(
                net.minecraft.client.renderer.rendertype.RenderType.entitySolid(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS),
                net.minecraft.client.renderer.rendertype.RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
        ^///?} elif < 1.21.4 {
        /^solidToTranslucent.put(
                net.minecraft.client.renderer.rendertype.RenderType.entitySolid(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS),
                ArmorHiderRenderTypes.translucentEntity(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
        ^///?}
    }

    public static net.minecraft.client.renderer.MultiBufferSource wrapTranslucentBufferSource(
            net.minecraft.client.renderer.MultiBufferSource original, float alpha) {
        return (net.minecraft.client.renderer.rendertype.RenderType renderType) -> {
            var translucent = solidToTranslucent.get(renderType);
            if (translucent != null) {
                return original.getBuffer(translucent);
            }
            if (renderType == Sheets.cutoutBlockSheet()) {
                return original.getBuffer(Sheets.translucentItemSheet());
            }
            return original.getBuffer(renderType);
        };
    }

    public static void registerSolidToTranslucent(net.minecraft.client.renderer.rendertype.RenderType solid, net.minecraft.client.renderer.rendertype.RenderType translucent) {
        solidToTranslucent.putIfAbsent(solid, translucent);
    }
    *///? }

    private static final String[] humanoidModelPartNames = {
            "head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg",
            "left_wing", "right_wing"
    };

    /**
     * Synchronizes the pose of the given model part and its children (when humanoid).
     * @param from The model part to synchronize.
     * @param to The model part to synchronize to.
     */
    public static void synchronisePoses(ModelPart from, ModelPart to) {
        copyPose(from, to);
        for (String name : humanoidModelPartNames) {
            if (from.hasChild(name) && to.hasChild(name)) {
                copyPose(from.getChild(name), to.getChild(name));
            }
        }
    }

    private static void copyPose(ModelPart from, ModelPart to){
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
        to.visible = from.visible;
        to.skipDraw = from.skipDraw;
    }

    /**
     * The local viewer's config - the source the settings screen writes the Iris dithering preferences to.
     * Falls back to the rendered player's config only if the local one is unavailable (never in a live client).
     */
    private de.zannagh.armorhider.net.packets.PlayerConfig armorHider$localViewerConfig() {
        var local = de.zannagh.armorhider.client.ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
        return local != null ? local : slotModification.config();
    }
}
