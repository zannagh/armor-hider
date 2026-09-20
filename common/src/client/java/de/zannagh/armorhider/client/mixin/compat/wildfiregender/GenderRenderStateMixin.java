// FGM 5.0.0-Beta.5+ (the 26.1.2+ pins, new package layout). Render-side half of the breast-physics
// relaxation; the simulation half is GenderPhysicsMixin. Gated like it (gender_physics, not gender).
//? if gender_physics && >= 26.1.2 {
package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.wildfire.api.IGenderArmor;
import com.wildfire.client.render.GenderRenderState;
import de.zannagh.armorhider.client.compat.GenderPhysicsRelaxation;
import de.zannagh.armorhider.client.compat.UndampedGenderArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Feeds Armor Hider's breast-physics relaxation to FGM's <em>render</em> side.
 * <p>
 * Beta.5 has two independent consumers of the worn chestplate's {@code IGenderArmor} physics profile,
 * and relaxing only one of them leaves the breasts visibly damped:
 * <ul>
 *   <li>{@code BreastPhysics.update(LivingEntity, IGenderArmor)} - the simulation, which
 *       {@code GenderPhysicsMixin} hands an undamped profile; and</li>
 *   <li>{@code GenderRenderState.<init>}, which resolves the real armor via
 *       {@code WildfireClientHelper.getArmorConfig(chestEquipment)} into its public {@code armor} field.
 *       {@code GenderLayer.setupRender} then reads {@code armor.physicsResistance()} and computes
 *       {@code bounceEnabled = hasBreastPhysics && (!isChestplateOccupied || resistance < 1.0f)}, and
 *       {@code setupTransformations} only applies the simulated positions/rotations when
 *       {@code bounceEnabled} is set. Diamond, iron, netherite and copper plates have resistance 1.0, so
 *       without this hook the relaxed simulation is computed and then never drawn.</li>
 * </ul>
 * This wraps that constructor lookup and substitutes the same {@link UndampedGenderArmor} wrapper when
 * {@link GenderPhysicsRelaxation} says the chest is hidden far enough. The wrapper forwards
 * {@code coversBreasts()} and the rest, so the breast armor itself is still drawn (and faded by
 * {@code GenderArmorLayerV5Mixin} + {@code EquipmentRenderMixin}); only the damping inputs are zeroed.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = GenderRenderState.class, remap = false)
public class GenderRenderStateMixin {

    @WrapOperation(
            method = "<init>",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lcom/wildfire/client/WildfireClientHelper;getArmorConfig(Lnet/minecraft/world/item/ItemStack;)Lcom/wildfire/api/IGenderArmor;")
    )
    private IGenderArmor armorHider$relaxRenderArmor(ItemStack stack, Operation<IGenderArmor> original,
                                                     @Local(argsOnly = true) LivingEntity entity) {
        IGenderArmor armor = original.call(stack);
        if (entity instanceof Player player && GenderPhysicsRelaxation.shouldRelaxFor(player)) {
            return UndampedGenderArmor.of(armor);
        }
        return armor;
    }
}
//?}
