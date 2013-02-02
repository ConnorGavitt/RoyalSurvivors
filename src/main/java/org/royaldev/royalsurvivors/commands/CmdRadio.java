package org.royaldev.royalsurvivors.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.PConfManager;
import org.royaldev.royalsurvivors.RUtils;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class CmdRadio implements CommandExecutor {

    private final RoyalSurvivors plugin;

    public CmdRadio(RoyalSurvivors instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("radio")) {
            if (!cs.hasPermission("rsurv.radio")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command is only available to players!");
                return true;
            }
            Player p = (Player) cs;
            Material radio;
            try {
                radio = Material.valueOf(Config.radioItem.toUpperCase());
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            if (!p.getInventory().contains(radio)) {
                cs.sendMessage(ChatColor.BLUE + "You don't have your radio.");
                return true;
            }
            PConfManager pcm = plugin.getUserdata(p);
            Boolean isOn = pcm.getBoolean("radio.on");
            if (isOn == null) isOn = false;
            if (args.length < 1) {
                if (isOn) {
                    p.sendMessage(ChatColor.BLUE + "You turn off your radio.");
                } else {
                    Object percent = pcm.get("radio.battery");
                    if (percent == null) percent = "100";
                    Integer percentage = Integer.valueOf(percent.toString());
                    if (percentage == null) percentage = 100;
                    if (percentage < 1 && Config.useRadioBattery) {
                        cs.sendMessage(ChatColor.BLUE + "You try to turn on your radio, but it seems to be out of battery.");
                        return true;
                    }
                    String channel = pcm.getString("radio.channel");
                    if (channel == null)
                        p.sendMessage(ChatColor.BLUE + "You turn on your radio, but all you hear is fuzz. You haven't set it to a channel yet.");
                    else p.sendMessage(ChatColor.BLUE + "You turn on your radio.");
                }
                pcm.set("radio.on", !isOn);
                return true;
            }
            String channel = args[0].toLowerCase();
            pcm.set("radio.channel", channel);
            p.sendMessage(ChatColor.BLUE + "You set your radio to channel " + ChatColor.GRAY + channel + ChatColor.BLUE + ".");
            return true;
        }
        return false;
    }

}
