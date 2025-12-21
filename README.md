# Armor Hider

A small server/client side mod to alter transparency of armor items (relayed to other players via server).

This mod is heavily inspired by Show Me Your Skin! (https://github.com/enjarai/show-me-your-skin). 
It's less feature complete but easier to raise to new Minecraft releases due to no-dependencies.

The settings are accessible via "Skin Customization" in game: 

<img width="516" height="441" alt="Screenshot-2025-11-13-16 12 13" src="https://github.com/user-attachments/assets/bc7fea45-1fe2-41c7-8151-0aa26dcdd130" />

Transparency percentages get applied to armor textures and glints: 

<img width="2648" height="1606" alt="acb0de564254ce67035bec2bb3b6098b693cf329" src="https://github.com/user-attachments/assets/a5fe22bb-ee83-47e3-8ef7-16e6a7d053ec" />

Your locally set preferences (on your client) will get sent to the server on change or join and vice versa and player joining a server will retrieve the config library from the server in order to apply the player preferences to other clients.

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