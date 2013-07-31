package org.royaldev.royalsurvivors.runners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.royaldev.royalsurvivors.configuration.ConfManager;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class DeathChestRemover implements Runnable {

    private final RoyalSurvivors plugin;

    public DeathChestRemover(RoyalSurvivors instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        ConfManager cm = ConfManager.getConfManager("otherdata.yml");
        ConfigurationSection cs = cm.getConfigurationSection("deathchests");
        if (cs == null) return;
        for (String key : cs.getKeys(false)) {
            if (!cs.getBoolean(key, false)) continue;
            String[] coords = key.split(",");
            if (coords.length < 4) continue; // world,x,y,z = 4
            if (!coords[0].equals(w.getName())) continue;
            int x, y, z;
            try {
                x = Integer.parseInt(coords[1]);
                y = Integer.parseInt(coords[2]);
                z = Integer.parseInt(coords[3]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            Location l = new Location(w, x, y, z);
            Block b = l.getBlock();
            if (!(b.getState() instanceof Chest)) continue;
            Chest c = (Chest) b.getState();
            Inventory i = c.getInventory();
            boolean empty = true;
            for (ItemStack is : i.getContents()) {
                if (is == null) continue;
                if (is.getType() == Material.AIR) continue;
                empty = false;
            }
            if (!empty) continue;
            b.setType(Material.AIR);
            cm.set("deathchests." + key, null);
        }
    }

}
