package de.zannagh.armorhider.client.render.interceptors;

import de.zannagh.armorhider.client.api.AhRenderModificationApi;
import de.zannagh.armorhider.client.api.AhRenderTypeFactory;
import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import de.zannagh.armorhider.client.suppressions.ConditionalSuppressor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

public class ArmorHiderEmptyRenderer implements AhRenderer {

    private final HashSet<ConditionalSuppressor> conditionalSuppressors = new HashSet<>();

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
    public void addConditionalSuppressor(ConditionalSuppressor suppressor) {
        conditionalSuppressors.add(suppressor);
    }

    @Override
    public HashSet<ConditionalSuppressor> getConditionalSuppressors() {
        return conditionalSuppressors;
    }

    @Override
    public RenderScope getTargetScope() {
        return RenderScope.ALL;
    }

    @Override
    public RenderType getTranslucentArmorRenderType(ResourceLocation texture) {
        if (customRenderTypeFactory != null){
            return customRenderTypeFactory.getTranslucentArmorRenderType(texture);
        }
        return ArmorHiderRenderTypes.translucentArmor(texture);
    }

    @Override
    public RenderType getTranslucentEntityRenderType(ResourceLocation texture) {
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
