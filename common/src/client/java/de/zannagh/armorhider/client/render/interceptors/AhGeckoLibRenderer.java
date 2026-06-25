package de.zannagh.armorhider.client.render.interceptors;

import com.geckolib.constant.DataTickets;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.HashMap;

//? if >= 1.21.9
import com.geckolib.renderer.base.GeoRenderState;

public class AhGeckoLibRenderer extends AbstractArmorHiderRenderer {

    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, @Nullable CallbackInfo ci) {
        if (!(identityCarrier instanceof IdentityCarrier carrier)) {
            return RenderInterceptionResult.ignore();
        }
        var mod = resolveModification(carrier, slot, stack);

        if (!mod.needsModification()) {
            return RenderInterceptionResult.ignore();
        }
        RenderInterceptionResult result;
        if (mod.shouldHide()) {
            result = new RenderInterceptionResult(true, true, RenderScope.ARMOR_PIECE, carrier, mod);
            return result;
        }
        result = new RenderInterceptionResult(true, false, RenderScope.ARMOR_PIECE, carrier, mod);

        var ctx = AhRenderManagementApi.enterScope(result);

        //? if >= 1.21.9 {
        if (identityCarrier instanceof GeoRenderState geoState) {
            GeoRenderState perSlotState = getPerSlotState(geoState, slot);
            if (perSlotState == null) {
                return result;
            }
            Integer originalColor = getRenderColor(perSlotState);
            if (originalColor == null) {
                return result;
            }
            pushGeckoLibColor(carrier.armorHider$playerName(), originalColor);
            int modifiedColor = ctx.renderModificationApi().applyArmorTransparency(originalColor);
            setRenderColor(perSlotState, modifiedColor);
        }
        //? }
        return result;
    }

    public void popAndApplyColor(Object renderState, EquipmentSlot slot) {
        if (!(renderState instanceof IdentityCarrier carrier)) {
            return;
        }
        //? if >= 1.21.9 {
        var savedColor = geckoLibColorStack.remove(carrier.armorHider$playerName());

        if (savedColor != null && renderState instanceof GeoRenderState geoState) {
            GeoRenderState perSlotState = getPerSlotState(geoState, slot);
            if (perSlotState != null) {
                setRenderColor(perSlotState, savedColor);
            }
        }
        //? }
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ALL;
    }

    //? if >= 1.21.9 {
    public static @Nullable GeoRenderState getPerSlotState(GeoRenderState topLevelState, EquipmentSlot slot) {
        var map = topLevelState.getGeckolibData(DataTickets.PER_SLOT_RENDER_DATA);
        if (map instanceof EnumMap<?,?> enumMap) {
            return enumMap.get(slot) instanceof GeoRenderState grs ? grs : null;
        }
        return null;
    }

    public static @Nullable Integer getRenderColor(GeoRenderState renderState) {
        return renderState.getGeckolibData(DataTickets.RENDER_COLOR);
    }

    public static void setRenderColor(GeoRenderState renderState, int color) {
        renderState.addGeckolibData(DataTickets.RENDER_COLOR, color);
    }
    //? }

    private final HashMap<String, Integer> geckoLibColorStack = new HashMap<>();

    private void pushGeckoLibColor(String carrierName, Integer color) {
        geckoLibColorStack.put(carrierName, color);
    }
}
