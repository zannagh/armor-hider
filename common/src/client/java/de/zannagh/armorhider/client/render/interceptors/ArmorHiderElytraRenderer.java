package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Default renderer for {@link RenderScope#ELYTRA}.
 * <p>
 * Short-circuits:
 * <ul>
 *   <li>player flying → don't enter the elytra scope at all (the body re-renders the elytra geometry);</li>
 *   <li>ElytraTrims loaded → don't enter the scope so ET's own pipeline drives rendering;</li>
 *   <li>modification {@code shouldHide} → cancel and signal.</li>
 * </ul>
 */
public class ArmorHiderElytraRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ELYTRA;
    }

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        IdentityCarrier carrier = identityCarrier instanceof IdentityCarrier ic ? ic : null;
        if (carrier == null) {
            setEmptyModification();
            return RenderInterceptionResult.ignore();
        }
        ItemStack elytraStack = stack != null ? stack : ItemsUtil.ELYTRA_ITEM_STACK;
        var mod = resolveModification(carrier, EquipmentSlot.CHEST, elytraStack);

        // Pass-through branches: do NOT enter scope. enterScope-with-mod would still register a
        // non-empty modification into the active-scope map, and downstream wraps then react to it
        // even though we logically "ignored" — that leaks into things like ElytraTrims rendering.
        if (mod.isEmpty()) {
            return RenderInterceptionResult.ignore();
        }
        // Flying must short-circuit BEFORE shouldHide so that 0%-opacity players still see the
        // elytra geometry while actually elytra-flying — the wings are the flight indicator.
        if (carrier.isPlayerFlying()) {
            return RenderInterceptionResult.ignore();
        }
        // With ElytraTrims present, ET's own rendering pipeline owns elytra appearance — collapse
        // to full-hide-or-vanilla. The non-hide case must NOT enter scope (would still leak our
        // mod into ET's submissions and re-introduce the blue-trim / missing-trim regressions).
        if (ArmorHiderClient.ET_LOADED && !mod.shouldHide()) {
            return RenderInterceptionResult.ignore();
        }

        if (mod.shouldHide()) {
            // Cancel only — do NOT enter the scope. The WingsLayer render is cancelled at HEAD, so
            // its RETURN (which exits the scope) never fires; entering here leaks a hide-scope
            // (alpha 0) for the rest of the entity render. On NeoForge that leaked ELYTRA scope is
            // read by NeoForgeArmorColorMixin (which wraps the shared SubmitNodeCollection) and
            // applied to every later model submit — turning the skull, offhand, etc. invisible.
            cancel(ci);
            return new RenderInterceptionResult(true, true, getTargetScope(), carrier, mod);
        }
        var result = new RenderInterceptionResult(true, false, getTargetScope(), carrier, mod);
        AhRenderManagementApi.enterScope(result);
        return result;
    }
}
