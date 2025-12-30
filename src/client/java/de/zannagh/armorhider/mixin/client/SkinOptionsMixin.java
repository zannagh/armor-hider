// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.config.ClientConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    
    @Unique
    private static final int armorHiderSegmentRow = 5;
    
    @Unique
    private static boolean shouldRenderPreview() {
        return MinecraftClient.getInstance().player != null;
    }

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
        body.addWidgetEntry(simpleOptionToWidget(helmetOption), null);

        SimpleOption<Double> chestOption = new SimpleOption<>(
                "armorhider.chest.transparency",
                SimpleOption.constantTooltip(Text.literal("Adjusts the chestplate transparency for your model between 0 and 1 in steps of 0.05. Applies to elytra as well.")),
                (text, value) -> Text.literal("Chestplate: " + String.format("%.0f%%", value * 100)),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                ClientConfigManager.get().chestTransparency,
                ClientConfigManager::setChestTransparency
        );
        body.addWidgetEntry(simpleOptionToWidget(chestOption), null);

        SimpleOption<Double> legsOption = new SimpleOption<>(
                "armorhider.legs.transparency",
                SimpleOption.constantTooltip(Text.literal("Adjusts the legs transparency for your model between 0 and 1 in steps of 0.05.")),
                (text, value) -> Text.literal("Leggings: " + String.format("%.0f%%", value * 100)),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                ClientConfigManager.get().legsTransparency,
                ClientConfigManager::setLegsTransparency
        );
        body.addWidgetEntry(simpleOptionToWidget(legsOption), null);

        SimpleOption<Double> bootsOption = new SimpleOption<>(
                "armorhider.boots.transparency",
                SimpleOption.constantTooltip(Text.literal("Adjusts the boot transparency for your model between 0 and 1 in steps of 0.05.")),
                (text, value) -> Text.literal("Boots: " + String.format("%.0f%%", value * 100)),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                ClientConfigManager.get().bootsTransparency,
                ClientConfigManager::setBootsTransparency
        );
        body.addWidgetEntry(simpleOptionToWidget(bootsOption), null);

        SimpleOption<Boolean> enableCombatHiding = SimpleOption.ofBoolean(
                "Armor in combat",
                SimpleOption.constantTooltip(Text.literal("Enables detection of combat to show your armor when you are in combat.")),
                (Text, Value) -> net.minecraft.text.Text.literal(Value ? "Enabled" : "Disabled"),
                ClientConfigManager.get().enableCombatDetection,
                ClientConfigManager::setCombatDetection
        );
        body.addWidgetEntry(simpleOptionToWidget(enableCombatHiding), null);
        
        if (ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin && shouldRenderPreview()) {
            SimpleOption<Boolean> combatHidingOnServer = SimpleOption.ofBoolean(
                    "Armor in combat (server)",
                    SimpleOption.constantTooltip(Text.literal("Enables detection of combat server-wide to force showing armor when a player is in combat. If enabled, this will override individual's detection setting.")),
                    (Text, Value) -> net.minecraft.text.Text.literal(Value ? "Enabled" : "Disabled"),
                    ClientConfigManager.getServerConfig().enableCombatDetection,
                    ClientConfigManager::setAndSendServerCombatDetection
            );
            body.addWidgetEntry(simpleOptionToWidget(combatHidingOnServer), null);
        }
    }
    
    @Unique
    private ClickableWidget simpleOptionToWidget(SimpleOption<?> simpleOption){
        int width = shouldRenderPreview() ? body.getRowWidth() / 2 : body.getRowWidth();
        return simpleOption.createWidget(gameOptions, body.getRowLeft(), body.getYOfNextEntry(), width);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks){
        super.render(context, mouseX, mouseY, deltaTicks);
        if (!shouldRenderPreview()) {
            return;
        }
        renderPlayerPreview(context, mouseX, mouseY, deltaTicks);
    }

    @Unique
    private void renderPlayerPreview(DrawContext context, int mouseX, int mouseY, float delta) {
        var current = ((GameOptionsScreen)(Object)this);
        if (!(current instanceof SkinOptionsScreen)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        
        int margin = 20;

        int rightHalfWidth = this.body.getRowWidth() / 2;
        int rightHalfStart = this.body.getRowLeft() + rightHalfWidth;
        
        int previewSize = rightHalfWidth - margin * 2;
        int previewX = rightHalfStart + rightHalfWidth / 2; // Center X of right half
        int previewY = this.body.getRowTop(armorHiderSegmentRow) + 2 + previewSize;

        int panelLeft = previewX - previewSize / 2 - 10;
        int panelTop = previewY - previewSize;
        int panelRight = previewX + previewSize / 2 + 10;
        int panelBottom = previewY + 20;
        context.fill(panelLeft, panelTop, panelRight, panelBottom, Color.darkGray.darker().getRGB());

        int borderColor = Colors.LIGHT_GRAY;
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, borderColor); // Top
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, borderColor); // Bottom
        context.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, borderColor); // Left
        context.fill(panelRight - 1, panelTop, panelRight, panelBottom, borderColor); // Right

        InventoryScreen.drawEntity(
            context,
            panelLeft,
            panelTop,
            panelRight,
            panelBottom, 
            (int)Math.round(previewSize * 0.4),
            0.25f,
            (float) mouseX,
            (float) mouseY,
            client.player
        );
    }
}
