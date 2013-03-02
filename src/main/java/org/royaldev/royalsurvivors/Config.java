package org.royaldev.royalsurvivors;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;

public class Config {

    private final RoyalSurvivors plugin;

    public Config(RoyalSurvivors instance) {
        plugin = instance;
        File config = new File(plugin.getDataFolder(), "config.yml");
        if (!config.exists()) {
            if (!config.getParentFile().mkdirs()) plugin.getLogger().warning("Could not create config.yml directory.");
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
        texturePackURL = c.getString("gameplay.player.texture_pack_url").trim();

        banOnDeath = c.getBoolean("gameplay.death.ban.enabled");
        deathChest = c.getBoolean("gameplay.death.drops_to_chest");
        spawnZombie = c.getBoolean("gameplay.death.spawn_zombie");
        useRadioBattery = c.getBoolean("gameplay.radio.battery.use");
        removeIfMax = c.getBoolean("gameplay.spawn_if_maxed");
        usePotions = c.getBoolean("gameplay.zombies.potions.enabled");
        debug = c.getBoolean("miscellaneous.debug");
        oceanZombies = c.getBoolean("gameplay.zombies.enable_ocean_zombies");
        useSpeed = c.getBoolean("gameplay.zombies.speed.enabled");
        useGrenades = c.getBoolean("gameplay.grenades.enabled");
        useBabies = c.getBoolean("gameplay.zombies.babies.enabled");
        babiesAlwaysFast = c.getBoolean("gameplay.zombies.babies.always_fast");
        toxicEnabled = c.getBoolean("gameplay.zombies.toxicSpray.enabled");
        allowLootChestBreak = c.getBoolean("loot_chests.allow_chest_break");
        refillIfNotEmpty = c.getBoolean("loot_chests.refill.if_not_empty");
        harderTorches = c.getBoolean("gameplay.harder_torches.enabled");
        useSquidLoot = c.getBoolean("miscellaneous.squid_loot.enabled");
        canPickupItems = c.getBoolean("gameplay.player.can_pick_up_items");
        checkVersion = c.getBoolean("miscellaneous.check_version");

        localChatRadius = c.getDouble("gameplay.radio.local_chat_radius");
        toxicRadius = c.getDouble("gameplay.zombies.toxicSpray.radius");
        lootChestRadius = c.getDouble("loot_chests.refill.radius");

        try {
            walkSpeed = Float.parseFloat(c.getString("gameplay.player.walk_speed"));
            flySpeed = Float.parseFloat(c.getString("gameplay.player.fly_speed"));
        } catch (NumberFormatException e) {
            walkSpeed = .2F;
            flySpeed = .1F;
        }

        deathChestRemoveInterval = c.getLong("miscellaneous.remove_death_chests_every");
        repairChestRunInterval = c.getLong("miscellaneous.repair_chests.repair_frequency");

        maxMobs = c.getInt("gameplay.max_mobs_allowed");
        hordeChance = c.getInt("gameplay.zombies.hordes.chance_out_of");
        hordeMin = c.getInt("gameplay.zombies.hordes.low");
        hordeMax = c.getInt("gameplay.zombies.hordes.high");
        banLength = c.getInt("gameplay.death.ban.length");
        batteryDrainAmount = c.getInt("gameplay.radio.battery.drain.amount");
        batteryDrainInterval = c.getInt("gameplay.radio.battery.drain.interval");
        potionChance = c.getInt("gameplay.zombies.potions.chance_out_of");
        minLevelPotion = c.getInt("gameplay.zombies.potions.minimum_level");
        thirstWalk = c.getInt("gameplay.thirst.drain.walk");
        thirstSprint = c.getInt("gameplay.thirst.drain.sprint");
        thirstSneak = c.getInt("gameplay.thirst.drain.sneak");
        thirstJump = c.getInt("gameplay.thirst.drain.jump");
        thirstFire = c.getInt("gameplay.thirst.drain.fire");
        thirstBreak = c.getInt("gameplay.thirst.drain.break");
        thirstPlace = c.getInt("gameplay.thirst.drain.place");
        thirstDamage = c.getInt("gameplay.thirst.drain.damage");
        thirstTakeDamage = c.getInt("gameplay.thirst.drain.take_damage");
        thirstSwing = c.getInt("gameplay.thirst.drain.swing");
        thirstSwingItem = c.getInt("gameplay.thirst.drain.swing_item");
        thirstMax = c.getInt("gameplay.thirst.drain.max");
        thirstSaturationMax = c.getInt("gameplay.thirst.saturation.max");
        thirstRestorePercent = c.getInt("gameplay.thirst.restore.amount");
        speedBoostLevel = c.getInt("gameplay.zombies.speed.level");
        gpsUpdateInterval = c.getInt("gameplay.gps_update_interval");
        grenadeHighDamage = c.getInt("gameplay.grenades.high_damage");
        grenadeLowDamage = c.getInt("gameplay.grenades.low_damage");
        grenadeHighBurn = c.getInt("gameplay.grenades.high_burn_time");
        grenadeLowBurn = c.getInt("gameplay.grenades.low_burn_time");
        babyZombieChance = c.getInt("gameplay.zombies.babies.chance_out_of");
        userdataSaveInterval = c.getInt("saving.save_interval");
        toxicInterval = c.getInt("gameplay.zombies.toxicSpray.interval");
        toxicDuration = c.getInt("gameplay.zombies.toxicSpray.duration");
        repairChestRepairAmount = c.getInt("miscellaneous.repair_chests.repair_amount");
        squidLootChance = c.getInt("miscellaneous.squid_loot.chance");
        maxHealth = c.getInt("gameplay.player.max_health");
        maxAir = c.getInt("gameplay.player.maximum_underwater_air");
        babySpeedLevel = c.getInt("gameplay.zombies.babies.speed_level");

        allowedCommands = c.getStringList("miscellaneous.allowed_commands");
        squidLootSets = c.getStringList("miscellaneous.squid_loot.loot_sets");

        if (maxHealth < 1) maxHealth = 1;
        if (maxAir < 0) maxAir = 0;

        if (babySpeedLevel < 0) babySpeedLevel = 0;

        if (walkSpeed > 1F || walkSpeed < -1F) walkSpeed = .2F;
        if (flySpeed > 1F || flySpeed < -1F) flySpeed = .1F;

        if (toxicRadius < 1D) toxicRadius = 1D;
        if (toxicInterval < 1) toxicInterval = 1;

        if (deathChestRemoveInterval < 1L) deathChestRemoveInterval = 5L;
        if (repairChestRunInterval < 1L) repairChestRunInterval = 5L;

        if (hordeChance < 2) hordeChance = 2;
        if (hordeMax < 0) hordeMax = 40;
        if (hordeMin < 0) hordeMin = 10;
        if (hordeMax < hordeMin) hordeMax = hordeMin;

        if (potionChance < 2) potionChance = 2;

        if (babyZombieChance < 2) babyZombieChance = 2;

        if (speedBoostLevel < 0) speedBoostLevel = 0;

        if (userdataSaveInterval < 1) userdataSaveInterval = 5;

        if (grenadeLowDamage < 0) grenadeLowDamage = 8;
        if (grenadeHighDamage < 0) grenadeHighDamage = 3;
        if (grenadeLowDamage < grenadeHighDamage) grenadeLowDamage = grenadeHighDamage;

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
    public static String texturePackURL;

    public static boolean banOnDeath;
    public static boolean deathChest;
    public static boolean spawnZombie;
    public static boolean useRadioBattery;
    public static boolean removeIfMax;
    public static boolean usePotions;
    public static boolean debug;
    public static boolean oceanZombies;
    public static boolean useSpeed;
    public static boolean useGrenades;
    public static boolean useBabies;
    public static boolean babiesAlwaysFast;
    public static boolean toxicEnabled;
    public static boolean allowLootChestBreak;
    public static boolean refillIfNotEmpty;
    public static boolean harderTorches;
    public static boolean useSquidLoot;
    public static boolean canPickupItems;
    public static boolean checkVersion;

    public static double localChatRadius;
    public static double toxicRadius;
    public static double lootChestRadius;

    public static long deathChestRemoveInterval;
    public static long repairChestRunInterval;

    public static int maxMobs;
    public static int hordeChance;
    public static int hordeMin;
    public static int hordeMax;
    public static int banLength;
    public static int batteryDrainAmount;
    public static int batteryDrainInterval;
    public static int potionChance;
    public static int minLevelPotion;
    public static int thirstWalk;
    public static int thirstSprint;
    public static int thirstSneak;
    public static int thirstJump;
    public static int thirstFire;
    public static int thirstPlace;
    public static int thirstBreak;
    public static int thirstDamage;
    public static int thirstTakeDamage;
    public static int thirstSwing;
    public static int thirstSwingItem;
    public static int thirstMax;
    public static int thirstSaturationMax;
    public static int thirstRestorePercent;
    public static int speedBoostLevel;
    public static int gpsUpdateInterval;
    public static int grenadeHighDamage;
    public static int grenadeLowDamage;
    public static int grenadeLowBurn;
    public static int grenadeHighBurn;
    public static int babyZombieChance;
    public static int userdataSaveInterval;
    public static int toxicInterval;
    public static int toxicDuration;
    public static int repairChestRepairAmount;
    public static int squidLootChance;
    public static int maxHealth;
    public static int maxAir;
    public static int babySpeedLevel;

    public static float walkSpeed;
    public static float flySpeed;

    public static Material radioMaterial;
    public static Material radioBatteryMaterial;

    public static List<String> allowedCommands;
    public static List<String> squidLootSets;

}
