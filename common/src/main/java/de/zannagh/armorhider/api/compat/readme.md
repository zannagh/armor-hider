Compat classes and resources for Armor Hider.

These are touched during mixin bootstrap (resource probing runs from the mixin plugins), so the constraint is to
avoid loading **Minecraft/game classes** — and anything that *transitively* triggers early MC class loading, most
notably the main `ArmorHider` class (it imports `net.minecraft` types). That is why `CompatManager` uses its own
standalone `EnrichedLogger` rather than a logger reached through mod/game code.

Referencing other MC-free Armor Hider utilities is fine; the boundary is Minecraft class loading, not Armor Hider
code in general.
