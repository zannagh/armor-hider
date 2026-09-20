// FGM 5.0.0-Beta.5+ (the 26.1.2+ pins, new package layout) dropped the per-player "Armor Physics Override"
// chokepoint (PlayerConfig#getArmorPhysicsOverride). Both of its former consumers now read the worn armor's
// IGenderArmor directly - BreastPhysics.update(LivingEntity, IGenderArmor) for the simulation and
// GenderRenderState.<init> (via WildfireClientHelper.getArmorConfig) for the render side - so
// GenderPhysicsMixin and GenderRenderStateMixin hand each of them this wrapper instead.
//? if gender_physics && >= 26.1.2 {
package de.zannagh.armorhider.client.compat;

import com.wildfire.api.IBreastArmorTexture;
import com.wildfire.api.IGenderArmor;

/**
 * An {@link IGenderArmor} view of the real worn armor whose physics inputs are zeroed - the same
 * zeroed {@link #physicsResistance()} / {@link #tightness()} FGM's own "Armor Physics Override" applies,
 * but per player. Everything else ({@link #coversBreasts()}, {@link #alwaysHidesBreasts()},
 * {@link #armorStandsCopySettings()}, {@link #texture()}) is forwarded unchanged, so the breast armor is
 * still drawn with its own texture; Armor Hider only stops it damping the bounce.
 */
public final class UndampedGenderArmor implements IGenderArmor {

    private final IGenderArmor real;

    private UndampedGenderArmor(IGenderArmor real) {
        this.real = real;
    }

    /**
     * @return {@code real} with its physics inputs zeroed; a wrapper is never wrapped twice.
     */
    public static IGenderArmor of(IGenderArmor real) {
        if (real instanceof UndampedGenderArmor) {
            return real;
        }
        return new UndampedGenderArmor(real == null ? IGenderArmor.EMPTY : real);
    }

    @Override
    public boolean coversBreasts() {
        return real.coversBreasts();
    }

    @Override
    public boolean alwaysHidesBreasts() {
        return real.alwaysHidesBreasts();
    }

    @Override
    public float physicsResistance() {
        return 0.0F;
    }

    @Override
    public float tightness() {
        return 0.0F;
    }

    @Override
    public boolean armorStandsCopySettings() {
        return real.armorStandsCopySettings();
    }

    @Override
    public IBreastArmorTexture texture() {
        return real.texture();
    }
}
//?}
