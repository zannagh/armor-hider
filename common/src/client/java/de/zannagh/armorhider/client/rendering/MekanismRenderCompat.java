//? if < 1.21.5 {
/*package de.zannagh.armorhider.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.lang.reflect.Field;

public final class MekanismRenderCompat extends RenderStateShard {

    private MekanismRenderCompat() {
        super("armor_hider_mek_compat", () -> {}, () -> {});
    }

    private static RenderType MEKASUIT_ORIGINAL;
    private static RenderType MEKASUIT_TRANSLUCENT;
    private static boolean initialized;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            Field mekasuitField = Class.forName("mekanism.client.render.MekanismRenderType")
                    .getDeclaredField("MEKASUIT");
            mekasuitField.setAccessible(true);
            MEKASUIT_ORIGINAL = (RenderType) mekasuitField.get(null);

            Field trackerField = Class.forName("mekanism.client.render.MekanismShaders")
                    .getDeclaredField("MEKASUIT");
            trackerField.setAccessible(true);
            Object tracker = trackerField.get(null);
            Field shardField = tracker.getClass().getDeclaredField("shard");
            shardField.setAccessible(true);
            ShaderStateShard shaderShard = (ShaderStateShard) shardField.get(tracker);

            MEKASUIT_TRANSLUCENT = RenderType.create(
                    "armor_hider_mekasuit_translucent",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    131_072, true, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(shaderShard)
                            .setTextureState(BLOCK_SHEET)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true)
            );
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static MultiBufferSource wrapForTransparency(MultiBufferSource original) {
        init();
        if (MEKASUIT_ORIGINAL == null || MEKASUIT_TRANSLUCENT == null) {
            return original;
        }
        RenderType opaqueType = MEKASUIT_ORIGINAL;
        RenderType translucentType = MEKASUIT_TRANSLUCENT;
        return (RenderType type) -> {
            if (type == opaqueType) {
                return original.getBuffer(translucentType);
            }
            return original.getBuffer(type);
        };
    }

    public static void flushTranslucentBatch(MultiBufferSource bufferSource, float alpha) {
        init();
        if (MEKASUIT_TRANSLUCENT == null) return;
        if (!(bufferSource instanceof MultiBufferSource.BufferSource bs)) return;
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        bs.endBatch(MEKASUIT_TRANSLUCENT);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
*///?}
