package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final HumanoidModel<AvatarRenderState> model;
    private final EquipmentAssetManager equipmentAssets;

    public CapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> p_116602_, EntityModelSet p_365418_, EquipmentAssetManager p_387093_) {
        super(p_116602_);
        this.model = new PlayerCapeModel(p_365418_.bakeLayer(ModelLayers.PLAYER_CAPE));
        this.equipmentAssets = p_387093_;
    }

    private boolean hasLayer(ItemStack p_372809_, EquipmentClientInfo.LayerType p_388089_) {
        Equippable equippable = p_372809_.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            EquipmentClientInfo equipmentclientinfo = this.equipmentAssets.get(equippable.assetId().get());
            return !equipmentclientinfo.getLayers(p_388089_).isEmpty();
        } else {
            return false;
        }
    }

    public void submit(PoseStack p_434174_, SubmitNodeCollector p_434543_, int p_432874_, AvatarRenderState p_445735_, float p_433069_, float p_435707_) {
        if (!p_445735_.isInvisible && p_445735_.showCape) {
            PlayerSkin playerskin = p_445735_.skin;
            if (playerskin.cape() != null) {
                if (!this.hasLayer(p_445735_.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
                    p_434174_.pushPose();
                    if (this.hasLayer(p_445735_.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) {
                        p_434174_.translate(0.0F, -0.053125F, 0.06875F);
                    }

                    p_434543_.submitModel(
                        this.model,
                        p_445735_,
                        p_434174_,
                        RenderTypes.entitySolid(playerskin.cape().texturePath()),
                        p_432874_,
                        OverlayTexture.NO_OVERLAY,
                        p_445735_.outlineColor,
                        null
                    );
                    p_434174_.popPose();
                }
            }
        }
    }
}
