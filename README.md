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
Armor Hider features a big selection of [customization options](#customizability) to have your player model drawn up to your liking. Additionally, all settings are [synchronized](#synchronization) in multiplayer and many of them can be adjusted via [administration](#administrative-settings).

- **Per-slot opacity sliders** for helmet, chestplate, leggings, boots and offhand
- **Enchantment glint control** to selectively hide the glint on any slot
- **Combat detection** lets you automatically show armor when in combat
- **Visibility settings** let you choose whether your armor should be drawn when you've used an invisibility potion
- **Full multiplayer sync** so other players see your settings when the server has the mod
- **Resource pack compatibility** for armor non-EMF or EMF armor models, with the option to use vanilla's armor in combat
- **Works client-side only** too without server mod required
- **Live in-game preview** of your changes
- **Keybindings** to quickly toggle Armor Hider or open the settings screen
- **Presets** to store your favorite configurations - including quick-loading by a keybind you can define yourself
- **Individual item** configurations provide more freedom if you're using custom items and want them handled by Armor Hider or not
- **Adjustable player-configs** allow you to choose (if the server allows) how other players are drawn - both generically applied or on a per-player basis
- **Admin controls** for server operators ([see below](#administrative-settings))

![Demo](https://github.com/user-attachments/assets/8e1e345c-2eff-49d8-b7e5-2e15c5df2221)

#### Compatibility
Armor Hider has explicit compatibility with some major mods focused around visuals. 

- [Essential](https://github.com/SparkUniverse/Essential-Mod)
- [Elytra Trims](https://codeberg.org/KikuGie/elytra-trims)
- [Wildfire Gender / Female Gender Mod](https://github.com/FemaleGenderMod/FemaleGenderMod)
- [GeckoLib](https://github.com/bernie-g/geckolib) (any custom armor mod using GeckoLib as intended should work out of the box)
- [Fantasy Armor](https://github.com/kend1e/FANTASY-ARMOR)
- [LuckPerms](https://luckperms.net/)
- [Iris](https://github.com/IrisShaders/Iris)
- [EMF](https://github.com/Traben-0/Entity_Model_Features) (most custom armor resource packs using EMF for rendering work right out of the box)
- [Accessories](https://modrinth.com/mod/accessories)/[Trinkets](https://modrinth.com/mod/trinkets)/[Curios](https://modrinth.com/mod/curios) (when either of these accessory-providers is available, you are able to choose whether they should be hidden as well or not)
- [Mekanism](https://modrinth.com/mod/mekanism)
- [Figura](https://modrinth.com/mod/figura) (as far as possible, mainly cross-compatibility to Mekanism)
- [ModMenu](https://modrinth.com/mod/modmenu)
- [Deeper and Darker](https://modrinth.com/mod/deeperdarker)
- [Ice and Fire CE](https://modrinth.com/mod/iceandfire-ce)
- [Wavey Capes](https://modrinth.com/mod/wavey-capes)

*If you're using a mod not yet supported, please open an issue on GitHub to let me know or drop a message on Discord.*

[![OpenBugs](https://img.shields.io/github/issues-search?query=repo%3Azannagh%2Farmor-hider%20is%3Aopen%20label%3Abug&logo=github&label=Open%20Bugs&color=red
)](https://github.com/zannagh/armor-hider/issues)
[![OpenFRs](https://img.shields.io/github/issues-search?query=repo%3Azannagh%2Farmor-hider%20is%3Aopen%20label%3Aenhancement&logo=github&label=Open%20Feature%20Requests&color=green
)](https://github.com/zannagh/armor-hider/issues)
[![ClosedIssues](https://img.shields.io/github/issues-closed/zannagh/armor-hider?label=Closed%20Issues&color=green&logo=github)](https://github.com/zannagh/armor-hider/issues)

I track issues (including feature requests) via GitHub and do my best to close out any bugs timely (plus, I get way too excited about new features myself..). If you don't have an account, feel free to join the Discord server and let me know there.

If you like my work and would like to support me, you can do so here:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/K3K41VR5H1)

---

## Customizability
Find Armor Hider's settings integrated into the game via "Skin Customization" in the game settings (or via 'Zannagh's Armor Hider' on older game versions). When in-game, the mod settings feature a live preview of your changes.

<p align="center">
<img alt="Armor Hider Ingame Settings" src="https://github.com/user-attachments/assets/ac6f48ce-47db-4901-8976-b1cbeb69a14e" />
</p>

### Armor Opacity and Adjustments
The mod allows you to define an opacity between 0% (hidden, cancelling the rendering) and 100% (the mod doesn't do anything) for each armor item slot. In addition, it's possible to specify whether skull items (skeleton/wither/... skull) should be affected by the helmet setting and if Elytras should be affected by the chestslot setting.
Furthermore, you can selectively hide the enchantment glint on any of the slots.

### Combat Detection
<details>
<summary>With combat detection enabled, your armor automatically becomes visible when you take or deal damage.</summary>



**Enabled Combat Detection**
![CombatDetection](https://github.com/user-attachments/assets/00f191e3-3997-4b83-ac41-7c169aae9d52)

**Disabled Combat Detection**
![NoCombatDetection](https://github.com/user-attachments/assets/fd7af6a4-8f5d-4a42-99bb-57804bc7ecca)
</details>

### Vanilla Armor in Combat
When armor resource packs using EMF (or not, default resource packs work out of the box) are installed, you can choose to have armor hider switch to the vanilla armor model once you enter combat.

### Visibility Switching
You can choose whether your armor should be drawn when you have the invisibility effect. Admins can override this server-wide to prevent fully-invisible players even if they have armor equipped.

### Presets
Armor Hider comes with five presets which you can load by clicking the corresponding button in the settings screen. Alternatively, define a keybind for 'Armor Hider - Load Preset' and afterward pressing this button and any number key between 1 and 5 anywhere in the game.

To change a preset, select it (as described above) and adjust the settings as you like. The preset automatically saves when the options screen gets closed.

### Offhand Opacity
Armor Hider additionally offers to hide the offhand slot if you don't want your fancy skin obstructed by a shield or other items.

### Accessories
If you have Trinkets/Curios/Accessories installed, you can choose whether a slider that maps to an accessory (hat -> head, necklace -> chest, belt -> leggings, ...) should be affected by Armor Hider too or not. If you're not using any of these mods, Armor Hider will suppress offering these settings.

## Synchronization
If the mod is installed on the server you're joining, all of your local preferences will get sent to the server on change or join. The server-side mod then takes care of relaying your preferences to all other clients. That way, if the mod is present on the server, everyone will see your avatar exactly as you've configured it (including combat detection, glint and other features).

Combat events are synchronized and not only client-side, so when the server runs the mod, you'll see another player's armor once they enter combat (granted they have the related setting enabled).

If a host is not yet running the mod, there's a setting available which lets you define whether the then 'unknown' players should have their armor rendering unaffected or carry over your own settings.

## Social Settings
<img width="443" height="194" alt="Social Settings" src="https://github.com/user-attachments/assets/219ff981-649e-43d6-98a6-a33577f7ae06" />
<img width="782" height="460" alt="image" src="https://github.com/user-attachments/assets/21df50da-7a1a-4e46-9d6c-e9627c82754b" />

Via a social settings button you are able to adjust how Armor Hider handles other players granularly. Three main settings define how this is handled:
- Other Players: Adjust/Vanilla sets if other players visual's get adjusted by Armor Hider at all or not (the server is able to override this)
- Unknown Players: My Config/Global lets you choose whether players that are not running the mod (or when you're connected to a server without Armor Hider) will be rendered using your own configuration on the main screen or the global configuration
- Use Global for Others: On/Off lets you choose whether - no matter what - you'd like the global config used on other players even if you're on an Armor Hider running server (while the server can still override this). 

For any other player you can define an individual configuration in addition, that will - if allowed by the server - determine how they are drawn, no matter the above.

## Advanced Settings
<p align="center">
<img alt="Armor Hider Advanced Settings" src="https://github.com/user-attachments/assets/e6758b0e-226c-4c1a-a4fc-fa9cb291d2f6" />
</p>

* **Disable Armor Hider features**: Globally disable Armor Hider on your client. If the server is forcing Armor Hider
  off, this setting is overridden
* **Settings Location**: You can choose where you want Armor Hider's settings to be displayed. Currently, Armor Hider defaults to adding a 'Zannagh's Armor Hider' button to the option screen. If you flip the advanced setting to display options in Skin Customization to 'ON', Armor Hider will inject its options into the vanilla Skin Customization screen instead

### Administrative Settings

For hosts, these settings require moderator/operator permissions (for compatibility reasons, the mod only checks for permission level >= 3) and are applied server-wide, overriding individual player preferences.

* **Armor in combat (server)**: Forces combat detection server-wide - when enabled, armor is always shown for any player in combat, overriding each player's individual combat detection setting - useful for PvP servers
* **Force Armor Hider off for all players**: Forces Armor Hider to be disabled for all players on the server (especially useful when you're running a PvP server and don't want people to hide other players' armors)
* **Armor While Invisible**: enforces that armor is rendered when a player has the invisible effect to prevent competitive advantage
* **Individual Player Configs**: Allows administrators to enable or disable whether connected clients can use the application of individual configurations (both the client-side global and per-player configs) to override how the client sees other players

## Server Communication Matrix

Your locally set preferences get sent to the server on change or join. Players joining a server will retrieve the
preference library from the server in order to apply other players' preferences on their client.

The following matrix shows how preferences are resolved depending on where the mod is installed:

| Mod on Server | Mod on Client | Behavior                                                                                                                                                                                                                                                                                                                                         |
|:---:|:---:|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Yes | Yes | Full functionality. Your preferences are synced to the server and relayed to other players. You see other players' actual configured armor opacity.                                                                                                                                                                                              |
| Yes | No | No effect. The server stores preferences but the vanilla client cannot render transparency changes.                                                                                                                                                                                                                                              |
| No | Yes | Client-side only. Other players' preferences cannot be determined. Depending on the **"individual configuration"** settings: your own opacity settings are applied to all players or a global 'other player config' is used. Optionally, all others can be just rendered as with vanilla. Combat events will not be passed around in the server. |
| No | No | No effect. The mod is not present.                                                                                                                                                                                                                                                                                                               |

## Community

Join the [Discord server](https://discord.gg/AMwbYqdmQb) for support, discussion, and feature requests.

## Issues and Feature Requests

As mentioned before, feel free to create an issue on the GitHub repository or reach out on Discord to make me aware of problems or ideas that could make this mod better.

## Versioning & Releases

All Minecraft versions are built from the `main` branch using [Stonecutter](https://stonecutter.kikugie.dev/) for multi-version support. [GitVersion](https://gitversion.net/) handles semantic versioning automatically. On CI, the version property is passed to the gradle build.

**Release flow:**

- **Prereleases** are created automatically on every push to `main` that includes code changes (commits prefixed with `ci:`, `docs:`, `build:`, or `chore:` are skipped)
- **Releases** are created manually via GitHub Releases with version validation
- All versions are published to [Modrinth](https://modrinth.com/mod/zannaghs-armor-hider) automatically on manual pre-releases or releases
- All versions are published to [CurseForge](https://www.curseforge.com/minecraft/mc-mods/armor-hider) automatically on manual pre-releases or releases

**Version format:**

- Releases: `0.7.2`
- Prereleases: `0.7.3-pre.1`, `0.7.3-pre.2`, etc.
