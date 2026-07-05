package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ArmorHiderEmptyRenderer implements AhRenderer {
    @Override
    public RenderInterceptionResult intercept(@Nullable Object identityCarrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack, CallbackInfo ci) {
        return RenderInterceptionResult.ignore();
    }

    @Override
    public AhRenderModificationApi getRenderModificationApi() {
        return RenderModifications.empty();
    }

    private AhRenderTypeFactory customRenderTypeFactory;

    public void registerRenderTypeFactory(AhRenderTypeFactory renderTypeFactory){
        customRenderTypeFactory = renderTypeFactory;
    }

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ALL;
    }

    @Override
    public RenderType getTranslucentArmorRenderType(Identifier texture) {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentArmorRenderType(texture);
        }
        return ArmorHiderRenderTypes.translucentArmor(texture);
    }

    @Override
    public RenderType getTranslucentEntityRenderType(Identifier texture) {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentEntityRenderType(texture);
        }
        return ArmorHiderRenderTypes.translucentEntity(texture);
    }

    @Override
    public RenderType getTranslucentArmorTrimRenderType(boolean decal) {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentArmorTrimRenderType(decal);
        }
        return ArmorHiderRenderTypes.translucentArmorTrim();
    }

    @Override
    public RenderType getTranslucentItemSheetRenderType() {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentItemSheetRenderType();
        }
        return ArmorHiderRenderTypes.translucentItemSheet();
    }
}
