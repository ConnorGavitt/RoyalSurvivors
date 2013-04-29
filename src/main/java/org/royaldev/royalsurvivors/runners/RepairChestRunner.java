package org.royaldev.royalsurvivors.runners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.royaldev.royalsurvivors.ConfManager;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class RepairChestRunner implements Runnable {

    private final RoyalSurvivors plugin;

    public RepairChestRunner(RoyalSurvivors instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        ConfManager cm = ConfManager.getConfManager("otherdata.yml");
        ConfigurationSection cs = cm.getConfigurationSection("repairchests");
        if (cs == null) return;
        for (String key : cs.getKeys(false)) {
            if (!cs.getBoolean(key + ".enabled", false)) continue;
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
            if (!(b.getState() instanceof Chest)) {
                b.setType(Material.AIR);
                b.setType(Material.CHEST);
            }
            Chest c = (Chest) b.getState();
            Inventory i = c.getInventory();
            for (ItemStack is : i) {
                if (is == null) continue;
                short durability = is.getDurability();
                if (durability == (short) 0) continue;
                durability -= Config.repairChestRepairAmount;
                if (durability < (short) 0) durability = (short) 0;
                is.setDurability(durability);
            }
        }
    }
}
