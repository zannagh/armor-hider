package de.zannagh.armorhider.mixin.client;

import net.minecraft.client.render.VertexConsumer;

public class AlphaModifyingVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alphaMultiplier;

    public AlphaModifyingVertexConsumer(VertexConsumer delegate, float alphaMultiplier) {
        this.delegate = delegate;
        this.alphaMultiplier = alphaMultiplier;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        return delegate.vertex(x, y, z);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        int modifiedAlpha = (int) (alpha * alphaMultiplier);
        return delegate.color(red, green, blue, modifiedAlpha);
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return delegate.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return delegate.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int uv) {
        return delegate.light(uv);
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return delegate.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return delegate.normal(x, y, z);
    }
}
