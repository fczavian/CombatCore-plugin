/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.util.Vector
 */
package com.combatcore.manager;

import com.combatcore.CombatCorePlugin;
import com.combatcore.config.CombatConfig;
import com.combatcore.util.TextUtil;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;

public class SafezoneIntegrationManager {
    private final CombatCorePlugin plugin;
    private final CombatConfig config;
    private boolean worldGuard7Installed;
    private boolean worldGuard6Installed;
    private boolean townyInstalled;
    private boolean factionsInstalled;
    private final Map<UUID, Long> wallCooldowns = new HashMap<UUID, Long>();
    private Method wg7GetInstance;
    private Method wg7GetPlatform;
    private Method wg7GetRegionContainer;
    private Method wg7CreateQuery;
    private Method wg7Adapt;
    private Method wg7GetApplicableRegions;
    private Method wg7QueryState;
    private Method wg7WrapPlayer;
    private Object wg7WorldGuardPluginInst;
    private Object wg7PvpFlag;
    private Object wg7PvpFlagArray;
    private Object wg7StateDeny;
    private Class<?> wg6PluginClass;
    private Method wg6GetRegionManager;
    private Method wg6GetApplicableRegions;
    private Method wg6QueryState;
    private Method wg6WrapPlayer;
    private Object wg6PvpFlag;
    private Object wg6PvpFlagArray;
    private Object wg6StateDeny;
    private Method townyApiGetInstance;
    private Method townyIsWilderness;
    private Method factionsGetInstance;
    private Method factionsGetByPlayer;
    private Method factionsGetFaction;
    private Method factionsIsSafeZone;

    public SafezoneIntegrationManager(CombatCorePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getCombatConfig();
        PluginManager manager = plugin.getServer().getPluginManager();
        if (manager.getPlugin("WorldGuard") != null) {
            this.setupWorldGuard();
        }
        if (manager.getPlugin("Towny") != null) {
            this.setupTowny();
        }
        if (manager.getPlugin("Factions") != null) {
            this.setupFactions();
        }
    }

