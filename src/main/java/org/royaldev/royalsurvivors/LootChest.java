package org.royaldev.royalsurvivors;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootChest {

    public class Loot {

        private final ItemStack base;
        private int chance;

        private Loot(ItemStack is) {
            base = is;
            chance = 100;
        }

        private Loot(ItemStack is, int chance) {
            base = is;
            if (chance < 1) chance = 100;
            this.chance = chance;
        }

        public void setChance(int chance) {
            this.chance = chance;
        }

        public ItemStack getBase() {
            return base;
        }

        public int getChance() {
            return chance;
        }
    }

    private final List<Loot> contents = new ArrayList<Loot>();
    private final Random r = new Random();

    public static LootChest getLootChest(String name) {
        try {
            return new LootChest(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int getRandIntBetween(int start, int end) {
        return r.nextInt(end - start + 1) + start;
    }

    private LootChest(String name) {
        FileConfiguration config = RoyalSurvivors.instance.getConfig();
        if (!config.isSet("loot_chests." + name)) throw new IllegalArgumentException("No such loot chest!");
        ConfigurationSection cs = config.getConfigurationSection("loot_chests." + name);
        for (String itemName : cs.getKeys(false)) {
            if (itemName == null) continue;
            ConfigurationSection item = cs.getConfigurationSection(itemName);
            Material m;
            try {
                m = Material.valueOf(itemName.toUpperCase());
            } catch (IllegalArgumentException e) {
                try {
                    m = Material.getMaterial(parseAmount(itemName));
                } catch (Exception ex) {
                    continue;
                }
            }
            ItemStack is = new ItemStack(m);
            setAmount(item, is);
            for (ItemStack iis : getLargestStacks(is)) {
                setDurability(item, iis);
                setName(item, iis);
                setLore(item, iis);
                setEnchantments(item, iis);
                synchronized (contents) {
                    contents.add(new Loot(iis, parseAmount(item.getString("chance", "100"))));
                }
            }
        }
    }

    private int parseAmount(String amount) {
        if (amount == null) return 0;
        String[] parts = amount.split("\\-");
        if (parts.length < 2) {
            if (parts.length > 0) {
                try {
                    return Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            } else return -1;
        }
        int start;
        int end;
        try {
            start = Integer.parseInt(parts[0]);
            end = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
        return getRandIntBetween(start, end);
    }

    private void setAmount(ConfigurationSection item, ItemStack is) {
        is.setAmount((item.isSet("amount")) ? parseAmount(item.getString("amount")) : 1);
    }

    private List<ItemStack> getLargestStacks(ItemStack is) {
        List<ItemStack> shrunk = new ArrayList<ItemStack>();
        if (is.getAmount() <= is.getMaxStackSize()) {
            shrunk.add(is.clone());
            return shrunk;
        }
        if (is.getMaxStackSize() == 1) {
            for (int i = 0; i < is.getAmount(); i++) {
                ItemStack nis = new ItemStack(is).clone();
                nis.setAmount(1);
                shrunk.add(nis);
            }
            return shrunk;
        }
        ItemStack nis = new ItemStack(is);
        nis.setAmount(is.getAmount() % is.getMaxStackSize());
        if (nis.getAmount() != 0) shrunk.add(nis);
        for (int i = 0; i < is.getAmount() / is.getMaxStackSize(); i++) {
            nis.setAmount(is.getMaxStackSize());
            shrunk.add(nis.clone());
        }
        return shrunk;
    }

    private void setEnchantments(ConfigurationSection item, ItemStack is) {
        if (!item.isSet("enchantments")) return;
        List<String> enchants = item.getStringList("enchantments");
        for (String enchant : enchants) {
            String[] parts = enchant.split(":");
            if (parts.length < 3) continue;
            Enchantment e;
            if (parts[0].equalsIgnoreCase("random")) {
                if (ZombieSpawner.getEquipmentType(is) != null)
                    e = ZombieSpawner.getRandomEnchantment(ZombieSpawner.getEquipmentType(is));
                else continue;
            } else {
                try {
                    e = Enchantment.getByName(parts[0]);
                } catch (Exception ex) {
                    continue;
                }
            }
            int level = parseAmount(parts[1]);
            if (level == -1) level = getRandIntBetween(1, e.getMaxLevel());
            if (parseAmount(parts[2]) > r.nextInt(100)) is.addUnsafeEnchantment(e, level);
        }
    }

    private void setLore(ConfigurationSection item, ItemStack is) {
        if (!item.isSet("lore")) return;
        ItemMeta im = is.getItemMeta();
        List<String> actualLore = im.getLore();
        if (actualLore == null) actualLore = new ArrayList<String>();
        for (String lore : item.getStringList("lore")) actualLore.add(RUtils.colorize(lore));
        im.setLore(actualLore);
        is.setItemMeta(im);
    }

    private void setName(ConfigurationSection item, ItemStack is) {
        if (!item.isSet("name")) return;
        String name = item.getString("name");
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(RUtils.colorize(name));
        is.setItemMeta(im);
    }

    private void setDurability(ConfigurationSection item, ItemStack is) {
        if (!item.isSet("durability")) return;
        short durability = (short) parseAmount(item.getString("durability"));
        if (durability == (short) -1) durability = (short) getRandIntBetween(0, is.getType().getMaxDurability());
        is.setDurability(durability);
    }

    public List<Loot> getContents() {
        List<Loot> tempContents = new ArrayList<Loot>();
        synchronized (contents) {
            tempContents.addAll(contents);
        }
        return tempContents;
    }

    /**
     * Gets a list of ItemStacks generated from the list of allowed items. Chance is already factored into this method,
     * so this list can be put into a chest as it is.
     *
     * @return List of ItemStacks
     */
    public List<ItemStack> getRandomLoot() {
        final List<ItemStack> random = new ArrayList<ItemStack>();
        for (Loot l : getContents()) {
            if (l.getChance() > r.nextInt(100)) random.add(l.getBase());
        }
        return random;
    }

}
