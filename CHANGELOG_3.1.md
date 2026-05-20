# sRandomRTP 3.1 - Stability, diagnostics, and Java 8 support. Update #11

Search tags: rtp, ртп, plugin, плагин, teleport, телепорт, random, рандом, randomtp, randomrtp, рандом тп, world

New Commands Added!

1. The following admin/support commands have been added:

/rtp settings
/rtp doctor
/rtp dump
/rtp stats
/rtp portal check
/rtp tpsbar
/rtp rambar
/rtp msptbar
/rtp allbars

1.1. /rtp settings - opens a clickable in-game menu for enabling or disabling plugin subcommands.
Permission: sRandomRTP.Command.Settings
Default: disabled in Settings/commands.yml

1.2. /rtp doctor - shows the plugin health status: Java version, server version, Paper/Folia, language, optional plugins, config versions, active RTP searches, portal tasks, and admin bossbar status.
Permission: sRandomRTP.Command.Doctor
Default: disabled in Settings/commands.yml

1.3. /rtp dump - creates a support zip in plugins/sRandomRTP/Diagnostics with diagnostics, recent errors, and runtime information.
Permission: sRandomRTP.Command.Dump
Default: disabled in Settings/commands.yml

1.4. /rtp stats - shows live runtime statistics: active searches, total RTP count, cooldowns, portal tasks, completed/cancelled/refunded teleports, and average search timings.
Permission: sRandomRTP.Command.Stats
Default: disabled in Settings/commands.yml

1.5. /rtp portal check - checks portals for missing worlds, duplicate world + portal names, and active portal tasks.
Permission: sRandomRTP.Command.Portal.Check
Default: disabled in Settings/commands.yml

1.6. /rtp tpsbar, /rtp rambar, /rtp msptbar, /rtp allbars - optional admin bossbars for TPS, RAM, MSPT, or all bars at once.
Permissions: sRandomRTP.Command.TpsBar, sRandomRTP.Command.RamBar, sRandomRTP.Command.MsptBar, sRandomRTP.Command.AllBars
Default: disabled in Settings/commands.yml


New Stability Features!

1. Chunk warming, portal particles/triggers, admin bossbars, and update-check callbacks now avoid unsafe async Bukkit API access.

2. RTP counting is now saved in batches instead of writing Data/rtpCount.yml after every teleport.

3. Cooldown-bypass teleports are counted correctly.

4. Cooldown permissions such as sRandomRtp.Cooldown.4 now work more consistently and also affect the bossbar countdown.

5. Reload now sends a successful reload message after the plugin finishes reloading.

6. /rtp cancel, movement cancellation, duplicate RTP requests, player quit during teleport, and refund handling were made safer.

7. The plugin remains Java 8 bytecode compatible and keeps the minimum Bukkit API marker at 1.16, so Minecraft 1.16+ servers can still load it while newer servers keep working.


Configuration & Localization Update!

config.yml: added diagnostic, Command-Aliases-Enabled, Command-Aliases, metrics.rtp.slow-request-threshold-ms, and updated permission comments.

Settings/commands.yml: added runtime switches for /rtp settings, /rtp doctor, /rtp dump, /rtp stats, /rtp portal check, and admin bossbar commands. These debug/admin commands are disabled by default.

Settings/admin-bars.yml: added configurable TPS, RAM, and MSPT bossbars with titles, colors, styles, thresholds, and messages.

Settings/biome.yml: added a dedicated profile for /rtp biome search behavior.

Settings/teleport.yml: added safer search and chunk options, including parallel-search and prefer-generated-chunks settings.

plugin.yml: permissions were expanded for the new commands, and static aliases were removed from plugin.yml.

Command aliases were moved to config.yml:

Command-Aliases-Enabled: false
Command-Aliases:
  - randomtp
  - randomteleport

Aliases are disabled by default. Set Command-Aliases-Enabled to true if you want /randomtp and /randomteleport to work.

Localizations in lang/*.yml were extended for:

/rtp settings, /rtp doctor, /rtp dump, /rtp stats, /rtp portal check, admin bossbars, invalid-command, reload success messages, and updated help lines.


Small Changes!

1. LuckPerms is no longer required as a hard dependency; optional integrations now fail more gracefully.

2. The unknown-command message is now configurable through localization instead of always using the server default.

3. Config updates now create backups and write Diagnostics/config-changes.txt.

4. Startup, reload, config-change, backup, and slow RTP diagnostics now run only when diagnostic: true is enabled.

5. Portal storage, portal cleanup, portal particles, and portal cooldown handling were improved.

6. PlaceholderAPI values and public RTP/portal events were added for integrations.

7. Automated tests were added and moved into tests/java, including checks for Java 8 bytecode, config versions, permissions, migrations, cooldowns, portals, biome RTP, and scheduler safety.

8. .gitignore was updated so IDE files, local agent files, runtime databases, logs, and build artifacts do not get into the repository.

9. Auto-reload now applies config.yml language/diagnostic/alias changes and Settings/commands.yml command switches after saving the file.


Notes

Update 3.1 is mainly a stability, diagnostics, and compatibility update after the large 3.0 release. Most new admin/support commands are disabled by default, so normal players will keep using the plugin like before unless you enable those tools in Settings/commands.yml.

Please report bugs on the Discord server or create a GitHub issue.
