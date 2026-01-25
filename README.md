# Armor Hider

[![Build](https://github.com/zannagh/armor-hider/actions/workflows/build.yml/badge.svg)](https://github.com/zannagh/armor-hider/actions/workflows/build.yml)
[![Publish](https://github.com/zannagh/armor-hider/actions/workflows/publish.yml/badge.svg)](https://github.com/zannagh/armor-hider/actions/workflows/publish.yml)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/zannaghs-armor-hider?logo=modrinth&label=Modrinth%20Downloads)](https://modrinth.com/mod/zannaghs-armor-hider)
[![Modrinth Version](https://img.shields.io/modrinth/v/zannaghs-armor-hider?logo=modrinth&label=Latest%20Version)](https://modrinth.com/mod/zannaghs-armor-hider/versions)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/8MMBjwXF)

A small server/client side mod to alter transparency of armor items (relayed to other players via server).

<p align="center">
<img width="359" height="400" alt="IngamePreview" src="https://github.com/user-attachments/assets/81731706-ef87-43e7-8dc3-bf7ca1f0e91a" />
</p>

This mod is heavily inspired by Show Me Your Skin! (https://github.com/enjarai/show-me-your-skin).

The settings are accessible via "Skin Customization" in game:
<p align="center">
<img width="495" height="400" alt="IngameSettings" src="https://github.com/user-attachments/assets/d279bf39-9e15-4350-b624-c21946295ddc" />
</p>

Your locally set preferences (on your client) will get sent to the server on change or join and vice versa and player
joining a server will retrieve the config library from the server in order to apply the player preferences to other
clients.

## Settings

* **Helmet**: Opacity slider (0-100%) for head slot, also applies to block like head items (e.g. skeleton block) if
  enabled via
* **Affect Skulls**: Enable or disable that skulls are affected by the helmet setting
* **Chestplate**: Opacity slider (0-100%) for chest slot, also applies to Elytra if enabled via
* **Affect Elytra**: Enable or disable that Elytra are affected by the chestplate setting
* **Leggings**: Opacity slider (0-100%) for legs slot
* **Boots**: Opacity slider (0-100%) for boot slot
* **Combat Detection**: Show your armor when you enter combat with another player or a mob
* **Armor in combat (server)**: Enable or disable that the combat detection is respected by an individual's player
  setting - when enabled, the client's setting is overridden and armor is always shown in combat, when disabled,
  individual client's settings will be used

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

Join the [Discord server](https://discord.gg/8MMBjwXF) for support, discussion, and feature requests.

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
- All versions are published to [Modrinth](https://modrinth.com/mod/zannaghs-armor-hider) with the format
  `fabric-[MC_VERSION]-[MOD_VERSION]`

**Version format:**

- Releases: `0.7.2`
- Prereleases: `0.7.3-pre.1`, `0.7.3-pre.2`, etc.