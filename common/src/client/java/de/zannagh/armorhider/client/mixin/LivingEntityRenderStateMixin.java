//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.client.scopes.IdentityStateCarrier;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Injects identity carrier delegation onto player render states so that the identity
 * captured during {@code extractRenderState} travels with the render state object to the
 * submission phase — no global ThreadLocal needed.
 * <p>
 * Targets {@code AvatarRenderState} in 1.21.4–1.21.8
 * by Stonecutter) so that only player render states carry identity — non-player entities
 * are filtered via {@code instanceof IdentityCarrier} in the rendering mixins.
 */
@Mixin(AvatarRenderState.class)
public class LivingEntityRenderStateMixin implements IdentityStateCarrier {

    @Unique
    private @Nullable IdentityCarrier armorHider$carrier;

    @Override
    public @Nullable String playerName() {
        return armorHider$carrier != null ? armorHider$carrier.playerName() : null;
    }

    @Override
    public void attachCarrier(@Nullable IdentityCarrier carrier) {
        armorHider$carrier = carrier;
    }

    @Override
    public @Nullable ItemStack customHeadItem() {
        HumanoidRenderState state = (HumanoidRenderState) (Object) this;
        if (state.wornHeadProfile != null) {
            return new ItemStack(Items.PLAYER_HEAD);
        } else if (state.wornHeadType != null) {
            return ItemsUtil.getItemStackFromSkullBlockType(state.wornHeadType);
        }
        return armorHider$carrier != null ? armorHider$carrier.customHeadItem() : null;
    }

    @Override
    public boolean isPlayerFlying() {
        return armorHider$carrier != null && armorHider$carrier.isPlayerFlying();
    }
}
//?}
