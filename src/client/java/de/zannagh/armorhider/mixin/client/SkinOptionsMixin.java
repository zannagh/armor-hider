// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.OptionElementFactory;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.rendering.PlayerPreviewRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
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
        
        OptionElementFactory optionElementFactory = new OptionElementFactory(this, body, gameOptions);
        if (MinecraftClient.getInstance().player != null) {
            optionElementFactory = optionElementFactory.withHalfWidthRendering();
        }
        
        optionElementFactory.addTextAsWidget(Text.translatable("armorhider.options.mod_title"));
        
        var helmetOption = optionElementFactory.buildDoubleOption(
                "armorhider.helmet.transparency", 
                Text.translatable("armorhider.options.helmet.tooltip"), 
                Text.translatable("armorhider.options.helmet.tooltip_narration"),
                Text.translatable("armorhider.options.helmet.button_text"),
                currentValue -> String.format("%.0f%%", currentValue * 100),
                ClientConfigManager.get().helmetTransparency,
                ClientConfigManager::setHelmetTransparency);
        optionElementFactory.addSimpleOptionAsWidget(helmetOption);

        var chestOption = optionElementFactory.buildDoubleOption(
                "armorhider.chestplate.transparency",
                Text.translatable("armorhider.options.chestplate.tooltip"),
                Text.translatable("armorhider.options.chestplate.tooltip_narration"),
                Text.translatable("armorhider.options.chestplate.button_text"),
                currentValue -> String.format("%.0f%%", currentValue * 100),
                ClientConfigManager.get().chestTransparency,
                ClientConfigManager::setChestTransparency);
        optionElementFactory.addSimpleOptionAsWidget(chestOption);

        var legsOption = optionElementFactory.buildDoubleOption(
                "armorhider.legs.transparency",
                Text.translatable("armorhider.options.leggings.tooltip"),
                Text.translatable("armorhider.options.leggings.tooltip_narration"),
                Text.translatable("armorhider.options.leggings.button_text"),
                currentValue -> String.format("%.0f%%", currentValue * 100),
                ClientConfigManager.get().legsTransparency,
                ClientConfigManager::setLegsTransparency);
        optionElementFactory.addSimpleOptionAsWidget(legsOption);

        var bootsOption = optionElementFactory.buildDoubleOption(
                "armorhider.boots.transparency",
                Text.translatable("armorhider.options.boots.tooltip"),
                Text.translatable("armorhider.options.boots.tooltip_narration"),
                Text.translatable("armorhider.options.boots.button_text"),
                currentValue -> String.format("%.0f%%", currentValue * 100),
                ClientConfigManager.get().bootsTransparency,
                ClientConfigManager::setBootsTransparency);
        optionElementFactory.addSimpleOptionAsWidget(bootsOption);
        
        SimpleOption<Boolean> enableCombatDetection = optionElementFactory.buildBooleanOption(
                Text.translatable("armorhider.options.combat_detection.title"),
                Text.translatable("armorhider.options.combat_detection.tooltip"),
                Text.translatable("armorhider.options.combat_detection.tooltip_narration"),
                ClientConfigManager.get().enableCombatDetection,
                ClientConfigManager::setCombatDetection
        );
        optionElementFactory.addSimpleOptionAsWidget(enableCombatDetection);
        
        if (ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin) {
            SimpleOption<Boolean> combatHidingOnServer = optionElementFactory.buildBooleanOption(
                    Text.translatable("armorhider.options.combat_detection_server.title"),
                    Text.translatable("armorhider.options.combat_detection_server.tooltip"),
                    Text.translatable("armorhider.options.combat_detection_server.tooltip_narration"),
                    ClientConfigManager.get().enableCombatDetection,
                    ClientConfigManager::setCombatDetection
            );
            optionElementFactory.addSimpleOptionAsWidget(combatHidingOnServer);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks){
        super.render(context, mouseX, mouseY, deltaTicks);
        PlayerPreviewRenderer.renderPlayerPreview(context, body, mouseX, mouseY);
    }
}
