package org.royaldev.royalsurvivors;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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
        return message.replaceAll("(?i)&([a-f0-9k-or])", "\u00a7$1");
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

}
