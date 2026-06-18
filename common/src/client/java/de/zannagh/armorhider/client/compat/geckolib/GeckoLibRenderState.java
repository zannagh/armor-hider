package de.zannagh.armorhider.client.compat.geckolib;

import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;
import com.geckolib.constant.DataTickets;

//? if >= 1.21.9
import com.geckolib.renderer.base.GeoRenderState;

import java.util.EnumMap;

public final class GeckoLibRenderState {

    private GeckoLibRenderState() {}

    //? if >= 1.21.9 {
    public static @Nullable GeoRenderState getPerSlotState(GeoRenderState topLevelState, EquipmentSlot slot) {
        var map = topLevelState.getGeckolibData(DataTickets.PER_SLOT_RENDER_DATA);
        if (map instanceof EnumMap<?,?> enumMap) {
            return enumMap.get(slot) instanceof GeoRenderState grs ? grs : null;
        }
        return null;
    }

    public static int getRenderColor(GeoRenderState renderState) {
        return renderState.getGeckolibData(DataTickets.RENDER_COLOR);
    }

    public static void setRenderColor(GeoRenderState renderState, int color) {
        renderState.addGeckolibData(DataTickets.RENDER_COLOR, color);
    }
    //? }
}