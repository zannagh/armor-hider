# Armor Hider
  
[![Latest](https://img.shields.io/github/v/release/zannagh/armor-hider?logo=github&label=Latest%20Release&color=green)](https://github.com/zannagh/armor-hider/releases)
[![LatestPre](https://img.shields.io/github/v/release/zannagh/armor-hider?include_prereleases&label=Latest%20(Pre)Release&logo=github)](https://github.com/zannagh/armor-hider/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/zannaghs-armor-hider?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/zannaghs-armor-hider)
[![Curseforge Downloads](https://img.shields.io/curseforge/dt/1475841?logo=curseforge&style=flat&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/armor-hider)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/AMwbYqdmQb)

<p align="center">
A mod to alter the transparency or fully hide armor and equipment with multiplayer support, without any dependencies - supporting a wide range of game versions and available for Fabric, Quilt and NeoForge.
</p>

<p align="center">
<img alt="Armor Hider Overview" src="https://github.com/user-attachments/assets/3e0d3758-afc6-41c0-b622-8eaed2ac6916" />
</p>

### Features
- **Per-slot opacity sliders** for helmet, chestplate, leggings, boots and offhand
- **Enchantment glint control** to selectively hide the glint on any slot
- **Combat detection** let you automatically show armor when in combat
- **Full multiplayer sync** so other players see your settings when the server has the mod
- **Works client-side only** too without  server mod required
- **Live in-game preview** of your changes
- **Keybindings** to quickly toggle Armor Hider or open the settings screen
- **Admin controls** for server operators (force armor visible, server-wide combat detection)

![Demo](https://github.com/user-attachments/assets/5e799db8-3f8c-4e30-b1e5-465d100f7b06)

[![Issues](https://img.shields.io/github/issues/zannagh/armor-hider?label=Open%20Issues&logo=github)](https://github.com/zannagh/armor-hider/issues)
[![OpenIssues](https://img.shields.io/github/issues-closed/zannagh/armor-hider?label=Closed%20Issues&color=green&logo=github)](https://github.com/zannagh/armor-hider/issues)

I track issues (including feature requests) via GitHub and do my best to close out any bugs timely (plus, I get way too excited about new features myself..). If you don't have an account, feel free to join the Discord server and let me know there.

---

## Customizability
Find Armor Hider's settings integrated into the game via "Skin Customization" in the game settings (or via 'Zannagh's Armor Hider' on older game versions). When in-game, the mod settings feature a live preview of your changes.

<p align="center">
<img width="960" height="540" alt="Armor Hider Ingame Settings" src="https://github.com/user-attachments/assets/a8d15a03-bfd8-4fb1-a166-6d6118e523e2" />
</p>

### Armor Opacity
The mod allows you to define an opacity between 0% (hidden, cancelling the rendering) and 100% (the mod doesn't do anything) for each armor item slot. In addition, it's possible to specify whether skull items (skeleton/wither/... skull) should be affected by the helmet setting and if Elytras should be affected by the chestslot setting.
Furthermore, you can selectively hide the enchantment glint on any of the slots.
If you'd like to have armors visible in combat, you can choose whether combat will temporarily show your armor or not, see [Combat Detection](#combat-detection) below.

### Offhand Opacity
Armor Hider additionally offers to hide the offhand slot if you don't want your fancy skin obstructed by a shield or other items.

## Synchronization
If the mod is installed on the server you're joining, all of your local preferences will get sent to the server on change or join. The server-side mod then takes care of relaying your preferences to all other clients. That way, if the mod is present on the server, everyone will see your avatar exactly as you've configured it (including combat detection, glint and other features).

If a host is not yet running the mod, there's a setting available which lets you define whether the then 'unknown' players should have their armor rendering unaffected or carry over your own settings.

## Advanced Settings
<p align="center">
<img width="960" height="540" alt="Armor Hider Advanced Settings" src="https://github.com/user-attachments/assets/eb4cc6d7-50b2-4210-b57c-9d04fcdf15d9" />
</p>

* **Apply your settings to unknown players**: Whether to use your own opacity settings or the default settings (armor
  shown normally, as without the mod) when a player's settings cannot be determined — for example when using the mod
  only client-side on an unmodded server (see [Server communication matrix](#server-communication-matrix) below)
* **Disable Armor Hider features**: Globally disable Armor Hider on your client. If the server is forcing Armor Hider
  off, this setting is overridden
* **Disable Armor Hider for other players**: Disable Armor Hider rendering for other players' armors only. If the server is forcing Armor Hider off or the global disable is set, this setting is ignored

### Administrative Settings

For hosts, these settings require moderator/operator permissions (for compatibility reasons, the mod only checks for permission level >= 3) and are applied server-wide, overriding individual player preferences.

* **Armor in combat (server)**: Forces combat detection server-wide — when enabled, armor is always shown for any player in combat, overriding each player's individual combat detection setting - useful for PvP servers
* **Force Armor Hider off for all players**: Forces Armor Hider to be disabled for all players on the server (especially useful when you're running a PvP server and don't want people to hide other players' armors)

### Combat Detection
<details>
<summary>Combat Detection</summary>

With combat detection enabled, your armor automatically becomes visible when you take or deal damage.

**Enabled Combat Detection**
![CombatDetection](https://github.com/user-attachments/assets/00f191e3-3997-4b83-ac41-7c169aae9d52)

**Disabled Combat Detection**
![NoCombatDetection](https://github.com/user-attachments/assets/fd7af6a4-8f5d-4a42-99bb-57804bc7ecca)
</details>

## Server Communication Matrix

Your locally set preferences get sent to the server on change or join. Players joining a server will retrieve the
preference library from the server in order to apply other players' preferences on their client.

The following matrix shows how preferences are resolved depending on where the mod is installed:

| Mod on Server | Mod on Client | Behavior |
|:---:|:---:|---|
| Yes | Yes | Full functionality. Your preferences are synced to the server and relayed to other players. You see other players' actual configured armor opacity. |
| Yes | No | No effect. The server stores preferences but the vanilla client cannot render transparency changes. |
| No | Yes | Client-side only. Other players' preferences cannot be determined. Depending on the **"Apply your settings to unknown players"** setting: **ON** — your own opacity settings are applied to all players; **OFF** — default settings are used (armor rendered normally, as without the mod). |
| No | No | No effect. The mod is not present. |

## Community

Join the [Discord server](https://discord.gg/AMwbYqdmQb) for support, discussion, and feature requests.

## Issues and Feature Requests

As mentioned before, feel free to create an issue on the GitHub repository or reach out on Discord to make me aware of problems or ideas that could make this mod better.

## Supported Game Versions and Loaders
![Static Badge](https://img.shields.io/badge/Fabric-1.20%20%3E%201.20.1-blue?logo=fabric)
![Static Badge](https://img.shields.io/badge/1.21%20%3E%201.21.1-blue)
![Static Badge](https://img.shields.io/badge/1.21.4%20%3E%201.21.11-blue)
![Static Badge](https://img.shields.io/badge/26.1%20snapshot-blue)

![Static Badge](https://img.shields.io/badge/Quilt-1.20%20%3E%201.20.1-purple)
![Static Badge](https://img.shields.io/badge/1.21%20%3E%201.21.1-purple)
![Static Badge](https://img.shields.io/badge/1.21.4%20%3E%201.21.11-purple)
![Static Badge](https://img.shields.io/badge/26.1%20snapshot-purple)

![Static Badge](https://img.shields.io/badge/NeoForge-1.21.4%20%3E%201.21.11-orange)

## Versioning & Releases

All Minecraft versions are built from the `main` branch using [Stonecutter](https://stonecutter.kikugie.dev/) for multi-version support. [GitVersion](https://gitversion.net/) handles semantic versioning automatically. On CI, the version property is passed to the gradle build.

**Release flow:**

- **Prereleases** are created automatically on every push to `main` that includes code changes (commits prefixed with `ci:`, `docs:`, `build:`, or `chore:` are skipped)
- **Releases** are created manually via GitHub Releases with version validation
- All versions are published to [Modrinth](https://modrinth.com/mod/zannaghs-armor-hider) automatically
- All versions are published to [CurseForge](https://www.curseforge.com/minecraft/mc-mods/armor-hider) automatically

**Version format:**

- Releases: `0.7.2`
- Prereleases: `0.7.3-pre.1`, `0.7.3-pre.2`, etc.
