package org.royaldev.royalsurvivors;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class RUtils {

    public static void dispNoPerms(CommandSender cs) {
        dispNoPerms(cs, false);
    }

    public static void dispNoPerms(CommandSender cs, boolean toAdmin) {
        if (!toAdmin) cs.sendMessage(ChatColor.BLUE + "Nothing happens...");
        else cs.sendMessage(ChatColor.RED + "You don't have permission for that!");
    }

    /**
     * Converts color codes to processed codes
     *
     * @param message Message with raw color codes
     * @return String with processed colors
     */
    public static String colorize(final String message) {
        if (message == null) return null;
        return message.replaceAll("(?i)&([a-f0-9k-or])", ChatColor.COLOR_CHAR + "$1");
    }

    /**
     * Removes color codes that have not been processed yet (&char)
     * <p/>
     * This fixes a common exploit where color codes can be embedded into other codes:
     * &&aa (replaces &a, and the other letters combine to make &a again)
     *
     * @param message String with raw color codes
     * @return String without raw color codes
     */
    public static String decolorize(String message) {
        Pattern p = Pattern.compile("(?i)&[a-f0-9k-or]");
        boolean contains = p.matcher(message).find();
        while (contains) {
            message = message.replaceAll("(?i)&[a-f0-9k-or]", "");
            contains = p.matcher(message).find();
        }
        return message;
    }

    public static boolean isInInfectedWorld(Entity e) {
        return isInInfectedWorld(e.getLocation());
    }

    public static boolean isInInfectedWorld(Location l) {
        return l.getWorld().getName().equalsIgnoreCase(Config.worldToUse);
    }

    public static boolean isOnLadder(Location l) {
        return l.getBlock().getType() == Material.LADDER;
    }

    public static boolean isOnLadder(Player p) {
        return isOnLadder(p.getLocation());
    }

    private static final Material[] noShade = new Material[]{
            Material.FENCE,
            Material.FENCE_GATE,
            Material.GLASS,
            Material.ICE,
            Material.IRON_FENCE,
            Material.LADDER,
            Material.NETHER_FENCE,
            Material.STONE_BUTTON,
            Material.THIN_GLASS,
            Material.WEB,
            Material.WOOD_BUTTON
    };

    public static boolean isInShade(Entity e) {
        Location l = (e instanceof LivingEntity) ? ((LivingEntity) e).getEyeLocation() : e.getLocation();
        Block b = l.getWorld().getHighestBlockAt(l);
        boolean providesShade = !ArrayUtils.contains(noShade, b.getType());
        boolean isAbovePlayer = l.getY() < b.getY();
        return providesShade && isAbovePlayer;
    }

    public static Command getCommand(String name) {
        try {
            final Field map = RoyalSurvivors.instance.getServer().getPluginManager().getClass().getDeclaredField("commandMap");
            map.setAccessible(true);
            final CommandMap cm = (CommandMap) map.get(RoyalSurvivors.instance.getServer().getPluginManager());
            return cm.getCommand(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
