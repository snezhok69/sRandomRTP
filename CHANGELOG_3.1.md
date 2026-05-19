# sRandomRTP 3.1 - Stability, safer RTP internals, and Java 8 compatibility

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

1. New biome.yml config was added for /rtp biome, including dedicated radii, per-world options, and two-phase search behavior.

2. Config files now use managed config-version values, default-key synchronization, and startup migrations.

3. config.yml gained metrics.rtp.slow-request-threshold-ms for slow RTP diagnostics.

4. teleport.yml gained parallel-search.max-global-inflight and prefer-generated-chunks settings.

5. Auto-reload now watches the main config, Settings, and lang files, validates YAML before applying changes, and reloads language files only when needed.

6. The unknown-command chat message is now configurable through the invalid-command localization key.

7. English and Russian localizations were expanded with invalid command text and updated RTP command help.


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

4. Improved cleanup for portal blocks, portal particles, pending teleports, and economy charges.

5. Fixed edge cases with duplicate RTP requests, player quit during teleport, expired requests, and refunds after cancelled teleports.

6. Permission checks, command messages, banned-world logic, and teleport startup checks were centralized to reduce duplicated behavior.

7. Plugin metadata was updated for the 3.1 release. The plugin is built for Java 8 bytecode and keeps the minimum Bukkit API marker at 1.16, so older 1.16 servers can still load it while newer servers continue to work.

8. Features that depend on newer server APIs now use fallbacks or stay unavailable on older cores.

9. Admin monitoring bossbars were removed from the public RTP command set so the plugin stays focused on random teleportation.

10. Automated tests were added and moved into tests/java, with coverage for cooldowns, configs, migrations, portals, biome RTP, chunk pressure, safe-Y checks, and scheduler guards.

11. .gitignore was updated to keep IDE files, local agent files, runtime databases, logs, and build artifacts out of the repository.


Notes

Update 3.1 is mainly a stability, diagnostics, and internal cleanup update after the large 3.0 release. A lot of teleport, portal, cooldown, and config internals changed, so please report any missed edge cases on the Discord server or by creating a GitHub issue.
