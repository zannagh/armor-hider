# Armor Hider

#### GitHub
[![Build](https://github.com/zannagh/armor-hider/actions/workflows/build.yml/badge.svg)](https://github.com/zannagh/armor-hider/actions/workflows/build.yml)
[![Publish](https://github.com/zannagh/armor-hider/actions/workflows/publish.yml/badge.svg)](https://github.com/zannagh/armor-hider/actions/workflows/publish.yml)
[![Latest](https://img.shields.io/github/v/release/zannagh/armor-hider?logo=github&label=Latest%20Release&color=lime
)](https://github.com/zannagh/armor-hider/releases)

#### Platforms
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/zannaghs-armor-hider?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/zannaghs-armor-hider)
[![Curseforge Downloads](https://img.shields.io/curseforge/dt/1475841?logo=curseforge&style=flat&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/armor-hider)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/AMwbYqdmQb)

#### Game Versions
![Static Badge](https://img.shields.io/badge/Fabric-1.20%20%3E%201.20.1-blue?logo=fabric)
![Static Badge](https://img.shields.io/badge/1.21%20%3E%201.21.1-blue)
![Static Badge](https://img.shields.io/badge/1.21.4%20%3E%201.21.11-blue)
![Static Badge](https://img.shields.io/badge/26.1%20snapshot-blue)
![Static Badge](https://img.shields.io/badge/Quilt-1.20%20%3E%201.20.1-purple)
![Static Badge](https://img.shields.io/badge/1.21%20%3E%201.21.1-purple)
![Static Badge](https://img.shields.io/badge/1.21.4%20%3E%201.21.11-purple)
![Static Badge](https://img.shields.io/badge/26.1%20snapshot-purple)
![Static Badge](https://img.shields.io/badge/NeoForge-1.20%20%3E%201.20.1-orange)
![Static Badge](https://img.shields.io/badge/1.21%20%3E%201.21.1-orange)
![Static Badge](https://img.shields.io/badge/1.21.4%20%3E%201.21.11-orange)


A small server/client side mod to alter transparency of armor items (relayed to other players via server).

<p align="center">
<img width="359" height="400" alt="IngamePreview" src="https://github.com/user-attachments/assets/81731706-ef87-43e7-8dc3-bf7ca1f0e91a" />
</p>

This mod is heavily inspired by Show Me Your Skin! (https://github.com/enjarai/show-me-your-skin).

The settings are accessible via "Skin Customization" in game (or via 'Zannagh's Armor Hider' in game options, depending on game version and available APIs):
<p align="center">
<img width="495" height="400" alt="IngameSettings" src="https://github.com/user-attachments/assets/d279bf39-9e15-4350-b624-c21946295ddc" />
</p>

Your locally set preferences (on your client) will get sent to the server on change or join and vice versa and player
joining a server will retrieve the config library from the server in order to apply the player preferences to other
clients.

## Settings

### Armor Opacity

* **Helmet**: Opacity slider (0-100%) for head slot
* **Affect Skulls**: When enabled, helmet opacity changes also affect skulls (non-armor head items like skeleton or
  creeper skulls)
* **Chestplate**: Opacity slider (0-100%) for chest slot
* **Affect Elytra**: When enabled, chestplate opacity also affects elytra
* **Leggings**: Opacity slider (0-100%) for legs slot
* **Boots**: Opacity slider (0-100%) for boot slot
* **Offhand**: Opacity slider (0-100%) for offhand slot

### Other Settings

* **Combat Detection**: Enables detection of combat to show your armor when you are in combat with a fade-off effect
* **Apply your settings to unknown players**: Whether to use your own opacity settings or the default settings (armor
  shown normally, as without the mod) when a player's settings cannot be determined — for example when using the mod
  only client-side on an unmodded server (see [Preferences Matrix](#preferences-matrix) below)
* **Disable Armor Hider features**: Globally disable Armor Hider on your client. If the server is forcing Armor Hider
  off, this setting is overridden
* **Disable Armor Hider for other players**: Disable Armor Hider rendering for other players' armors only. If the
  server is forcing Armor Hider off or the global disable is set, this setting is ignored

### Administrative Settings

These settings require moderator/operator permissions (due to compatibility reasons, the mod only checks for permission level >= 3) and are applied server-wide, overriding individual player preferences.

* **Armor in combat (server)**: Forces combat detection server-wide — when enabled, armor is always shown for any
  player in combat, overriding each player's individual combat detection setting
* **Force Armor Hider off for all players**: Forces Armor Hider to be disabled for all players on the server (useful when you're running a PvP server and don't want people to hide their armor or other's armors from their perspective)

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

## Demo

<details>
<summary>Settings</summary>

**Main Menu Settings**
![MainMenuSettings](https://github.com/user-attachments/assets/f20d0847-ea5e-4107-a87c-bc7b620233cd)

**Ingame Settings (Admin)**
![IngameSettings](https://github.com/user-attachments/assets/91ef2bba-84a7-4888-b823-af048e31b89a)

</details>
<details>
<summary>Combat Detection</summary>

**Enabled Combat Detection**
![CombatDetection](https://github.com/user-attachments/assets/00f191e3-3997-4b83-ac41-7c169aae9d52)

**Disabled Combat Detection**
![NoCombatDetection](https://github.com/user-attachments/assets/fd7af6a4-8f5d-4a42-99bb-57804bc7ecca)
</details>

## Community

Join the [Discord server](https://discord.gg/AMwbYqdmQb) for support, discussion, and feature requests.

## Issues and Feature Requests

Feel free to create an issue on the GitHub repository or reach out on Discord to make me aware of problems or ideas
that could make this mod better.

## Versioning & Releases

All Minecraft versions are built from the `main` branch using [Stonecutter](https://stonecutter.kikugie.dev/) for
multi-version support. [GitVersion](https://gitversion.net/) handles semantic versioning automatically.

**Release flow:**

- **Prereleases** are created automatically on every push to `main` that includes code changes (commits prefixed
  with `ci:`, `docs:`, `build:`, or `chore:` are skipped)
- **Releases** are created manually via GitHub Releases with version validation
- All versions are published to [Modrinth](https://modrinth.com/mod/zannaghs-armor-hider) automatically
- All versions are published to [Curseforge](https://www.curseforge.com/minecraft/mc-mods/armor-hider) automatically

**Version format:**

- Releases: `0.7.2`
- Prereleases: `0.7.3-pre.1`, `0.7.3-pre.2`, etc.