package org.royaldev.royalsurvivors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class SurvivorsListener implements Listener {

    private final RoyalSurvivors plugin;
    private final Random r = new Random();

    public SurvivorsListener(RoyalSurvivors instance) {
        plugin = instance;
    }

    /**
     * Checks to see if the ingredients of the recipes are the same.
     *
     * @param a Recipe A
     * @param b Recipe B
     * @return If A & B have the same ingredients
     */
    private boolean shapelessRecipesMatch(ShapelessRecipe a, ShapelessRecipe b) {
        return a.getIngredientList().containsAll(b.getIngredientList());
    }

    /**
     * Checks to see if the ingredients (<strong>not order</strong>) of the recipes are the same.
     *
     * @param a Recipe A
     * @param b Recipe B
     * @return If A & B have the same ingredients
     */
    private boolean shapedRecipesMatch(ShapedRecipe a, ShapedRecipe b) {
        return a.getIngredientMap().values().containsAll(b.getIngredientMap().values());
    }

    /**
     * Sanitizes a message for use in vanilla chat.
     *
     * @param format Format of message to sanitize
     * @return Sanitized format
     */
    private String sanitizeChat(final String format) {
        return format.replace("%", "%%");
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

    private boolean isInInfectedWorld(Entity e) {
        return e.getWorld().getName().equalsIgnoreCase(Config.worldToUse);
    }

    private boolean isInInfectedWorld(Location l) {
        return l.getWorld().getName().equalsIgnoreCase(Config.worldToUse);
    }

    private int nextInt(int start, int end) {
        return r.nextInt(end - start + 1) + start;
    }

    private void handleLocalChat(AsyncPlayerChatEvent e, boolean radio) {
        Player p = e.getPlayer();
        if (!radio) e.getRecipients().clear();
        List<Player> remove = new ArrayList<Player>();
        List<Entity> nearby = p.getNearbyEntities(Config.localChatRadius, Config.localChatRadius, Config.localChatRadius);
        for (Entity ent : nearby) {
            if (!(ent instanceof Player)) continue;
            Player t = (Player) ent;
            if (t.equals(p) && radio) continue;
            t.sendMessage(p.getName() + ChatColor.RESET + ": " + e.getMessage());
            remove.add(t);
        }
        if (!nearby.contains(p) && !radio) p.sendMessage(p.getName() + ChatColor.RESET + ": " + e.getMessage());
        e.getRecipients().removeAll(remove);
        if (!radio) e.setCancelled(true);
    }

    private void handleRadioChat(AsyncPlayerChatEvent e) {
        handleLocalChat(e, true);
        e.setMessage(sanitizeChat(e.getMessage()));
        Player p = e.getPlayer();
        if (!hasRadio(p)) {
            e.getRecipients().clear();
            return;
        }
        String channel = plugin.getUserdata(p).getString("radio.channel");
        if (channel == null) {
            e.getRecipients().clear();
            return;
        }
        List<Player> toRemove = new ArrayList<Player>();
        for (Player t : e.getRecipients()) {
            PConfManager pcm = plugin.getUserdata(t);
            String theirChannel = pcm.getString("radio.channel");
            Boolean isOn = pcm.getBoolean("radio.on");
            if (isOn == null) isOn = false;
            if (theirChannel == null || !theirChannel.equalsIgnoreCase(channel) || !hasRadio(t) || !isOn)
                toRemove.add(t);
        }
        e.getRecipients().removeAll(toRemove);
        e.setFormat(ChatColor.GRAY.toString() + ChatColor.ITALIC + "[RADIO] " + ChatColor.RESET + e.getPlayer().getName() + ": " + e.getMessage());
    }

    /**
     * Removes color codes that have not been processed yet (&char)
     * <p/>
     * This fixes a common exploit where color codes can be embedded into other codes:
     * &&aa (replaces &a, and the other letters combine to make &a again)
     *
     * @param message String with raw color codes
     * @return String without raw color codes
     */
    private String decolorize(String message) {
        Pattern p = Pattern.compile("(?i)&[a-f0-9k-or]");
        boolean contains = p.matcher(message).find();
        while (contains) {
            message = message.replaceAll("(?i)&[a-f0-9k-or]", "");
            contains = p.matcher(message).find();
        }
        return message;
    }

    @EventHandler
    public void banHandler(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        PConfManager pcm = plugin.getUserdata(p);
        Boolean banned = pcm.getBoolean("banned");
        if (banned == null) banned = false;
        Long banExpiresAfter = pcm.getLong("banexpiresafter");
        if (banExpiresAfter == null) banExpiresAfter = -1L;
        if (!banned || banExpiresAfter <= 0L) return;
        // if time now is after the time ban should expire
        if (new Date().getTime() > banExpiresAfter) {
            p.setBanned(false);
            e.setResult(PlayerLoginEvent.Result.ALLOWED);
            pcm.setBoolean(false, "banned");
            pcm.set(null, "banexpiresafter");
            return;
        }
        e.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        e.setKickMessage(Config.banMessage);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!isInInfectedWorld(e.getPlayer())) return;
        Player p = e.getPlayer();
        p.setLevel(0); // no XP for you!
        PConfManager pcm = plugin.getUserdata(p);
        Float thirst = pcm.getFloat("thirst");
        if (thirst == null) {
            thirst = 1F;
        }
        p.setExp(thirst);
        pcm.setFloat(thirst, "thirst");
    }

    @EventHandler
    public void firstRadio(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        if (p.hasPlayedBefore()) return;
        p.getInventory().addItem(new ItemStack(Config.radioMaterial, 1));
    }

    @EventHandler
    public void thirsty(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        PConfManager pcm = plugin.getUserdata(p);
        Float thirst = pcm.getFloat("thirst");
        if (thirst == null) {
            thirst = 1F;
        }
        p.setExp(thirst);
        pcm.setFloat(thirst, "thirst");
    }

    @EventHandler
    public void mineXP(BlockExpEvent e) {
        if (!isInInfectedWorld(e.getBlock().getLocation())) return;
        e.setExpToDrop(0);
    }

    @EventHandler
    public void minimizeXP(PlayerExpChangeEvent e) {
        if (!isInInfectedWorld(e.getPlayer())) return;
        Player p = e.getPlayer();
        PConfManager pcm = plugin.getUserdata(p);
        Float thirst = pcm.getFloat("thirst");
        if (thirst == null) {
            thirst = 1F;
            pcm.setFloat(thirst, "thirst");
        }
        p.setExp(thirst);
        p.setLevel(0);
    }

    @EventHandler
    public void zombieMurdersRealFast(EntityDamageByEntityEvent e) {
        if (!isInInfectedWorld(e.getDamager())) return;
        if (!isInInfectedWorld(e.getEntity())) return;
        if (!(e.getDamager() instanceof Zombie)) return;
        e.setDamage(e.getDamage() * 5);
    }

    @EventHandler
    public void sniperRifleDamage(EntityDamageEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;
        e.setDamage(e.getDamage() * 4);
    }

    @EventHandler
    public void onAnyDeath(EntityDeathEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        e.setDroppedExp(0); // we don't want XP to be dropped in our post-apocalyptic world
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        LivingEntity le = e.getEntity();
        if (le instanceof Player) return;
        if (le instanceof Zombie) {
            if (nextInt(1, 8) == 4) e.getDrops().add(new ItemStack(Material.BONE, nextInt(1, 3)));
            if (nextInt(1, 10) == 4) e.getDrops().add(new ItemStack(Material.SULPHUR, nextInt(1, 2)));
            if (nextInt(1, 75) == 4) e.getDrops().add(new ItemStack(Material.GLOWSTONE_DUST, nextInt(1, 2)));
        }
    }

    private Block getAdjacentEmptyBlock(Block b) {
        Block a = b.getRelative(BlockFace.NORTH);
        if (!a.isEmpty()) return a;
        a = b.getRelative(BlockFace.EAST);
        if (!a.isEmpty()) return a;
        a = b.getRelative(BlockFace.WEST);
        if (!a.isEmpty()) return a;
        a = b.getRelative(BlockFace.SOUTH);
        if (!a.isEmpty()) return a;
        return null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        Player p = e.getEntity();
        e.setDroppedExp(0);
        if (e.getDrops().isEmpty()) return;
        Location place = p.getLocation();
        Block b = place.getBlock();
        if (!b.isEmpty()) {
            place.setY(place.getWorld().getHighestBlockYAt(place) + 1);
            b = place.getBlock();
        }
        b.setType(Material.CHEST);
        Chest c = (Chest) b.getState();
        List<ItemStack> notAdded = new ArrayList<ItemStack>();
        for (ItemStack is : e.getDrops())
            for (ItemStack notFit : c.getInventory().addItem(is).values()) notAdded.add(notFit);
        List<ItemStack> stillLeftOver = new ArrayList<ItemStack>();
        if (!notAdded.isEmpty()) {
            b = getAdjacentEmptyBlock(b);
            if (b == null) {
                e.getDrops().removeAll(notAdded);
                return;
            }
            b.setType(Material.CHEST);
            c = (Chest) b.getState();
            for (ItemStack is : notAdded)
                for (ItemStack stillNo : c.getInventory().addItem(is).values()) stillLeftOver.add(stillNo);
        }
        e.getDrops().clear();
        if (!stillLeftOver.isEmpty()) e.getDrops().addAll(stillLeftOver);
    }

    @EventHandler
    public void zombify(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isInInfectedWorld(p)) return;
        if (!Config.spawnZombie) return;
        ZombieSpawner.spawnLeveledZombie(p.getLocation());
    }

    @EventHandler
    public void stopAllCommands(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        if (p.hasPermission("rsurv.allowcommands")) return;
        String[] split = e.getMessage().split(" ");
        if (split.length < 1) return;
        String root = split[0].substring(1); // the command label (remove /)
        PluginCommand pc = plugin.getCommand(root);
        if (pc == null) {
            p.sendMessage(ChatColor.BLUE + "Nothing happens...");
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void radioChatter(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        PConfManager pcm = plugin.getUserdata(p);
        Boolean isOn = pcm.getBoolean("radio.on");
        e.setMessage(decolorize(e.getMessage()));
        List<Player> toRemove = new ArrayList<Player>();
        for (Player t : e.getRecipients()) {
            if (isInInfectedWorld(t)) continue;
            toRemove.add(t);
        }
        e.getRecipients().removeAll(toRemove);
        if (isOn == null) isOn = false;
        if (!isOn) handleLocalChat(e, false);
        else handleRadioChat(e);
        if (!e.getRecipients().contains(p)) e.getRecipients().add(p);
    }

    @EventHandler
    public void zombieIsMasterRace(CreatureSpawnEvent e) {
        if (e.getEntityType() == EntityType.ZOMBIE) return; // don't force spawn zombies when they're spawning already!
        if (!isInInfectedWorld(e.getEntity())) return;
        World w = e.getLocation().getWorld();
        if (r.nextInt(Config.hordeChance) == Config.hordeChance - 1) {
            if (e.getEntityType() == EntityType.SQUID && !Config.oceanZombies) return;
            for (int i = 0; i < nextInt(Config.hordeMin, Config.hordeMax); i++)
                ZombieSpawner.spawnLeveledZombie(e.getLocation());
            e.setCancelled(true);
            return;
        }
        if (w.getTime() > 0L && w.getTime() < 12000L) { // only zombies in the daytime
            e.setCancelled(true);
            if (e.getEntityType() == EntityType.SQUID && !Config.oceanZombies) return; // don't spawn squid zombies
            if (nextInt(1, 10) < 8) ZombieSpawner.spawnLeveledZombie(e.getLocation()); // 4/5 chance
            return;
        }
        if (!(e.getEntity() instanceof Monster)) return; // don't replace nice animals
        e.setCancelled(true);
        ZombieSpawner.spawnLeveledZombie(e.getLocation());
    }

    @EventHandler
    public void zombiesAreNotVampires(EntityCombustEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        if (e.getEntityType() != EntityType.ZOMBIE) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onRefillBattery(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!(isInInfectedWorld(p))) return;
        if (!(e.getRecipe() instanceof ShapelessRecipe)) return;
        ShapelessRecipe slr = (ShapelessRecipe) e.getRecipe();
        if (!shapelessRecipesMatch(slr, plugin.batteryRefill)) return;
        plugin.getUserdata(p).setInteger(100, "radio.battery");
    }

    @EventHandler
    public void onStackBottles(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!(isInInfectedWorld(p))) return;
        ItemStack clicked = e.getCursor();
        ItemStack holding = e.getCurrentItem();
        if (clicked == null || holding == null) return;
        if (clicked.getType() != Material.POTION || clicked.getDurability() != (short) 0) return;
        if (holding.getType() != Material.POTION || holding.getDurability() != (short) 0) return;
        clicked.setAmount(clicked.getAmount() + holding.getAmount());
        e.setCurrentItem(null);
        e.setCursor(clicked);
    }

    @EventHandler
    public void stopVanillaCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!isInInfectedWorld(p)) return;
        Recipe rr = e.getRecipe();
        ItemStack result = rr.getResult();
        if (rr instanceof ShapedRecipe) {
            ShapedRecipe srr = (ShapedRecipe) rr;
            if (result.getType() == Material.BOW && !shapedRecipesMatch(srr, plugin.bowRecipe)) e.setCancelled(true);
            if (result.getType() == Material.ARROW && !srr.getIngredientMap().values().containsAll(plugin.arrowRecipe.getIngredientList()))
                e.setCancelled(true);
        }
        if (e.isCancelled()) e.setResult(Event.Result.DENY);
    }

    @EventHandler
    public void onChangeLevel(PlayerLevelChangeEvent e) {
        Player p = e.getPlayer();
        if (!(isInInfectedWorld(p))) return;
        p.setLevel(0);
        PConfManager pcm = plugin.getUserdata(p);
        Float thirst = pcm.getFloat("thirst");
        if (thirst == null) {
            thirst = 1F;
            pcm.setFloat(thirst, "thirst");
        }
        p.setExp(thirst);
    }

    @EventHandler
    public void onUseHealItem(PlayerInteractEvent e) {
        if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) return;
        final Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        final ItemStack hand = p.getItemInHand();
        if (hand.getType() != Material.MELON) return;
        if (hand.getDurability() != (short) 14) return;
        if (p.getMaxHealth() == p.getHealth()) return; // don't waste heals
        int newHealth = p.getHealth() + 8;
        if (newHealth > p.getMaxHealth()) newHealth = p.getMaxHealth();
        p.setHealth(newHealth);
        // until Bukkit fixes removing the last item in interact events, workaround
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
                    ItemStack is = p.getInventory().getItem(slot);
                    if (is == null) continue;
                    if (!is.equals(hand)) continue;
                    is.setAmount(is.getAmount() - 1);
                    p.getInventory().setItem(slot, is);
                }
            }
        });
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        e.setExpToDrop(0);
        PConfManager pcm = plugin.getUserdata(p);
        Float thirst = pcm.getFloat("thirst");
        if (thirst == null) {
            thirst = 1F;
            pcm.setFloat(thirst, "thirst");
        }
        p.setExp(thirst);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(e.getRespawnLocation())) return;
        PConfManager pcm = plugin.getUserdata(p);
        Float thirst = pcm.getFloat("thirst");
        if (thirst == null) thirst = 1F;
        p.setExp(thirst);
        pcm.setFloat(thirst, "thirst");
    }

    @EventHandler
    public void hardcore(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isInInfectedWorld(p)) return;
        if (!Config.banOnDeath) return;
        p.setBanned(true);
        PConfManager pcm = plugin.getUserdata(p);
        pcm.setBoolean(true, "banned");
        pcm.setLong((Config.banLength < 0) ? 0L : (Config.banLength * 60 * 20) + new Date().getTime(), "banexpiresafter");
    }

    @EventHandler
    public void onDrinky(PlayerInteractEvent e) {
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) return;
        final Player p = e.getPlayer();
        final ItemStack hand = p.getItemInHand();
        if (hand == null) return;
        if (hand.getType() != Material.POTION) return;
        if (hand.getDurability() != (short) 0) return;
        if (!isInInfectedWorld(p)) return;
        if (!p.isSneaking()) return;
        PConfManager pcm = plugin.getUserdata(p);
        Float thirst = pcm.getFloat("thirst");
        if (thirst == null) thirst = 1F;
        thirst += Config.thirstRestorePercent / 100F;
        if (thirst > 1F) thirst = 1F;
        pcm.setFloat(thirst, "thirst");
        p.setExp(thirst);
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
                    ItemStack is = p.getInventory().getItem(slot);
                    if (is == null) continue;
                    if (!is.equals(hand)) continue;
                    is.setAmount(is.getAmount() - 1);
                    p.getInventory().setItem(slot, is);
                }
                p.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
            }
        });
    }

}
