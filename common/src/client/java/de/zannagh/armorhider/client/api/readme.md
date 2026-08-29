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
| `ArmorHiderRenderApi` | Predicate-driven rules that hide, fade or de-glint armor / elytra / off-hand items per player and slot. | **Start here.** Third-party mods that just want their own hide condition, with no mixin and no `AhRenderer`. |
| `AhRenderRule` | Opaque handle returned by every `ArmorHiderRenderApi` registration. | Keep it to `unregister()` the rule later. |
| `AhHideContext` | What Armor Hider knows about the piece being rendered (player name, lazily resolved `Player`, slot, stack, elytra flag, config, pre-rule opacity). | Passed to the `*Matching(...)` rule variants. |
| `AhRenderRuleBuilder` / `AhRenderRuleCondition` | Staged builder for full control: target, priority, owner, sharing, effect, condition. | Anyone needing a priority, an owner or `shared()` - the convenience methods have none of them. |

All API interfaces are marked `@ApiStatus.NonExtendable` - only Armor Hider itself implements
them, and they expose only static entry points. The mutator side of `AhRenderManagementApi`
is additionally marked `@ApiStatus.Internal`.

## Cookbook

### Hide equipment on your own condition (no mixin needed)

```java
// Hide the helmet while the player is sleeping.
AhRenderRule helmetRule = ArmorHiderRenderApi.hideArmorWhen(
        EquipmentSlot.HEAD, Player::isSleeping);

// Fade the elytra to 30% while sprinting, and drop its glint.
ArmorHiderRenderApi.setElytraOpacityWhen(0.3f, Player::isSprinting);
ArmorHiderRenderApi.disableElytraGlintWhen(Player::isSprinting);

// Off-hand goes away entirely while sneaking.
ArmorHiderRenderApi.hideOffhandWhen(Player::isShiftKeyDown);

// Later, e.g. when your feature is toggled off:
helmetRule.unregister();
```

Need the slot, the stack, the resolved config or the player name rather than the entity? Use the
`*Matching` variants, which take a `Predicate<AhHideContext>`:

```java
ArmorHiderRenderApi.hideArmorWhenMatching(EquipmentSlot.CHEST, ctx ->
        ctx.stack().is(Items.NETHERITE_CHESTPLATE) && ctx.playerName().startsWith("[AFK]"));
```

### Let the other players see it too: `shared()`

A plain rule is **viewer-side**: it applies to every player *this* client renders. `shared()` declares
the other kind of rule — a statement about *me* — and Armor Hider then broadcasts what it resolves to
for the local player, so everybody else sees the same thing.

```java
// Everyone sees my helmet come off while I sleep, not just me.
ArmorHiderRenderApi.rule(EquipmentSlot.HEAD)
        .owner(MY_MOD_ID)
        .shared()
        .hide()
        .when(Player::isSleeping);
```

Predicates are arbitrary code and cannot be serialised, so the **outcome** is what travels: the owning
client evaluates its shared rules against itself once per client tick and sends the resulting opacity
and glint per target whenever it changes. That means:

- The condition is evaluated against the **local player** for this purpose, with the local player's own
  stack, config and pre-rule opacity in the `AhHideContext`. A shared rule's predicate therefore runs
  once more per tick than an unshared one.
- Sharing is **additive**: the rule still evaluates locally exactly as before.
- Other clients see a change one server round-trip later, and repeated changes are coalesced (at most
  one packet per 250 ms) — this is not the tool for a per-frame flicker.
- Nothing is sent to a server that does not run Armor Hider (or the Paper plugin): the outgoing packet
  gate suppresses it and the rule stays local-only. Only the *owning* client needs the mod that
  registered the rule; every other client needs Armor Hider.
- A receiving client applies the outcome through its own rule pipeline, so all the limits below still
  hold there — it is skipped wherever *that viewer's* Armor Hider renders vanilla, and a local rule with
  a stronger priority still wins. A remote outcome participates at `defaultPriority()`.
- A glint-only rule broadcasts the glint alone. It never carries the sender's own opacity, so it cannot
  drag the sender's configured transparency onto anybody else's screen.
- The server relays the **authenticated** name and id, never what the sender claimed, so a client
  cannot hide somebody else's armor by announcing state under their name. It is keyed by display name
  like every other player lookup here, with the same caveat.
- State is cleared for a player when they stop matching, when they leave, and when they rejoin — a
  previous session's claim never lingers.

### Take full control: priority and owners

Priority, owner tagging and `shared()` live only on the builder, reached via `rule(slot)` or `elytraRule()`.
Target, effect and condition are all compile-enforced — a rule missing any of them does not compile.
A builder is safe to hold and reuse for any number of rules: each effect setter returns a fresh
immutable condition snapshot, so no registration can inherit a previous one's effect. Note that
`priority(...)`, `owner(...)` and `shared()` must come before the effect — the effect hands off to
`AhRenderRuleCondition` and there is no way back.

```java
ArmorHiderRenderApi.rule(EquipmentSlot.LEGS)
        .owner(MY_MOD_ID)                    // for unregisterAll(...)
        .priority(ArmorHiderRenderApi.defaultPriority() - 1)   // lower = stronger
        .opacity(0.5f)
        .andDisableGlint()
        .whenMatching(ctx -> isStealthed(ctx.player()));

ArmorHiderRenderApi.elytraRule().owner(MY_MOD_ID).hide().when(Player::isSprinting);

ArmorHiderRenderApi.unregisterAll(MY_MOD_ID);
```

