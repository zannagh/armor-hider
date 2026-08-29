//? if fabric {
package de.zannagh.armorhider.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.zannagh.armorhider.client.utils.McClientUtils;
import net.minecraft.client.Minecraft;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Resolved per click, not once at construction: the flag can be set after ModMenu has already
        // asked for the factory. A null screen is ModMenu's own "no config screen" signal (its default
        // factory returns exactly that), so the config button simply stays inert.
        return parent -> de.zannagh.armorhider.ArmorHider.isApiOnly()
                ? null
                : McClientUtils.getPreferredSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
//?}