package de.zannagh.armorhider.client.util;

import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.scopes.EntityRenderScope;
import de.zannagh.armorhider.client.scopes.ItemRenderScope;
import de.zannagh.armorhider.client.scopes.ScopeProvider;
//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? if < 1.21.4
//import net.minecraft.world.entity.LivingEntity;


public final class ScopeUtils {
    
    //? if >= 1.21.4
    public static EntityRenderScope enrichIfNullOrSentinel(@NotNull ScopeProvider provider, @Nullable EntityRenderScope scope, @NotNull LivingEntityRenderState enrichment) {
    //? if < 1.21.4
    //public static EntityRenderScope enrichIfNullOrSentinel(@NotNull ScopeProvider provider, @Nullable EntityRenderScope scope, @NotNull LivingEntity enrichment) {
        if (ScopeUtils.isNullOrSentinel(scope)) {
            provider.enrichEntityScope(enrichment);
            scope = provider.entityScope();
        }
        return scope;
    }
    
    public static boolean isNullOrSentinel(@Nullable EntityRenderScope scope) {
        return scope == null || scope == EntityRenderScope.SENTINEL;
    }
    
    public static @Nullable ItemRenderScope getItemScopeIfModifiable(@NotNull ScopeProvider provider) {
        var scope = provider.itemScope();
        if (scope == null || !scope.shouldModify() || !RenderDecisions.shouldModifyEquipment(provider)) {
            return null;
        }
        return scope;
    }
}