**Precedence.** The strongest matching rule decides outright: only rules at the strongest priority
present contribute, and **lower numeric values are stronger** (matching the renderer registry).
Among equal-priority matching rules, the lowest opacity wins. The winning opacity *replaces* the
value Armor Hider derived from the user's config — so a rule can also make a piece more visible
than the user configured.

**Priority bands opacity only — glint ignores it.** Any matching glint rule suppresses the glint
whatever its priority, and nothing can force a glint back on. The asymmetry is deliberate:
"disable" is the only direction a glint rule can express, so banding it would only ever let an
unrelated opacity rule silently swallow another mod's glint rule. The consequence to be aware of:
a rule that *loses* the opacity band still contributes its glint, so a high-priority
`opacity(1.0f).andDisableGlint()` rule can be overruled on opacity by nothing at all and still have
a weaker rule's glint suppression applied on top.

**Evaluation model.** A predicate is evaluated **exactly once** per player, per slot, per
modification, on the render thread, always with the real worn stack in `ctx.stack()`. Keep
predicates cheap. A predicate that throws counts as non-matching and is logged at most once per
minute per rule; only after five consecutive throws is the rule dropped, and any successful
evaluation resets that streak — so a transient `ctx.player()` NPE while the entity is out of render
distance does not kill your rule for the session, and an intermittently-throwing rule stays visible
in the log instead of reporting once and then misbehaving silently.


**Limits.**
- Rules are skipped wherever Armor Hider deliberately renders vanilla: global kill switch, "disable
  Armor Hider on others", a per-player disable, an excluded item, and skulls with "opacity affects
  hats/skulls" off.
- Targetable slots are `HEAD`, `CHEST`, `LEGS`, `FEET` and `OFFHAND`, plus elytra via
  `elytraRule()`. `MAINHAND`, `BODY` and `SADDLE` throw at registration — Armor Hider has no render
  path for them, so a rule there could never fire.
- A `Predicate<Player>` rule does not match while the entity is unresolvable on the client (out of
  render distance, removed). Use the `*Matching` form if the name alone is enough.
- Opacity below `0.05` (Armor Hider's smallest step) is a **full hide**, not a very faint render.
- `hideElytraWhen(...)` covers plain elytra wings only. An **armored elytra** follows the CHEST
  rules instead, because Armor Hider's own config treats it as chest armor rather than as an
  elytra — use `hideArmorWhen(EquipmentSlot.CHEST, ...)` to catch it.
- Player lookup is keyed by display name, so two players sharing one display name resolve to
  whichever was iterated last. This matches how Armor Hider identifies players everywhere else.
- With no rules registered and nothing shared by anyone, no predicate runs and nothing is allocated —
  evaluation short-circuits on two volatile reads and an array index per slot.
- Rules are re-derived from the user's configured opacity on every evaluation, never layered onto a
  previous result, so a predicate that stops matching reverts immediately rather than latching.

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
        return standardIntercept(carrier, EquipmentSlot.CHEST, carrier.armorHider$getItemBySlot(EquipmentSlot.CHEST), ci);
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

### Use Armor Hider as a library, without its UI

A mod that drives Armor Hider entirely through this API usually does not want Armor Hider showing up
as a mod of its own next to it. One call takes the whole front end away:

```java
// From a Fabric ModInitializer/ClientModInitializer, or a NeoForge mod constructor.
ArmorHider.useAsApiOnly();
```

Gone: the three keybinds (toggle, open settings, load preset — they also stop appearing in the vanilla
Controls screen) and every entry point into the settings screens (the Options-screen button, the Skin
Customization panel, the ModMenu config factory, the keybind).

Kept: all networking (handshake, config sync, combat-log relay, the shared-rule transport above), the
whole render pipeline and API, and the end user's own Armor Hider configuration — it still loads,
persists, syncs and drives rendering, only the UI to change it is gone. A fresh install sits at vanilla
defaults, so nothing hides unless your rules say so, while an existing user's saved settings keep
working.

Call it as early as you can: mod initialisers run before `Options` is loaded, which is when the
keybinds would be installed. A later call still works — the mappings are stripped on the next client
tick — it just means they existed briefly. The switch is one-way and idempotent.

## Conventions

- Render-type methods on `AhRenderModificationApi` use `Object` rather than `RenderType` so the
  API stays game-version independent. Cast via `instanceof RenderType` at the call site.
- Priority is **lower-is-stronger** for the renderer registry (MC-modding convention). The
  built-in default priority is exposed via `AhRenderInterceptionRegistryApi.defaultPriority()`.
- Rule opacities are **absolute**, never relative: they replace the computed value rather than
  scaling it.
- Priority is **lower-is-stronger** for render rules too, and is settable only through
  `ArmorHiderRenderApi.rule(...)` / `.elytraRule()`. The convenience methods deliberately have no
  priority overloads so a bare `0` can never be read as a priority where an opacity was meant.
- `RenderScope.ALL` is a *fallback* - a renderer registered with target scope `ALL` is only
  consulted when no renderer is registered for the requested specific scope.
