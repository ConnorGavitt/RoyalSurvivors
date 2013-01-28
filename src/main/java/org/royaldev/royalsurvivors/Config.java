package org.royaldev.royalsurvivors;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class Config {

    private final RoyalSurvivors plugin;

    public Config(RoyalSurvivors instance) {
        plugin = instance;
        File config = new File(plugin.getDataFolder(), "config.yml");
        if (!config.exists()) {
            config.getParentFile().mkdirs();
            plugin.saveDefaultConfig();
        }
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        worldToUse = c.getString("gameplay.world_to_use");
        radioItem = c.getString("gameplay.radio.item");
        radioBattery = c.getString("gameplay.radio.battery.item");
        banMessage = c.getString("gameplay.death.ban.message");

        saveUDOnChange = c.getBoolean("saving.save_on_change");
        banOnDeath = c.getBoolean("gameplay.death.ban.enabled");
        deathChest = c.getBoolean("gameplay.death.drops_to_chest");
        spawnZombie = c.getBoolean("gameplay.death.spawn_zombie");
        useRadioBattery = c.getBoolean("gameplay.radio.battery.use");
        removeIfMax = c.getBoolean("gameplay.spawn_if_maxed");
        usePotions = c.getBoolean("gameplay.zombies.potions.enabled");
        debug = c.getBoolean("miscellaneous.debug");
        oceanZombies = c.getBoolean("gameplay.zombies.enable_ocean_zombies");
        useSpeed = c.getBoolean("gameplay.zombies.speed.enabled");

        localChatRadius = c.getLong("gameplay.radio.local_chat_radius");

        maxMobs = c.getInt("gameplay.max_mobs_allowed");
        hordeChance = c.getInt("gameplay.zombies.hordes.chance_out_of");
        hordeMin = c.getInt("gameplay.zombies.hordes.low");
        hordeMax = c.getInt("gameplay.zombies.hordes.high");
        banLength = c.getInt("gameplay.death.ban.length");
        batteryDrainAmount = c.getInt("gameplay.radio.battery.drain.amount");
        batteryDrainInterval = c.getInt("gameplay.radio.battery.drain.interval");
        potionChance = c.getInt("gameplay.zombies.potions.chance_out_of");
        minLevelPotion = c.getInt("gameplay.zombies.potions.minimum_level");
        thirstPercent = c.getInt("gameplay.thirst.drain.amount");
        thirstInterval = c.getInt("gameplay.thirst.drain.interval");
        thirstRestorePercent = c.getInt("gameplay.thirst.restore.amount");
        speedBoostLevel = c.getInt("gameplay.zombies.speed.level");
        gpsUpdateInterval = c.getInt("gameplay.gps_update_interval");

        if (hordeChance < 2) hordeChance = 2;
        if (hordeMax < 0) hordeMax = 40;
        if (hordeMin < 0) hordeMin = 10;
        if (hordeMax < hordeMin) hordeMax = hordeMin;

        if (potionChance < 2) potionChance = 2;

        if (speedBoostLevel < 0) speedBoostLevel = 0;

        try {
            radioMaterial = Material.valueOf(radioItem);
        } catch (Exception e) {
            radioMaterial = Material.COMPASS;
        }
        try {
            radioBatteryMaterial = Material.valueOf(radioBattery);
        } catch (Exception e) {
            radioBatteryMaterial = Material.GLOWSTONE_DUST;
        }

        if (banMessage == null) banMessage = "You died.";
        banMessage = RUtils.colorize(banMessage);
    }

    public static String worldToUse;
    public static String radioItem;
    public static String radioBattery;
    public static String banMessage;

    public static boolean saveUDOnChange;
    public static boolean banOnDeath;
    public static boolean deathChest;
    public static boolean spawnZombie;
    public static boolean useRadioBattery;
    public static boolean removeIfMax;
    public static boolean usePotions;
    public static boolean debug;
    public static boolean oceanZombies;
    public static boolean useSpeed;

    public static double localChatRadius;

    public static int maxMobs;
    public static int hordeChance;
    public static int hordeMin;
    public static int hordeMax;
    public static int banLength;
    public static int batteryDrainAmount;
    public static int batteryDrainInterval;
    public static int potionChance;
    public static int minLevelPotion;
    public static int thirstPercent;
    public static int thirstInterval;
    public static int thirstRestorePercent;
    public static int speedBoostLevel;
    public static int gpsUpdateInterval;

    public static Material radioMaterial;
    public static Material radioBatteryMaterial;

}
