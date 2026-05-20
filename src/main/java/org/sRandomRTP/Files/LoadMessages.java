package org.sRandomRTP.Files;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Collections;
import java.util.List;

public class LoadMessages {
    public static volatile List<String> nopermissionreload;
    public static volatile List<String> reloadingwait;
    public static volatile List<String> reloadingstart;
    public static volatile List<String> successfullyreload;
    public static volatile List<String> nopermissioncommand;
    public static volatile List<String> invalidcommand = Collections.singletonList("&a[sRandomRTP] &cInvalid command!");
    public static volatile List<String> messagescooldownMessage;
    public static volatile List<String> teleportdamagedcancel;
    public static volatile List<String> teleportmovecancel;
    public static volatile List<String> teleportcancel;
    public static volatile List<String> teleportyes;
    public static volatile List<String> loading;
    public static volatile List<String> locationNotFound;
    public static volatile List<String> newVersionMessage;
    public static volatile List<String> CheckingVersion;
    public static volatile List<String> LatestVersionMessage;
    public static volatile List<String> ErrorCheckingVersionMessage;
    public static volatile List<String> teleportationinprogress;
    public static volatile String bossbar;
    public static volatile List<String> nosearching;
    public static volatile String titleMessage;
    public static volatile String subtitleMessage;
    public static volatile List<String> noplayerworldnear;
    public static volatile List<String> noplayerservenear;
    public static volatile List<String> commandhelp;
    public static volatile String actionBarMessage;
    public static volatile List<String> insufficient_funds;
    public static volatile List<String> error_withdrawing;
    public static volatile List<String> insufficient_hunger;
    public static volatile List<String> insufficient_levels;
    public static volatile List<String> banned_world;
    public static volatile List<String> player_nearby;
    public static volatile List<String> insufficient_health;
    public static volatile List<String> insufficient_items;
    public static volatile List<String> noadvancementnether;
    public static volatile List<String> noadvancementend;
    public static volatile List<String> PluginEnabledMessage;
    public static volatile List<String> PluginDisabledMessage;
    public static volatile List<String> teleportBackFailure;
    public static volatile List<String> teleportBackSuccess;
    public static volatile List<String> regionsempty;
    public static volatile List<String> regionManager;
    public static volatile List<String> bannedbiome;
    public static volatile List<String> rtpplayerteleportsuccesssender;
    public static volatile List<String> rtpplayerteleportsuccesstarget;
    public static volatile List<String> rtpplayerrequestinitiator;
    public static volatile List<String> rtpplayertimeout;
    public static volatile List<String> rtpplayercanceled;
    public static volatile List<String> rtpplayersendernotified;
    public static volatile List<String> rtpplayernoactiveteleport;
    public static volatile List<String> rtpplayeralreadyrequested;
    public static volatile List<String> rtpplayerteleportrequestsent;
    public static volatile List<String> error_radius;
    public static volatile List<String> error_teleport_yourself;
    public static volatile List<String> no_active_requests_accept;
    public static volatile List<String> error_radius_portal;
    public static volatile List<String> error_portal_name;
    public static volatile List<String> error_portal_name_characters;
    public static volatile List<String> error_portal_name_already_exists;
    public static volatile List<String> error_insufficient_space;
    public static volatile List<String> success_portal_created;
    public static volatile List<String> error_cooldown_wait;
    public static volatile List<String> error_page_number_invalid;
    public static volatile List<String> error_command_format;
    public static volatile List<String> error_no_portals;
    public static volatile List<String> error_page_not_found;
    public static volatile List<String> error_portal_not_found;
    public static volatile List<String> error_world_not_found;
    public static volatile List<String> portal_list_header;
    public static volatile String portal_delete_button;
    public static volatile String portal_delete_hover;
    public static volatile String portal_tp_button;
    public static volatile String portal_tp_hover;
    public static volatile String portal_coordinates;
    public static volatile String portal_prev_button;
    public static volatile String portal_prev_hover;
    public static volatile String portal_prev_disabled;
    public static volatile String portal_next_button;
    public static volatile String portal_next_hover;
    public static volatile String portal_next_disabled;
    public static volatile String portal_space;
    public static volatile List<String> portal_teleport_success;
    public static volatile List<String> portal_delete_success;
    public static volatile List<String> diagnostics_doctor_lines = Collections.emptyList();
    public static volatile List<String> diagnostics_stats_lines = Collections.emptyList();
    public static volatile List<String> diagnostics_dump_created = Collections.emptyList();
    public static volatile List<String> diagnostics_dump_failed = Collections.emptyList();
    public static volatile List<String> diagnostics_portal_check = Collections.emptyList();
    public static volatile String settings_header = "&a[sRandomRTP] &6Settings &7(%page%/%max_page%)";
    public static volatile String settings_description = "&7Debug commands and admin bossbars are controlled here. Permissions still apply.";
    public static volatile String settings_category = "&8- &e%category%";
    public static volatile String settings_category_player = "Player";
    public static volatile String settings_category_admin = "Admin";
    public static volatile String settings_category_debug = "Debug";
    public static volatile String settings_flag_line = "  &7%command% &8(%id%) &7perm: &f%permission% ";
    public static volatile String settings_status_on = "&a[ON]";
    public static volatile String settings_status_off = "&c[OFF]";
    public static volatile String settings_status_locked = "&8[locked]";
    public static volatile String settings_toggle_hover = "&7Toggle %command%";
    public static volatile String settings_prev_button = "&a[< Previous]";
    public static volatile String settings_prev_disabled = "&8[< Previous]";
    public static volatile String settings_prev_hover = "&7Open previous settings page";
    public static volatile String settings_next_button = "&a[Next >]";
    public static volatile String settings_next_disabled = "&8[Next >]";
    public static volatile String settings_next_hover = "&7Open next settings page";
    public static volatile List<String> settings_unknown = Collections.emptyList();
    public static volatile List<String> settings_locked = Collections.emptyList();
    public static volatile List<String> settings_invalid_mode = Collections.emptyList();
    public static volatile List<String> settings_save_failed = Collections.emptyList();
    public static volatile List<String> settings_changed = Collections.emptyList();
    public static volatile List<String> settings_usage = Collections.emptyList();
    public static volatile List<String> settings_command_disabled = Collections.emptyList();
    public static volatile String commandhelp_settings = "&a/rtp settings - &6opens command toggles.";
    public static volatile String commandhelp_doctor = "&a/rtp doctor - &6shows startup/config health.";
    public static volatile String commandhelp_stats = "&a/rtp stats - &6shows runtime metrics.";
    public static volatile String commandhelp_dump = "&a/rtp dump - &6creates a support bundle.";
    public static volatile String commandhelp_portal_check = "&a/rtp portal check - &6checks portal health.";
    public static volatile String commandhelp_tpsbar = "&a/rtp tpsbar - &6toggles the TPS admin bossbar.";
    public static volatile String commandhelp_rambar = "&a/rtp rambar - &6toggles the RAM admin bossbar.";
    public static volatile String commandhelp_msptbar = "&a/rtp msptbar - &6toggles the MSPT admin bossbar.";
    public static volatile String commandhelp_allbars = "&a/rtp allbars - &6toggles all admin bossbars.";
    public static volatile List<String> redirect_world;
    public static volatile List<String> banned_world_sender;
    public static volatile List<String> successMessage_chunky;
    public static volatile List<String> cancelMessage_chunky;
    public static volatile List<String> chunkyradius_chunky;
    public static volatile List<String> portalradius;
    public static volatile List<String> worldborder_error;
    public static volatile List<String> rederictworldnear_error;
    public static volatile List<String> portalform;
    public static volatile List<String> error_portal_shape_radius;
    public static volatile List<String> error_break_portal_block;
    public static volatile String titleMessage_loading;
    public static volatile String subtitleMessage_loading;
    public static volatile List<String> longteleportwait;

