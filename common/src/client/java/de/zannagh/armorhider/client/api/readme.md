# `de.zannagh.armorhider.client.api`

Public, game-version-independent client API for Armor Hider. Use these interfaces to query Armor
Hider state, plug in custom render behaviour, or hook combat detection from third-party code.

## What's here

| Interface | What it is | Who calls it |
|---|---|---|
| `AhRenderManagementApi` | Render-pipeline state: phase flags, active scopes, current player name. | Anyone querying scope state. Internal mutators are marked `@ApiStatus.Internal` and reserved for the mod's own mixins. |
| `AhRenderInterceptionRegistryApi` | Registry of `AhRenderer`s keyed by `RenderScope`. | Anyone installing a custom renderer. |
| `AhRenderer` | Per-scope renderer that drives the interception decision (build modification, decide cancel/intercept, store render-modification API). | Implement to customise a single scope's behaviour. |
| `AhRenderTypeFactory` | Factory for translucent armor / entity / trim / item-sheet render types. | Implement and pass to a renderer's `registerRenderTypeFactory` to swap render pipelines (e.g. shader compat). |
| `AhRenderModificationApi` | Pass-through-safe operations that apply a modification to colors, render types and glint. | Queried via `AhRenderManagementApi.getActiveScope(scope).renderModificationApi()`. |
| `AhCombatApi` | Client-side combat detection hooks. | Called from damage-event mixins; query `shouldLogCombatForPlayer` from third-party combat-source code. |

All API interfaces are marked `@ApiStatus.NonExtendable` — only Armor Hider itself implements
them, and they expose only static entry points. The mutator side of `AhRenderManagementApi`
is additionally marked `@ApiStatus.Internal`.

## Cookbook

### Read the active scope from a mixin

```java
var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
if (ctx.isEmpty()) {
    return original.call(/* ... */);
}
int modifiedColor = ctx.renderModificationApi().applyArmorTransparency(color);
```

### Replace the built-in cape renderer

```java
public class MyCapeRenderer extends AbstractArmorHiderRenderer {
    @Override public RenderScope getTargetScope() { return RenderScope.CAPE; }
    @Override public RenderInterceptionResult interceptFrom(IdentityCarrier carrier, CallbackInfo ci) {
        // ... compute the modification you want; call super for the standard path
        return standardIntercept(carrier, EquipmentSlot.CHEST, carrier.getItemBySlot(EquipmentSlot.CHEST), ci);
    }
}

// Register at a lower priority value than the default to take precedence.
AhRenderInterceptionRegistryApi.register(
    new MyCapeRenderer(),
    AhRenderInterceptionRegistryApi.defaultPriority() - 1);
```

### Plug in a custom render-type pipeline

```java
AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ARMOR_PIECE)
    .registerRenderTypeFactory(new MyTranslucentArmorFactory());
```

## Conventions

- Render-type methods on `AhRenderModificationApi` use `Object` rather than `RenderType` so the
  API stays game-version independent. Cast via `instanceof RenderType` at the call site.
- Priority is **lower-is-stronger** for the renderer registry (MC-modding convention). The
  built-in default priority is exposed via `AhRenderInterceptionRegistryApi.defaultPriority()`.
- `RenderScope.ALL` is a *fallback* — a renderer registered with target scope `ALL` is only
  consulted when no renderer is registered for the requested specific scope.
