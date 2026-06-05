package de.zannagh.armorhider.client.common;

public record RenderInterceptionResult(
        boolean shouldIntercept,
        boolean shouldCancel
) {

    public static RenderInterceptionResult ignore() {
        return new RenderInterceptionResult(false, false);
    }
}
