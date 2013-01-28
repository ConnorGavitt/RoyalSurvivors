package org.royaldev.royalsurvivors.runners;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.PConfManager;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class ThirstUpdater implements Runnable {

    private final RoyalSurvivors plugin;

    public ThirstUpdater(RoyalSurvivors instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            if (p.isDead()) continue;
            PConfManager pcm = plugin.getUserdata(p);
            Float f = pcm.getFloat("thirst");
            if (f == null) {
                f = 1F;
                p.setExp(f);
                pcm.setFloat(f, "thirst");
                continue;
            }
            f -= Config.thirstPercent / 100F;
            p.setExp(f);
            pcm.setFloat(f, "thirst");
            if (f <= 0F) {
                p.setHealth(0);
                p.sendMessage(ChatColor.BLUE + "You have died of dehydration.");
            }
        }
    }

}
