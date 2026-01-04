package org.sRandomRTP.DifferentMethods;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.chunky.api.ChunkyAPI;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalDataBlocks;
import org.sRandomRTP.DataPortals.PortalDataTasks;
import org.sRandomRTP.Main;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.bukkit.boss.BossBar;
import org.sRandomRTP.Utils.PlayerResourceMap;
    
public class Variables {
    //
    public static Main instance;
    public static ChunkyAPI chunkyAPI;

    public static Main getInstance() {
        return instance;
    }

    //
    public static PlayerResourceMap<BossBar> bossBars = new PlayerResourceMap<>();
    public static PlayerResourceMap<WrappedTask> teleportTasks = new PlayerResourceMap<>();
    public static PlayerResourceMap<WrappedTask> particleTasks = new PlayerResourceMap<>();
    public static Map<String, Long> cooldowns = new HashMap<>();
    public static Map<String, Long> biomeCooldowns = new HashMap<>();
    public static Map<String, Boolean> playerSearchStatus = new ConcurrentHashMap<>();
    public static Map<String, CommandSender> senderSendMessage = new HashMap<>();
    public static Map<String, CommandSender> commandSenderMap = new HashMap<>();
    public static Map<String, Boolean> playerConfirmStatus = new HashMap<>();
    public static Map<Material, Integer> itemMap = new HashMap<>();
    public static Map<Integer, Integer> rtpCount = new HashMap<>();
    public static PlayerResourceMap<Location> initialPositions = new PlayerResourceMap<>();
    public static final Map<String, AtomicBoolean> suitableLocationFound = new ConcurrentHashMap<>();
    //
    public static String currentSearchingPlayer = null;
    //
    public static FileConfiguration teleportfile;
    public static FileConfiguration soundfile;
    public static FileConfiguration bossbarfile;
    public static FileConfiguration nearfile;
    public static FileConfiguration titlefile;
    public static FileConfiguration economyfile;
    public static FileConfiguration effectfile;
    public static FileConfiguration particlesfile;
    public static FileConfiguration farfile;
    public static FileConfiguration middlefile;
    public static FileConfiguration portalfile;
    public static FileConfiguration chunkfile;
    //
    public static FileConfiguration langEnFile;
    public static FileConfiguration langRuFile;
    public static FileConfiguration langEsFile;
    public static FileConfiguration langDeFile;
    public static FileConfiguration langFrFile;
    public static FileConfiguration langItFile;
    public static FileConfiguration langPtFile;
    public static FileConfiguration langZhFile;
    public static FileConfiguration langJaFile;
    public static FileConfiguration langKoFile;
    public static FileConfiguration langArFile;
    public static FileConfiguration langPlFile;
    public static FileConfiguration langViFile;
    public static FileConfiguration langUaFile;
    public static FileConfiguration langTrFile;
    public static FileConfiguration langCustomFile;
    //
    public static List<Material> blockList = new ArrayList<>();
    public static List<String> messages = new ArrayList<>();
    //
    public static WrappedTask autoCheckVersionTask;
    public static WrappedTask commandReloadTask;
    //
    public static Pattern RGB_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    public static String pluginName = "§a[sRandomRTP]";
    //
    public static Economy econ = null;
    //
    public static void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getConsoleSender().sendMessage("Vault plugin not found!");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
        }
    }

    //
    public static void clearTeleportFlags(Player player) {
        suitableLocationFound.remove(player.getName());
    }

    //
    public static HttpURLConnection connection = null;
    //
    public static boolean pluginToggle = false;
    //
    public static FoliaLib foliaLib;

    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    //
    public static Connection connectionSQLPortal = null;
    //
    public static Map<Location, Material> placedBlocks = new HashMap<>();
    public static Map<String, Map<String, PortalData>> playerPortals = new HashMap<>();
    public static Map<String, PortalDataBlocks> playerPortalsBlocks = new HashMap<>();
    public static Map<String, PortalDataTasks> playerPortalsTasks = new HashMap<>();
    public static Map<String, World> targetWorlds = new HashMap<>();

    //
    public static void initializePlugin(Main plugin) {
        instance = plugin;
        if (foliaLib == null) {
            foliaLib = new FoliaLib(plugin);
        }
    }
}
