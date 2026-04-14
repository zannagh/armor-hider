package de.zannagh.armorhider.client.scopes;

import de.zannagh.armorhider.log.DebugTracer;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal rendering context tracked via ThreadLocals.
 * <p>
 * Only three pieces of state:
 * <ul>
 *   <li><b>inLevelRender</b> — true during 3D world rendering (for {@code PlayerMixin})</li>
 *   <li><b>inEntityRender</b> — true during an individual entity's render (for {@code PlayerMixin})</li>
 *   <li><b>activeModification</b> — the current equipment modification being applied (for deep render interceptors)</li>
 * </ul>
 * Layer mixins resolve identity directly from the entity / render state via {@link IdentityCarrier},
 * then create an {@link ActiveModification} and store it here for downstream render interceptors.
 */
public final class RenderContext {

    private final ThreadLocal<Boolean> inLevelRender = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<Boolean> inEntityRender = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<String> currentPlayerName = new ThreadLocal<>();
    private final ThreadLocal<ActiveModification> activeModification = new ThreadLocal<>();

    // --- Level render (GameRendererMixin) ---

    public void enterLevelRender() {
        inEntityRender.set(false);
        currentPlayerName.remove();
        activeModification.remove();
        inLevelRender.set(true);
        DebugTracer.scopeEnterLevelRender();
    }

    public void exitLevelRender() {
        activeModification.remove();
        currentPlayerName.remove();
        inEntityRender.set(false);
        inLevelRender.set(false);
        DebugTracer.scopeExitLevelRender();
    }

    public boolean isInLevelRender() {
        return inLevelRender.get();
    }

    // --- Entity render (EntityRenderDispatcherMixin) ---

    public void enterEntityRender() {
        activeModification.remove();
        currentPlayerName.remove();
        inEntityRender.set(true);
        DebugTracer.scopeEnterEntityRender();
    }

    public void exitEntityRender() {
        activeModification.remove();
        currentPlayerName.remove();
        inEntityRender.set(false);
        DebugTracer.scopeExitEntityRender();
    }

    public boolean isInEntityRender() {
        return inEntityRender.get();
    }

    // --- Current player name (for inner methods that lack entity/render state access) ---

    public void setCurrentPlayer(@Nullable String name) {
        currentPlayerName.set(name);
    }

    public @Nullable String currentPlayerName() {
        return currentPlayerName.get();
    }

    // --- Active modification (layer mixins → deep render interceptors) ---

    public void setActiveModification(@NotNull ActiveModification mod) {
        activeModification.set(mod);
        DebugTracer.scopeEnterItemRender(mod.slot(), mod.playerName(), mod.transparency());
    }

    public void clearActiveModification() {
        if (activeModification.get() != null) {
            activeModification.remove();
            DebugTracer.scopeExitItemRender();
        }
    }

    public @Nullable ActiveModification activeModification() {
        return activeModification.get();
    }

    public boolean hasActiveModification() {
        return activeModification.get() != null;
    }

    public boolean hasActiveModification(@NotNull EquipmentSlot slot) {
        var mod = activeModification.get();
        return mod != null && mod.slot() == slot;
    }

}
