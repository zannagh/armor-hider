package de.zannagh.armorhider.client.api.render;

import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.configuration.SlotModification;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.log.DebugTracer;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.NonNull;

/**
 * Default implementation for {@link ArmorHiderRenderingScopeApi}
 */
public class ArmorHiderRenderingScopeApiImpl implements ArmorHiderRenderingScopeApi {

    private final ThreadLocal<Boolean> inLevelRender = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<Boolean> inEntityRender = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<String> currentPlayerName = new ThreadLocal<>();
    private final ThreadLocal<SlotModification> activeModification = new ThreadLocal<>();

    @Override
    public boolean isInLevelRender() {
        return inLevelRender.get();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setInLevelRender(boolean inLevelRender) {
        if (inLevelRender) {
            DebugTracer.scopeEnterLevelRender();
        }
        else {
            DebugTracer.scopeExitLevelRender();
        }
        clearActiveModification();
        setCurrentPlayer("");
        setInEntityRender(false);

        this.inLevelRender.set(inLevelRender);
    }

    @Override
    public boolean isInEntityRender() {
        return inEntityRender.get();
    }

    @Override
    public void setInEntityRender(boolean inEntityRender) {
        if (inEntityRender) {
            DebugTracer.scopeEnterEntityRender();
        }
        else {
            DebugTracer.scopeExitEntityRender();
        }
        clearActiveModification();
        currentPlayerName.remove();
        this.inEntityRender.set(inEntityRender);
    }

    @Override
    public @NonNull String currentlyHandledPlayerName() {
        if (currentPlayerName.get() == null) {
            return "";
        }
        return currentPlayerName.get();
    }

    @Override
    public void setCurrentPlayer(String playerName) {
        currentPlayerName.set(playerName);
    }

    @Override
    public boolean hasActiveModification() {
        return currentlyActiveModification().isEmpty();
    }

    @Override
    public boolean hasActiveModificationFor(EquipmentSlot slot) {
        if (!hasActiveModification()) {
            return false;
        }
        return currentlyActiveModification().slot() == slot;
    }

    @Override
    public @NonNull SlotModification currentlyActiveModification() {
        if (activeModification.get() == null) {
            return SlotModification.empty();
        }
        return activeModification.get();
    }

    @Override
    public void setActiveModification(SlotModification modification) {
        DebugTracer.scopeEnterItemRender(modification.slot(),  modification.playerName(), modification.transparency());
        activeModification.set(modification);
    }

    @Override
    public void clearActiveModification() {
        DebugTracer.scopeExitItemRender();
        activeModification.remove();
    }

    @Override
    public boolean shouldEnforceVanillaRendering() {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) {
            return false;
        }
        String playerName = currentlyHandledPlayerName();
        if (playerName.isBlank()) {
            return false;
        }

        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);

        if (!ArmorHiderClient.CLIENT_CONFIG_MANAGER.shouldApplyCombatDetection(config)) {
            return false;
        }
        if (!ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerName)) {
            return false;
        }

        return config.inCombatUseDefaultModel.getValue();
    }
}