    private void setupWorldGuard() {
        boolean isWg7;
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            isWg7 = true;
        }
        catch (ClassNotFoundException e) {
            isWg7 = false;
        }
        if (isWg7) {
            this.setupWorldGuard7();
        } else {
            this.setupWorldGuard6();
        }
    }

    private void setupWorldGuard7() {
        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            this.wg7GetInstance = wgClass.getMethod("getInstance", new Class[0]);
            this.wg7GetPlatform = wgClass.getMethod("getPlatform", new Class[0]);
            Object wgInstance = this.wg7GetInstance.invoke(null, new Object[0]);
            Object platform = this.wg7GetPlatform.invoke(wgInstance, new Object[0]);
            this.wg7GetRegionContainer = platform.getClass().getMethod("getRegionContainer", new Class[0]);
            Object regionContainer = this.wg7GetRegionContainer.invoke(platform, new Object[0]);
            this.wg7CreateQuery = regionContainer.getClass().getMethod("createQuery", new Class[0]);
            Object query = this.wg7CreateQuery.invoke(regionContainer, new Object[0]);
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            this.wg7Adapt = bukkitAdapter.getMethod("adapt", Location.class);
            Class<?> weLocationClass = Class.forName("com.sk89q.worldedit.util.Location");
            this.wg7GetApplicableRegions = query.getClass().getMethod("getApplicableRegions", weLocationClass);
            Class<?> applicableRegionSetClass = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            this.wg7PvpFlag = flagsClass.getField("PVP").get(null);
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            this.wg7PvpFlagArray = Array.newInstance(stateFlagClass, 1);
            Array.set(this.wg7PvpFlagArray, 0, this.wg7PvpFlag);
            Class<?> regionAssociableClass = Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable");
            Class<?> stateFlagArrayClass = Class.forName("[Lcom.sk89q.worldguard.protection.flags.StateFlag;");
            this.wg7QueryState = applicableRegionSetClass.getMethod("queryState", regionAssociableClass, stateFlagArrayClass);
            Class<?> stateFlagStateClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag$State");
            this.wg7StateDeny = stateFlagStateClass.getField("DENY").get(null);
            Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            this.wg7WorldGuardPluginInst = wgPluginClass.getMethod("inst", new Class[0]).invoke(null, new Object[0]);
            this.wg7WrapPlayer = wgPluginClass.getMethod("wrapPlayer", Player.class);
            this.worldGuard7Installed = true;
        }
        catch (Exception ex) {
            this.worldGuard7Installed = false;
            this.plugin.getLogger().warning("[CombatCore] Failed to initialize WorldGuard 7 integration: " + ex.getMessage());
        }
    }

    private void setupWorldGuard6() {
        try {
            this.wg6PluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            this.wg6GetRegionManager = this.wg6PluginClass.getMethod("getRegionManager", World.class);
            Class<?> regionManagerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
            this.wg6GetApplicableRegions = regionManagerClass.getMethod("getApplicableRegions", Location.class);
            Class<?> applicableRegionSetClass = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
            Class<?> defaultFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.DefaultFlag");
            this.wg6PvpFlag = defaultFlagClass.getField("PVP").get(null);
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            this.wg6PvpFlagArray = Array.newInstance(stateFlagClass, 1);
            Array.set(this.wg6PvpFlagArray, 0, this.wg6PvpFlag);
            Class<?> regionAssociableClass = Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable");
            Class<?> stateFlagArrayClass = Class.forName("[Lcom.sk89q.worldguard.protection.flags.StateFlag;");
            this.wg6QueryState = applicableRegionSetClass.getMethod("queryState", regionAssociableClass, stateFlagArrayClass);
            Class<?> stateFlagStateClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag$State");
            this.wg6StateDeny = stateFlagStateClass.getField("DENY").get(null);
            this.wg6WrapPlayer = this.wg6PluginClass.getMethod("wrapPlayer", Player.class);
            this.worldGuard6Installed = true;
        }
        catch (Exception ex) {
            this.worldGuard6Installed = false;
            this.plugin.getLogger().warning("[CombatCore] Failed to initialize WorldGuard 6 integration: " + ex.getMessage());
        }
    }

    private void setupTowny() {
        try {
            Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            this.townyApiGetInstance = apiClass.getMethod("getInstance", new Class[0]);
            this.townyIsWilderness = apiClass.getMethod("isWilderness", Location.class);
            this.townyInstalled = true;
        }
        catch (Exception ignored) {
            this.townyInstalled = false;
        }
    }

    private void setupFactions() {
        try {
            Class<?> fPlayersClass = Class.forName("com.massivecraft.factions.FPlayers");
            this.factionsGetInstance = fPlayersClass.getMethod("getInstance", new Class[0]);
            this.factionsGetByPlayer = fPlayersClass.getMethod("getByPlayer", Player.class);
            Class<?> fPlayerClass = Class.forName("com.massivecraft.factions.FPlayer");
            this.factionsGetFaction = fPlayerClass.getMethod("getFaction", new Class[0]);
            Class<?> factionClass = Class.forName("com.massivecraft.factions.Faction");
            this.factionsIsSafeZone = factionClass.getMethod("isSafeZone", new Class[0]);
            this.factionsInstalled = true;
        }
        catch (Exception ignored) {
            this.factionsInstalled = false;
        }
    }

    public void onPlayerMove(PlayerMoveEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        if (this.config.safezoneIntegrationMode == null || this.config.safezoneIntegrationMode.equalsIgnoreCase("off")) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("combatcore.safezone.bypass")) {
            return;
        }
        if (this.locationsEqual(event.getFrom(), event.getTo())) {
            return;
        }
        boolean wasInSafezone = this.isInSafezone(player, event.getFrom());
        boolean isInSafezone = this.isInSafezone(player, event.getTo());
        if (wasInSafezone && !isInSafezone) {
            return;
        }
        if (!wasInSafezone && !isInSafezone) {
            return;
        }
        if (isInSafezone && this.config.safezoneIntegrationMode.equalsIgnoreCase("deny") && this.plugin.getCombatManager().isTagged(player)) {
            event.setCancelled(true);
            Vector knockback = event.getFrom().toVector().subtract(event.getTo().toVector()).normalize();
            if (Double.isNaN(knockback.getX()) || knockback.lengthSquared() == 0.0) {
                knockback = player.getLocation().getDirection().multiply(-1.0).setY(0.0);
            }
            knockback.multiply(0.5).setY(0.2);
            player.setVelocity(knockback);
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            if (!this.wallCooldowns.containsKey(uuid) || now - this.wallCooldowns.get(uuid) > 1000L) {
                this.wallCooldowns.put(uuid, now);
                this.showBorderWall(player, event.getTo().getBlock().getLocation());
                player.sendMessage(TextUtil.color(this.config.messages.getOrDefault("safezone-deny", "&cYou cannot enter a safe zone while combat tagged.")));
            }
            return;
        }
        if (isInSafezone && this.config.safezoneIntegrationMode.equalsIgnoreCase("extend") && this.plugin.getCombatManager().isTagged(player)) {
            this.plugin.getCombatManager().refreshCombatTag(player);
            player.sendMessage(TextUtil.color(this.config.messages.getOrDefault("safezone-extended", "&eYour combat tag has been extended while in a safe zone.")));
        }
    }

    private void showBorderWall(Player player, Location center) {
        HashSet<Location> blocksToSend = new HashSet<Location>();
        Vector dir = player.getLocation().getDirection().setY(0.0).normalize();
        if (dir.lengthSquared() == 0.0) {
            return;
        }
        Vector right = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
        for (int y = 0; y < 3; ++y) {
            for (int r = -1; r <= 1; ++r) {
                Location loc = center.clone().add(0.0, (double)y, 0.0).add(right.clone().multiply((double)r));
                blocksToSend.add(loc);
                player.sendBlockChange(loc, Bukkit.createBlockData((Material)Material.RED_STAINED_GLASS));
            }
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (Location loc : blocksToSend) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }, 40L);
    }

    public boolean isInSafezone(Player player) {
        if (player == null) {
            return false;
        }
        return this.isInSafezone(player, player.getLocation());
    }

    private boolean isInSafezone(Player player, Location location) {
        if (location == null) {
            return false;
        }
        if ((this.worldGuard7Installed || this.worldGuard6Installed) && this.isInWorldGuardRegion(player, location)) {
            return true;
        }
        if (this.townyInstalled && this.isInTownySafezone(location)) {
            return true;
        }
        return this.factionsInstalled && player != null && this.isInFactionsSafezone(player);
    }

    private boolean isInWorldGuardRegion(Player player, Location location) {
        try {
            if (this.worldGuard7Installed) {
                Object weLocation;
                Object wgInstance = this.wg7GetInstance.invoke(null, new Object[0]);
                Object platform = this.wg7GetPlatform.invoke(wgInstance, new Object[0]);
                Object regionContainer = this.wg7GetRegionContainer.invoke(platform, new Object[0]);
                Object query = this.wg7CreateQuery.invoke(regionContainer, new Object[0]);
                Object applicableRegionSet = this.wg7GetApplicableRegions.invoke(query, weLocation = this.wg7Adapt.invoke(null, location));
                if (applicableRegionSet == null) {
                    return false;
                }
                Object state = this.wg7QueryState.invoke(applicableRegionSet, null, this.wg7PvpFlagArray);
                return state != null && state.toString().equals("DENY");
            }
            if (this.worldGuard6Installed) {
                Plugin wgPlugin = this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
                if (wgPlugin == null || !this.wg6PluginClass.isInstance(wgPlugin)) {
                    return false;
                }
                Object regionManager = this.wg6GetRegionManager.invoke((Object)wgPlugin, location.getWorld());
                if (regionManager == null) {
                    return false;
                }
                Object applicableRegionSet = this.wg6GetApplicableRegions.invoke(regionManager, location);
                if (applicableRegionSet == null) {
                    return false;
                }
                Object state = this.wg6QueryState.invoke(applicableRegionSet, null, this.wg6PvpFlagArray);
                return state != null && state.toString().equals("DENY");
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().warning("[CombatCore] Reflection error in WorldGuard check: " + ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

    private boolean isInTownySafezone(Location location) {
        try {
            Object townyApiInstance = this.townyApiGetInstance.invoke(null, new Object[0]);
            Object result = this.townyIsWilderness.invoke(townyApiInstance, location);
            return result instanceof Boolean && (Boolean)result != false;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private boolean isInFactionsSafezone(Player player) {
        try {
            Object factionsInstance = this.factionsGetInstance.invoke(null, new Object[0]);
            Object fPlayer = this.factionsGetByPlayer.invoke(factionsInstance, player);
            if (fPlayer == null) {
                return false;
            }
            Object faction = this.factionsGetFaction.invoke(fPlayer, new Object[0]);
            if (faction == null) {
                return false;
            }
            Object result = this.factionsIsSafeZone.invoke(faction, new Object[0]);
            return result instanceof Boolean && (Boolean)result != false;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private boolean locationsEqual(Location a, Location b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getWorld() == b.getWorld() && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }
}
