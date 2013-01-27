package org.royaldev.royalsurvivors.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.royaldev.royalsurvivors.RUtils;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class CmdSurvivors implements CommandExecutor {

    private final RoyalSurvivors plugin;

    public CmdSurvivors(RoyalSurvivors instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("survivors")) {
            if (!cs.hasPermission("rsurv.survivors")) {
                RUtils.dispNoPerms(cs, true);
                return true;
            }
            if (args.length < 1 || args[0].equalsIgnoreCase("reload")) {
                plugin.c.reloadConfig();
                cs.sendMessage(ChatColor.BLUE + plugin.getDescription().getName() + ChatColor.GRAY + " v" + plugin.getDescription().getVersion() + ChatColor.BLUE + " reloaded.");
                return true;
            }
            cs.sendMessage(ChatColor.RED + "Invalid subcommand.");
            return true;
        }
        return false;
    }

}
