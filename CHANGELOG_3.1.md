# sRandomRTP 3.1 - Stability, admin debug tools, and safer RTP internals

Admin Debug Tools!

1. The following admin monitoring commands are debug tools and are disabled in public installs by default through Settings/commands.yml:

/rtp tpsbar
/rtp rambar
/rtp msptbar
/rtp allbars

1.1. /rtp tpsbar - shows or hides an admin bossbar with the current server TPS.
Permission: sRandomRTP.Command.TpsBar

1.2. /rtp rambar - shows or hides an admin bossbar with server RAM usage.
Permission: sRandomRTP.Command.RamBar

1.3. /rtp msptbar - shows or hides an admin bossbar with the current MSPT value.
Permission: sRandomRTP.Command.MsptBar

1.4. /rtp allbars - enables or disables all admin monitoring bossbars at once.
Permission: sRandomRTP.Command.AllBars

1.5. Public servers do not see these commands in tab completion or /rtp help until they are enabled in Settings/commands.yml or through /rtp settings.


Admin Monitoring Features!

1. Added configurable TPS, RAM, and MSPT bossbars for server monitoring.

2. Each bar can have its own title, color, style, update format, thresholds, and enabled/disabled state in admin-bars.yml.

3. Commands and tab completion now respect permissions, config state, and server metric support.

4. Admin bars are cleaned up correctly when players leave, the plugin reloads, or the plugin disables.


Teleport & Search Improvements!

1. The RTP flow was rebuilt around shared handlers for the default, near, far, middle, biome, base, world, player, and portal teleport paths.

2. Added a request lifecycle manager that tracks active teleports, cancels replaced/offline/expired requests, and prevents duplicate final teleports.

3. Parallel coordinate search is now adaptive: the plugin can limit global in-flight searches and back off when chunk, TPS, or MSPT pressure is too high.

4. Added an optional "prefer already generated chunks first" phase before allowing new chunk generation.

5. Chunk acquisition and preloading were improved with safer Paper/Folia scheduling, urgent PaperLib requests, capped neighbor preloading, and chunk-pressure telemetry.

6. Safe location checks were rewritten around loaded chunks, biome/block snapshots, Nether/End resolvers, hazard checks, deep-water checks, leaves, slopes, and cancellation-aware searching.

7. /rtp biome now has its own biome.yml profile, accepts biome names through spaces or commas, normalizes case/hyphens/spaces, and rejects blocked biomes earlier.

8. Recent teleport locations are tracked so the plugin can avoid sending players back to the same area too often.

9. Cooldown permissions were fixed: sRandomRtp.Cooldown.N now works consistently and also controls the bossbar countdown duration.


Configuration & Localization Update!

1. New config files were added:

admin-bars.yml - TPS/RAM/MSPT bossbar settings. The commands are disabled by default in Settings/commands.yml.
biome.yml - dedicated radius and search settings for /rtp biome, including per-world options and two-phase search behavior.

2. Config files now use managed config-version values, default-key synchronization, and startup migrations.

3. config.yml gained metrics.rtp.slow-request-threshold-ms for slow RTP diagnostics.

4. teleport.yml gained parallel-search.max-global-inflight and prefer-generated-chunks settings.

5. Auto-reload now watches the main config, Settings, and lang files, validates YAML before applying changes, and reloads language files only when needed.

6. The unknown-command chat message is now configurable through the invalid-command localization key.

7. English and Russian localizations were expanded with admin bar messages, invalid command text, and updated command help. Public help output hides debug admin commands while their command switches are disabled.


Architecture & Diagnostics!

1. The plugin now uses a central PluginContext with service-based access to shared systems.

2. Config loading was centralized through ConfigRegistry and immutable ConfigCache snapshots.

3. Runtime global state was moved into RuntimeStateRegistry to make reloads, shutdown, and cleanup safer.

4. Release checking is handled by ReleaseCheckService with caching, in-flight request de-duplication, and stronger outdated-version warnings.

