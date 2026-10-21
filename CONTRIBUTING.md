# Contributing

## Layout

All code is in `client/src/main/java/gg/norisk/cosmetics/unlocker/`:

| Package | What |
|---|---|
| `NoRiskCosmeticsClientMod` | Entry point: loads local data, registers events and commands, starts addons |
| `api/` | `ClientHooks` (extension points), `ClientAddon` (optional modules via `ServiceLoader`) |
| `compat/` | `CompatCheck`: finds NoRisk hooks that broke after an update |
| `cosmetics/` | Local storage of outfits, nametags, icons and the emote wheel |
| `spotify/` | Spotify nametag without the HUD, song covers |
| `mixin/<feature>/` | Mixins into NoRisk, grouped by feature: `unlock`, `emote`, `nametag`, `icon`, `spotify`, `player`, `shield` |

## Adding a mixin

1. Put it into the matching `mixin/<feature>/` package and add it to `noriskcosmetics.mixins.json` (`"<feature>.<ClassName>"`).
2. Use `@Mixin(targets = "...", remap = false)` for NoRisk classes. NoRisk is not obfuscated.
3. Fail soft: catch exceptions inside the injector and fall back to NoRisk's behaviour. Nothing may crash the game.
4. `CompatCheck` picks up the target class and the injector `method` names on its own. If code calls a NoRisk API directly (not via a mixin), register it with `CompatCheck.require("pkg.Class#method")`.

## Extension points

Mixins and core code fire `ClientHooks` (outfit/nametag changed, outfit selected, local emote, player loaded, cover shared, `/nrcu` status lines and subcommands). Addons can also replace a player's cape texture (`capeOverride`) and the held shield (`heldShield`, drawn by NoRisk's shield renderer), and add a section to the menu card (`ClientAddon.settingsCard`). An addon implements `ClientAddon`, is listed in `META-INF/services/gg.norisk.cosmetics.unlocker.api.ClientAddon` and registers listeners in `init()`. Without addons every hook is a no-op. Listener errors are logged and never reach the caller.

## Code style

- Java 25, 4 spaces, one feature per class. Comments explain *why* (NoRisk quirks), not *what*.
- Log through `Log` (`NRC-Unlocker` in `latest.log`), never `System.out`.
- Chat messages through `Chat`. Player-facing text is German.

## After a NoRisk update

1. Run `client/setup-libs.ps1` again and rebuild. Compile errors show NoRisk API changes in direct calls.
2. Start the game, join a world, run `/nrcu status`. Every entry under "Hooks kaputt" is a mixin target or API that moved.
3. Find the new name in NoRisk's jar (e.g. with a decompiler; don't commit decompiled code) and update the mixin or `CompatCheck.require` entry.

## Pull requests

- Never commit NoRisk jars or decompiled NoRisk code (`libs/` is ignored).
- Test in game on MC 26.3 with the current NoRisk version and say which features you checked.
