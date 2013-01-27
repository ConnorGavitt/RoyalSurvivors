package org.royaldev.royalsurvivors;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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

}
