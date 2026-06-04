package de.zannagh.armorhider.client.api.render;

import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record RenderInterceptionResult(
        boolean shouldIntercept,
        boolean shouldCancel,
        // TODO: Figure out a way to specify this is null if shouldIntercept is false, but otherwise isn't
        IdentityCarrier carrier,
        ItemStack itemStack,
        EquipmentSlot slot
) {

    public static RenderInterceptionResult shouldUseVanilla(){
        return new RenderInterceptionResult(false, false, null, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
    }
}
