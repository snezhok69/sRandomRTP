package org.sRandomRTP.DifferentMethods;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
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
import java.util.regex.Pattern;

public class Variables {
    //
    public static Main instance;
    public static ChunkyAPI chunkyAPI;

    public static Main getInstance() {
        return instance;
    }

    //
    public static Map<Player, BossBar> bossBars = new HashMap<>();
    public static Map<Player, WrappedTask> teleportTasks = new ConcurrentHashMap<>();
    public static Map<Player, WrappedTask> particleTasks = new HashMap<>();
    public static Map<String, Long> cooldowns = new HashMap<>();
    public static Map<String, Boolean> playerSearchStatus = new HashMap<>();
    public static Map<String, CommandSender> senderSendMessage = new HashMap<>();
    public static Map<String, CommandSender> commandSenderMap = new HashMap<>();
    public static Map<String, Boolean> playerConfirmStatus = new HashMap<>();
    public static Map<Material, Integer> itemMap = new HashMap<>();
    public static Map<Integer, Integer> rtpCount = new HashMap<>();
    public static Map<Player, Location> initialPositions = new HashMap<>();
    //
    public static FileConfiguration teleportfile;
    public static FileConfiguration soundfile;
    public static FileConfiguration bossbarfile;
    public static FileConfiguration nearfile;
    public static FileConfiguration titlefile;
    public static FileConfiguration economyfile;
    public static FileConfiguration effectfile;
    public static FileConfiguration particlesfile;
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

    public static void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getConsoleSender().sendMessage("Vault plugin not found!");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
        }
    }

    public static HttpURLConnection connection = null;
    //
    public static boolean pluginToggle = false;
    //
    public static FoliaLib foliaLib;

    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public static Connection connectionSQLPortal = null;

    public static Map<Location, Material> placedBlocks = new HashMap<>();
    public static Map<String, Map<String, PortalData>> playerPortals = new HashMap<>();
    public static Map<String, PortalDataBlocks> playerPortalsBlocks = new HashMap<>();
    public static Map<String, PortalDataTasks> playerPortalsTasks = new HashMap<>();
    public static Map<String, World> targetWorlds = new HashMap<>();
}