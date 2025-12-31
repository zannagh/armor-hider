# Armor Hider

A small server/client side mod to alter transparency of armor items (relayed to other players via server).

<p align="center">
<img width="359" height="400" alt="IngamePreview" src="https://github.com/user-attachments/assets/81731706-ef87-43e7-8dc3-bf7ca1f0e91a" />
</p>

This mod is heavily inspired by Show Me Your Skin! (https://github.com/enjarai/show-me-your-skin).

The settings are accessible via "Skin Customization" in game: 
<p align="center">
<img width="495" height="400" alt="IngameSettings" src="https://github.com/user-attachments/assets/d279bf39-9e15-4350-b624-c21946295ddc" />
</p>

Your locally set preferences (on your client) will get sent to the server on change or join and vice versa and player joining a server will retrieve the config library from the server in order to apply the player preferences to other clients.

## Settings
* **Helmet**: Opacity slider (0-100%) for head slot, also applies to block like head items (e.g. skeleton block) or hats
* **Chestplate**: Opacity slider (0-100%) for chest slot, also applies to Elytra
* **Leggings**: Opacity slider (0-100%) for legs slot
* **Boots**: Opacity slider (0-100%) for boot slot
* **Combat Detection**: Show your armor when you enter combat with another player or a mob
* **Armor in combat (server)**: Enable or disable that the combat detection is respected by an individual's player setting - when enabled, the client's setting is overridden and armor is always shown in combat, when disabled, individual client's settings will be used

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

## Issues and Feature Requests

Feel free to create an issue on the GitHub repository to make me aware of problems or ideas that could make this mod better.

## Versioning

CI runs are using GitVersion to versionize (see GitVersion.yml), whereas local builds will only use git tag information (via `git describe --tags`) to versionize.

GitVersion configuration:
- no labels on branches
- no version bump on tagged commits (when creating release in GitHub)
- ignore changes to GitVersion.yml or documentational .md files
- ignore changes to GitHub workflows
- ignore version numbers in branches (to not get Minecraft targets versions into artifacts version)
- no version bump on branch creation
- tag prefix regex to match Minecraft target version plus mod version (i.e. v1.21.11-0.1.0 will be matched to 0.1.0)
