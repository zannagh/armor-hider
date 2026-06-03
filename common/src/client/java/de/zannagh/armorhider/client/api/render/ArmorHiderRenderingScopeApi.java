package de.zannagh.armorhider.client.api.render;

import de.zannagh.armorhider.client.api.configuration.SlotModification;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.NonNull;

public interface ArmorHiderRenderingScopeApi {
    /**
     * Whether the rendering scope is currently in level rendering (not UI or other game logic tests).
     * This is the highest level of the rendering scope.
     * @return True if the rendering scope is currently in level rendering.
     */
    boolean isInLevelRender();

    /**
     * Sets the rendering scope to be in level rendering.
     * This internally should reset entity render scope, player name and configuration in any case, as it marks a reset of the render pipeline.
     * @param inLevelRender True to set the rendering scope to be in level rendering.
     */
    void setInLevelRender(boolean inLevelRender);

    /**
     * Whether the rendering scope is currently in entity rendering.
     * @return True if the rendering scope is currently in entity rendering.
     */
    boolean isInEntityRender();
    void setInEntityRender(boolean inEntityRender);
    @NonNull String currentlyHandledPlayerName();
    void setCurrentPlayer(String playerName);

    boolean hasActiveModification();

    boolean hasActiveModificationFor(EquipmentSlot slot);

    @NonNull SlotModification currentlyActiveModification();

    void setActiveModification(SlotModification modification);

    void clearActiveModification();

    default boolean shouldEnforceVanillaRendering() { return false; }
}
