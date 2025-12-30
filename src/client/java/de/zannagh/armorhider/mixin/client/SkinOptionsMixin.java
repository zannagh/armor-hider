// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.rendering.PlayerPreviewRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(GameOptionsScreen.class)
public abstract class SkinOptionsMixin extends Screen {

    @Shadow
    protected OptionListWidget body;
    
    @Final
    @Shadow
    protected GameOptions gameOptions;

    protected SkinOptionsMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onAddOptions(CallbackInfo ci) {
        var current = ((GameOptionsScreen)(Object)this);
        if (!(current instanceof SkinOptionsScreen)) {
            return;
        }

        if (body == null) {
            return;
        }

        this.body.addHeader(Text.literal("Zannagh's Armor Hider"));

        SimpleOption<Double> helmetOption = new SimpleOption<>(
                "armorhider.helmet.transparency",
                SimpleOption.constantTooltip(Text.literal("Adjusts the helmet transparency for your model between 0 and 1 in steps of 0.05. Applies to skulls and hats as well.")),
                (text, value) -> Text.literal("Helmet: " + String.format("%.0f%%", value * 100)),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                ClientConfigManager.get().helmetTransparency,
                ClientConfigManager::setHelmetTransparency
        );
        body.addWidgetEntry(PlayerPreviewRenderer.simpleOptionToWidget(helmetOption, gameOptions, body), null);

        SimpleOption<Double> chestOption = new SimpleOption<>(
                "armorhider.chest.transparency",
                SimpleOption.constantTooltip(Text.literal("Adjusts the chestplate transparency for your model between 0 and 1 in steps of 0.05. Applies to elytra as well.")),
                (text, value) -> Text.literal("Chestplate: " + String.format("%.0f%%", value * 100)),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                ClientConfigManager.get().chestTransparency,
                ClientConfigManager::setChestTransparency
        );
        body.addWidgetEntry(PlayerPreviewRenderer.simpleOptionToWidget(chestOption, gameOptions, body), null);

        SimpleOption<Double> legsOption = new SimpleOption<>(
                "armorhider.legs.transparency",
                SimpleOption.constantTooltip(Text.literal("Adjusts the legs transparency for your model between 0 and 1 in steps of 0.05.")),
                (text, value) -> Text.literal("Leggings: " + String.format("%.0f%%", value * 100)),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                ClientConfigManager.get().legsTransparency,
                ClientConfigManager::setLegsTransparency
        );
        body.addWidgetEntry(PlayerPreviewRenderer.simpleOptionToWidget(legsOption, gameOptions, body), null);

        SimpleOption<Double> bootsOption = new SimpleOption<>(
                "armorhider.boots.transparency",
                SimpleOption.constantTooltip(Text.literal("Adjusts the boot transparency for your model between 0 and 1 in steps of 0.05.")),
                (text, value) -> Text.literal("Boots: " + String.format("%.0f%%", value * 100)),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                ClientConfigManager.get().bootsTransparency,
                ClientConfigManager::setBootsTransparency
        );
        body.addWidgetEntry(PlayerPreviewRenderer.simpleOptionToWidget(bootsOption, gameOptions, body), null);

        SimpleOption<Boolean> enableCombatHiding = SimpleOption.ofBoolean(
                "Combat Detection",
                SimpleOption.constantTooltip(Text.literal("Enables detection of combat to show your armor when you are in combat.")),
                (Text, Value) -> net.minecraft.text.Text.literal(Value ? "ON" : "OFF"),
                ClientConfigManager.get().enableCombatDetection,
                ClientConfigManager::setCombatDetection
        );
        body.addWidgetEntry(PlayerPreviewRenderer.simpleOptionToWidget(enableCombatHiding, gameOptions, body), null);
        
        if (ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            SimpleOption<Boolean> combatHidingOnServer = SimpleOption.ofBoolean(
                    "Armor in combat (server)",
                    SimpleOption.constantTooltip(Text.literal("Enables detection of combat server-wide to force showing armor when a player is in combat. If enabled, this will override individual's detection setting.")),
                    (Text, Value) -> net.minecraft.text.Text.literal(Value ? "ON" : "OFF"),
                    ClientConfigManager.getServerConfig().enableCombatDetection,
                    ClientConfigManager::setAndSendServerCombatDetection
            );
            body.addWidgetEntry(PlayerPreviewRenderer.simpleOptionToWidget(combatHidingOnServer, gameOptions, body), null);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks){
        super.render(context, mouseX, mouseY, deltaTicks);
        PlayerPreviewRenderer.renderPlayerPreview(context, body, mouseX, mouseY);
    }
}
