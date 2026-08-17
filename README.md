# Armor Hider
  
[![Latest](https://img.shields.io/github/v/release/zannagh/armor-hider?logo=github&label=Latest%20Release&color=green)](https://github.com/zannagh/armor-hider/releases)
[![LatestPre](https://img.shields.io/github/v/release/zannagh/armor-hider?include_prereleases&label=Latest%20(Pre)Release&logo=github)](https://github.com/zannagh/armor-hider/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/zannaghs-armor-hider?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/zannaghs-armor-hider)
[![Curseforge Downloads](https://img.shields.io/curseforge/dt/1475841?logo=curseforge&style=flat&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/armor-hider)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/AMwbYqdmQb)

<p align="center">
A no-dependency mod to the transparency or fully hide armor and equipment with multiplayer support, without any dependencies - supporting a wide range of game versions and available for Fabric, Quilt and NeoForge with a custom built UI that makes it feel like as if it would have been shipped with the game.
</p>

<p align="center">
<img alt="Armor Hider Overview" src="https://github.com/user-attachments/assets/3e0d3758-afc6-41c0-b622-8eaed2ac6916" />
</p>

### Features
Armor Hider features a big selection of customization options to have your player model drawn up to your liking. Additionally, all settings are synchronized in multiplayer and many of them can be adjusted via administration. For more details on the features and how to configure them, follow the links in the list below.

* [Per-slot opacity](https://github.com/zannagh/armor-hider/wiki/Configuration#opacity-adjustments-and-slot-configuration) sliders for helmet, chestplate, leggings, boots and offhand
* [Enchantment glint control](https://github.com/zannagh/armor-hider/wiki/Configuration#opacity-adjustments-and-slot-configuration) to selectively hide the glint on any slot
* [Combat detection](https://github.com/zannagh/armor-hider/wiki/Configuration#combat-detection) lets you automatically show armor when in combat
* [Visibility](https://github.com/zannagh/armor-hider/wiki/Configuration#respect-invisibility) settings let you choose whether your armor should be drawn when you've used an invisibility potion
* [Full multiplayer sync](https://github.com/zannagh/armor-hider/wiki/Multiplayer-Sync) so other players see your settings when the server has the mod
* [Resource pack compatibility](https://github.com/zannagh/armor-hider/wiki/Configuration#respect-invisibility) for armor non-EMF or EMF armor models, with the option to [use vanilla's armor](https://github.com/zannagh/armor-hider/wiki/Configuration#switch-to-vanilla-armor-in-combat) in combat
* Works client-side only too without server mod required
* Live in-game preview of your changes
* [Keybindings](https://github.com/zannagh/armor-hider/wiki/Configuration#keybinds) to quickly toggle Armor Hider or open the settings screen
* [Presets](https://github.com/zannagh/armor-hider/wiki/Configuration#presets) to store your favorite configurations - including quick-loading by a keybind you can define yourself
* [Individual item configurations](https://github.com/zannagh/armor-hider/wiki/Configuration#item-handling) provide more freedom if you're using custom items and want them handled by Armor Hider or not
* [Adjustable player-configs](https://github.com/zannagh/armor-hider/wiki/Configuration#individual-player-configuration) allow you to choose (if the server allows) how other players are drawn - both generically applied or on a per-player basis
* [Admin controls](https://github.com/zannagh/armor-hider/wiki/Advanced-Settings) for server operators
* Adjustable behavior for [compatible mods](https://github.com/zannagh/armor-hider/wiki/Configuration#compatibilities), see the full compatibility list at [compatibilities](https://github.com/zannagh/armor-hider/wiki/Compatibilities)

<p align="center">
<img alt="Demo" src="https://github.com/user-attachments/assets/8e1e345c-2eff-49d8-b7e5-2e15c5df2221" />
</p>

#### Compatibility

Armor Hider has explicit compatibility with major mods focused around visuals, like [Essential](https://github.com/SparkUniverse/Essential-Mod), [Elytra Trims](https://codeberg.org/KikuGie/elytra-trims), [Female Gender Mod](https://github.com/FemaleGenderMod/FemaleGenderMod), [GeckoLib](https://github.com/bernie-g/geckolib), [Accessories](https://modrinth.com/mod/accessories)/[Trinkets](https://modrinth.com/mod/trinkets)/[Curios](https://modrinth.com/mod/curios), [Wavey Capes](https://modrinth.com/mod/wavey-capes), [Iris](https://github.com/IrisShaders/Iris), [EMF](https://github.com/Traben-0/Entity_Model_Features) and many more. 

For detailed information on which compatibilities are built-in and what each compatibility does, check out the [Wiki](https://github.com/zannagh/armor-hider/wiki/Compatibilities).

*If you're using a mod, datapack or plugin not yet supported, please open an issue on GitHub to let me know or drop a message on the Discord Server.*

[![OpenBugs](https://img.shields.io/github/issues-search?query=repo%3Azannagh%2Farmor-hider%20is%3Aopen%20label%3Abug&logo=github&label=Open%20Bugs&color=red
)](https://github.com/zannagh/armor-hider/issues)
[![OpenFRs](https://img.shields.io/github/issues-search?query=repo%3Azannagh%2Farmor-hider%20is%3Aopen%20label%3Aenhancement&logo=github&label=Open%20Feature%20Requests&color=green
)](https://github.com/zannagh/armor-hider/issues)
[![ClosedIssues](https://img.shields.io/github/issues-closed/zannagh/armor-hider?label=Closed%20Issues&color=green&logo=github)](https://github.com/zannagh/armor-hider/issues)

I track issues and requests via [GitHub](https://github.com/zannagh/armor-hider/issues) and do my best to close out any bugs timely. If you don't have an account, feel free to join the Discord server and let me know there.

If you like my work and would like to support me, you can do so here:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/K3K41VR5H1)

---

## Community

Join the [Discord server](https://discord.gg/AMwbYqdmQb) for support, discussion, and feature requests.

## Issues and Feature Requests

As mentioned before, feel free to create an issue on the GitHub repository or reach out on Discord to make me aware of problems or ideas that could make this mod better.

## Versioning & Releases

All Minecraft versions are built from the `main` branch using [Stonecutter](https://stonecutter.kikugie.dev/) for multi-version support. [GitVersion](https://gitversion.net/) handles semantic versioning automatically. On CI, the version property is passed to the gradle build.

**Release flow:**

* **Prereleases** are created automatically on every push to `main` that includes code changes (commits prefixed with `ci:`, `docs:`, `build:`, or `chore:` are skipped)
* **Releases** are created manually via GitHub Releases with version validation
* All versions are published to [Modrinth](https://modrinth.com/mod/zannaghs-armor-hider) automatically on manual pre-releases (auto-prereleases are not published) or releases
* All versions are published to [CurseForge](https://www.curseforge.com/minecraft/mc-mods/armor-hider) automatically on manual pre-releases (auto-prereleases are not published) or releases

**Version format:**

* Releases: `0.7.2`
* Prereleases: `0.7.3-pre.1`, `0.7.3-pre.2`, etc.
