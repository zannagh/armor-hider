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

Two coverage streams are collected. The **Tier-1 (unit)** stream is on-the-fly JaCoCo over the unit
tests; the **Tier-3 (E2E)** stream is offline-instrumented coverage from a real FCGT client run (see
*E2E coverage* below). Running the unit tests emits both an XML and an HTML report:

- **IDE:** run a test class/`common`/`paper` with *Run with Coverage* for inline gutter highlighting.
- **Per-module report files:** after `./gradlew test` (or `:paper:test` / `:common:<active>:test`) open
  `paper/build/reports/jacoco/test/html/index.html` and
  `common/versions/<active>/build/reports/jacoco/test/html/index.html`.
- **One merged number:** `./gradlew aggregatedCoverage` sums the active common variant + paper into
  `build/reports/jacoco/aggregate/html/index.html` (and `jacocoAggregate.xml`). Only the *active*
  common variant is included - every stonecutter variant carries an identical class copy, so
  aggregating all of them would inflate the denominator.
- **Mixins are excluded** from all coverage (`**/mixin/**`). In the unit stream they never execute
  (Tier 1 launches no client). In the E2E stream they *do* execute, but Mixin transplants `@Inject`
  handler bytecode onto the vanilla **target** class, so the handler never runs on our mixin class and
  JaCoCo cannot attribute it - counting the package would only show a misleading 0%. The plain
  render/logic classes the handlers delegate to (e.g. `client/render/**`) *are* credited by E2E.
- **E2E coverage (Tier 3):** a real FCGT client can't be measured on-the-fly - Fabric's `KnotClassLoader`
  loads the mod classes through a path JaCoCo's agent never sees (an on-the-fly `.exec` contains 3500+
  library classes and *zero* `de.zannagh.armorhider` ones). So the E2E path uses **offline
  instrumentation**: `-Psmoke.coverage` makes `runClientGametest` pre-instrument the client classes in
  place (mixins skipped), run with the JaCoCo runtime on the classpath, and dump
  `build/jacoco/e2e-client.exec`; `./gradlew e2eCoverage` then reports it (over the clean backup classes)
  to `build/reports/jacoco/e2e/`. A single scenario already credits ~40-50% of the mod's lines (booting a
  client exercises most init/config/render code); the full batch covers the render pipeline, config, net
  and GUI. Needs JaCoCo >= 0.8.14 (26.x is Java 25 bytecode) - pinned via `jacoco.version` in
  `gradle.properties`. Uploaded to Codecov under the `e2e` flag by the nightly/on-demand `coverage` job in
  `smoke.yml` (offline instrumentation adds overhead, so it stays off the PR gate).
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
- **Smoke gate** (`smoke.yml`): Headless FCGT gate on PRs into `main`. Boots a real client under
  Xvfb + Mesa software GL, which only the self-hosted `fcgt` runner provides - the `detect` job checks
  whether one is online and, if not (or on a fork PR), the gate self-skips and the required
  `smoke-result` check still passes. When a runner is present it runs the full FCGT scenario batch on
  fabric-26.2 in both compat modes plus a parallel boot sweep of the other current versions (~6 min,
  under the 7-min cap). The exhaustive loader/version matrix and the `e2e` coverage upload run nightly.
  Set **`smoke-result`** (not `smoke`) as the required check in branch protection.
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