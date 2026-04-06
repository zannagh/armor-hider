//? if >= 1.21.9 {
package de.zannagh.armorhider.client.rendering;

import net.minecraft.world.entity.EquipmentSlot;

import java.lang.reflect.Method;
import java.util.EnumMap;

/**
 * Reflective access to GeckoLib 5's render state color data.
 * Uses reflection to avoid a compile-time dependency on GeckoLib,
 * which may not have artifacts for all supported MC versions.
 * <p>
 * GeckoLib 5 stores per-slot render states in {@code DataTickets.PER_SLOT_RENDER_DATA}
 * (an {@code EnumMap<EquipmentSlot, RenderState>}) on the top-level render state.
 * Each per-slot state has its own cached {@code DataTickets.RENDER_COLOR}.
 */
public final class GeckoLibRenderState {

    private static volatile boolean initialized;
    private static Class<?> geoRenderStateClass;
    private static Method getGeckolibData;
    private static Method getOrDefaultGeckolibData;
    private static Method addGeckolibData;
    private static Object renderColorTicket;
    private static Object perSlotRenderDataTicket;

    private GeckoLibRenderState() {}

    private static void ensureInitialized() {
        if (initialized) return;
        synchronized (GeckoLibRenderState.class) {
            if (initialized) return;
            try {
                geoRenderStateClass = Class.forName("software.bernie.geckolib.renderer.base.GeoRenderState");
                Class<?> ticketClass = Class.forName("software.bernie.geckolib.constant.dataticket.DataTicket");
                Class<?> dataTicketsClass = Class.forName("software.bernie.geckolib.constant.DataTickets");
                renderColorTicket = dataTicketsClass.getField("RENDER_COLOR").get(null);
                perSlotRenderDataTicket = dataTicketsClass.getField("PER_SLOT_RENDER_DATA").get(null);
                getGeckolibData = geoRenderStateClass.getMethod("getGeckolibData", ticketClass);
                getOrDefaultGeckolibData = geoRenderStateClass.getMethod("getOrDefaultGeckolibData", ticketClass, Object.class);
                addGeckolibData = geoRenderStateClass.getMethod("addGeckolibData", ticketClass, Object.class);
            } catch (Exception ignored) {}
            initialized = true;
        }
    }

    public static boolean isGeoRenderState(Object renderState) {
        ensureInitialized();
        return geoRenderStateClass != null && geoRenderStateClass.isInstance(renderState);
    }

    /**
     * Gets the per-slot render state from the top-level render state's
     * {@code PER_SLOT_RENDER_DATA} EnumMap.
     */
    public static Object getPerSlotState(Object topLevelRenderState, EquipmentSlot slot) {
        ensureInitialized();
        if (getOrDefaultGeckolibData == null || perSlotRenderDataTicket == null) return null;
        try {
            Object map = getOrDefaultGeckolibData.invoke(topLevelRenderState, perSlotRenderDataTicket, null);
            if (map instanceof EnumMap<?, ?> enumMap) {
                return enumMap.get(slot);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static int getRenderColor(Object renderState) {
        ensureInitialized();
        if (getGeckolibData == null) return -1;
        try {
            return (Integer) getGeckolibData.invoke(renderState, renderColorTicket);
        } catch (Exception e) {
            return -1;
        }
    }

    public static void setRenderColor(Object renderState, int color) {
        ensureInitialized();
        if (addGeckolibData == null) return;
        try {
            addGeckolibData.invoke(renderState, renderColorTicket, color);
        } catch (Exception ignored) {}
    }
}
//?}

//? if < 1.21.9 {
/*package de.zannagh.armorhider.client.rendering;

public final class GeckoLibRenderState {
    private GeckoLibRenderState() {}
}
*///?}
