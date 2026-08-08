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

## Testing

Tests fall into three tiers by *how* they run. They cannot all live in one folder - each tier is
pinned by its execution model - so know which is which:

| Tier | Where | What it is | How to run |
| --- | --- | --- | --- |
| **1 - unit** | `common/src/test/`, `paper/src/test/` | Pure-JVM JUnit over the logic classes (config/serialization, schema, channel selection). No Minecraft launched. Must live in the module whose classes it tests (classpath). | `./gradlew test` |
| **2 - in-game FCGT** | `common/src/client/.../smoke/` | `FabricClientGameTest` entrypoints that run *inside a launched client* (world build, equip, render). Not JUnit - cannot be discovered by the IDE or `gradle test`; only a live client runs them. Registered as fabric entrypoints, so they must stay in the client source set. | driven by Tier 3 (below) |
| **3 - spawning JUnit** | `smoke/src/smokeTest/` | JUnit that forks `./gradlew` to boot real clients (the boot matrix) and PaperMC/Folia servers (E2E). Drives the Tier-2 FCGT tests via `runClientGametest`. Deliberately black-box - no dependency on the mod. | `./gradlew smokeTest` |

Two commands, two Develocity timelines:

- **`./gradlew test`** (and `check`) - Tier 1 only. Fast, spawns nothing. This is what CI runs per PR.
- **`./gradlew smokeTest`** - Tier 3, which spawns clients/servers and drives Tier 2. Never pulled in
  by `test`/`check`. Filters (JVM `-D` system properties): `-Dsmoke.only=<variant>`,
  `-Dsmoke.exclude=`, `-Dsmoke.compat=all|none|k1,k2`, `-Dsmoke.phase=boot|entity-render`,
  `-Dsmoke.delay.ms=`, `-Dsmoke.paper.e2e=true`.

Each in-game FCGT scenario is also surfaced as its own IDE-runnable node by `FcgtScenarioTest` (one
`fabric-26.2` launch per scenario) - click a single scenario in the IDE without booting the whole batch.

### Coverage & build scans

Tier-1 tests are wired to [JaCoCo](https://www.jacoco.org/) (the spawning tiers fork separate JVMs,
so they are not measured). Running the unit tests emits both an XML and an HTML report:

- **IDE:** run a test class/`common`/`paper` with *Run with Coverage* for inline gutter highlighting.
- **Per-module report files:** after `./gradlew test` (or `:paper:test` / `:common:<active>:test`) open
  `paper/build/reports/jacoco/test/html/index.html` and
  `common/versions/<active>/build/reports/jacoco/test/html/index.html`.
- **One merged number:** `./gradlew aggregatedCoverage` sums the active common variant + paper into
  `build/reports/jacoco/aggregate/html/index.html` (and `jacocoAggregate.xml`). Only the *active*
  common variant is included - every stonecutter variant carries an identical class copy, so
  aggregating all of them would inflate the denominator.
- **Mixins are excluded** from all coverage (`**/mixin/**`): they only execute inside a live
  client/server (Tier 2/3), never in the JVM unit tests, so counting them would just depress the
  number. This is a temporary carve-out - they are meant to be covered later.
- **CI:** the PR build runs `aggregatedCoverage`, posts the merged coverage % as a PR comment
  (`madrapps/jacoco-report`, which shows *changed-lines* coverage), and uploads every report as a
  `coverage-*` artifact. Report dirs live under `build/` and are never committed.
- **Trend (Codecov):** the merged XML is also uploaded to [Codecov](https://about.codecov.io/), which
  tracks project coverage over time and shows the "was yy%, now xx% ▲/▼" delta vs `main`. The `main`
  baseline is produced by `coverage.yml` (runs on push to `main`); PRs upload from `build.yml`. Status
  checks are informational (see `.github/codecov.yml`), so a dip never blocks a merge.
- **Test Analytics (Codecov):** the Gradle JUnit result XML (`**/build/test-results/test/*.xml`) is
  uploaded via `codecov/test-results-action`, giving per-test run times, failure rates, and flaky-test
  detection. Uploaded with `if: !cancelled()` so a failing run still reports its failed tests.

**Build scans** publish to `scans.gradle.com` only when opted in via `ARMOR_HIDER_BUILD_SCAN_PUBLISH=true`
(gated in `settings.gradle.kts`), so a plain clone never uploads its builds. CI sets it on the runner and
the scan link appears in the GitHub Actions job summary; maintainers `export` it in their shell to publish
local builds too.

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