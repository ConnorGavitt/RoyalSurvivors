package org.royaldev.royalsurvivors.runners;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.royaldev.royalsurvivors.Config;
import org.royaldev.royalsurvivors.PConfManager;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class ColdRunner implements Runnable {

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
        PConfManager pcm = plugin.getUserdata(p);
        if (!pcm.exists()) pcm.createFile();
        float cold = (pcm.isSet("cold")) ? pcm.getFloat("cold") : (float) Config.coldMax;
        cold *= Config.coldMax;
        cold += amount;
        cold /= Config.coldMax;
        pcm.set("cold", cold);
        p.setWalkSpeed(cold);
    }

    /**
     * Gets the cold saturation of a player.
     *
     * @param p Player to get saturation of
     * @return Saturation out of coldSaturationMax.
     */
    private float getWarmth(final Player p) {
        PConfManager pcm = plugin.getUserdata(p);
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
        PConfManager pcm = plugin.getUserdata(p);
        if (!pcm.exists()) pcm.createFile();
        pcm.set("coldSaturation", getWarmth(p) + amount);
    }

    @Override
    public void run() {
        if (!Config.coldEnabled) return;
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            if (!isInColdBiome(p)) continue;
            changeCold(p, -Config.coldDrain);
        }
    }

}
