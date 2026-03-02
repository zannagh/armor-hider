package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArmorRenderPipeline {

    /**
     * Checks whether a given equipment slot should be considered visually hidden for a player.
     * This is used to make other mods (e.g. Essential) see hidden armor slots as empty,
     * so they can render custom skins or cosmetics in place of the hidden armor.
     *
     * @param playerName the name of the player whose equipment is being checked
     * @param slot the equipment slot to check
     * @param itemInSlot the item currently in the slot (used for skull/elytra special-case checks)
     * @return true if the slot should appear empty to other mods
     */
    
}
