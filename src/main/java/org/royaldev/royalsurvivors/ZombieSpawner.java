package org.royaldev.royalsurvivors;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ZombieSpawner {

    private enum EquipmentType {
        BOOTS, BOW, CHESTPLATE, HELM, LEGGINGS, WEAPON
    }

    private static final Object spawnLock = new Object();

    private static Enchantment[] concat(Enchantment[] a, Enchantment[] b) {
        int aLen = a.length;
        int bLen = b.length;
        Enchantment[] c = new Enchantment[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    private static final Enchantment[] allEnchants = new Enchantment[]{Enchantment.THORNS, Enchantment.PROTECTION_ENVIRONMENTAL, Enchantment.PROTECTION_EXPLOSIONS, Enchantment.PROTECTION_FIRE, Enchantment.PROTECTION_PROJECTILE, Enchantment.DURABILITY};
    private static final Map<EquipmentType, Enchantment[]> enchants = new HashMap<EquipmentType, Enchantment[]>() {
        {
            put(EquipmentType.HELM, concat(new Enchantment[]{Enchantment.WATER_WORKER, Enchantment.OXYGEN}, allEnchants));
            put(EquipmentType.CHESTPLATE, allEnchants);
            put(EquipmentType.LEGGINGS, allEnchants);
            put(EquipmentType.BOOTS, concat(new Enchantment[]{Enchantment.PROTECTION_FALL}, allEnchants));
            put(EquipmentType.WEAPON, new Enchantment[]{Enchantment.DAMAGE_ALL, Enchantment.DAMAGE_ARTHROPODS, Enchantment.DAMAGE_UNDEAD, Enchantment.KNOCKBACK, Enchantment.FIRE_ASPECT, Enchantment.LOOT_BONUS_MOBS});
            put(EquipmentType.BOW, new Enchantment[]{Enchantment.ARROW_DAMAGE, Enchantment.ARROW_FIRE, Enchantment.ARROW_INFINITE, Enchantment.ARROW_KNOCKBACK});

        }
    };
    private static final Material[] helms = new Material[]{Material.LEATHER_HELMET, Material.IRON_HELMET, Material.GOLD_HELMET, Material.DIAMOND_HELMET, Material.CHAINMAIL_HELMET};
    private static final Material[] chestplates = new Material[]{Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLD_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE};
    private static final Material[] leggings = new Material[]{Material.LEATHER_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLD_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.CHAINMAIL_LEGGINGS};
    private static final Material[] boots = new Material[]{Material.LEATHER_BOOTS, Material.IRON_BOOTS, Material.GOLD_BOOTS, Material.DIAMOND_BOOTS, Material.CHAINMAIL_BOOTS};
    private static final Material[] weapon = new Material[]{Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, Material.DIAMOND_SWORD};

    private final static Random r = new Random();

    private static int nextInt(int start, int end) {
        return r.nextInt(end - start + 1) + start;
    }

    private static int getNumberMobs(World w) {
        List<Entity> ents = w.getEntities();
        w.getEntities().removeAll(w.getPlayers());
        return ents.size();
    }

    private static Zombie spawnZombieBase(Location l) {
        synchronized (spawnLock) {
            World w = l.getWorld();
            if (getNumberMobs(w) > Config.maxMobs && Config.maxMobs > -1) {
                if (!Config.removeIfMax) return null;
                for (Entity e : w.getEntities()) {
                    if (!(e instanceof Zombie) || e instanceof Player) continue;
                    e.remove();
                    break;
                }
            }
            return (Zombie) w.spawnEntity(l, EntityType.ZOMBIE);
        }
    }

    private static PotionEffectType[] allowedEffects = new PotionEffectType[]{
            PotionEffectType.DAMAGE_RESISTANCE,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.INCREASE_DAMAGE,
            PotionEffectType.INVISIBILITY,
            PotionEffectType.JUMP,
            PotionEffectType.REGENERATION,
            PotionEffectType.SLOW,
            PotionEffectType.SPEED,
            PotionEffectType.WEAKNESS
    };

    /**
     * Applies 0 - 2 potion effects on a zombie.
     * <p/>
     * These effects last for Integer.MAX_VALUE ticks (~3.4 years).
     *
     * @param z Zombie to apply effects to.
     */
    private static void applyPotionEffects(Zombie z) {
        for (int i = 0; i < nextInt(0, 2); i++) {
            RoyalSurvivors.debugStatic("effect applied, bro!");
            RoyalSurvivors.debugStatic("Location: " + z.getLocation());
            PotionEffectType pet = allowedEffects[r.nextInt(allowedEffects.length)];
            int amplifier = r.nextInt(3);
            RoyalSurvivors.debugStatic("pet: " + pet);
            RoyalSurvivors.debugStatic("amplifier: " + amplifier);
            z.addPotionEffect(new PotionEffect(pet, Integer.MAX_VALUE, amplifier));
        }
    }

    private static ItemStack getRandomItemStack(EquipmentType et) {
        ItemStack is;
        Enchantment ench;
        int amount = r.nextInt(4);
        switch (et) {
            case HELM:
                is = new ItemStack(helms[r.nextInt(helms.length)], 1);
                for (int i = 0; i < amount; i++) {
                    ench = enchants.get(et)[r.nextInt(enchants.get(et).length)];
                    is.addEnchantment(ench, nextInt(1, ench.getMaxLevel()));
                }
                break;
            case CHESTPLATE:
                is = new ItemStack(chestplates[r.nextInt(chestplates.length)], 1);
                for (int i = 0; i < amount; i++) {
                    ench = enchants.get(et)[r.nextInt(enchants.get(et).length)];
                    is.addEnchantment(ench, nextInt(1, ench.getMaxLevel()));
                }
                break;
            case LEGGINGS:
                is = new ItemStack(leggings[r.nextInt(leggings.length)], 1);
                for (int i = 0; i < amount; i++) {
                    ench = enchants.get(et)[r.nextInt(enchants.get(et).length)];
                    is.addEnchantment(ench, nextInt(1, ench.getMaxLevel()));
                }
                break;
            case BOOTS:
                is = new ItemStack(boots[r.nextInt(boots.length)], 1);
                for (int i = 0; i < amount; i++) {
                    ench = enchants.get(et)[r.nextInt(enchants.get(et).length)];
                    is.addEnchantment(ench, nextInt(1, ench.getMaxLevel()));
                }
                break;
            case WEAPON:
                is = new ItemStack(weapon[r.nextInt(weapon.length)], 1);
                for (int i = 0; i < amount; i++) {
                    ench = enchants.get(et)[r.nextInt(enchants.get(et).length)];
                    is.addEnchantment(ench, nextInt(1, ench.getMaxLevel()));
                }
                break;
            default:
                return null;
        }
        return is;
    }

    private static void setEntityEquipment(EntityEquipment ee, int level) {
        ee.setHelmetDropChance(r.nextFloat());
        ee.setChestplateDropChance(r.nextFloat());
        ee.setLeggingsDropChance(r.nextFloat());
        ee.setBootsDropChance(r.nextFloat());
        ee.setItemInHandDropChance(r.nextFloat());
        switch (level) {
            case 1:
                // 4% chance
                if (nextInt(1, 25) == 10) ee.setChestplate(getRandomItemStack(EquipmentType.LEGGINGS));
                break;
            case 2:
                // 8.3% chance
                if (nextInt(1, 12) == 8) {
                    ee.setChestplate(getRandomItemStack(EquipmentType.LEGGINGS));
                    ee.setItemInHand(getRandomItemStack(EquipmentType.WEAPON));
                }
                break;
            case 3:
                // 14.29% chance
                if (nextInt(1, 7) == 4) {
                    ee.setChestplate(getRandomItemStack(EquipmentType.CHESTPLATE));
                    ee.setItemInHand(getRandomItemStack(EquipmentType.WEAPON));
                    ee.setLeggings(getRandomItemStack(EquipmentType.LEGGINGS));
                }
                break;
            case 4:
                // 33.3% chance
                if (nextInt(1, 3) == 2) {
                    ee.setChestplate(getRandomItemStack(EquipmentType.CHESTPLATE));
                    ee.setItemInHand(getRandomItemStack(EquipmentType.WEAPON));
                    ee.setLeggings(getRandomItemStack(EquipmentType.LEGGINGS));
                    ee.setHelmet(getRandomItemStack(EquipmentType.HELM));
                }
                break;
            case 5:
            case 6:
            case 7:
                // 50% chance
                if (r.nextBoolean()) {
                    ee.setChestplate(getRandomItemStack(EquipmentType.CHESTPLATE));
                    ee.setItemInHand(getRandomItemStack(EquipmentType.WEAPON));
                    ee.setLeggings(getRandomItemStack(EquipmentType.LEGGINGS));
                    ee.setHelmet(getRandomItemStack(EquipmentType.HELM));
                    ee.setBoots(getRandomItemStack(EquipmentType.BOOTS));
                }
                break;
        }
    }

    public static EquipmentType getEquipmentType(ItemStack is) {
        if (ArrayUtils.contains(helms, is.getType())) return EquipmentType.HELM;
        else if (ArrayUtils.contains(chestplates, is.getType())) return EquipmentType.CHESTPLATE;
        else if (ArrayUtils.contains(leggings, is.getType())) return EquipmentType.LEGGINGS;
        else if (ArrayUtils.contains(boots, is.getType())) return EquipmentType.BOOTS;
        else if (ArrayUtils.contains(weapon, is.getType())) return EquipmentType.WEAPON;
        else if (is.getType() == Material.BOW) return EquipmentType.BOW;
        return null;
    }

    public static Enchantment getRandomEnchantment(EquipmentType et) {
        return enchants.get(et)[r.nextInt(enchants.get(et).length)];
    }

    public static Zombie spawnLeveledZombie(Location l) {
        return spawnLeveledZombie(l, r.nextInt(8));
    }

    /**
     * Attempts to guess the level of a zombie based off of its health.
     *
     * @param health Health (in half hearts) of zombie
     * @return Guessed level
     */
    public static double guessLevel(double health) {
        health -= (double) r.nextInt(20);
        health /= (double) nextInt(10, 20);
        return health;
    }

    private final static DecimalFormat df = new DecimalFormat("0.#");

    public static void applyNameplate(Zombie z, int level, double currentHealth, double maxHealth) {
        if (!Config.useNameplates) return;
        if (level < 1) level = 1;
        if (level > 7) level = 7;
        String format = Config.nameplateFormat;
        format = format.replace("{currenthealth}", df.format(currentHealth));
        format = format.replace("{maxhealth}", df.format(maxHealth));
        format = format.replace("{currenthearts}", df.format(currentHealth / 2D));
        format = format.replace("{maxhearts}", df.format(maxHealth / 2D));
        format = format.replace("{level}", String.valueOf(level));
        if (format.length() > 32) format = format.substring(0, 32);
        z.setCustomName(format);
        z.setCustomNameVisible(Config.nameplateAlwaysVisible);
    }

    public static void applyNameplate(Zombie z, int level) {
        applyNameplate(z, level, z.getHealth(), z.getMaxHealth());
    }

    /**
     * Gets the metadata RoyalSurvivors stores on a zombie at spawn in a Map format.
     *
     * @param z Zombie to get metadata of
     * @return Map of objects or null if no metadata
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getSurvivorsData(Zombie z) {
        final List<MetadataValue> metadata = z.getMetadata("rsurv");
        for (MetadataValue mv : metadata) {
            if (mv == null) continue;
            if (!mv.getOwningPlugin().getName().equals(RoyalSurvivors.instance.getName())) continue;
            Object value = mv.value();
            if (!(value instanceof Map)) continue;
            return (Map<String, Object>) value;
        }
        return null;
    }

    public static void applyZombieCharacteristics(Zombie z, int level) {
        if (level < 1) level = 1;
        if (level > 7) level = 7;
        if (Config.useBabies && nextInt(1, Config.babyZombieChance) == Config.babyZombieChance - 1) {
            if (Config.babiesAlwaysFast)
                z.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, Config.babySpeedLevel));
            z.setBaby(true);
        }
        z.setMaxHealth(r.nextInt(20) + (nextInt(10, 20) * level));
        z.setHealth(z.getMaxHealth());
        setEntityEquipment(z.getEquipment(), level);
        if (level >= Config.minLevelPotion && Config.usePotions)
            if (nextInt(1, Config.potionChance) == Config.potionChance - 1) applyPotionEffects(z);
        if (Config.useSpeed)
            z.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, Config.speedBoostLevel));
        if (Config.useNameplates) applyNameplate(z, level);
        final Map<String, Object> info = new HashMap<String, Object>();
        info.put("level", level);
        z.setMetadata("rsurv", new FixedMetadataValue(RoyalSurvivors.instance, info));
    }

    /**
     * Do not call inside of a creature spawn event, as this will cause a stack overflow.
     *
     * @param l     Location to spawn zombie at
     * @param level Level of zombie
     * @return Zombie spawned
     */
    public static Zombie spawnLeveledZombie(Location l, int level) {
        if (level < 1) level = 1; // 1 is min level (max health 20)
        if (level > 7) level = 7; // 7 is max level (max health 160)
        Zombie z = spawnZombieBase(l);
        if (z == null) return null;
        applyZombieCharacteristics(z, level);
        RoyalSurvivors.debugStatic("zombie: (" + level + ", " + z.getMaxHealth() + ", " + z.getEquipment() + ", " + z.getLocation() + ")");
        return z;
    }

}
