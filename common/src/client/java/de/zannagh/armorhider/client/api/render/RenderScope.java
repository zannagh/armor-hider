package de.zannagh.armorhider.client.api.render;

/**
 * Identifies the render scope that a modification applies to.
 * Each scope is independently managed — layer mixins enter/exit their scope,
 * and deep interceptors query the specific scope they care about.
 *
 * @since 0.12.0
 */
public enum RenderScope {
    ARMOR_PIECE,
    ELYTRA,
    CAPE,
    OFFHAND,
    HEAD
}
