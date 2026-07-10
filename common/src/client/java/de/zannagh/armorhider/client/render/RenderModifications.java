package de.zannagh.armorhider.client.render;

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

    public RenderModifications(SlotModification slotModification) {
        this.slotModification = slotModification;
        this.itemInfo = slotModification.itemInfo();
    }

    public static RenderModifications empty() {
        return new RenderModifications(SlotModification.empty());
    }

    public AhRenderTypeFactory renderTypes() {
        return customRenderTypeFactory == null ? RenderTypeFactory.getInstance() : customRenderTypeFactory;
    }

    // --- Render type modifications ---

    public RenderType getSkullRenderLayer(Identifier texture, RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            return originalLayer;
        }
        return getTranslucentEntityRenderType(texture);
    }

    public RenderType getTranslucentArmorRenderType(Identifier texture, RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            return originalLayer;
        }
        return getTranslucentArmorRenderType(texture);
    }

    public RenderType getTrimRenderLayer(boolean decal, RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
            return originalLayer;
        }
        return getTranslucentArmorTrimRenderType(decal);
    }

    public RenderType getTranslucentItemRenderType(RenderType originalLayer) {
        if (slotModification.isEmpty() || !slotModification.needsModification()) {
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
        customRenderTypeFactory = renderTypeFactory;
    }

    @Override
    public void setColorTransformer(AhColorTransformer colorTransformer) {
        customColorTransformer = colorTransformer;
    }

    @Override
    public AhColorTransformer colors() {
        return customColorTransformer == null ? DefaultColorTransformer.getInstance() : customColorTransformer;
    }

    public RenderType getTranslucentArmorRenderType(Identifier texture) {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentArmorRenderType(texture);
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
            "head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"
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
}
