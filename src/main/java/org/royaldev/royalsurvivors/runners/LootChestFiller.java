package org.royaldev.royalsurvivors.runners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.royaldev.royalsurvivors.configuration.ConfManager;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.LootChest;
import org.royaldev.royalsurvivors.RoyalSurvivors;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootChestFiller implements Runnable {

    private final RoyalSurvivors plugin;

    public LootChestFiller(RoyalSurvivors instance) {
        plugin = instance;
    }

    private Inventory shuffleInventory(Inventory i) {
        List<Integer> unusedSlots = new ArrayList<Integer>();
        for (int slot = 0; slot < i.getSize(); slot++) {
            unusedSlots.add(slot);
        }
        ItemStack[] contents = i.getContents().clone();
        i.clear();
        for (ItemStack is : contents) {
            int slot = unusedSlots.get(new Random().nextInt(unusedSlots.size()));
            i.setItem(slot, is);
            unusedSlots.remove(Integer.valueOf(slot));
        }
        return i;
    }

    @Override
    public void run() {
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        ConfManager cm = ConfManager.getConfManager("otherdata.yml");
        ConfigurationSection cs = cm.getConfigurationSection("lootchests");
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
            boolean empty = true;
            for (ItemStack is : i.getContents()) {
                if (is == null) continue;
                if (is.getType() == Material.AIR) continue;
                empty = false;
            }
            if (!empty && !Config.refillIfNotEmpty) continue;
            if (!c.getInventory().getViewers().isEmpty()) continue; // don't regen when people are looking
            if (Config.lootChestRadius > -1) {
                boolean noPlayers = true;
                for (Player p : w.getPlayers()) {
                    double distance = p.getLocation().distanceSquared(b.getLocation());
                    if (distance <= Math.pow(Config.lootChestRadius, 2)) { // if a player is inside radius
                        noPlayers = false;
                        break;
                    }
                }
                if (!noPlayers) continue;
            }
            if (!cs.isSet(key + ".emptiedat")) cs.set(key + ".emptiedat", System.currentTimeMillis());
            long emptiedAt = cs.getLong(key + ".emptiedat");
            long refill = cs.getInt(key + ".refill") * 60000L; // minutes to milliseconds
            if (emptiedAt + refill > System.currentTimeMillis()) continue; // if it's not refill time, yet
            final LootChest lc = LootChest.getLootChest(cs.getString(key + ".loot_chest"));
            if (lc == null) continue;
            c.getInventory().clear();
            for (ItemStack is : lc.getRandomLoot()) c.getInventory().addItem(is);
            shuffleInventory(c.getInventory());
            cs.set(key + ".emptiedat", null);
            RoyalSurvivors.debugStatic("Refilled chest at " + c.getLocation());
        }
    }

}
