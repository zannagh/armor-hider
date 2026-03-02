package de.zannagh.armorhider.scopes;

/**
 * Represents the outermost rendering scope.
 * Active whenever GameRenderer.render() is executing.
 *
 * When this scope is absent, we are in game logic (tick processing,
 * inventory interactions) and mixins must return real items to prevent
 * item loss during equipment swaps.
 */
public final class RenderFrameScope {

    private static final RenderFrameScope INSTANCE = new RenderFrameScope();

    private RenderFrameScope() {}

    public static RenderFrameScope instance() {
        return INSTANCE;
    }
}
