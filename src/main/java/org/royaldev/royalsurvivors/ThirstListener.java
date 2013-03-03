package org.royaldev.royalsurvivors;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class ThirstListener implements Listener {

    private final RoyalSurvivors plugin;

    public ThirstListener(RoyalSurvivors instance) {
        plugin = instance;
    }

    /**
     * Changes a player's thirst by a set amount.
     *
     * @param p      Player to change thirst of
     * @param amount Amount to change thirst by (can be negative) - out of thirstMax
     */
    private void changeThirst(final Player p, final float amount) {
        if (getThirstSaturation(p) > 0F) {
            changeThirstSaturation(p, amount);
            return;
        }
        PConfManager pcm = plugin.getUserdata(p);
        if (!pcm.exists()) pcm.createFile();
        float thirst = (pcm.isSet("thirst")) ? pcm.getFloat("thirst") : 1F;
        thirst *= Config.thirstMax;
        thirst += amount;
        thirst /= Config.thirstMax;
        pcm.set("thirst", thirst);
        p.setExp(thirst);
    }

    /**
     * Gets the thirst saturation of a player.
     *
     * @param p Player to get saturation of
     * @return Saturation out of thirstSaturationMax.
     */
    private float getThirstSaturation(final Player p) {
        PConfManager pcm = plugin.getUserdata(p);
        if (!pcm.exists()) pcm.createFile();
        return (pcm.isSet("thirstSaturation")) ? pcm.getFloat("thirstSaturation") : (float) Config.thirstSaturationMax;
    }

    /**
     * Changes a player's thirst saturation by a set amount.
     *
     * @param p      Player to change thirst saturation of
     * @param amount Amount to change thirst saturation by (can be negative) - out of thirstSaturationMax
     */
    private void changeThirstSaturation(final Player p, final float amount) {
        PConfManager pcm = plugin.getUserdata(p);
        if (!pcm.exists()) pcm.createFile();
        pcm.set("thirstSaturation", getThirstSaturation(p) + amount);
    }

    @EventHandler
    public void onDrinky(PlayerItemConsumeEvent e) {
        final Player p = e.getPlayer();
        final ItemStack hand = e.getItem();
        if (hand == null || hand.getType() != Material.POTION || hand.getDurability() != (short) 0 || !RUtils.isInInfectedWorld(p))
            return;
        PConfManager pcm = plugin.getUserdata(p);
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) thirst = 1F;
        if (thirst >= 1F) {
            e.setCancelled(true); // let's not waste water bottles
            return;
        }
        thirst += Config.thirstRestorePercent / 100F;
        if (thirst > 1F) thirst = 1F;
        pcm.set("thirst", thirst);
        pcm.set("thirstSaturation", (float) Config.thirstSaturationMax);
        p.setExp(thirst);
    }

    @EventHandler
    public void thirsty(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!RUtils.isInInfectedWorld(p)) return;
        PConfManager pcm = plugin.getUserdata(p);
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) thirst = 1F;
        p.setExp(thirst);
        pcm.set("thirst", thirst);
    }

    @EventHandler
    public void oohYouTouchMyTaLaLa(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!RUtils.isInInfectedWorld(p) || p.getGameMode() == GameMode.CREATIVE) return;
        if (p.getVehicle() != null && p.getVehicle() instanceof Minecart) return; // don't penalize for minecarts
        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return; // looking around
        PConfManager pcm = plugin.getUserdata(p);
        Float saturation = pcm.getFloat("thirstSaturation");
        if (saturation == null) saturation = (float) Config.thirstSaturationMax;
        if (saturation > 0F) {
            if (p.isSprinting()) saturation -= Config.thirstSprint;
            else if (p.isSneaking()) saturation -= Config.thirstSneak;
            else saturation -= Config.thirstWalk;
            if (to.getY() > from.getY() && !RUtils.isOnLadder(p)) saturation -= Config.thirstJump;
            pcm.set("thirstSaturation", saturation);
            return;
        }
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) thirst = 1F;
        thirst *= Config.thirstMax;
        if (p.isSprinting()) thirst -= Config.thirstSprint;
        else if (p.isSneaking()) thirst -= Config.thirstSneak;
        else thirst -= Config.thirstWalk;
        Biome b = p.getLocation().getBlock().getBiome();
        long time = p.getWorld().getTime();
        if ((b == Biome.DESERT || b == Biome.DESERT_HILLS) && (time > 0L && time < 12000L) && !RUtils.isInShade(p))
            thirst -= Config.thirstDesert;
        // jump check (disregard ladders)
        if (to.getY() > from.getY() && !RUtils.isOnLadder(p)) thirst -= Config.thirstJump;
        if (thirst <= 0F) {
            p.sendMessage(ChatColor.BLUE + "You have died of dehydration.");
            p.setLastDamageCause(new EntityDamageEvent(p, EntityDamageEvent.DamageCause.CUSTOM, p.getHealth()));
            p.setHealth(0);
            p.setExp(1F);
            pcm.set("thirst", 1F);
            return;
        }
        thirst /= Config.thirstMax;
        pcm.set("thirst", thirst);
        p.setExp(thirst);
    }

    @EventHandler
    public void thirstFire(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FIRE && e.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK)
            return;
        Player p = (Player) e.getEntity();
        if (!RUtils.isInInfectedWorld(p)) return;
        changeThirst(p, -Config.thirstFire); // remove thirst amount (must be negative)
    }

    @EventHandler
    public void thirstBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!RUtils.isInInfectedWorld(p)) return;
        changeThirst(p, -Config.thirstBreak);
    }

    @EventHandler
    public void thirstPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!RUtils.isInInfectedWorld(p)) return;
        changeThirst(p, -Config.thirstBreak);
    }

    @EventHandler
    public void thirstDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (!RUtils.isInInfectedWorld(p)) return;
        changeThirst(p, -Config.thirstDamage);
    }

    @EventHandler
    public void thirstTakeDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (!RUtils.isInInfectedWorld(p)) return;
        changeThirst(p, -Config.thirstTakeDamage);
    }

    @EventHandler
    public void thirstSwing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player p = e.getPlayer();
        if (!RUtils.isInInfectedWorld(p)) return;
        final ItemStack hand = p.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) changeThirst(p, -Config.thirstSwing);
        else changeThirst(p, -Config.thirstSwingItem);
    }

}
