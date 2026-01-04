# sRandomRTP 3.0 – Portal Horizons

## Teleport system overhaul
- Rebuilt the random teleport engine with request tracking, chunk preloading, and golden-angle coordinate generation to deliver faster, safer locations even under Folia using the new `TeleportRequestManager`, `TeleportWithChunkLoading`, and `GenerateCoordinates` pipeline.
- Added a configurable chunk warming scheduler (`chunk-warming` and `chunk-loading.yml`) so servers can proactively keep spawn/player surroundings loaded and avoid lag spikes when teleporting.
- Expanded region safety features with per-player search cancellation, region-radius checks for `/rtp base`, and smarter biome/block filtering (including automatic cave biome blocking and async banned list loading with logging hooks).

## New commands and integrations
- Introduced `/rtp far` and `/rtp middle` with dedicated radius presets (`far.yml`, `middle.yml`) and bossbar-controlled cooldowns for long-range exploration.
- Added `/rtp portal <set|del|list>` for creating persistent teleport portals backed by SQLite storage, configurable shapes (circle/square), protected frames, particle effects, and optional per-portal RTP triggers.
- Integrated Chunky via `/rtp chunky <radius|stop>` so admins can pre-generate or cancel chunk fills directly through the plugin when the Chunky API is available.
- Extended `/rtp player` with optional world targeting, request expiration feedback, and richer accept/deny handling controlled by `teleport.rtp-player-messages`.
- Re-introduced `/rtp biome <biome>` (or `<biome1, biome2>`) with its own cooldown, boss bar countdown, and Folia-safe search pipeline so players can target up to two specific biomes without sacrificing the new safety checks.

## Configuration and localization updates
- Ship new configuration files (`portal.yml`, `far.yml`, `middle.yml`, `chunk-loading.yml`) alongside additional keys in `teleport.yml` such as `regionradius`, `rtp-player-messages`, Nether/End achievement gates, and redirect messaging for banned worlds.
- Language packs were renamed to the new `lang/*.yml` format, refreshed with dozens of portal/chunky strings, and now auto-reload together with Settings edits while the server is running.
- Added console filtering toggles (`Disable-Moved-Too-Quickly-Messages`) and logging improvements so administrators can opt into detailed diagnostics without spam.
- Teleport timeout settings (`teleport-timeout`) have been moved from `config.yml` to `teleport.yml` — all timeout options (enable, duration) are unified and managed only there.
- chunk-warming is now fully configured via `chunk-loading.yml` (removed from `config.yml`); all in-code references updated for consistency.
- Helper files like `README.md` and other non-yml docs are no longer auto-copied from resources, fixing plugin startup errors. These files remain as documentation inside the JAR or repository only.
- Per-world radius support for `far` and `middle` RTP commands is now fully applied, matching behavior with the standard RTP logic.

## Platform & infrastructure
- Marked the plugin as `folia-supported` and bundled FoliaLib so boss bars, timers, and portal tasks run safely across Paper and Folia cores.
- Portals, boss bars, and teleport cooldown resources now live in thread-safe `PlayerResourceMap` containers, preventing leaks on reloads and disconnects.
- Portal metadata, block compositions, and active Folia tasks are persisted in the new `Portals.db` SQLite database to survive restarts.

## Gameplay quality of life
- Players breaking blocks (or protected portal components) now cleanly cancel pending teleports and receive localized feedback instead of silent failures.
- Redirects to fallback worlds and advancement checks for Nether/End teleports keep players from bypassing progression while surfacing clear messages to both caller and target.
- Action titles, subtitles, and success messages gained the `%y%` placeholder alongside dedicated “long teleport wait” prompts for slow searches.

## Deprecations & removals
- Legacy synchronous auto-update tasks and hard-coded language file lists were replaced by Folia-aware scheduling and dynamic watchers.

## Upgrade guidance
- Because 3.0 rewrites every command, config, locale, and database schema, **delete your old `Settings/`, `lang/`, `Data/` (and `Portals.db`) files—or simply remove the entire `plugins/sRandomRTP/` folder—before starting the server** so fresh defaults can be generated without conflicts.
