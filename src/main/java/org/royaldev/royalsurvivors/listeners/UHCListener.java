package org.royaldev.royalsurvivors.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.RUtils;

import java.util.Random;

public class UHCListener implements Listener {

    private final Random r = new Random();

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (!Config.uhcRegenOff || !(e.getEntity() instanceof Player)) return;
        final Player p = (Player) e.getEntity();
        if (!RUtils.isInInfectedWorld(p)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent e) {
        if (!Config.uhcGoldDrops) return;
        Entity ent = e.getEntity();
        if (!RUtils.isInInfectedWorld(ent) || !(ent instanceof Zombie)) return;
        if (r.nextDouble() * 100D > Config.uhcGoldDropsChance) return;
        e.getDrops().add(new ItemStack(Material.GOLD_INGOT, r.nextInt(4)));
    }

}
