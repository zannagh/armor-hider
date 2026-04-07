//? if >= 1.21.9 && < 1.21.11 {
package de.zannagh.armorhider.client.compat.elytratrims;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.ColorMath;
import de.zannagh.armorhider.client.rendering.RenderTypeResolver;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import dev.kikugie.elytratrims.api.render.ETRenderParameters;
import dev.kikugie.elytratrims.api.render.ETRenderingAPI;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * ElytraTrims compatibility via their public rendering API.
 * Registers a {@link ETRenderingAPI.Callback} that applies Armor Hider's
 * hiding and transparency to all ElytraTrims decorators (trims, patterns, etc.).
 */
public class ElytraTrimsCompat implements CompatLoader {

    public void init() {
        ETRenderingAPI.wrapRenderCall((parameters, collector, operation) -> {
            var ctx = ArmorHiderClient.RENDER_CONTEXT;
            ActiveModification mod = ctx.activeModification();

            // If no modification was already set by our ElytraRenderMixin (i.e. ET
            // renders in a separate layer), try to derive one from the render state.
            boolean ownedMod = false;
            if (mod == null && parameters.object() instanceof IdentityCarrier carrier) {
                if (carrier.isPlayerFlying()) {
                    return operation.apply(parameters, collector);
                }
                mod = carrier.createModification(EquipmentSlot.CHEST, parameters.stack());
                ownedMod = true;
            }

            if (mod == null) {
                return operation.apply(parameters, collector);
            }

            try {
                if (mod.shouldHide()) {
                    return false;
                }

                if (mod.transparency() < 1.0) {
                    int alpha = (int) (mod.transparency() * 255);
                    int modifiedColor = ColorMath.withAlpha(parameters.color(), alpha);
                    var translucentRender = RenderTypeResolver.translucentArmor(parameters.texture());
                    var modifiedParams = new ETRenderParameters(
                            parameters.elytra(), parameters.object(), parameters.stack(),
                            parameters.matrices(), translucentRender, parameters.sprite(),
                            parameters.texture(), parameters.light(), modifiedColor,
                            parameters.overlay(), parameters.outline(), parameters.order()
                    );
                    return operation.apply(modifiedParams, collector);
                }

                return operation.apply(parameters, collector);
            } finally {
                if (ownedMod) {
                    ctx.clearActiveModification();
                }
            }
        });
        ArmorHider.LOGGER.info("ElytraTrims compatibility loaded.");
    }
}
//?}
