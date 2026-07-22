package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
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
 *   <li>ElytraTrims loaded (pre-4.x / MC &lt; 1.21.9) → don't enter the scope so ET's own pipeline drives
 *       rendering; on 1.21.9+ we DO enter and {@code ETElytraTrimSubmitMixin} fades ET's trim submits from
 *       the active ELYTRA scope;</li>
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
        ItemStack elytraStack = stack != null ? stack : ItemsUtil.elytraItemStack();
        var mod = resolveModification(carrier, EquipmentSlot.CHEST, elytraStack);

        // Pass-through branches: do NOT enter scope. enterScope-with-mod would still register a
        // non-empty modification into the active-scope map, and downstream wraps then react to it
        // even though we logically "ignored" — that leaks into things like ElytraTrims rendering.
        if (mod.isEmpty()) {
            return RenderInterceptionResult.ignore();
        }
        // Flying must short-circuit BEFORE shouldHide so that 0%-opacity players still see the
        // elytra geometry while actually elytra-flying — the wings are the flight indicator.
        if (carrier.ah$isPlayerFlying()) {
            return RenderInterceptionResult.ignore();
        }
        //? if < 1.21.9 {
        /*// Older ElytraTrims (pre-4.x, no decorator API) owns elytra appearance and cannot be faded —
        // collapse to full-hide-or-vanilla. The non-hide case must NOT enter scope (would leak our mod
        // into ET's submissions and re-introduce the blue-trim / missing-trim regressions of #49).
        if (CompatManager.requiresCompatTo(CompatFlags.ELYTRA_TRIMS) && !mod.shouldHide()) {
            return RenderInterceptionResult.ignore();
        }
        *///?}
        // On 1.21.9+, ElytraTrims 4.x is faded rather than collapsed: we DO enter the scope under ET on
        // the non-hide path, so the base elytra fades via the ELYTRA scope and ETElytraTrimSubmitMixin
        // fades ET's trim submits from the same scope, in lockstep. The hide path below still cancels
        // outright (ET's renderLayers then never runs, so there is nothing to fade).

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
