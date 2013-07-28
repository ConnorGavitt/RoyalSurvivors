package org.royaldev.royalsurvivors.runners;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.PConfManager;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class ColdRunner implements Runnable {

    // TODO: Need armor? Perhaps armor reduces rate of chill.

    private final RoyalSurvivors plugin;

    public ColdRunner(RoyalSurvivors instance) {
        plugin = instance;
    }

    private final Biome[] coldBiomes = new Biome[]{
            Biome.FROZEN_OCEAN,
            Biome.FROZEN_RIVER,
            Biome.ICE_MOUNTAINS,
            Biome.ICE_PLAINS,
            Biome.TAIGA,
            Biome.TAIGA_HILLS
    };

    private boolean isInColdBiome(Player p) {
        Biome b = p.getLocation().getBlock().getBiome();
        return ArrayUtils.contains(coldBiomes, b);
    }

    private boolean isWarmBlock(Material m) {
        return Config.warmBlocks.contains(m.name());
    }

    /**
     * Returns the number of warm blocks around the player. This should be multiplied against the warm amount.
     *
     * @param p Player to get warm strength of
     * @return Number of warm blocks
     */
    private int getWarmStrength(Player p) {
        final Location l = p.getLocation();
        final int lX = l.getBlockX();
        final int lY = l.getBlockY();
        final int lZ = l.getBlockZ();
        int strength = 0;
        int radius = (Config.coldHotBlockRadius < 0) ? Config.coldHotBlockRadius * -1 : Config.coldHotBlockRadius;
        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z < radius; z++) {
                    Block b = p.getWorld().getBlockAt(x + lX, y + lY, z + lZ);
                    if (!isWarmBlock(b.getType())) continue;
                    strength++;
                }
            }
        }
        return strength;
    }

    /**
     * Changes a player's cold by a set amount.
     *
     * @param p      Player to change cold of
     * @param amount Amount to change cold by (can be negative) - out of coldMax
     */
    private void changeCold(final Player p, final float amount) {
        if (getWarmth(p) > 0F) {
            changeWarmth(p, amount);
            return;
        }
        PConfManager pcm = PConfManager.getPConfManager(p);
        if (!pcm.exists()) pcm.createFile();
        float cold = (pcm.isSet("cold")) ? pcm.getFloat("cold") : (float) Config.coldMax;
        cold *= Config.coldMax;
        cold += amount;
        cold = Config.walkSpeed * (cold / Config.coldMax);
        if (cold <= 0F) cold = 0F;
        if (cold >= Config.walkSpeed) cold = Config.walkSpeed;
        pcm.set("cold", cold / Config.walkSpeed);
        p.setWalkSpeed(cold);
    }

    /**
     * Gets the cold saturation of a player.
     *
     * @param p Player to get saturation of
     * @return Saturation out of coldSaturationMax.
     */
    private float getWarmth(final Player p) {
        PConfManager pcm = PConfManager.getPConfManager(p);
        if (!pcm.exists()) pcm.createFile();
        return (pcm.isSet("coldSaturation")) ? pcm.getFloat("coldSaturation") : (float) Config.coldSaturationMax;
    }

    /**
     * Changes a player's cold saturation by a set amount.
     *
     * @param p      Player to change cold saturation of
     * @param amount Amount to change cold saturation by (can be negative) - out of coldSaturationMax
     */
    private void changeWarmth(final Player p, final float amount) {
        PConfManager pcm = PConfManager.getPConfManager(p);
        if (!pcm.exists()) pcm.createFile();
        pcm.set("coldSaturation", getWarmth(p) + amount);
    }

    @Override
    public void run() {
        if (!Config.coldEnabled) return;
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE) continue;
            if (!isInColdBiome(p)) {
                if (getWarmth(p) < Config.coldSaturationMax) changeWarmth(p, Config.coldSaturationMax);
                if (p.getWalkSpeed() < Config.walkSpeed) changeCold(p, Config.coldRestore * 5);
                continue;
            }
            int warmStrength = getWarmStrength(p);
            int drainAmount = (warmStrength > 0) ? Config.coldRestore * warmStrength : -Config.coldDrain;
            changeCold(p, drainAmount);
            if (p.getWalkSpeed() <= 0F) {
                p.sendMessage(ChatColor.BLUE + "You have died by freezing.");
                p.setHealth(0);
                changeCold(p, Config.coldMax);
                changeWarmth(p, Config.coldSaturationMax);
            }
        }
    }

}
