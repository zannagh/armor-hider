Mixin Plugins for Armor Hider (NeoForge + Fabric).

These classes run during mixin bootstrap, before Minecraft classes are safe to load. The constraint here is to
avoid loading **Minecraft/game classes** — and anything that *transitively* triggers early MC class loading, most
notably the main `ArmorHider` class (it imports `net.minecraft` types). Do not reference such code from here.

Referencing MC-free Armor Hider utilities is fine and expected — e.g. the compat probing in
`de.zannagh.armorhider.api.compat.CompatManager` and the standalone `EnrichedLogger`, neither of which loads any
Minecraft class.
