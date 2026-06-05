package de.zannagh.armorhider.client.api.implementations;

import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.api.ArmorHiderRenderApi;
import de.zannagh.armorhider.client.common.GlobalRenderScope;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.RenderScopeContext;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.log.DebugTracer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;

public class ArmorHiderRenderApiImpl implements ArmorHiderRenderApi {

    private final EnumSet<GlobalRenderScope> scopeFlags = EnumSet.noneOf(GlobalRenderScope.class);
    private String currentPlayerName = "";
    private final EnumMap<RenderScope, RenderScopeContext> activeScopes = new EnumMap<>(RenderScope.class);

    // --- Phase flags ---

    @Override
    public EnumSet<GlobalRenderScope> getScopeFlags() {
        return scopeFlags;
    }

    @Override
    public void setInLevelRender() {
        DebugTracer.scopeEnterLevelRender();
        ArmorHiderRenderApi.super.setInLevelRender();
    }

    @Override
    public void setInEntityRender() {
        DebugTracer.scopeEnterEntityRender();
        ArmorHiderRenderApi.super.setInEntityRender();
    }

    @Override
    public void exitEntityRender() {
        DebugTracer.scopeExitEntityRender();
        ArmorHiderRenderApi.super.exitEntityRender();
    }

    @Override
    public void exitInLevelRender() {
        DebugTracer.scopeExitLevelRender();
        ArmorHiderRenderApi.super.exitInLevelRender();
    }

    @Override
    public @NonNull String currentlyHandledPlayerName() {
        return currentPlayerName;
    }

    @Override
    public void setCurrentPlayer(String playerName) {
        currentPlayerName = playerName;
    }

    // --- Per-scope context management ---

    @Override
    public RenderScopeContext enterScope(RenderScope scope, @Nullable IdentityCarrier carrier,
                                   @Nullable EquipmentSlot slot, @Nullable ItemStack item) {
        if (carrier == null) {
            var ctx = RenderScopeContext.empty(scope);
            activeScopes.put(scope, ctx);
            return ctx;
        }

        setCurrentPlayer(carrier.armorHider$playerName());

        SlotModification mod;
        if (slot != null) {
            mod = carrier.getModification(slot, item);
        } else {
            mod = SlotModification.empty();
        }

        var ctx = new RenderScopeContext(scope, carrier, mod, new RenderModifications(mod));
        activeScopes.put(scope, ctx);

        if (!mod.isEmpty()) {
            DebugTracer.scopeEnterItemRender(mod.slot(), mod.playerName(), mod.transparency());
        }

        return ctx;
    }

    @Override
    public void exitScope(RenderScope scope) {
        activeScopes.remove(scope);
        DebugTracer.scopeExitItemRender();
    }

    @Override
    public @NonNull RenderScopeContext getActiveScope(RenderScope scope) {
        var ctx = activeScopes.get(scope);
        return ctx != null ? ctx : RenderScopeContext.empty(scope);
    }

    @Override
    public boolean hasScopeModification(RenderScope scope) {
        var ctx = activeScopes.get(scope);
        return ctx != null && !ctx.isEmpty();
    }

    private void clearAllScopes() {
        activeScopes.clear();
    }

    private static RenderScope slotToScope(EquipmentSlot slot, ItemInfo info) {
        return switch (slot) {
            case HEAD -> RenderScope.HEAD;
            case OFFHAND -> RenderScope.OFFHAND;
            case CHEST -> {
                if (info.isElytra()) {
                    yield RenderScope.ELYTRA;
                }
                yield RenderScope.ARMOR_PIECE;
            }

            default -> RenderScope.ARMOR_PIECE;
        };
    }

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
