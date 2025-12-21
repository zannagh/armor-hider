package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EquipmentSlot;

public class ArmorHiderClient implements ClientModInitializer {
    
    public static ThreadLocal<EquipmentSlot> CurrentSlot =  ThreadLocal.withInitial(() -> null);
    public static ThreadLocal<ArmorModificationInfo> CurrentArmorMod = ThreadLocal.withInitial(() -> null);

    public static boolean shouldNotInterceptRender(Object renderState) {
        return renderState instanceof PlayerEntityRenderState;
    }

    public static void trySetCurrentSlotFromEntityRenderState(LivingEntityRenderState livingEntityRenderState){
        if (livingEntityRenderState == null) {
            return;
        }
        
        if (livingEntityRenderState instanceof PlayerEntityRenderState playerEntityRenderState
                && ArmorHiderClient.CurrentSlot.get() != null) {
            var configByEntityState = tryResolveConfigFromPlayerEntityState(ArmorHiderClient.CurrentSlot.get(), playerEntityRenderState);
            ArmorHiderClient.CurrentArmorMod.set(configByEntityState);
        }
    }
    
    @Override
	public void onInitializeClient() {
        ArmorHider.LOGGER.info("Armor Hider client initializing...");
        ClientPlayNetworking.registerGlobalReceiver(SettingsS2CPacket.IDENTIFIER, (payload, context) -> {
            ArmorHider.LOGGER.info("Armor Hider received configuration from server.");
            
            var config = payload.config();
            
            if (config == null) {
                ArmorHider.LOGGER.error("Failed to load settings packet.");
                return;
            }
            
            ClientConfigManager.setServerConfig(config);
            ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
        });
        ClientConfigManager.load();
        ClientPlayConnectionEvents.JOIN.register((handler,  packetSender,  client) ->{
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ClientConfigManager.updateName(playerName);
            ClientConfigManager.updateId(handler.getProfile().id());
            ClientPlayNetworking.send(new SettingsC2SPacket(ClientConfigManager.get()));
        });
	}

    public static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(EquipmentSlot slot, PlayerEntityRenderState state){
        return state.displayName == null 
                ? new ArmorModificationInfo(slot, ClientConfigManager.get())
                : new ArmorModificationInfo(slot, ClientConfigManager.getConfigForPlayer(state.displayName.getString()));
    }
    
}