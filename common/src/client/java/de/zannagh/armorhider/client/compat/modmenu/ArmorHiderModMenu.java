//? if fabric {
package de.zannagh.armorhider.client.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import net.minecraft.client.Minecraft;

public class ArmorHiderModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> McClientUtils.getPreferredSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
//?}