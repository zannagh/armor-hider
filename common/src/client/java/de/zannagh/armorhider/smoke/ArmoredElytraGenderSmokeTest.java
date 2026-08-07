// Regression test for the "Armored Elytra" (dorkix) + Female Gender Mod breast-armor interaction.
// A dorkix armored elytra is a plain Items.ELYTRA carrying the chestplate in CUSTOM_DATA; FGM reads the
// raw elytra, finds no humanoid armor layer on its asset, and skips the breast armor entirely, so it
// vanishes. ArmoredElytraCompat + the GenderArmorLayerMixin substitution restore it by handing FGM the
// stored chestplate for its asset lookup. Only needs the FGM jar (the gender smoke row) - NOT the dorkix
// mod, since the breast fix is entirely on Armor Hider's side.
//? if fcgt && >= 26.2-1.pre {
package de.zannagh.armorhider.smoke;

import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.enums.Gender;
import com.wildfire.main.entitydata.PlayerConfig;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Armored Elytra (dorkix) + Female Gender Mod breast-armor smoke.
 * <p>
 * With a faded chest opacity the breast render-type swap fires while the breast is drawn. This asserts
 * the swap climbs for BOTH a normal chestplate (control) and a dorkix-format armored elytra (the fix):
 * before the {@code GenderArmorLayerMixin} substitution the armored-elytra case measured a flat {@code 0}
 * because FGM never drew the breast for an elytra item. No combat is used - the report is a pure render
 * regression.
 */
public final class ArmoredElytraGenderSmokeTest implements FabricClientGameTest {

    private static final double FADED = 0.7;

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Armored-elytra + gender smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    state.setGenerateStructures(false);
                })
                .create()) {

            context.runOnClient(client -> {
                var player = client.player;
                if (player == null) {
                    throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
                }
                PlayerConfig genderConfig = WildfireGender.getOrAddPlayerById(player.getUUID());
                genderConfig.updateGender(Gender.FEMALE);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

                ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                // A persisted disableArmorHider (from a prior run/keybind) renders everything vanilla,
                // so force the master switch on for this test.
                config.disableArmorHider.setValue(false);
                config.chestOpacity.setValue(FADED);
                config.opacityAffectingElytra.setValue(true);
            });
            context.waitTicks(20);

            // ── Control: a normal chestplate - the faded breast must be drawn (swap climbs). ─────────
            context.runOnClient(client -> client.player.setItemSlot(EquipmentSlot.CHEST,
                    new ItemStack(Items.DIAMOND_CHESTPLATE)));
            context.waitTicks(10);
            long control = breastSwapDelta(context);
            context.takeScreenshot("armorhider_ae_gender_1_chestplate");
            ArmorHider.LOGGER.info("[smoke/fcgt] CONTROL chestplate: breast swap delta = {}", control);

            // ── Armored elytra (dorkix item). After the fix the breast must still be drawn. ──────────
            context.runOnClient(client -> {
                var player = client.player;
                var ops = RegistryOps.create(NbtOps.INSTANCE, player.registryAccess());
                CompoundTag customData = new CompoundTag();
                customData.put("armored_elytra:elytra",
                        ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.ELYTRA)).getOrThrow());
                customData.put("armored_elytra:chestplate",
                        ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.DIAMOND_CHESTPLATE)).getOrThrow());
                ItemStack armoredElytra = new ItemStack(Items.ELYTRA);
                armoredElytra.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
                player.setItemSlot(EquipmentSlot.CHEST, armoredElytra);
            });
            context.waitTicks(10);
            long withElytra = breastSwapDelta(context);
            context.takeScreenshot("armorhider_ae_gender_2_armoredelytra");
            // The compat is gated on the Armored Elytra mod being present: with the mod, the stored
            // chestplate is substituted so FGM draws the breast; without it the item is just a plain
            // elytra and must be left untouched. Assert whichever the current mod set implies.
            boolean dorkixPresent = context.computeOnClient(client ->
                    de.zannagh.armorhider.api.compat.CompatManager.requiresCompatTo(
                            de.zannagh.armorhider.api.compat.CompatFlags.ARMORED_ELYTRA));
            ArmorHider.LOGGER.info("[smoke/fcgt] ARMORED-ELYTRA: breast swap delta = {}, dorkix present = {}",
                    withElytra, dorkixPresent);

            if (control <= 0) {
                throw new IllegalStateException("[smoke/fcgt] control breast swap did not climb (" + control
                        + ") - the test cannot detect a drawn breast, so the armored-elytra result is meaningless");
            }
            if (dorkixPresent && withElytra <= 0) {
                throw new IllegalStateException("[smoke/fcgt] Armored Elytra mod present but its breast was not drawn"
                        + " (swap delta " + withElytra + ") - the getArmorConfig substitution / EQUIPPABLE wrap did not"
                        + " restore FGM's breast armor");
            }
            if (!dorkixPresent && withElytra > 0) {
                throw new IllegalStateException("[smoke/fcgt] Armored Elytra mod ABSENT but the armored-elytra breast"
                        + " was drawn (swap delta " + withElytra + ") - the CUSTOM_DATA substitution fired without the"
                        + " mod present; it must be gated on CompatFlags.ARMORED_ELYTRA");
            }

            // ── Armored elytra with opacityAffectElytra OFF: the breast is chest armor, not a wing, so it
            //    must STILL fade in lockstep with the hidden chestplate (the swap keeps climbing). Turning
            //    the elytra toggle off keeps the wings visible - the whole point of an armored elytra - but
            //    must not spare the breast. Before interceptArmor substituted the underlying chestplate the
            //    breast delegated to the ELYTRA renderer and, with this toggle off, rendered fully opaque
            //    (swap delta 0) while the vanilla body plate hid: the reported bug. ────────────────────────
            context.runOnClient(client -> {
                var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                        .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
                config.opacityAffectingElytra.setValue(false);
            });
            context.waitTicks(10);
            long elytraToggleOff = breastSwapDelta(context);
            context.takeScreenshot("armorhider_ae_gender_3_elytratoggleoff");
            ArmorHider.LOGGER.info("[smoke/fcgt] ARMORED-ELYTRA opacityAffectElytra=OFF: breast swap delta = {}",
                    elytraToggleOff);
            if (dorkixPresent && elytraToggleOff <= 0) {
                throw new IllegalStateException("[smoke/fcgt] Armored-elytra breast did not fade with opacityAffectElytra"
                        + " OFF (swap delta " + elytraToggleOff + ") - it is following the elytra toggle instead of the"
                        + " chest slot; interceptArmor must substitute the underlying chestplate so the breast is scoped"
                        + " ARMOR_PIECE like the body plate");
            }
            if (!dorkixPresent && elytraToggleOff > 0) {
                throw new IllegalStateException("[smoke/fcgt] Armored Elytra mod ABSENT but the armored-elytra breast"
                        + " faded with opacityAffectElytra OFF (swap delta " + elytraToggleOff + ") - the chestplate"
                        + " substitution fired without the mod present; it must be gated on CompatFlags.ARMORED_ELYTRA");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] Armored-elytra + gender smoke passed (control={}, armoredElytra={},"
                    + " elytraToggleOff={}, dorkixPresent={})", control, withElytra, elytraToggleOff, dorkixPresent);
        }
    }

    private static long breastSwapDelta(ClientGameTestContext context) {
        long before = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        context.waitTicks(10);
        long after = context.computeOnClient(client -> ArmorHiderRenderTypes.breastArmorTranslucentSwapCount());
        return after - before;
    }
}
//?}
