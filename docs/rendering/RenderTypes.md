Armor Hider uses its own render types to enable transparency on items/entities where vanilla doesn't.

You can check the factory at `common/src/client/java/de/zannagh/armorhider/client/rendering/RenderTypeFactory.java` (useful for other modders who want to mixin to the resolution of RenderTypes).

You can check the actual render types themselves at `common/src/client/java/de/zannagh/armorhider/client/rendering/ArmorHiderRenderTypes.java`.

# Render types provided

- **translucentArmor** — armor layers with translucent blending and no depth write
- **translucentEntity** — general entity rendering (shields, banners, skulls) with no depth write
- **translucentArmorTrim** — armor trims using the `ARMOR_TRIMS_SHEET` atlas
- **translucentItemSheet** — block/item atlas items (e.g. cutout block items swapped to translucent)

# Version-specific implementations

The Minecraft render pipeline has changed significantly across versions, so `ArmorHiderRenderTypes` has four separate code paths:

| Version range | Approach |
|---|---|
| < 1.21.4 | `CompositeState` with `COLOR_WRITE` mask to suppress depth writes. Class extends `RenderStateShard` to access protected shard fields. |
| 1.21.4 | Same `CompositeState` approach but with `TriState`-based `TextureStateShard` and dedicated `RENDERTYPE_ARMOR_TRANSLUCENT_SHADER`. |
| 1.21.5 – 1.21.10 | `RenderPipeline.Snippet` cloning with `writeDepth = false`. Pipeline fields + `CompositeState` builder for render type creation. |
| 1.21.11+ | `RenderSetup` API with `RenderPipeline.Snippet` cloning. `DepthStencilState` replaces individual depth fields (>= 26.1-snapshot-10). `BindGroupLayout` replaces samplers/uniforms (>= 26.2-snapshot). |

# Access wideners

Because `RenderType.create()` and `RenderType.CompositeState` are not public in vanilla Minecraft, the `multiloader-loom` convention plugin dynamically generates an access widener at build time (`build/generated/armor-hider.accesswidener`). The widener entries vary by version:

- **< 1.21.5**: Widens `CompositeState` inner class and the 7-arg `create()` method.
- **1.21.5 – 1.21.10**: Additionally widens the pipeline-based `create()` and all `CompositeStateBuilder` methods (which became protected in 1.21.5).
- **>= 1.21.11**: Widens the `RenderSetup`-based `create(String, RenderSetup)` in the `rendertype` package.

The fabric module references the common module's access widener via the `multiloader-loom` convention plugin.

# Notes
- On versions smaller than 1.21 it's virtually impossible to disable depth writes for translucent entities in third person. Entities render before translucent terrain, so disabling depth writes causes water/ice to render *in front of* the item instead of behind it. Hence, 1.20.x uses vanilla `entityTranslucent` (with depth writes) for the third-person buffer wrapping in `RenderModifications.solidToTranslucent`. First person is unaffected because the hand renders after translucent terrain.
- NeoForge makes most of these members public via access transformers, so the access widener is effectively a no-op on NeoForge builds.