package org.royaldev.royalsurvivors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
        return e.getWorld().getName().equalsIgnoreCase(Config.worldToUse);
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

}
