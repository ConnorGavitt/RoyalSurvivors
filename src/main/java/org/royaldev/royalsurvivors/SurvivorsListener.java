package org.royaldev.royalsurvivors;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SurvivorsListener implements Listener {

    private final RoyalSurvivors plugin;
    private final Random r = new Random();

    public SurvivorsListener(RoyalSurvivors instance) {
        plugin = instance;
    }

    private void setPlayerCharacteristics(Player p) {
        p.setHealth(Config.maxHealth);
        p.setMaxHealth(Config.maxHealth);
        p.setWalkSpeed(Config.walkSpeed);
        p.setFlySpeed(Config.flySpeed);
        p.setMaximumAir(Config.maxAir);
        p.setCanPickupItems(Config.canPickupItems);
        if (!Config.texturePackURL.isEmpty()) p.setTexturePack(Config.texturePackURL);
    }

    private void resetPlayerCharacteristics(Player p) {
        p.setMaxHealth(20);
        p.setHealth(20);
        p.setWalkSpeed(.2F);
        p.setFlySpeed(.1F);
        p.setMaxHealth(300);
        p.setCanPickupItems(true);
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

    private boolean isOnLadder(Location l) {
        return l.getBlock().getType() == Material.LADDER;
    }

    private boolean isOnLadder(Player p) {
        return isOnLadder(p.getLocation());
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
        return p.getInventory().contains(Config.radioMaterial);
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
        if (!radio) {
            Logger.getLogger("Minecraft").info(p.getName() + ChatColor.RESET + ": " + e.getMessage());
            e.setCancelled(true);
        }
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
        if (!pcm.isSet("banned") || !pcm.isSet("banexpiresafter")) return;
        boolean banned = pcm.getBoolean("banned");
        long banExpiresAfter = pcm.getLong("banexpiresafter");
        if (!banned || banExpiresAfter <= 0L) return;
        // if time now is after the time ban should expire
        if (new Date().getTime() > banExpiresAfter) {
            p.setBanned(false);
            e.setResult(PlayerLoginEvent.Result.ALLOWED);
            pcm.set("banned", false);
            pcm.set(null, "banexpiresafter");
            return;
        }
        e.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        e.setKickMessage(Config.banMessage);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        if (!isInInfectedWorld(e.getPlayer())) return;
        Player p = e.getPlayer();
        setPlayerCharacteristics(p);
        p.setLevel(0); // no XP for you!
        PConfManager pcm = plugin.getUserdata(p);
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) thirst = 1F;
        p.setExp(thirst);
        pcm.set("thirst", thirst);
    }

    @EventHandler
    public void firstRadio(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        if (p.hasPlayedBefore()) return;
        p.getInventory().addItem(new ItemStack(Config.radioMaterial, 1));
    }

    @EventHandler
    public void phosphorousGrenades(ProjectileHitEvent e) {
        if (!Config.useGrenades) return;
        Projectile p = e.getEntity();
        if (!isInInfectedWorld(p)) return;
        if (!(p instanceof Snowball)) return;
        Location hit = p.getLocation();
        hit.getWorld().createExplosion(hit, 0F, false);
        List<Entity> toFire = p.getNearbyEntities(6D, 6D, 6D);
        for (Entity ent : toFire) {
            if (!(ent instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) ent;
            le.damage((int) Math.ceil(le.getMaxHealth() / nextInt(Config.grenadeHighDamage, Config.grenadeLowDamage)));
            le.setFireTicks(nextInt(Config.grenadeLowBurn, Config.grenadeHighBurn) * 20);
        }
    }

    @EventHandler
    public void thirsty(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        PConfManager pcm = plugin.getUserdata(p);
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) thirst = 1F;
        p.setExp(thirst);
        pcm.set("thirst", thirst);
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
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) {
            thirst = 1F;
            pcm.set("thirst", thirst);
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
    public void smeltFast(FurnaceSmeltEvent e) {
        if (!(e.getBlock().getState() instanceof Furnace)) return;
        Furnace f = (Furnace) e.getBlock().getState();
        if (!isInInfectedWorld(f.getLocation())) return;
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = f.getLocation();
        String path = "furnaces." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        if (!cm.getBoolean(path, false)) return;
        f.setCookTime((short) 140); // 7 seconds in = 3 seconds left
    }

    @EventHandler
    public void fuelFast(FurnaceBurnEvent e) {
        if (!(e.getBlock().getState() instanceof Furnace)) return;
        Furnace f = (Furnace) e.getBlock().getState();
        if (!isInInfectedWorld(f.getLocation())) return;
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = f.getLocation();
        String path = "furnaces." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        if (!cm.getBoolean(path, false)) return;
        f.setCookTime((short) 140); // 7 seconds in = 3 seconds left
    }

    @EventHandler
    public void fastFurnaceBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
        Block b = e.getBlock();
        if (!(b.getState() instanceof Furnace)) return;
        Furnace f = (Furnace) b.getState();
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = f.getLocation();
        String path = "furnaces." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        if (!cm.getBoolean(path, false)) return;
        cm.set(path, null);
        e.setCancelled(true);
        Collection<ItemStack> drops = e.getBlock().getDrops();
        e.getBlock().setType(Material.AIR);
        l.getWorld().dropItemNaturally(l, plugin.furnace);
        for (ItemStack is : drops) {
            if (is == null) continue;
            if (is.getType() == Material.FURNACE) continue;
            l.getWorld().dropItemNaturally(l, is);
        }
    }

    @EventHandler
    public void fastFurnacePlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        ItemStack hand = e.getItemInHand();
        if (hand == null) return;
        if (hand.getType() != Material.FURNACE) return;
        ItemMeta him = hand.getItemMeta();
        ItemMeta fim = plugin.furnace.getItemMeta();
        String hdn = him.getDisplayName();
        if (hdn == null) return;
        if (!hdn.equals(fim.getDisplayName())) return;
        List<String> hl = him.getLore();
        if (hl == null) return;
        if (!hl.containsAll(fim.getLore())) return;
        Block b = e.getBlockPlaced();
        if (!(b.getState() instanceof Furnace)) return;
        Furnace f = (Furnace) b.getState();
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = f.getLocation();
        String path = "furnaces." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        cm.set(path, true);
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        LivingEntity le = e.getEntity();
        if (le instanceof Player) return;
        if (le instanceof Zombie) {
            if (nextInt(1, 8) == 4) e.getDrops().add(new ItemStack(Material.BONE, nextInt(1, 3)));
            if (nextInt(1, 10) == 4) e.getDrops().add(new ItemStack(Material.SULPHUR, nextInt(1, 2)));
            if (nextInt(1, 12) == 4) e.getDrops().add(new ItemStack(Material.STRING, nextInt(2, 3)));
            if (nextInt(1, 25) == 4) {
                ItemStack is = plugin.arrow.clone();
                is.setAmount(nextInt(1, 5));
                e.getDrops().add(is);
            }
            if (nextInt(1, 75) == 4) {
                ItemStack is = plugin.recharge.clone();
                is.setAmount(nextInt(1, 2));
                e.getDrops().add(is);
            }
        }
    }

    private Block getAdjacentEmptyBlock(Block b) {
        Block a = b.getRelative(BlockFace.NORTH);
        if (a.isEmpty()) return a;
        a = b.getRelative(BlockFace.EAST);
        if (a.isEmpty()) return a;
        a = b.getRelative(BlockFace.WEST);
        if (a.isEmpty()) return a;
        a = b.getRelative(BlockFace.SOUTH);
        if (a.isEmpty()) return a;
        return null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        Player p = e.getEntity();
        ConfManager cm = plugin.getConfig("otherdata.yml");
        e.setDroppedExp(0);
        if (e.getDrops().isEmpty()) return;
        Location place = p.getLocation();
        Block b = place.getBlock();
        if (!b.isEmpty()) {
            place.setY(place.getWorld().getHighestBlockYAt(place) + 1);
            b = place.getBlock();
        }
        b.setType(Material.CHEST);
        Location l = b.getLocation();
        String path = "deathchests." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        cm.set(path, true);
        Chest c = (Chest) b.getState();
        List<ItemStack> notAdded = new ArrayList<ItemStack>();
        for (ItemStack is : e.getDrops())
            for (ItemStack notFit : c.getInventory().addItem(is).values()) notAdded.add(notFit);
        List<ItemStack> stillLeftOver = new ArrayList<ItemStack>();
        if (!notAdded.isEmpty()) {
            b = getAdjacentEmptyBlock(b);
            if (b == null) {
                e.getDrops().clear(); // remove all drops
                e.getDrops().addAll(notAdded); // add what didn't fit
                return;
            }
            b.setType(Material.CHEST);
            l = b.getLocation();
            path = "deathchests." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
            cm.set(path, true);
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
        Zombie z = ZombieSpawner.spawnLeveledZombie(p.getLocation());
        if (z == null) return;
        EntityEquipment ze = z.getEquipment();
        EntityEquipment pe = p.getEquipment();
        if (ze == null || pe == null) return;
        ze.setArmorContents(pe.getArmorContents());
        ze.setHelmetDropChance(0F);
        ze.setChestplateDropChance(0F);
        ze.setLeggingsDropChance(0F);
        ze.setBootsDropChance(0F);
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
            pc = plugin.getServer().getPluginCommand(root);
            if (pc != null) {
                if (Config.allowedCommands.contains(pc.getName())) return;
                for (String alias : pc.getAliases()) if (Config.allowedCommands.contains(alias)) return;
            }
            p.sendMessage(ChatColor.BLUE + "Nothing happens...");
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void radioChatter(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        PConfManager pcm = plugin.getUserdata(p);
        boolean isOn = pcm.getBoolean("radio.on", false);
        e.setMessage(decolorize(e.getMessage()));
        List<Player> toRemove = new ArrayList<Player>();
        for (Player t : e.getRecipients()) {
            if (isInInfectedWorld(t)) continue;
            toRemove.add(t);
        }
        e.getRecipients().removeAll(toRemove);
        if (!isOn) handleLocalChat(e, false);
        else handleRadioChat(e);
        if (!e.getRecipients().contains(p)) e.getRecipients().add(p);
    }

    @EventHandler
    public void zombieIsMasterRace(CreatureSpawnEvent e) {
        if (!isInInfectedWorld(e.getEntity())) return;
        if (e.getEntityType() == EntityType.ZOMBIE) { // don't force spawn zombies when they're spawning already!
            ZombieSpawner.applyZombieCharacteristics((Zombie) e.getEntity(), r.nextInt(8));
            return;
        }
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
        Entity ent = e.getEntity();
        if (!(ent instanceof Monster) && !(ent instanceof Slime)) return;
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
        plugin.getUserdata(p).set("radio.battery", 100);
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
            if (Config.harderTorches && result.getType() == Material.TORCH && !srr.getIngredientMap().values().containsAll(plugin.torchRecipe.getIngredientList()))
                e.setCancelled(true);
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
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) {
            thirst = 1F;
            pcm.set("thirst", thirst);
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
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) {
            thirst = 1F;
            pcm.set("thirst", thirst);
        }
        p.setExp(thirst);
    }

    private final List<String> respawns = new ArrayList<String>();

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        World w = plugin.getServer().getWorld(Config.worldToUse);
        if (w == null) return;
        Player p = e.getPlayer();
        synchronized (respawns) {
            if (!respawns.contains(p.getName())) return;
        }
        if (!isInInfectedWorld(e.getRespawnLocation())) e.setRespawnLocation(w.getSpawnLocation());
        PConfManager pcm = plugin.getUserdata(p);
        p.setExp(1F);
        pcm.set("thirst", 1F);
        synchronized (respawns) {
            respawns.remove(p.getName());
        }
    }

    @EventHandler
    public void spawnInWorldIfDiedInWorld(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isInInfectedWorld(p.getLocation())) return;
        synchronized (respawns) {
            respawns.add(p.getName());
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void myDingDingDong(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(e.getRespawnLocation())) return;
        setPlayerCharacteristics(p);
    }

    @EventHandler
    public void destroyBlock(BlockBreakEvent e) {
        if (!Config.useGrenades) return;
        Material m = e.getBlock().getType();
        if (m != Material.SNOW && m != Material.SNOW_BLOCK) return;
        e.setCancelled(true); // let's cancel so no drops
        e.getBlock().setType(Material.AIR); // still have it go away
    }

    @EventHandler
    public void removeBullets(ProjectileHitEvent e) {
        Entity ent = e.getEntity();
        if (!isInInfectedWorld(ent)) return;
        if (!(ent instanceof Arrow)) return;
        ent.remove();
    }

    @EventHandler
    public void hardcore(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isInInfectedWorld(p)) return;
        if (!Config.banOnDeath) return;
        p.setBanned(true);
        PConfManager pcm = plugin.getUserdata(p);
        pcm.set("banned", true);
        pcm.set("banexpiresafter", (Config.banLength < 0) ? 0L : (Config.banLength * 60 * 20) + new Date().getTime());
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
        //if (!p.isSneaking()) return;
        PConfManager pcm = plugin.getUserdata(p);
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) thirst = 1F;
        if (thirst >= 1F) return; // let's not waste water bottles
        thirst += Config.thirstRestorePercent / 100F;
        if (thirst > 1F) thirst = 1F;
        pcm.set("thirst", thirst);
        pcm.set("thirstSaturation", (float) Config.thirstSaturationMax);
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
                    break;
                }
                p.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
            }
        });
    }

    @EventHandler
    public void oohYouTouchMyTaLaLa(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
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
            if (from.getY() != to.getY() && !isOnLadder(p)) saturation -= Config.thirstJump;
            pcm.set("thirstSaturation", saturation);
            return;
        }
        float thirst = pcm.getFloat("thirst");
        if (!pcm.isSet("thirst")) thirst = 1F;
        thirst *= Config.thirstMax;
        if (p.isSprinting()) thirst -= Config.thirstSprint;
        else if (p.isSneaking()) thirst -= Config.thirstSneak;
        else thirst -= Config.thirstWalk;
        // jump check (disregard ladders)
        if (from.getY() != to.getY() && !isOnLadder(p)) thirst -= Config.thirstJump;
        if (thirst <= 0F) {
            p.sendMessage(ChatColor.BLUE + "You have died of dehydration.");
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
    public void deathChestBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!isInInfectedWorld(b.getLocation())) return;
        if (!(b.getState() instanceof Chest)) return;
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = b.getLocation();
        String path = "deathchests." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        boolean isDeathChest = cm.getBoolean(path, false);
        if (!isDeathChest) return;
        e.setCancelled(true);
        Chest c = (Chest) b.getState();
        ItemStack[] drops = c.getBlockInventory().getContents().clone(); // inv of only this chest
        c.getBlockInventory().clear();
        b.setType(Material.AIR);
        for (ItemStack is : drops) {
            if (is == null) continue;
            l.getWorld().dropItemNaturally(l, is);
        }
        cm.set(path, null);
    }

    @EventHandler
    public void toxicSprayUse(PlayerInteractEvent e) {
        if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) return;
        final Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        final ItemStack hand = p.getItemInHand();
        if (hand.getType() != Material.INK_SACK) return;
        if (hand.getDurability() != (short) 4) return;
        ItemMeta him = hand.getItemMeta();
        ItemMeta fim = plugin.toxicspray.getItemMeta();
        String hdn = him.getDisplayName();
        if (hdn == null) return;
        if (!hdn.equals(fim.getDisplayName())) return;
        List<String> hl = him.getLore();
        if (hl == null) return;
        if (!hl.containsAll(fim.getLore())) return;
        PConfManager pcm = plugin.getUserdata(p);
        if (pcm.getBoolean("toxicspray_on", false)) return; // don't waste toxicsprays
        pcm.set("toxicspray_on", true);
        pcm.set("toxicspray_expire", new Date().getTime() + (Config.toxicDuration * 1000));
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
                p.sendMessage(ChatColor.BLUE + "You sprayed the toxic fumes on yourself. They will last for " + Config.toxicDuration + " seconds.");
            }
        });
    }

    private Inventory shuffleInventory(Inventory i) {
        List<Integer> unusedSlots = new ArrayList<Integer>();
        for (int slot = 0; slot < i.getSize(); slot++) unusedSlots.add(slot);
        ItemStack[] contents = i.getContents().clone();
        i.clear();
        for (ItemStack is : contents) {
            int slot = unusedSlots.get(r.nextInt(unusedSlots.size()));
            i.setItem(slot, is);
            unusedSlots.remove(Integer.valueOf(slot));
        }
        return i;
    }

    @EventHandler
    public void signListen(SignChangeEvent e) {
        /*
        0: [Loot]
        1: set name
        2: refill time (minutes)
        3: empty
         */
        Player p = e.getPlayer();
        if (!p.hasPermission("rsurv.loot")) return;
        final Block b = e.getBlock();
        if (!isInInfectedWorld(b.getLocation())) return;
        if (!e.getLine(0).equalsIgnoreCase("[Loot]")) return;
        if (e.getLine(1).trim().isEmpty()) {
            p.sendMessage(ChatColor.BLUE + "You must include the loot chest name.");
            return;
        }
        if (e.getLine(2).trim().isEmpty()) {
            p.sendMessage(ChatColor.BLUE + "You must include a refill time.");
            return;
        }
        int refill;
        try {
            refill = Integer.parseInt(e.getLine(2).trim());
        } catch (NumberFormatException ex) {
            p.sendMessage(ChatColor.BLUE + "Your refill time must be a whole number.");
            return;
        }
        if (refill < 1) {
            p.sendMessage(ChatColor.BLUE + "Your refill time must be more than zero.");
            return;
        }
        final LootChest lc = LootChest.getLootChest(e.getLine(1));
        if (lc == null) {
            p.sendMessage(ChatColor.BLUE + "No such loot chest.");
            return;
        }
        byte data = b.getData();
        b.setType(Material.AIR);
        b.setType(Material.CHEST);
        if (!(b.getState() instanceof Chest)) {
            p.sendMessage(ChatColor.BLUE + "An error ocurred. Please try again.");
            return;
        }
        Chest c = (Chest) b.getState();
        b.setData(signToChest(data));
        for (ItemStack is : lc.getRandomLoot()) c.getInventory().addItem(is);
        shuffleInventory(c.getInventory());
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = b.getLocation();
        String path = "lootchests." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        cm.set(path + ".enabled", true);
        cm.set(path + ".loot_chest", e.getLine(1).trim());
        cm.set(path + ".refill", refill);
    }

    private byte signToChest(byte b) {
        switch (b) {
            case 0x0: // south
            case 0x1: // south-southwest
            case 0xF: // south-southeast
                return 0x3; // south
            case 0x2: // southwest
            case 0x3: // west-southwest
            case 0x4: // west
            case 0x5: // west-northwest
            case 0x6: // northwest
                return 0x4; // west
            case 0x7: // north-northwest
            case 0x8: // north
            case 0x9: // north-northeast
                return 0x2; // north
            case 0xA: // northeast
            case 0xB: // east-northeast
            case 0xC: // east
            case 0xD: // east-southeast
            case 0xE: // southeast
                return 0x5; // east
        }
        return b;
    }

    @EventHandler
    public void breakLootChest(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (!isInInfectedWorld(b.getLocation())) return;
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = b.getLocation();
        String path = "lootchests." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        if (!cm.getBoolean(path + ".enabled", false)) return;
        if (!p.hasPermission("rsurv.loot") && !Config.allowLootChestBreak) {
            e.setCancelled(true);
            return;
        }
        cm.set(path, null);
        p.sendMessage(ChatColor.BLUE + "You have broken a loot chest.");
    }

    @EventHandler
    public void placeRepairChest(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!isInInfectedWorld(p)) return;
        ItemStack hand = e.getItemInHand();
        if (hand == null) return;
        if (hand.getType() != Material.CHEST) return;
        ItemMeta him = hand.getItemMeta();
        ItemMeta fim = plugin.repairChest.getItemMeta();
        String hdn = him.getDisplayName();
        if (hdn == null) return;
        if (!hdn.equals(fim.getDisplayName())) return;
        List<String> hl = him.getLore();
        if (hl == null) return;
        if (!hl.containsAll(fim.getLore())) return;
        Block b = e.getBlockPlaced();
        if (!(b.getState() instanceof Chest)) return;
        Chest c = (Chest) b.getState();
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = c.getLocation();
        String path = "repairchests." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        cm.set(path + ".enabled", true);
    }

    @EventHandler
    public void breakRepairChest(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!isInInfectedWorld(b.getLocation())) return;
        ConfManager cm = plugin.getConfig("otherdata.yml");
        Location l = b.getLocation();
        String path = "repairchests." + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        if (!cm.getBoolean(path + ".enabled", false)) return;
        cm.set(path, null);
        e.setCancelled(true);
        Collection<ItemStack> drops = e.getBlock().getDrops();
        e.getBlock().setType(Material.AIR);
        l.getWorld().dropItemNaturally(l, plugin.repairChest);
        for (ItemStack is : drops) {
            if (is == null) continue;
            if (is.getType() == Material.CHEST) continue;
            l.getWorld().dropItemNaturally(l, is);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void squidLoot(EntityDamageByEntityEvent e) {
        if (!Config.useSquidLoot) return;
        if (r.nextInt(100) > Config.squidLootChance) return;
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        Entity ent = e.getEntity();
        if (!(ent instanceof Squid)) return;
        Squid squid = (Squid) ent;
        if (squid.isDead()) return;
        if (squid.getHealth() <= 0) return;
        if (squid.getHealth() - e.getDamage() > 0) return;
        if (!isInInfectedWorld(squid) || !isInInfectedWorld(p)) return;
        List<String> lootSets = Config.squidLootSets;
        if (lootSets.isEmpty()) return;
        String lootSet = lootSets.get(r.nextInt(lootSets.size()));
        final LootChest lc = LootChest.getLootChest(lootSet);
        if (lc == null) return;
        Location l = squid.getLocation();
        World w = l.getWorld();
        for (ItemStack drop : lc.getRandomLoot()) w.dropItemNaturally(l, drop);
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (isInInfectedWorld(p)) setPlayerCharacteristics(p);
        else resetPlayerCharacteristics(p);
    }
}
