//? if >= 1.21.2 {
package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.common.PlayerModificationInfo;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.IdentityStateCarrier;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//? if >= 1.21.4 {
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.Items;
//?}

/**
 * Injects identity carrier delegation onto player render states so that the identity
 * captured during {@code extractRenderState} travels with the render state object to the
 * submission phase — no global ThreadLocal needed.
 * <p>
 * Targets {@code AvatarRenderState} from 1.21.2 onward (the render-state era). Only player
 * render states carry identity — non-player entities are filtered via
 * {@code instanceof IdentityCarrier} in the rendering mixins. Without this the render state has
 * no identity and every render-state-based scope (armor/head/elytra/third-person offhand) fails
 * to enter — the reason 1.21.2/1.21.3 rendered everything opaque except the first-person offhand.
 * <p>
 * In 1.21.2/1.21.3 the render state does not yet expose {@code wornHeadProfile}/{@code wornHeadType}
 * (added in 1.21.4), so {@link #ah$getCustomHeadItem()} falls back to the entity carrier there.
 */
@Mixin(AvatarRenderState.class)
public class LivingEntityRenderStateMixin implements IdentityStateCarrier {

    @Unique
    private @Nullable IdentityCarrier armorHider$carrier;

    @Override
    public @Nullable String armorHider$playerName() {
        return armorHider$carrier != null ? armorHider$carrier.armorHider$playerName() : null;
    }


    @Override
    public void ah$attachCarrier(@Nullable IdentityCarrier carrier) {
        armorHider$carrier = carrier;
    }

    @Override
    public @Nullable IdentityCarrier ah$getCarrier() {
        return armorHider$carrier;
    }

    @Override
    public @Nullable ItemStack ah$getCustomHeadItem() {
        //? if >= 1.21.4 {
        HumanoidRenderState state = (HumanoidRenderState) (Object) this;
        if (state.wornHeadProfile != null) {
            return new ItemStack(Items.PLAYER_HEAD);
        } else if (state.wornHeadType != null) {
            return ItemsUtil.getItemStackFromSkullBlockType(state.wornHeadType);
        }
        //?}
        return armorHider$carrier != null ? armorHider$carrier.ah$getCustomHeadItem() : null;
    }

    @Override
    public boolean ah$isPlayerFlying() {
        return armorHider$carrier != null && armorHider$carrier.ah$isPlayerFlying();
    }

    @Override
    public boolean armorHider$isPlayerInvisible() {
        return armorHider$carrier != null && armorHider$carrier.armorHider$isPlayerInvisible();
    }

    @Override
    public boolean ah$isPlayerBlocking() {
        return armorHider$carrier != null && armorHider$carrier.ah$isPlayerBlocking();
    }

    @Override
    public PlayerModificationInfo armorHider$getPlayerModifications() {
        return armorHider$carrier != null ? armorHider$carrier.armorHider$getPlayerModifications() : null;
    }

    @Override
    public @NonNull ItemStack armorHider$getItemBySlot(EquipmentSlot slot) {
        return armorHider$carrier == null ? ItemStack.EMPTY : armorHider$carrier.armorHider$getItemBySlot(slot);
    }

}
//?}
