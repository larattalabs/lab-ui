<p align="center">
  <img src=".github/hero.jpg" alt="Architect's design library screen: tabs, filter pills, a card grid with a scrollbar, a preview pane and buttons, all in the Warm Studio kit" width="100%">
</p>

<p align="center">
  <a href="https://github.com/larattalabs/lab-ui/actions/workflows/ci.yml"><img src="https://github.com/larattalabs/lab-ui/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-B7410E" alt="MIT license"></a>
  <a href="https://www.minecraft.net"><img src="https://img.shields.io/badge/Minecraft-26.3-6B7280" alt="Minecraft 26.3"></a>
  <a href="https://fabricmc.net"><img src="https://img.shields.io/badge/mod%20loader-Fabric-6B7280" alt="Fabric"></a>
</p>

<p align="center"><sub>Architect's design library, drawn with the Warm Studio screen kit that lab-ui ships.</sub></p>

Lab UI is the shared client UI library of the Laratta Labs Minecraft mods (AgentCraft Worlds, Architect and Steward): one copy of the Warm Studio screen kit, world-space UI and crash guard instead of three drifting ports. It is a Fabric library mod for **Minecraft 26.3** and **Java 25**, moved out of [AgentCraft Worlds](https://github.com/larattalabs/agentcraft-worlds) rather than rewritten.

| | |
|---|---|
| Mod id | `lab_ui` |
| Packages | `dev.larattalabs.labui.*` |
| Maven coordinates | `dev.larattalabs:lab_ui` (GitHub Packages) |
| Resource namespace | `lab_ui` |

## Quick start

### Use it in your mod

The jar is published to GitHub Packages at `https://maven.pkg.github.com/larattalabs/lab-ui`, as `dev.larattalabs:lab_ui:<version>` (latest: `0.1.0`).

**1. Get a token.** GitHub Packages' Maven registry needs one **even for public packages**: a token with the `read:packages` scope (a classic personal access token, or your GitHub CLI login). With the GitHub CLI:

```sh
gh auth refresh -s read:packages
export GITHUB_ACTOR=$(gh api user --jq .login) GITHUB_TOKEN=$(gh auth token)
```

Or put them in `~/.gradle/gradle.properties` as `gpr.user` / `gpr.key`.

**2. Add the repository and the dependency** to your `build.gradle` (Loom 1.18 with the non-remapping `net.fabricmc.fabric-loom` plugin, the same setup as the three mods), and set `lab_ui_version=0.1.0` in `gradle.properties`:

```groovy
repositories {
	maven {
		name = "LabUi"
		url = uri("https://maven.pkg.github.com/larattalabs/lab-ui")
		credentials {
			username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).getOrElse("")
			password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).getOrElse("")
		}
		content { includeGroup "dev.larattalabs" }
	}
}

dependencies {
	// Minecraft 26.x is unobfuscated, so the non-remapping Loom has no mod* configurations:
	// use plain `implementation` (`modImplementation` does not exist there and fails the build).
	implementation "dev.larattalabs:lab_ui:${project.lab_ui_version}"
	include "dev.larattalabs:lab_ui:${project.lab_ui_version}"    // jar-in-jar: ships inside your mod jar
}
```

With `splitEnvironmentSourceSets()`, `implementation` covers both `main` and `client`. The pure classes (`dev.larattalabs.labui.ui.*`) can be used from `main`. Everything under `dev.larattalabs.labui.client.*` is client-only.

`include` nests only `lab_ui-<version>.jar`. Fabric API and the loader stay your own dependencies.

**3. Declare the range** in your `fabric.mod.json`. Then a clash between two mods that bundle different lab-ui versions becomes a clear loader error, not a `NoSuchMethodError`:

```json
"depends": { "lab_ui": ">=0.1.0 <0.2.0" }
```

**CI in a consumer repo.** Steward's CI already resolves `dev.larattalabs:architect_mc` from GitHub Packages this way: give the workflow `permissions: packages: read`, and set the env `GITHUB_ACTOR: ${{ github.actor }}` and `GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}` for the Gradle step. If a consumer run still gets 401/403 on `lab_ui`, there are two fixes. Either grant that repo access in the package settings (github.com/larattalabs/lab-ui/packages, then "Package settings", then "Manage Actions access"), or use a PAT secret with `read:packages` instead.

### Build it from source

You need git and JDK 25. On macOS, `brew install openjdk@25`; on Linux, any JDK 25 build (for example Eclipse Temurin). Gradle comes with the wrapper.

```sh
git clone https://github.com/larattalabs/lab-ui.git
cd lab-ui
export JAVA_HOME=/opt/homebrew/opt/openjdk@25   # macOS with Homebrew; on Linux, point it at your JDK 25
./gradlew build                 # compile, unit tests (TextDepth, LeaderGaps, Guard), jar in build/libs/
./gradlew publishToMavenLocal   # dev.larattalabs:lab_ui:<version> in ~/.m2
```

The first build downloads Minecraft and Fabric, which takes a few minutes. To test a consumer against an unreleased lab-ui, run `publishToMavenLocal` and add `mavenLocal()` to the consumer's repositories.

## How it works

- **Screen kit.** The "Warm Studio" GUI sprites, panels, buttons, scrollbars and style tokens (`Kit`, `Panels`, `UiStyle`, `TextUtil`, `UiBits`).
- **World-space UI.** Billboards, nine-sliced plates, text lift, speech bubbles and face displays (`WorldUi`, `SpeechBubble`, `DisplayDraw`, `ScreenStyle`).
- **Crash guard.** HUD and tick handlers whose failures are logged once and swallowed, so they never crash the game (`Guard`, `GuardedHud`).

It has no entrypoint, so nothing runs until a consumer calls it. Consumers bundle it jar-in-jar. The pure classes (`dev.larattalabs.labui.ui.*`) have unit tests; the client classes are exercised by the consuming mods.

### One copy at runtime

When several mods bundle lab-ui, Fabric loads **one** copy: the highest version bundled. So:

- **`Guard` state is shared.** Its failure counts and pending `inject`s are global across mods. Prefix every kind with your mod id (`steward.hud`, `architect_mc.tick`). The log line reads "lab_ui: the <kind> handler failed …".
- **Style data comes only from lab-ui's own files.** `Kit` and `UiStyle` read `assets/lab_ui/gui/kit.json`, `assets/lab_ui/gui/ui-style.json` and `assets/lab_ui/palette.json`. A consumer's own copies of those files are no longer read.
- **Breaking changes reach other mods too.** In 0.x a minor bump may break the API. Keep your `depends` range inside one minor version.

### Sprites and their ids

The kit sprites live in lab-ui's own namespace. They sit at `assets/lab_ui/textures/gui/sprites/kit/` and vanilla stitches them into the GUI atlas.

- **New id.** A sprite's id is now `lab_ui:kit/<name>`, e.g. `lab_ui:kit/panel_paper`.
- **Old ids.** Before, it was `agentcraft_worlds:kit/<name>`, `architect_mc:kit/<name>` or `steward_mc:kit/<name>`.
- **Reference them through `Kit`.** Use `Kit.PANEL_PAPER`, `Kit.button(primary, state)`, `Kit.dot(family, halo)`, `Kit.card(status)`, `Kit.progressFill(tint)`, `Kit.progressRing(p)`, `Kit.icon(name)` or `Kit.id(name)`.
- **Or use the id directly.** `LabUi.id("kit/<name>")` works too.

Only code that builds `<yourmod>:kit/...` ids by hand changes, along with any resource pack that overrode them. Agent portraits (`textures/gui/portrait/`) stay in AgentCraft.

## Project structure

```
src/main/java/dev/larattalabs/labui/        LabUi (ids, logger) and ui/: TextDepth, LeaderGaps, Guard (pure, unit-tested)
src/client/java/dev/larattalabs/labui/client/
  ui/                                       Kit, Panels, UiStyle, TextUtil, WorldUi, GuardedHud
  hud/                                      UiBits
  monitor/                                  DisplayDraw, ScreenStyle
  world/                                    SpeechBubble
src/main/resources/assets/lab_ui/           kit sprites, gui/kit.json, gui/ui-style.json, palette.json
src/test/java/                              JUnit tests for the pure classes
.github/workflows/                          ci.yml (build, test, jar contents), publish.yml (release tags)
```

## Migrating from AgentCraft Worlds, Architect or Steward

### Class map: from AgentCraft Worlds (`dev.agentcraft.*`)

| AgentCraft Worlds | lab-ui |
|---|---|
| `dev.agentcraft.ui.TextDepth` | `dev.larattalabs.labui.ui.TextDepth` |
| `dev.agentcraft.ui.LeaderGaps` | `dev.larattalabs.labui.ui.LeaderGaps` |
| `dev.agentcraft.ui.Guard` | `dev.larattalabs.labui.ui.Guard` |
| `dev.agentcraft.client.ui.WorldUi` | `dev.larattalabs.labui.client.ui.WorldUi` |
| `dev.agentcraft.client.ui.GuardedHud` | `dev.larattalabs.labui.client.ui.GuardedHud` |
| `dev.agentcraft.client.ui.Kit` | `dev.larattalabs.labui.client.ui.Kit` |
| `dev.agentcraft.client.ui.Panels` | `dev.larattalabs.labui.client.ui.Panels` |
| `dev.agentcraft.client.ui.UiStyle` | `dev.larattalabs.labui.client.ui.UiStyle`: no `agentOnDark` / `agentOnLight` |
| `dev.agentcraft.client.ui.TextUtil` | `dev.larattalabs.labui.client.ui.TextUtil` |
| `dev.agentcraft.client.hud.UiBits` | `dev.larattalabs.labui.client.hud.UiBits`: no agent-name or portrait helpers |
| `dev.agentcraft.client.monitor.DisplayDraw` | `dev.larattalabs.labui.client.monitor.DisplayDraw` |
| `dev.agentcraft.client.monitor.ScreenStyle` | `dev.larattalabs.labui.client.monitor.ScreenStyle`: no `name(agentId)` |
| `dev.agentcraft.client.agents.SpeechBubble` | `dev.larattalabs.labui.client.world.SpeechBubble`: `Addressees` seam |
| `AgentCraft.id(...)`, `AgentCraft.LOGGER` (as used by these classes) | `dev.larattalabs.labui.LabUi.id(...)`, `LabUi.LOGGER` |

### Class map: from Architect (`dev.larattalabs.architect.*`) and Steward (`dev.larattalabs.steward.*`)

Steward's kit is a port of Architect's, which is a port of AgentCraft's. Both match lab-ui except for the package and namespace.

| Architect / Steward | lab-ui |
|---|---|
| `<mod>.client.ui.Kit` | `dev.larattalabs.labui.client.ui.Kit` |
| `<mod>.client.ui.Panels` | `dev.larattalabs.labui.client.ui.Panels` |
| `<mod>.client.ui.UiStyle` | `dev.larattalabs.labui.client.ui.UiStyle`: identical API |
| `<mod>.client.ui.TextUtil` | `dev.larattalabs.labui.client.ui.TextUtil` |
| `<mod>.client.ui.GuardedHud` | `dev.larattalabs.labui.client.ui.GuardedHud` |
| `<mod>.client.hud.UiBits` | `dev.larattalabs.labui.client.hud.UiBits`: identical API |
| `<mod>.ui.Guard` | `dev.larattalabs.labui.ui.Guard` |

Here `<mod>` is `dev.larattalabs.architect` or `dev.larattalabs.steward`. Not in lab-ui, so keep your own: `HasParent`, `ScreenPause`, `OwnedConfirm`, `UiRules`, `ToastStack`.

### What changed in the move

Behaviour is unchanged apart from these seams.

**Namespace.** Every `agentcraft_worlds:` identifier is now `lab_ui:`. This covers the kit sprites, the internal render-pipeline names (`lab_ui:pipeline/world_ui_solid`, `lab_ui:pipeline/displays_solid`) and the dynamic white texture (`lab_ui:dynamic/displays_white`). `kit.json` is rewritten to match.

**`UiStyle` and `UiBits` take Architect's trimmed form.**
- `UiStyle` drops `agentOnDark` / `agentOnLight`, which fell back to AgentCraft's cast.
- `UiBits` drops `nameOnLight`, `nameOnDark`, `isUser`, `userName`, `agentName`, `hasPortrait`, `face` and `framedPortrait`, which read the Foreman and the cast.
- AgentCraft keeps these in a helper of its own.
- `ui-style.json` still carries the `agents.*` tokens, so `UiStyle.color("agents.<id>.text_on_dark")` works.

**`ScreenStyle.name(agentId)` is dropped** because it called `agentOnDark` / `agentOnLight`. AgentCraft's monitors pick the agent colour themselves.

**`SpeechBubble` is decoupled from AgentCraft's protocol and cast.**
- **Show calls.** `show(AgentSay, age)` is now `show(text, to, ts, age)`, and `showLate(AgentSay, age, ticksAgo)` is now `showLate(text, to, ts, age, ticksAgo)`.
- **Addressees.** "@name" resolution goes through the `SpeechBubble.Addressees` interface (`name(id)`, `color(id)`; null means the default), passed to `new SpeechBubble(addressees)`. With `Addressees.NONE` the bubble shows the id in `paper.link`.
  - "user" is still the local player's name.
  - "all", null and "" still get no prefix.
- **Visibility.** The members that used to be package-private are now public: `tick`, `clear`, `visibility`, `layout`, `stackHeight`, `submit`, `prefix` and `TEXT_LIFT`.
- **AgentCraft's resolver.** It keeps the old behaviour by returning the Foreman agent's name, and `UiStyle`-agentOnLight for cast members.

**`Guard`'s log line** starts with `lab_ui:` instead of `AgentCraft:`.

### Not in lab-ui (yet)

These are AgentCraft-shaped and have no small seam that would make them generic without changing behaviour.

| Class | Why it stays out |
|---|---|
| `PlateStack`, `Nameplate`, `PlateLayout` | Bound to `AgentRenderState`, `AgentLife` and agent roles. |
| `HudLayout`, toasts, `HudSounds` | Foreman listeners. |
| `StatusMap`, `DisplayText` | Foreman task and decision types. |
| `UiRules` | Agent targeting, lecterns and `Anchors` are mixed in with the generic Enter / two-press / `KeyRepeat` rules. Those generic rules are a 0.2.0 candidate. |

## Versioning

Semantic versioning. In 0.x a minor bump (0.1 → 0.2) may break the API; a patch never does.

The version lives in `gradle.properties` (`mod_version`).

**To release:** bump it on `main`, wait for CI to go green, then push the tag `v<mod_version>`. The Publish workflow checks that the tag matches `mod_version`, then builds and publishes `dev.larattalabs:lab_ui:<version>` (jar and sources jar) to GitHub Packages.

## License

[MIT](LICENSE) © Laratta Labs · Built by [Laratta Labs](https://larattalabs.com). Based on [AgentCraft](https://github.com/blendi-remade/agentcraft).

LICENSE carries two copyright lines: AgentCraft contributors, for upstream AgentCraft by blendi-remade, and Laratta Labs, for the changes made in AgentCraft Worlds and lab-ui. [NOTICE](NOTICE) lists which files come from upstream AgentCraft, which were written in the fork, and where the sprites and style data come from. The kit PNGs are upstream's Warm Studio art, byte-identical.

Both files ship inside the jar, so they travel with every jar-in-jar copy. Keep them when you redistribute.
