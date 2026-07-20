package de.zannagh.armorhider.client;

import de.zannagh.armorhider.mixin.NeoForgeClientMixinPlugin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class MixinPlugin extends NeoForgeClientMixinPlugin {

    public MixinPlugin() {
        // FMLEnvironment is NeoForge-only, so the dedicated-server check lives here (not in the common
        // intermediate) and is passed up as a plain boolean. Everything else — the mixin list,
        // resource-probe onLoad, getMixins filtering — is inherited.
        //? if >= 1.21.9 {
        /*super(FMLEnvironment.getDist() == Dist.DEDICATED_SERVER);
        *///?} else {
        super(FMLEnvironment.dist == Dist.DEDICATED_SERVER);
        //?}
    }
}
