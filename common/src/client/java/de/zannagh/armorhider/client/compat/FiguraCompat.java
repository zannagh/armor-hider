package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.api.compat.CompatFlags;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Arrays;

public final class FiguraCompat {

    public static boolean shouldEnforceHeadRendering(EquipmentSlot slot, Boolean... additionalChecks) {
        boolean anyAdditionalChecksFailed = Arrays.stream(additionalChecks).anyMatch(check -> !check);
        if (anyAdditionalChecksFailed) {
            return false;
        }
        return CompatManager.requiresCompatTo(CompatFlags.FIGURA) && slot == EquipmentSlot.HEAD;
    }
}
