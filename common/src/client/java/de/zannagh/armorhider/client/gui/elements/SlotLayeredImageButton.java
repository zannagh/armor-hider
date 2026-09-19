package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.eunomia.client.gui.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link LayeredImageButton} that is bound to a single {@link EquipmentSlot}. Subclasses pick their
 * sprites and tooltips from {@link #slot}; a null slot means the button is not slot-specific.
 */
public abstract class SlotLayeredImageButton extends LayeredImageButton {

    @Nullable protected final EquipmentSlot slot;

    public SlotLayeredImageButton(@Nullable EquipmentSlot slot, boolean initial, int width, int height, Component message, OnPress onPress) {
        super(initial, width, height, message, onPress);
        this.slot = slot;
    }

    /**
     * The equipment slot this button controls, or null when it is not tied to one.
     * @return the bound equipment slot, or null.
     */
    public @Nullable EquipmentSlot slot() {
        return slot;
    }
}
