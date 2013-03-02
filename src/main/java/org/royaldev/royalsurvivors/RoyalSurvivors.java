package org.royaldev.royalsurvivors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.royaldev.royalsurvivors.commands.CmdGPS;
import org.royaldev.royalsurvivors.commands.CmdRadio;
import org.royaldev.royalsurvivors.commands.CmdSurvivors;
import org.royaldev.royalsurvivors.runners.BatteryRunner;
import org.royaldev.royalsurvivors.runners.CompassUpdater;
import org.royaldev.royalsurvivors.runners.DeathChestRemover;
import org.royaldev.royalsurvivors.runners.LootChestFiller;
import org.royaldev.royalsurvivors.runners.RepairChestRunner;
import org.royaldev.royalsurvivors.runners.UserdataSaver;
import org.royaldev.royalsurvivors.runners.ZombieSpray;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoyalSurvivors extends JavaPlugin {

    // TODO: Radio block?

    public static File dataFolder;

    private final int minVersion = 2636;

    public Config c;
    public ShapelessRecipe batteryRefill;
    public ShapelessRecipe waterBottle;
    public ShapelessRecipe arrowRecipe;
    public ShapelessRecipe torchRecipe;
    public ShapedRecipe bowRecipe;

    public ItemStack recharge;
    public ItemStack arrow;
    public ItemStack furnace;
    public ItemStack toxicSpray;
    public ItemStack repairChest;

    public static RoyalSurvivors instance;

    public final Map<String, PConfManager> pconfs = new HashMap<String, PConfManager>();
    public final Map<String, ConfManager> confs = new HashMap<String, ConfManager>();

    private final Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)(\\-SNAPSHOT)?(\\-local\\-(\\d{8}\\.\\d{6})|\\-(\\d+))?");

    public RoyalSurvivors() {
        super();
        instance = this;
    }

    /**
     * Registers a command in the server. If the command isn't defined in plugin.yml
     * the NPE is caught, and a warning line is sent to the console.
     *
     * @param ce      CommandExecutor to be registered
     * @param command Command name as specified in plugin.yml
     * @param jp      Plugin to register under
     */
    private void registerCommand(CommandExecutor ce, String command, JavaPlugin jp) {
        try {
            jp.getCommand(command).setExecutor(ce);
        } catch (NullPointerException e) {
            getLogger().warning("Could not register command \"" + command + "\" - not registered in plugin.yml (" + e.getMessage() + ")");
        }
    }

    /**
     * Gets the ConfManager for a path local to the plugin directory.
     *
     * @param path ex. "furnaces.yml"
     * @return ConfManager
     */
    public ConfManager getConfig(String path) {
        synchronized (confs) {
            if (confs.containsKey(path)) return confs.get(path);
            ConfManager cm = new ConfManager(path);
            if (!cm.exists()) cm.createFile();
            confs.put(path, cm);
            return cm;
        }
    }

    public PConfManager getUserdata(CommandSender cs) {
        return getUserdata(cs.getName());
    }

    public PConfManager getUserdata(String s) {
        synchronized (pconfs) {
            if (pconfs.containsKey(s)) return pconfs.get(s);
            PConfManager pcm = new PConfManager(s);
            if (!pcm.exists()) pcm.createFile();
            pconfs.put(s, pcm);
            return pcm;
        }
    }

    public static void debugStatic(Object o) {
        if (!Config.debug) return;
        System.out.println("[" + RoyalSurvivors.instance.getDescription().getName() + "] " + o);
    }

    public void debug(Object o) {
        if (!Config.debug) return;
        System.out.println("[" + getDescription().getName() + "] " + o);
    }

    private boolean versionCheck() {
        if (!Config.checkVersion) return true;
        Pattern p = Pattern.compile(".+b(\\d+)jnks.+");
        Matcher m = p.matcher(getServer().getVersion());
        if (!m.matches() || m.groupCount() < 1) {
            getLogger().warning("Could not get CraftBukkit version! No version checking will take place.");
            return true;
        }
        Integer currentVersion;
        try {
            currentVersion = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            getLogger().warning("Could not get CraftBukkit version! No version checking will take place.");
            return true;
        }
        return currentVersion == null || currentVersion >= minVersion;
    }

    private void addAllRecipes() {
        ItemStack bow = new ItemStack(Material.BOW, 1);
        ItemMeta im = bow.getItemMeta();
        im.setDisplayName(ChatColor.RESET + "M24 Sniper Rifle");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Uses .308 Caliber Rounds."));
        bow.setItemMeta(im);
        bowRecipe = new ShapedRecipe(bow);
        bowRecipe.shape(
                " G ",
                "WPI",
                "WL "
        );
        bowRecipe.setIngredient('G', Material.GLASS).setIngredient('W', Material.WOOD, -1).setIngredient('I', Material.IRON_INGOT).setIngredient('L', Material.LEVER).setIngredient('P', Material.SULPHUR);
        getServer().addRecipe(bowRecipe);
        arrow = new ItemStack(Material.ARROW, 8);
        im = arrow.getItemMeta();
        im.setDisplayName(ChatColor.RESET + ".308 Caliber Round");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Used with the M24 Sniper Rifle."));
        arrow.setItemMeta(im);
        arrowRecipe = new ShapelessRecipe(arrow);
        arrowRecipe.addIngredient(2, Material.IRON_INGOT);
        getServer().addRecipe(arrowRecipe);
        ShapelessRecipe slr = new ShapelessRecipe(new ItemStack(Material.PORK, 1));
        slr.addIngredient(Material.ROTTEN_FLESH).addIngredient(Material.GOLD_NUGGET).addIngredient(Material.WATER_BUCKET);
        getServer().addRecipe(slr);
        ItemStack chargedCompass = new ItemStack(Config.radioMaterial, 1);
        im = chargedCompass.getItemMeta();
        im.setLore(Arrays.asList(ChatColor.GRAY + "Battery: 100%"));
        chargedCompass.setItemMeta(im);
        batteryRefill = new ShapelessRecipe(chargedCompass);
        batteryRefill.addIngredient(Config.radioMaterial).addIngredient(Config.radioBatteryMaterial);
        getServer().addRecipe(batteryRefill);
        recharge = new ItemStack(Material.GLOWSTONE_DUST, 1);
        im = recharge.getItemMeta();
        im.setDisplayName(ChatColor.RESET + "Battery Recharge");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Used with a dead radio."));
        recharge.setItemMeta(im);
        slr = new ShapelessRecipe(recharge);
        slr.addIngredient(Material.INK_SACK, 15).addIngredient(Material.TORCH);
        getServer().addRecipe(slr);
        waterBottle = new ShapelessRecipe(new ItemStack(Material.GLASS_BOTTLE));
        waterBottle.addIngredient(Material.POTION, 0);
        getServer().addRecipe(waterBottle);
        ItemStack medpack = new ItemStack(Material.MELON, 1, (short) 14);
        im = medpack.getItemMeta();
        im.setDisplayName(ChatColor.RESET + "Medpack");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Restores 4 hearts of health on use."));
        medpack.setItemMeta(im);
        slr = new ShapelessRecipe(medpack);
        slr.addIngredient(2, Material.INK_SACK, 2).addIngredient(Material.PAPER);
        getServer().addRecipe(slr);
        ItemStack grenade = new ItemStack(Material.SNOW_BALL, 1);
        im = grenade.getItemMeta();
        im.setDisplayName(ChatColor.RESET + "White Phosphorous Grenade");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Explodes on impact."));
        grenade.setItemMeta(im);
        slr = new ShapelessRecipe(grenade);
        slr.addIngredient(Material.SULPHUR).addIngredient(Material.EGG).addIngredient(Material.FLINT);
        getServer().addRecipe(slr);
        furnace = new ItemStack(Material.FURNACE, 1, (short) 14);
        im = furnace.getItemMeta();
        im.setDisplayName(ChatColor.RESET + "Modified Furnace");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Smelts items faster."));
        furnace.setItemMeta(im);
        ShapedRecipe sr = new ShapedRecipe(furnace);
        sr.shape("RRR", "RFR", "RRR").setIngredient('R', Material.REDSTONE).setIngredient('F', Material.FURNACE);
        getServer().addRecipe(sr);
        toxicSpray = new ItemStack(Material.INK_SACK, 1, (short) 4);
        im = toxicSpray.getItemMeta();
        im.setDisplayName(ChatColor.RESET + "Toxic Zombie Spray");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Damages all zombies fatally around the wearer."));
        toxicSpray.setItemMeta(im);
        slr = new ShapelessRecipe(toxicSpray);
        slr.addIngredient(Material.INK_SACK, 4).addIngredient(Material.TORCH);
        getServer().addRecipe(slr);
        if (Config.harderTorches) {
            ItemStack torch = new ItemStack(Material.TORCH, 8);
            torchRecipe = new ShapelessRecipe(torch);
            torchRecipe.addIngredient(Material.STICK).addIngredient(Material.COAL).addIngredient(Material.FLINT);
            getServer().addRecipe(slr);
        }
        slr = new ShapelessRecipe(new ItemStack(Material.SLIME_BALL, 4));
        slr.addIngredient(Material.CLAY_BALL).addIngredient(Material.ROTTEN_FLESH).addIngredient(Material.WATER_BUCKET);
        getServer().addRecipe(slr);
        repairChest = new ItemStack(Material.CHEST);
        im = repairChest.getItemMeta();
        im.setDisplayName(ChatColor.RESET + "Repair Chest");
        im.setLore(Arrays.asList(ChatColor.GRAY + "Repairs items stored in it."));
        repairChest.setItemMeta(im);
        sr = new ShapedRecipe(repairChest);
        sr.shape("RIR", "ICI", "RIR").setIngredient('I', Material.IRON_BLOCK).setIngredient('C', Material.CHEST).setIngredient('R', Material.REDSTONE);
        getServer().addRecipe(sr);
    }

    @Override
    public void onEnable() {

        if (!versionCheck()) {
            getLogger().severe("This version of CraftBukkit is too old to run " + getDescription().getName() + ".");
            getLogger().info(getDescription().getName() + " requires CB build >= " + minVersion + ".");
            getLogger().info("Disabling plugin. This check can be turned off in the configuration.");
            setEnabled(false);
            return;
        }

        dataFolder = getDataFolder();

        c = new Config(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SurvivorsListener(this), this);

        BukkitScheduler bs = getServer().getScheduler();
        bs.runTaskTimer(this, new BatteryRunner(this), Config.batteryDrainInterval * 60L * 20L, Config.batteryDrainInterval * 60L * 20L);
        bs.runTaskTimer(this, new CompassUpdater(this), 0L, Config.gpsUpdateInterval * 20L);
        bs.runTaskTimer(this, new ZombieSpray(this), 20L, Config.toxicInterval);
        bs.runTaskTimer(this, new LootChestFiller(this), 20L, 200L);
        bs.runTaskTimer(this, new RepairChestRunner(this), 20L, Config.repairChestRunInterval);
        bs.runTaskTimerAsynchronously(this, new UserdataSaver(this), 20L, Config.userdataSaveInterval * 60L * 20L);
        if (Config.deathChestRemoveInterval > 0L)
            bs.runTaskTimer(this, new DeathChestRemover(this), 0L, Config.deathChestRemoveInterval * 60L * 20L);

        addAllRecipes();

        registerCommand(new CmdRadio(this), "radio", this);
        registerCommand(new CmdSurvivors(this), "survivors", this);
        registerCommand(new CmdGPS(this), "gps", this);

        try {
            Matcher matcher = versionPattern.matcher(getDescription().getVersion());
            matcher.matches();
            // 1 = base version
            // 2 = -SNAPSHOT
            // 5 = build #
            String versionMinusBuild = (matcher.group(1) == null) ? "Unknown" : matcher.group(1);
            String build = (matcher.group(5) == null) ? "local build" : matcher.group(5);
            if (matcher.group(2) == null) build = "release";
            Metrics m = new Metrics(this);
            Metrics.Graph g = m.createGraph("Version");
            g.addPlotter(
                    new Metrics.Plotter(versionMinusBuild + "~=~" + build) {
                        @Override
                        public int getValue() {
                            return 1;
                        }
                    }
            );
            m.addGraph(g);
            if (m.start()) getLogger().info("Metrics enabled. Thank you!");
            else getLogger().info("Metrics disabled. If you want to help keep accurate statistics, turn it on!");
        } catch (Exception e) {
            getLogger().warning("Couldn't start Metrics!");
        }
    }

    @Override
    public void onDisable() {
        synchronized (pconfs) {
            for (PConfManager pcm : pconfs.values()) pcm.forceSave();
        }
        synchronized (confs) {
            for (ConfManager cm : confs.values()) cm.forceSave();
        }
    }

}
