package de.zannagh.armorhider.client.compat.figura;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Arrays;

public final class FiguraCompat {

    public static boolean shouldEnforceHeadRendering(EquipmentSlot slot, Boolean... additionalChecks) {
        boolean anyAdditionalChecksFailed = Arrays.stream(additionalChecks).anyMatch(check -> !check);
        if (anyAdditionalChecksFailed) {
            return false;
        }
        return ArmorHiderClient.FIGURA_LOADED && slot == EquipmentSlot.HEAD;
    }
}
