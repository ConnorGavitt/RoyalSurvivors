package org.royaldev.royalsurvivors.runners;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.configuration.PConfManager;
import org.royaldev.royalsurvivors.RoyalSurvivors;

import java.util.List;

public class ZombieSpray implements Runnable {

    private final RoyalSurvivors plugin;

    public ZombieSpray(RoyalSurvivors instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        if (!Config.toxicEnabled) return;
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            PConfManager pcm = PConfManager.getPConfManager(p);
            if (!pcm.getBoolean("toxicspray_on", false)) continue;
            if (pcm.getLong("toxicspray_expire", 0L) <= System.currentTimeMillis()) {
                pcm.set("toxicspray_on", false);
                pcm.set("toxicspray_expire", null);
                p.sendMessage(ChatColor.BLUE + "The toxic fumes wear off.");
                continue;
            }
            List<Entity> ents = p.getNearbyEntities(Config.toxicRadius, Config.toxicRadius, Config.toxicRadius);
            for (Entity e : ents) {
                if (e.getType() != EntityType.ZOMBIE) continue;
                Zombie z = (Zombie) e;
                z.damage(z.getMaxHealth() / 2);
            }
        }
    }

}
