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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RoyalSurvivors extends JavaPlugin {

    public static File dataFolder;

    public Config c;
    public ShapelessRecipe batteryRefill;
    public ShapelessRecipe waterBottle;
    public ShapelessRecipe arrowRecipe;
    public ShapedRecipe bowRecipe;

    private final Map<String, PConfManager> pconfs = new HashMap<String, PConfManager>();

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
        System.out.println("[RoyalSurvivors] " + o);
    }

    public void debug(Object o) {
        if (!Config.debug) return;
        System.out.println("[" + getDescription().getName() + "] " + o);
    }

    private void addRecipes() {
        bowRecipe = new ShapedRecipe(new ItemStack(Material.BOW, 1));
        bowRecipe.shape(
                " G ",
                "WPI",
                "WL "
        );
        bowRecipe.setIngredient('G', Material.GLASS).setIngredient('W', Material.WOOD).setIngredient('I', Material.IRON_INGOT).setIngredient('L', Material.LEVER).setIngredient('P', Material.SULPHUR);
        getServer().addRecipe(bowRecipe);
        arrowRecipe = new ShapelessRecipe(new ItemStack(Material.ARROW, 8));
        arrowRecipe.addIngredient(2, Material.IRON_INGOT);
        getServer().addRecipe(arrowRecipe);
        ShapelessRecipe slr = new ShapelessRecipe(new ItemStack(Material.PORK, 1));
        slr.addIngredient(Material.ROTTEN_FLESH).addIngredient(Material.GOLD_NUGGET).addIngredient(Material.WATER_BUCKET);
        getServer().addRecipe(slr);
        ItemStack chargedCompass = new ItemStack(Config.radioMaterial, 1);
        ItemMeta im = chargedCompass.getItemMeta();
        im.setLore(Arrays.asList(ChatColor.GRAY + "Battery: 100%"));
        chargedCompass.setItemMeta(im);
        batteryRefill = new ShapelessRecipe(chargedCompass);
        batteryRefill.addIngredient(Config.radioMaterial);
        batteryRefill.addIngredient(Config.radioBatteryMaterial);
        getServer().addRecipe(batteryRefill);
        slr = new ShapelessRecipe(new ItemStack(Material.GLOWSTONE_DUST));
        slr.addIngredient(Material.INK_SACK, 15).addIngredient(Material.TORCH);
        getServer().addRecipe(slr);
        waterBottle = new ShapelessRecipe(new ItemStack(Material.GLASS_BOTTLE));
        waterBottle.addIngredient(Material.POTION, 0);
        getServer().addRecipe(waterBottle);
        slr = new ShapelessRecipe(new ItemStack(Material.MELON, 1, (short) 14));
        slr.addIngredient(Material.INK_SACK, 2);
        slr.addIngredient(Material.INK_SACK, 2);
        slr.addIngredient(Material.PAPER);
        getServer().addRecipe(slr);
        slr = new ShapelessRecipe(new ItemStack(Material.SNOW_BALL, 1));
        slr.addIngredient(Material.SULPHUR).addIngredient(Material.EGG).addIngredient(Material.FLINT);
        getServer().addRecipe(slr);
    }

    @Override
    public void onEnable() {
        dataFolder = getDataFolder();

        c = new Config(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SurvivorsListener(this), this);

        BukkitScheduler bs = getServer().getScheduler();
        bs.runTaskTimer(this, new BatteryRunner(this), Config.batteryDrainInterval * 60L * 20L, Config.batteryDrainInterval * 60L * 20L);
        bs.runTaskTimer(this, new CompassUpdater(this), 0L, Config.gpsUpdateInterval * 20L);

        addRecipes();

        registerCommand(new CmdRadio(this), "radio", this);
        registerCommand(new CmdSurvivors(this), "survivors", this);
        registerCommand(new CmdGPS(this), "gps", this);
    }

    @Override
    public void onDisable() {
        if (!Config.saveUDOnChange) for (PConfManager pcm : pconfs.values()) pcm.forceSave();
    }

}
