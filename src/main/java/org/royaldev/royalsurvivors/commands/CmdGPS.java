package org.royaldev.royalsurvivors.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalsurvivors.PConfManager;
import org.royaldev.royalsurvivors.RUtils;
import org.royaldev.royalsurvivors.RoyalSurvivors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmdGPS implements CommandExecutor {

    private final RoyalSurvivors plugin;

    public CmdGPS(RoyalSurvivors instance) {
        plugin = instance;
    }

    private boolean hasGPS(Player p) {
        return p.getInventory().contains(Material.COMPASS);
    }

    // Map<Requester, List<Requests>>
    private final Map<String, List<String>> linkRequests = new HashMap<String, List<String>>();

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("gps")) {
            if (!cs.hasPermission("rsurv.gps")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command is only available to players!");
                return true;
            }
            Player p = (Player) cs;
            if (!hasGPS(p)) {
                cs.sendMessage(ChatColor.BLUE + "You don't have a GPS.");
                return true;
            }
            if (args.length < 1) {
                cs.sendMessage(ChatColor.BLUE + "You look at your GPS.");
                return true;
            }
            PConfManager pcm = plugin.getUserdata(p);
            String subcommand = args[0];
            if (subcommand.equalsIgnoreCase("here")) {
                p.setCompassTarget(p.getLocation());
                p.sendMessage(ChatColor.BLUE + "Your GPS is now pointing at this position.");
                pcm.set("gps.points.type", "static");
                pcm.setLocation("gps.points.static", p.getLocation());
            } else if (subcommand.equalsIgnoreCase("link")) {
                if (args.length < 2) {
                    p.sendMessage(ChatColor.BLUE + "Your GPS needs to know who to link with.");
                    return true;
                }
                Player t = plugin.getServer().getPlayer(args[1]);
                if (t == null || !hasGPS(t)) {
                    p.sendMessage(ChatColor.BLUE + "That GPS' signal cannot be found.");
                    return true;
                }
                Boolean isLinked = pcm.getBoolean("gps.links." + t.getName());
                if (isLinked == null) isLinked = false;
                if (isLinked) {
                    p.sendMessage(ChatColor.BLUE + "Your GPS is already linked to that signal.");
                    return true;
                }
                List<String> theirRequests = linkRequests.get(t.getName());
                if (theirRequests != null) {
                    if (theirRequests.contains(p.getName())) {
                        plugin.getUserdata(p.getName()).set("gps.links." + t.getName(), true);
                        plugin.getUserdata(t.getName()).set("gps.links." + p.getName(), true);
                        List<String> requests = linkRequests.get(p.getName());
                        if (requests != null) {
                            requests.remove(t.getName());
                            linkRequests.put(p.getName(), requests);
                        }
                        requests = linkRequests.get(t.getName());
                        if (requests != null) {
                            requests.remove(p.getName());
                            linkRequests.put(t.getName(), requests);
                        }
                        p.sendMessage(ChatColor.BLUE + "Your GPS is now linked.");
                        t.sendMessage(ChatColor.BLUE + "Your GPS beeps; your link request with " + ChatColor.GRAY + p.getName() + ChatColor.BLUE + " has been accepted.");
                        return true;
                    }
                }
                List<String> requests = linkRequests.get(p.getName());
                if (requests == null) requests = new ArrayList<String>();
                if (requests.contains(t.getName())) {
                    p.sendMessage(ChatColor.BLUE + "Your GPS has already sent a link request to that signal.");
                    return true;
                }
                requests.add(t.getName());
                linkRequests.put(p.getName(), requests);
                p.sendMessage(ChatColor.BLUE + "Your GPS has requested a link to that signal.");
                t.sendMessage(ChatColor.BLUE + "Your GPS beeps; it has a new link request from " + ChatColor.GRAY + p.getName() + ChatColor.BLUE + ".");
            } else if (subcommand.equalsIgnoreCase("sethome")) {
                p.sendMessage(ChatColor.BLUE + "Your GPS has saved this location as your home.");
                pcm.setLocation("gps.home", p.getLocation());
            } else if (subcommand.equalsIgnoreCase("home")) {
                Location l = pcm.getLocation("gps.home");
                if (l == null) {
                    p.sendMessage(ChatColor.BLUE + "Your have not set a home on your GPS.");
                    return true;
                }
                p.sendMessage(ChatColor.BLUE + "Your GPS is now pointing at your saved home.");
                p.setCompassTarget(l);
                pcm.set("gps.points.type", "home");
            } else if (subcommand.equalsIgnoreCase("player")) {
                if (args.length < 2) {
                    p.sendMessage(ChatColor.BLUE + "Your GPS needs a signal to point towards.");
                    return true;
                }
                Player t = plugin.getServer().getPlayer(args[1]);
                if (t == null || !hasGPS(t)) {
                    p.sendMessage(ChatColor.BLUE + "That GPS' signal could not be found.");
                    return true;
                }
                if (!pcm.getBoolean("gps.links." + t.getName())) {
                    p.sendMessage(ChatColor.BLUE + "Your GPS is not linked to that signal.");
                    return true;
                }
                pcm.set("gps.points.type", "player");
                pcm.set("gps.points.playername", t.getName());
                p.setCompassTarget(t.getLocation());
                p.sendMessage(ChatColor.BLUE + "Your GPS is now pointing towards that signal.");
            } else if (subcommand.equalsIgnoreCase("coord") || subcommand.equalsIgnoreCase("coords") || subcommand.equalsIgnoreCase("coordinate") || subcommand.equalsIgnoreCase("coordinates")) {
                if (args.length < 4) {
                    p.sendMessage(ChatColor.BLUE + "The GPS must be told three coordinates.");
                    return true;
                }
                double x, y, z;
                try {
                    x = Double.parseDouble(args[1]);
                    y = Double.parseDouble(args[2]);
                    z = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.BLUE + "The coordinates were invalid.");
                    return true;
                }
                Location pointTo = new Location(p.getWorld(), x, y, z);
                pcm.set("gps.points.type", "location");
                pcm.setLocation("gps.points.location", pointTo);
                p.setCompassTarget(pointTo);
                p.sendMessage(ChatColor.BLUE + "Your GPS is now pointing towards those coordinates.");
            } else p.sendMessage(ChatColor.BLUE + "Nothing happens...");
            return true;
        }
        return false;
    }

}
