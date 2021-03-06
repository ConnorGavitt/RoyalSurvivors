package org.royaldev.royalsurvivors.runners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.configuration.PConfManager;
import org.royaldev.royalsurvivors.RoyalSurvivors;

import java.util.Arrays;
import java.util.List;

public class BatteryRunner implements Runnable {

    private final RoyalSurvivors plugin;

    public BatteryRunner(RoyalSurvivors instance) {
        plugin = instance;
    }

    private boolean hasRadio(Player p) {
        Material radio;
        try {
            radio = Material.valueOf(Config.radioItem.toUpperCase());
        } catch (Exception ex) {
            return false;
        }
        return p.getInventory().contains(radio);
    }

    private int getRadioSlot(Player p) {
        for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
            ItemStack is = p.getInventory().getItem(slot);
            if (is == null || is.getType() != Config.radioMaterial) continue;
            return slot;
        }
        return -1;
    }

    @Override
    public void run() {
        if (!Config.useRadioBattery) return;
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            PConfManager pcm = PConfManager.getPConfManager(p);
            if (!pcm.getBoolean("radio.on", false)) continue;
            if (!hasRadio(p)) continue;
            int slot = getRadioSlot(p);
            if (slot < 0) continue;
            ItemStack is = p.getInventory().getItem(slot);
            ItemMeta im = is.getItemMeta();
            int percentage = pcm.getInt("radio.battery", 100);
            if (percentage < 1) continue; // :(
            percentage -= Config.batteryDrainAmount;
            if (percentage < 1) {
                percentage = 0;
                p.sendMessage(ChatColor.BLUE + "Your radio flickers off.");
                pcm.set("radio.on", false);
                pcm.set("radio.battery", percentage);
                List<String> lore = im.getLore();
                lore.set(0, ChatColor.GRAY + "Battery: " + percentage + "%");
                im.setLore(lore);
                is.setItemMeta(im);
                p.getInventory().setItem(slot, is);
                pcm.set("radio.battery", percentage);
                continue;
            }
            if (!im.hasLore()) im.setLore(Arrays.asList(ChatColor.GRAY + "Battery: " + percentage + "%"));
            else {
                List<String> lore = im.getLore();
                lore.set(0, ChatColor.GRAY + "Battery: " + percentage + "%");
                im.setLore(lore);
            }
            is.setItemMeta(im);
            p.getInventory().setItem(slot, is);
            pcm.set("radio.battery", percentage);
        }
    }

}
