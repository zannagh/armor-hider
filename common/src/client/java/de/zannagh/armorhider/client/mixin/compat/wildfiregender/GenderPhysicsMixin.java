// Gated on `gender_physics`, NOT `gender`: `gender` tracks the *layer* API generation (modern
// GenderArmorLayer vs the legacy inline GenderLayer), which has nothing to do with the physics API.
// getArmorPhysicsOverride()Z exists on com.wildfire.main.entitydata.PlayerConfig on both generations -
// including FGM 3.2.2 (kKffHCGl), the only build FGM ships for NeoForge - so gating this on `gender`
// left every NeoForge user with fully damped breasts behind hidden armor.
// Still gated to >= 1.21: fabric-1.20.1 pins an FGM build (nYZ0oktX) that predates
// com.wildfire.main.entitydata.PlayerConfig, so this would not compile there.
// FGM 5.0.0-Beta.5+ (the 26.1.2+ pins, new package layout) removed that chokepoint altogether; see the
// >= 26.1.2 branch below and GenderRenderStateMixin.
//? if gender_physics && >= 1.21 {
package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import de.zannagh.armorhider.client.compat.GenderPhysicsRelaxation;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.1.2 {
import com.wildfire.api.IGenderArmor;
import com.wildfire.client.physics.BreastPhysics;
import de.zannagh.armorhider.client.compat.UndampedGenderArmor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//? } else {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.main.entitydata.PlayerConfig;
import net.minecraft.client.Minecraft;
*///? }

// Compatibility mixin for Wildfire's Female Gender Mod breast physics (FGM 5.0.0-Beta.5+), SIMULATION half.
// Beta.5 turned the per-player "Armor Physics Override" into a client-global ConfigOverrides.armorPhysics
// flag; the per-entity damping inputs are now read straight off the worn armor's IGenderArmor
// (physicsResistance() / tightness()) by TWO independent consumers, and relaxing only one of them leaves
// the breasts visibly damped:
//   1. BreastPhysics.update(LivingEntity, IGenderArmor) - the simulation. This mixin hands it an
//      UndampedGenderArmor view of the real armor (both breasts route through update via
//      BothBreastsPhysics.tick).
//   2. GenderRenderState.<init> - resolves the real armor into GenderRenderState.armor, from which
//      GenderLayer.setupRender derives bounceEnabled (resistance < 1.0f) and gates every simulated
//      position/rotation read in setupTransformations. GenderRenderStateMixin covers that one.
// GenderPhysicsRelaxation owns the decision for both.
//? if >= 26.1.2 {
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = BreastPhysics.class, remap = false)
public class GenderPhysicsMixin {

    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, require = 0)
    private IGenderArmor armorHider$relaxPhysicsWhenChestHidden(IGenderArmor original, LivingEntity entity) {
        // Resolved live from config (not the render-time PlayerModificationInfo cache, which is only
        // rebuilt on a dirty flag and would lag behind an opacity change at physics-tick time).
        boolean relaxed = entity instanceof Player player && GenderPhysicsRelaxation.shouldRelaxFor(player);
        ArmorHiderRenderTypes.recordGenderPhysicsTick(relaxed);
        return relaxed ? UndampedGenderArmor.of(original) : original;
    }
}
//? } else {
/*/^*
 * Compatibility mixin for Wildfire's Female Gender Mod breast physics.
 * <p>
 * The mod damps the breast "jiggle" using the equipped chestplate's
 * {@code IGenderArmor.physicsResistance()} / {@code tightness()}, so a rigid plate suppresses the
 * bounce even when Armor Hider has visually hidden that plate. FGM already exposes exactly the
 * behaviour we want under its own "Armor Physics Override" setting, which zeroes both values -
 * so rather than fake an empty chest stack, we simply force that override on for players whose
 * chest Armor Hider is hiding. {@link GenderPhysicsRelaxation} owns that decision, including why it
 * uses the configured opacity rather than {@code SlotModification#shouldHide()}.
 * <p>
 * {@code getArmorPhysicsOverride()} is the single point both consumers read:
 * <ul>
 *   <li>{@code BreastPhysics.update} - the simulation itself (zeroes resistance + tightness), and</li>
 *   <li>{@code GenderRenderState.<init>} - the per-frame render capture.</li>
 * </ul>
 * Hooking it therefore covers the whole pipeline; hooking either consumer alone leaves the other
 * damped. {@link PlayerConfig} overrides the method, so the mixin must target the subclass - a
 * mixin on {@code EntityConfig} would be bypassed by virtual dispatch for players.
 ^/
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = PlayerConfig.class, remap = false)
public class GenderPhysicsMixin {

    @ModifyReturnValue(method = "getArmorPhysicsOverride", at = @At("RETURN"), require = 0)
    private boolean armorHider$relaxPhysicsWhenChestHidden(boolean original) {
        boolean relaxed = original || armorHider$shouldRelaxChestPhysics();
        ArmorHiderRenderTypes.recordGenderPhysicsTick(relaxed && !original);
        return relaxed;
    }

    /^*
     * Whether Armor Hider is hiding the chest of the player this config belongs to far enough to stop
     * FGM damping the physics with it. The config carries only a UUID, so the player is resolved from
     * the client level.
     ^/
    private boolean armorHider$shouldRelaxChestPhysics() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        // EntityConfig#uuid is public final; PlayerConfig extends it.
        var uuid = ((EntityConfig) (Object) this).uuid;
        if (uuid == null) {
            return false;
        }
        // Scan the (short) loaded-player list rather than Level#getEntity(UUID), which does not exist
        // before 1.21.8 - Level#players() is stable across every version this mod targets.
        Player player = null;
        for (Player candidate : level.players()) {
            if (uuid.equals(candidate.getUUID())) {
                player = candidate;
                break;
            }
        }
        if (player == null) {
            return false;
        }
        // Resolved live from config (not the render-time PlayerModificationInfo cache, which is only
        // rebuilt on a dirty flag and would lag behind an opacity change at physics-tick time).
        return GenderPhysicsRelaxation.shouldRelaxFor(player);
    }
}
*///? }
//?}
