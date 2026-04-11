# sRandomRTP 3.0 – Portals, new ranges, and Folia support

New commands!​
1. Added commands:

/rtp far  
/rtp middle  
/rtp portal set <name> <radius> [circle|square]  
/rtp portal del <name>  
/rtp portal list [-p:<page>]  
/rtp chunky <radius>  
/rtp chunky stop  
/rtp player <player> [world]  
/rtp biome <biome|biome1,biome2>  

1.1. /rtp far – teleports players to a long-range zone using radii from far.yml. Permission: sRandomRTP.Command.Far  
1.2. /rtp middle – teleports to a mid-range zone using radii from middle.yml. Permission: sRandomRTP.Command.Middle  
1.3. /rtp portal <set|del|list> – builds and manages protected portals (circle/square) with particles, configurable materials, per-portal cooldowns, and SQLite persistence. Permission: sRandomRTP.Command.Portal  
1.4. /rtp chunky <radius|stop> – starts or stops Chunky-based pregeneration directly from the plugin. Permission: sRandomRTP.Command.Chunky  
1.5. /rtp player <player> [world] – can send a target to another world, honors banned-world redirects, and notifies sender/target. Permission: sRandomRTP.Command.Player  
1.6. /rtp biome <biome1,biome2> – accepts biome lists and uses dedicated banned block/biome rules for this command. Permission: sRandomRTP.Command.Biome  
1.7. /rtp accept and /rtp deny now use request timeouts and double-request protection. Permissions: sRandomRTP.Command.Accept / sRandomRTP.Command.Deny  
1.8. /rtp back and /rtp base respect the new height rules, world checks, and chunk preloading. Permissions: sRandomRTP.Command.Back / sRandomRTP.Command.Base  
1.9. Permission sRandomRtp.Cooldown.N still sets per-player cooldowns; the global bypass was renamed to sRandomRTP.Command.bypass.

New teleportation features!​
1. Separate range presets: base (teleport.yml), middle (middle.yml), and far (far.yml) with circle/square selection, absolute coordinates, and per-world overrides.  
2. Y-limits for overworld/Nether/End, automatic cave and ocean/river biome blocking, and strict world-border protection.  
3. Search timeouts (attempt/total), parallel candidate batches, and the long-teleport-wait hint keep searches from stalling.  
4. Extra cancel triggers: mouse movement, player movement, damage taken, and block breaking—each with its own cooldown flag.  
5. Advancement checks on /rtp world, flexible banned-world redirects, and optional world targeting inside /rtp player.  
6. Chunk warming and destination preloading (chunk-warming/chunk-loading) smooth out teleports on both Paper and Folia.  
7. Portals can run configured commands on entry and optionally trigger RTP into a chosen world.

Configuration and localization updates!​
1. Settings:
   - config.yml: Disable-Moved-Too-Quickly-Messages, short language codes (en, ru, ...), refreshed permission list.  
   - teleport.yml: minY/minY-nether/minY-end, block-cave-biomes, block-ocean-river-biomes, per-world radii, coordinate-generation, use-absolute-coordinates, parallel-search, teleport-timeout, break-block-cancel-rtp, and banned-world redirect.  
   - far.yml & middle.yml: dedicated radii and coordinate shapes for the new range commands.  
   - portal.yml: portal materials/particles, block protection, post-jump cooldown, and command lists on entry.  
   - chunk-loading.yml: preload radius, timeouts, limits, and the chunk-warming scheduler.  
   - near.yml: simplified radii for /rtp near.  
2. Locales moved to lang/*.yml with new keys:
   - long-teleport-wait, titleMessage-loading/subtitleMessage-loading, worldborder-error, redirect-world/rederictworldnear-error;  
   - full portal flow, block protection, Chunky messages, richer /rtp player prompts, and the %y% placeholder in teleported/title/subtitle.  
3. Turkish (tr.yml) was added and all filenames are unified.

Platform and dependencies!​
1. Plugin version 3.0, API 1.16, folia-supported flag, and mandatory LuckPerms.  
2. FoliaLib and PaperLib are shaded for safe scheduling; SQLite stores portal data.  
3. Chunky added as softdepend; WorldEdit/Vault/PlaceholderAPI stay optional.  
4. Maven: shade 3.4.1 with relocated libraries plus tcoded/codemc repositories for new artifacts.

Miscellaneous improvements!​
1. Logging is more flexible, and “moved too quickly” spam can be suppressed in console.  
2. Teleport success messages now show the Y coordinate and refreshed hints for long searches.  
3. Fixed duplicate /rtp player requests, banned-world sends, portal cleanup on delete, and graceful handling of missing WorldGuard/Vault/Chunky.  
4. Added guards against invalid radii, portal names, and empty list pages to avoid crashes or hangs.  
5. Numerous small safety and task-management optimizations across the teleport pipeline.

Notes
- The 3.0 update was developed for a long time, so some edge cases might be missed. Please report bugs or issues on the Discord server or by opening a GitHub issue.
