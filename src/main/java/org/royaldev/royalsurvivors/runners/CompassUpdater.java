package org.royaldev.royalsurvivors.runners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.PConfManager;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class CompassUpdater implements Runnable {

    private final RoyalSurvivors plugin;

    public CompassUpdater(RoyalSurvivors instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            if (!p.getInventory().contains(Material.COMPASS)) continue;
            PConfManager pcm = PConfManager.getPConfManager(p);
            String compassType = pcm.getString("gps.points.type");
            if (compassType == null) continue;
            if (compassType.equalsIgnoreCase("player")) {
                String name = pcm.getString("gps.points.playername");
                if (name == null) continue;
                Player t = plugin.getServer().getPlayer(name);
                if (t == null || !t.getInventory().contains(Material.COMPASS)) continue;
                p.setCompassTarget(t.getLocation());
            } else if (compassType.equalsIgnoreCase("static")) {
                Location l = pcm.getLocation("gps.points.static");
                if (l == null) continue;
                p.setCompassTarget(l);
            } else if (compassType.equalsIgnoreCase("home")) {
                Location l = pcm.getLocation("gps.home");
                if (l == null) continue;
                p.setCompassTarget(l);
            } else if (compassType.equalsIgnoreCase("location")) {
                Location pointAt = pcm.getLocation("gps.points.location");
                if (pointAt == null) continue;
                p.setCompassTarget(pointAt);
            }
        }
    }

}
