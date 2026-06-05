package de.zannagh.armorhider.client.api.render;

/**
 * Defines global rendering scopes that determine the context in which specific
 * rendering modifications or operations are applied. These scopes serve as
 * high-level identifiers used to manage and query the rendering pipeline.
 * Each scope operates independently and can be used to restrict rendering
 * behavior to a specific set of elements or a particular phase of the rendering
 * process.
 */
public enum GlobalRenderScope {
    /**
     * The highest scope available, entering a level render usually resets any lower scopes.
     */
    LEVEL_RENDER,
    /**
     * Represents the rendering scope that is specific to individual entities.
     * This scope is managed independently and is typically entered or exited
     * during the rendering of individual entities. It is used to identify
     * and control render modifications that apply exclusively to entity-related
     * elements within the rendering pipeline.
     */
    ENTITY_RENDER;
}