5. DiagnosticsService now reports startup and reload diagnostics, while TeleportMetrics can log slow RTP requests and teleport summaries.

6. Portal logic was split into dedicated repository, state store, particle, block placement, trigger, and cooldown classes.

7. Portal SQL storage now has cleaner async execution, indices, schema versioning, and migrations for portal shape data.


Small Changes & Fixes!

1. LuckPerms is no longer a hard plugin dependency; optional integrations are handled more gracefully.

2. The new bypass permission is sRandomRTP.Command.bypass, while the legacy sRandomRTP.Cooldown.bypass still works for compatibility.

3. Fixed configurable invalid command messages instead of always using the default server text.

4. Improved cleanup for portal blocks, portal particles, admin bars, pending teleports, and economy charges.

5. Fixed edge cases with duplicate RTP requests, player quit during teleport, expired requests, and refunds after cancelled teleports.

6. Permission checks, command messages, banned-world logic, and teleport startup checks were centralized to reduce duplicated behavior.

7. Plugin metadata was updated for the 3.1 release. The plugin is built for Java 8 bytecode and keeps the minimum Bukkit API marker at 1.16, so older 1.16 servers can still load it while newer servers continue to work.

8. Java target stays compatible with Java 8, several dependencies were refreshed, and unused build dependencies were removed. Features that depend on newer server APIs now use fallbacks or stay unavailable on older cores.

9. If your server is still running Java 8, sRandomRTP 3.1 should still be able to load. For very old server setups, 3.0 or 2.9/2.9-FIX may be safer fallback versions.

10. Automated tests were added and moved into tests/java, with coverage for cooldowns, configs, migrations, admin bars, portals, biome RTP, chunk pressure, safe-Y checks, and scheduler guards.

11. .gitignore was updated to keep IDE files, local agent files, runtime databases, logs, and build artifacts out of the repository.


Additional Stability Polish!

1. Added /rtp doctor, /rtp dump, /rtp stats, and /rtp portal check to make support and server diagnostics easier.

2. Chunk warming, portal particles/triggers, admin bars, and release-check callbacks now avoid unsafe async Bukkit API access.

3. RTP usage counting is now saved in batches instead of writing Data/rtpCount.yml after every teleport, and cooldown-bypass teleports are counted correctly.

4. Config updates now create backups and write Diagnostics/config-changes.txt, while startup/reload diagnostics are written even when normal logs are disabled.

5. Added PlaceholderAPI values and public RTP/portal events for integrations, plus extra tests for config versions, plugin.yml permissions, scheduler safety, and Java 8 bytecode.

6. Added Settings/commands.yml and /rtp settings, a clickable in-game menu for enabling or disabling RTP subcommands without removing their permission checks.

7. Debug/support commands now have clearer switches: /rtp doctor, /rtp dump, /rtp stats, /rtp portal check, and the admin bossbar commands can be toggled from the same debug section.

8. /rtp portal check is treated as a debug/support command and now has its own permission node: sRandomRTP.Command.Portal.Check.

9. New debug/settings command output is now localized through lang/*.yml, including /rtp doctor, /rtp dump, /rtp stats, /rtp settings, /rtp portal check, and the admin bossbar help lines.

10. Admin bossbar commands remain in the plugin, but public builds keep them disabled by default through Settings/commands.yml; /rtp settings can toggle /rtp tpsbar, /rtp rambar, /rtp msptbar, and /rtp allbars visibility after the settings command is enabled.

11. /randomtp and /randomteleport were moved out of plugin.yml into config.yml → Command-Aliases, with Command-Aliases-Enabled as a quick true/false switch.


Notes

Update 3.1 is mainly a stability, diagnostics, and internal cleanup update after the large 3.0 release. A lot of teleport, portal, cooldown, and config internals changed, so please report any missed edge cases on the Discord server or by creating a GitHub issue.