    public static void loadMessages(YamlConfiguration langFile) {
        nopermissionreload = langFile.getStringList("messages.no-permission-reload");
        reloadingwait = langFile.getStringList("messages.reloading-wait");
        reloadingstart = langFile.getStringList("messages.reloading-start");
        successfullyreload = langFile.getStringList("messages.successfully-reload");
        nopermissioncommand = langFile.getStringList("messages.no-permission");
        invalidcommand = withFallback(langFile.getStringList("messages.invalid-command"),
                "&a[sRandomRTP] &cInvalid command!");
        messagescooldownMessage = langFile.getStringList("messages.cooldownMessage");
        teleportdamagedcancel = langFile.getStringList("messages.teleport-damaged-cancel");
        teleportmovecancel = langFile.getStringList("messages.teleport-move-cancel");
        teleportcancel = langFile.getStringList("messages.teleport-cancel");
        teleportyes = langFile.getStringList("messages.teleported");
        loading = langFile.getStringList("messages.loading");
        locationNotFound = langFile.getStringList("messages.locationNotFound");
        newVersionMessage = langFile.getStringList("messages.newVersionMessage");
        CheckingVersion = langFile.getStringList("messages.CheckingVersion");
        LatestVersionMessage = langFile.getStringList("messages.Latest-Version-Message");
        ErrorCheckingVersionMessage = langFile.getStringList("messages.Error-Checking-Version-Message");
        teleportationinprogress = langFile.getStringList("messages.teleportation-in-progress");
        bossbar = langFile.getString("messages.boss-bar");
        if (bossbar == null) bossbar = "";
        nosearching = langFile.getStringList("messages.no-searching");
        titleMessage = langFile.getString("messages.titleMessage");
        if (titleMessage == null) titleMessage = "";
        subtitleMessage = langFile.getString("messages.subtitleMessage");
        if (subtitleMessage == null) subtitleMessage = "";
        noplayerservenear = langFile.getStringList("messages.noplayerservenear");
        noplayerworldnear = langFile.getStringList("messages.noplayerworldnear");
        commandhelp = langFile.getStringList("messages.commandhelp");
        actionBarMessage = langFile.getString("messages.actionBarMessage");
        if (actionBarMessage == null) actionBarMessage = "";
        insufficient_funds = langFile.getStringList("messages.insufficient_funds");
        error_withdrawing = langFile.getStringList("messages.error_withdrawing");
        insufficient_hunger = langFile.getStringList("messages.insufficient_hunger");
        insufficient_levels = langFile.getStringList("messages.insufficient_levels");
        banned_world = langFile.getStringList("messages.banned_world");
        player_nearby = langFile.getStringList("messages.player_nearby");
        insufficient_health = langFile.getStringList("messages.insufficient_health");
        insufficient_items = langFile.getStringList("messages.insufficient_items");
        noadvancementnether = langFile.getStringList("messages.no-advancement-nether");
        noadvancementend = langFile.getStringList("messages.no-advancement-end");
        PluginEnabledMessage = langFile.getStringList("messages.Plugin-Enabled-Message");
        PluginDisabledMessage = langFile.getStringList("messages.Plugin-Disabled-Message");
        teleportBackSuccess = langFile.getStringList("messages.teleportBackSuccess");
        teleportBackFailure = langFile.getStringList("messages.teleportBackFailure");
        regionsempty = langFile.getStringList("messages.regionsempty");
        regionManager = langFile.getStringList("messages.regionManager");
        bannedbiome = langFile.getStringList("messages.bannedbiome");
        rtpplayerteleportsuccesssender = langFile.getStringList("messages.rtp-player-teleport-success-sender");
        rtpplayerteleportsuccesstarget = langFile.getStringList("messages.rtp-player-teleport-success-target");
        rtpplayerrequestinitiator = langFile.getStringList("messages.rtp-player-request-initiator");
        rtpplayertimeout = langFile.getStringList("messages.rtp-player-timeout");
        rtpplayercanceled = langFile.getStringList("messages.rtp-player-canceled");
        rtpplayersendernotified = langFile.getStringList("messages.rtp-player-sender-notified");
        rtpplayernoactiveteleport = langFile.getStringList("messages.rtp-player-no-active-teleport");
        rtpplayeralreadyrequested = langFile.getStringList("messages.rtp-player-already-requested");
        rtpplayerteleportrequestsent = langFile.getStringList("messages.rtp-player-teleport-request-sent");
        error_radius = langFile.getStringList("messages.error-radius");
        error_teleport_yourself = langFile.getStringList("messages.error-teleport-yourself");
        no_active_requests_accept = langFile.getStringList("messages.no-active-requests-accept");
        error_radius_portal = langFile.getStringList("messages.error-radius-portal");
        error_portal_name = langFile.getStringList("messages.error-portal-name");
        error_portal_name_characters = langFile.getStringList("messages.error-portal-name-characters");
        error_portal_name_already_exists = langFile.getStringList("messages.error-portal-name-already-exists");
        error_insufficient_space = langFile.getStringList("messages.error-insufficient-space");
        success_portal_created = langFile.getStringList("messages.success-portal-created");
        error_cooldown_wait = langFile.getStringList("messages.error-cooldown-wait");
        error_page_number_invalid = langFile.getStringList("messages.error-page-number-invalid");
        error_command_format = langFile.getStringList("messages.error-command-format");
        error_no_portals = langFile.getStringList("messages.error-no-portals");
        error_page_not_found = langFile.getStringList("messages.error-page-not-found");
        error_portal_not_found = langFile.getStringList("messages.error-portal-not-found");
        error_world_not_found = langFile.getStringList("messages.error-world-not-found");
        portal_list_header = langFile.getStringList("messages.portal-list-header");
        portal_delete_button = langFile.getString("messages.portal-delete-button");
        if (portal_delete_button == null) portal_delete_button = "";
        portal_delete_hover = langFile.getString("messages.portal-delete-hover");
        if (portal_delete_hover == null) portal_delete_hover = "";
        portal_tp_button = langFile.getString("messages.portal-tp-button");
        if (portal_tp_button == null) portal_tp_button = "";
        portal_tp_hover = langFile.getString("messages.portal-tp-hover");
        if (portal_tp_hover == null) portal_tp_hover = "";
        portal_coordinates = langFile.getString("messages.portal-coordinates");
        if (portal_coordinates == null) portal_coordinates = "";
        portal_prev_button = langFile.getString("messages.portal-prev-button");
        if (portal_prev_button == null) portal_prev_button = "";
        portal_prev_hover = langFile.getString("messages.portal-prev-hover");
        if (portal_prev_hover == null) portal_prev_hover = "";
        portal_prev_disabled = langFile.getString("messages.portal-prev-disabled");
        if (portal_prev_disabled == null) portal_prev_disabled = "";
        portal_next_button = langFile.getString("messages.portal-next-button");
        if (portal_next_button == null) portal_next_button = "";
        portal_next_hover = langFile.getString("messages.portal-next-hover");
        if (portal_next_hover == null) portal_next_hover = "";
        portal_next_disabled = langFile.getString("messages.portal-next-disabled");
        if (portal_next_disabled == null) portal_next_disabled = "";
        portal_space = langFile.getString("messages.portal-space");
        if (portal_space == null) portal_space = "";
        portal_teleport_success = langFile.getStringList("messages.portal-teleport-success");
        portal_delete_success = langFile.getStringList("messages.portal-delete-success");
        diagnostics_doctor_lines = withFallback(langFile.getStringList("messages.diagnostics.doctor-lines"),
                "&a[sRandomRTP] &6Doctor",
                "&7Plugin: &f%plugin_version% &8| &7Java: &f%java_version%",
                "&7Server: &f%server_version%",
                "&7Folia: &f%folia%",
                "&7Language: &f%language%",
                "&7Integrations: &fWorldGuard=%worldguard%, Vault=%vault%, Chunky=%chunky%, PlaceholderAPI=%placeholderapi%",
                "&7Active searches: &f%active_searches% &8| &7Portal tasks: &f%portal_tasks%",
                "&7Config versions: &f%config_versions%");
        diagnostics_stats_lines = withFallback(langFile.getStringList("messages.diagnostics.stats-lines"),
                "&a[sRandomRTP] &6Runtime stats",
                "&7Active searches: &f%active_searches%",
                "&7Total RTP uses: &f%rtp_uses%",
                "&7Cooldowns: &f%cooldowns% &8| &7Biome cooldowns: &f%biome_cooldowns%",
                "&7Portal tasks: &f%portal_tasks% &8| &7Portal blocks: &f%portal_blocks%",
                "&7Completed/cancelled/refunds: &f%completed%&7/&f%cancelled%&7/&f%refunds%",
                "&7Avg coordinate/safeY/chunk ms: &f%coordinate_avg%&7/&f%safe_y_avg%&7/&f%chunk_avg%");
        diagnostics_dump_created = withFallback(langFile.getStringList("messages.diagnostics.dump-created"),
                "&a[sRandomRTP] &aSupport dump created: &f%path%");
        diagnostics_dump_failed = withFallback(langFile.getStringList("messages.diagnostics.dump-failed"),
                "&a[sRandomRTP] &cFailed to create support dump: %error%");
        diagnostics_portal_check = withFallback(langFile.getStringList("messages.diagnostics.portal-check"),
                "&a[sRandomRTP] &6Portal check: &ftotal=%total% &7missing_worlds=&f%missing_worlds% &7duplicate_world_names=&f%duplicate_world_names% &7tasks=&f%tasks%");
        settings_header = getString(langFile, "messages.settings.header", settings_header);
        settings_description = getString(langFile, "messages.settings.description", settings_description);
        settings_category = getString(langFile, "messages.settings.category", settings_category);
        settings_category_player = getString(langFile, "messages.settings.categories.player", settings_category_player);
        settings_category_admin = getString(langFile, "messages.settings.categories.admin", settings_category_admin);
        settings_category_debug = getString(langFile, "messages.settings.categories.debug", settings_category_debug);
        settings_flag_line = getString(langFile, "messages.settings.flag-line", settings_flag_line);
        settings_status_on = getString(langFile, "messages.settings.status-on", settings_status_on);
        settings_status_off = getString(langFile, "messages.settings.status-off", settings_status_off);
        settings_status_locked = getString(langFile, "messages.settings.status-locked", settings_status_locked);
        settings_toggle_hover = getString(langFile, "messages.settings.toggle-hover", settings_toggle_hover);
        settings_prev_button = getString(langFile, "messages.settings.prev-button", settings_prev_button);
        settings_prev_disabled = getString(langFile, "messages.settings.prev-disabled", settings_prev_disabled);
        settings_prev_hover = getString(langFile, "messages.settings.prev-hover", settings_prev_hover);
        settings_next_button = getString(langFile, "messages.settings.next-button", settings_next_button);
        settings_next_disabled = getString(langFile, "messages.settings.next-disabled", settings_next_disabled);
        settings_next_hover = getString(langFile, "messages.settings.next-hover", settings_next_hover);
        settings_unknown = withFallback(langFile.getStringList("messages.settings.unknown"),
                "&a[sRandomRTP] &cUnknown setting: &f%id%");
        settings_locked = withFallback(langFile.getStringList("messages.settings.locked"),
                "&a[sRandomRTP] &e%command% is protected from in-game toggles.");
        settings_invalid_mode = withFallback(langFile.getStringList("messages.settings.invalid-mode"),
                "&a[sRandomRTP] &cUse: /rtp settings toggle %id% [on|off]");
        settings_save_failed = withFallback(langFile.getStringList("messages.settings.save-failed"),
                "&a[sRandomRTP] &cFailed to save Settings/commands.yml: %error%");
        settings_changed = withFallback(langFile.getStringList("messages.settings.changed"),
                "&a[sRandomRTP] &a%command% command is now %state%&a.");
        settings_usage = withFallback(langFile.getStringList("messages.settings.usage"),
                "&a[sRandomRTP] &6Usage:",
                "&7/rtp settings [page]",
                "&7/rtp settings toggle <id> [on|off]");
        settings_command_disabled = withFallback(langFile.getStringList("messages.settings.command-disabled"),
                "&a[sRandomRTP] &cThis command is disabled in &fSettings/commands.yml&c: &e%id%");
        commandhelp_settings = getString(langFile, "messages.command-help.settings", commandhelp_settings);
        commandhelp_doctor = getString(langFile, "messages.command-help.doctor", commandhelp_doctor);
        commandhelp_stats = getString(langFile, "messages.command-help.stats", commandhelp_stats);
        commandhelp_dump = getString(langFile, "messages.command-help.dump", commandhelp_dump);
        commandhelp_portal_check = getString(langFile, "messages.command-help.portal-check", commandhelp_portal_check);
        commandhelp_tpsbar = getString(langFile, "messages.command-help.tpsbar", commandhelp_tpsbar);
        commandhelp_rambar = getString(langFile, "messages.command-help.rambar", commandhelp_rambar);
        commandhelp_msptbar = getString(langFile, "messages.command-help.msptbar", commandhelp_msptbar);
        commandhelp_allbars = getString(langFile, "messages.command-help.allbars", commandhelp_allbars);
        redirect_world = langFile.getStringList("messages.redirect-world");
        banned_world_sender = langFile.getStringList("messages.banned-world-sender");
        successMessage_chunky = langFile.getStringList("messages.successMessage-chunky");
        cancelMessage_chunky = langFile.getStringList("messages.cancelMessage-chunky");
        chunkyradius_chunky = langFile.getStringList("messages.chunkyradius-chunky");
        portalradius = langFile.getStringList("messages.portalradius");
        portalform = langFile.getStringList("messages.portalform");
        worldborder_error = langFile.getStringList("messages.worldborder-error");
        rederictworldnear_error = langFile.getStringList("messages.rederictworldnear-error");
        error_portal_shape_radius = langFile.getStringList("messages.error-portal-shape-radius");
        error_break_portal_block = langFile.getStringList("messages.error-break-portal-block");
        titleMessage_loading = langFile.getString("messages.titleMessage-loading");
        if (titleMessage_loading == null) titleMessage_loading = "";
        subtitleMessage_loading = langFile.getString("messages.subtitleMessage-loading");
        if (subtitleMessage_loading == null) subtitleMessage_loading = "";
        longteleportwait = langFile.getStringList("messages.long-teleport-wait");
    }

    private static String getString(YamlConfiguration langFile, String path, String fallback) {
        String value = langFile.getString(path);
        return value == null ? fallback : value;
    }

    private static List<String> withFallback(List<String> messages, String... fallback) {
        if (messages != null && !messages.isEmpty()) {
            return messages;
        }
        java.util.ArrayList<String> defaults = new java.util.ArrayList<>();
        if (fallback != null) {
            Collections.addAll(defaults, fallback);
        }
        return defaults;
    }
}
