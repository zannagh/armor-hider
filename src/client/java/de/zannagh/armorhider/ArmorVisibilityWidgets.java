package de.zannagh.armorhider;

import net.minecraft.client.gui.widget.CyclingButtonWidget;

public class ArmorVisibilityWidgets {
    private static CyclingButtonWidget<Boolean> helmetButton;
    private static CyclingButtonWidget<Boolean> chestButton;
    private static CyclingButtonWidget<Boolean> legsButton;
    private static CyclingButtonWidget<Boolean> bootsButton;

    public static void setHelmetButton(CyclingButtonWidget<Boolean> button) {
        helmetButton = button;
    }

    public static void setChestButton(CyclingButtonWidget<Boolean> button) {
        chestButton = button;
    }

    public static void setLegsButton(CyclingButtonWidget<Boolean> button) {
        legsButton = button;
    }

    public static void setBootsButton(CyclingButtonWidget<Boolean> button) {
        bootsButton = button;
    }

    public static void updateAllButtons(boolean enabled) {
        if (helmetButton != null) {
            helmetButton.setValue(enabled);
        }
        if (chestButton != null) {
            chestButton.setValue(enabled);
        }
        if (legsButton != null) {
            legsButton.setValue(enabled);
        }
        if (bootsButton != null) {
            bootsButton.setValue(enabled);
        }
    }

    public static void clear() {
        helmetButton = null;
        chestButton = null;
        legsButton = null;
        bootsButton = null;
    }
}
