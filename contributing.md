# Contributing

Feel free to fork and PR or create a branch within the repo and create a PR into main.

The main branch is protected against direct pushes - any changes should be PR'd.

Since this project is under MIT, feel free to take the code and do as you please with it as long as you're referencing
this repository and the authors.

## Multi-Version Development

This project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build for multiple Minecraft versions from a
single codebase. Version-specific code uses Stonecutter's conditional syntax:

```java
//? if >= 1.21.9
useNewApi();
//? if < 1.21.9
/*useOldApi();*/
```

All versions are built from the `main` branch - there are no separate version branches.

## CI/CD

- **Build workflow** (`build.yml`): Runs on pull requests to validate compilation and tests
- **Publish workflow** (`publish.yml`): Runs on pushes to `main` and manual releases
  - Automatic prereleases on `main` pushes (skips `ci:`/`docs:`/`build:`/`chore:` commits)
  - Manual releases via GitHub Releases with version validation
  - Publishes to [Modrinth](https://modrinth.com/mod/zannaghs-armor-hider) for all supported Minecraft versions

## Versioning

[GitVersion](https://gitversion.net/) handles semantic versioning automatically (see `GitVersion.yml`).

- Prereleases use the format `x.x.x-pre.N`
- Version bumps are controlled via commit messages: `+semver: major`, `+semver: minor`, `+semver: patch`
- Commits prefixed with `ci:`, `docs:`, `build:`, or `chore:` do not trigger releases

## Community

Join the [Discord server](https://discord.gg/AMwbYqdmQb) for discussion and support.