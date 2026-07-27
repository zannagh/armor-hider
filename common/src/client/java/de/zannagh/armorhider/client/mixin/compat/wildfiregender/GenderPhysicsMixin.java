// Gated to >= 1.21: fabric-1.20.1 pins an FGM build (nYZ0oktX) that predates
// com.wildfire.main.entitydata.PlayerConfig, so this would not compile there.
//? if gender && >= 1.21 {
package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.main.entitydata.PlayerConfig;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Compatibility mixin for Wildfire's Female Gender Mod breast physics.
 * <p>
 * The mod damps the breast "jiggle" using the equipped chestplate's
 * {@code IGenderArmor.physicsResistance()} / {@code tightness()}, so a rigid plate suppresses the
 * bounce even when Armor Hider has visually hidden that plate. FGM already exposes exactly the
 * behaviour we want under its own "Armor Physics Override" setting, which zeroes both values —
 * so rather than fake an empty chest stack, we simply force that override on for players whose
 * chest Armor Hider is fully hiding.
 * <p>
 * {@code getArmorPhysicsOverride()} is the single point both consumers read:
 * <ul>
 *   <li>{@code BreastPhysics.update} — the simulation itself (zeroes resistance + tightness), and</li>
 *   <li>{@code GenderRenderState.<init>} — the per-frame render capture.</li>
 * </ul>
 * Hooking it therefore covers the whole pipeline; hooking either consumer alone leaves the other
 * damped. {@link PlayerConfig} overrides the method, so the mixin must target the subclass — a
 * mixin on {@code EntityConfig} would be bypassed by virtual dispatch for players.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(value = PlayerConfig.class, remap = false)
public class GenderPhysicsMixin {

    @ModifyReturnValue(method = "getArmorPhysicsOverride", at = @At("RETURN"), require = 0)
    private boolean armorHider$relaxPhysicsWhenChestHidden(boolean original) {
        boolean relaxed = original || armorHider$isChestFullyHidden();
        de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes
                .recordGenderPhysicsTick(relaxed && !original);
        return relaxed;
    }

    /**
     * Whether Armor Hider is fully hiding the chest of the player this config belongs to. The config
     * carries only a UUID, so the player is resolved from the client level.
     */
    private boolean armorHider$isChestFullyHidden() {
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
        // before 1.21.8 — Level#players() is stable across every version this mod targets.
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
        String playerName = PlayerNameUtil.getPlayerName(player);
        if (playerName == null) {
            return false;
        }
        // Resolved live from config (not the render-time PlayerModificationInfo cache, which is only
        // rebuilt on a dirty flag and would lag behind an opacity change at physics-tick time).
        return SlotModification.of(playerName, EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST))
                .shouldHide();
    }
}
//?}
