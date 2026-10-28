# Lab UI: agent guide

Lab UI is a public Fabric library mod (Minecraft 26.3, Java 25): the shared client UI of the Laratta Labs mods. It holds
the Warm Studio screen kit (`Kit`, `Panels`, `UiStyle`, `TextUtil`, `UiBits`), world-space UI (`WorldUi`,
`SpeechBubble`, `DisplayDraw`, `ScreenStyle`) and the crash guard (`Guard`, `GuardedHud`). Mod id `lab_ui`, packages
`dev.larattalabs.labui.*`, maven `dev.larattalabs:lab_ui`, resource namespace `lab_ui`. Consumers bundle it jar-in-jar.
README.md has the class map, the components (0.2.0), the consumer snippet and what changed when code moved in from AgentCraft,
Architect and Steward. CHANGELOG.md lists what each version added.

## Consumers

Notify all three before changing the API, and say which version carries the change:
- AgentCraft Worlds (`~/Developer/agentcraft`, larattalabs/agentcraft-worlds)
- Architect (`~/Developer/LarattaLabs/Minecraft/architect-mc`)
- Steward (`~/Developer/LarattaLabs/Minecraft/steward-mc`)

Fabric loads one copy of lab-ui for all of them (the highest bundled version), so a breaking change reaches every mod
installed next to the one that shipped it.

## Build, test, publish

- `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 GRADLE_USER_HOME=$PWD/.gradle-home`. In a new checkout or worktree, first
  `cp -c -R ~/Developer/LarattaLabs/Minecraft/architect-mc/.gradle-home . && rm -f .gradle-home/daemon/*/registry.bin*` (an APFS clone,
  cheap; the copied daemon registry would otherwise reach the original's live daemons; never run `./gradlew --stop`), then `./gradlew build --offline`.
- `./gradlew build` compiles, runs the unit tests (every pure class in `dev.larattalabs.labui.ui`) and builds the jar. CI (`ci.yml`) also
  checks the jar carries the classes, sprites, style files, LICENSE and NOTICE.
- `./gradlew publishToMavenLocal` for testing a consumer against an unreleased version (`mavenLocal()` in the consumer).
- Release: bump `mod_version` in `gradle.properties` on main, CI green, push tag `v<mod_version>`. `publish.yml` checks
  the tag equals `mod_version` and publishes to GitHub Packages.

## Versioning

Semver. In 0.x a minor bump may break the API, a patch never does. Consumers declare `"lab_ui": ">=0.X.0 <0.(X+1).0"`.
Prefer additive changes (new methods, new overloads) over changing signatures.

## Licence and attribution

MIT, two copyright lines (AgentCraft contributors; Laratta Labs, lab-ui changes). Code or art taken from
AgentCraft (upstream blendi-remade/agentcraft or the fork) keeps both lines: add each such file to NOTICE with its source
path and whether it is upstream-derived or fork-written (check with `git log` / `git cat-file -e upstream/main:<path>` in
the AgentCraft checkout). LICENSE and NOTICE ship inside the jar; keep that.

## Hard rules

- Never commit secrets, tokens, `.env`, logs or `.gradle-home/`.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. PR descriptions carry no
  "Generated with Claude Code" footer.
- No `rm -rf` outside build/temp dirs, no `git reset --hard`, no `git clean -fdx`, no force-push, no deleting tags or
  published packages.
- Move code, don't rewrite it: when extracting from a consumer, keep behaviour identical and list every seam in README.md.
- No Anthropic API calls and no Discord notifications from this repo or its tooling.
