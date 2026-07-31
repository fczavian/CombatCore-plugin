/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityShootBowEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.entity.ProjectileLaunchEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package com.combatcore.listener;

import com.combatcore.CombatCorePlugin;
import com.combatcore.manager.CombatManager;
import com.combatcore.manager.CommandRestrictionManager;
import com.combatcore.manager.SafezoneIntegrationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener
implements Listener {
    private final CombatCorePlugin plugin;
    private final CombatManager combatManager;
    private final CommandRestrictionManager commandRestrictionManager;
    private final SafezoneIntegrationManager safezoneIntegrationManager;

    public CombatListener(CombatCorePlugin plugin, CombatManager combatManager, CommandRestrictionManager commandRestrictionManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.commandRestrictionManager = commandRestrictionManager;
        this.safezoneIntegrationManager = new SafezoneIntegrationManager(plugin);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getPunishmentManager().applyPendingPunishments(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        this.combatManager.trackProjectile(event);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        this.combatManager.trackProjectileLaunch(event);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        this.combatManager.trackExplosive(event);
        this.combatManager.handleDirectDamage(event);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onEntityDamage(EntityDamageEvent event) {
        this.combatManager.handleDamage(event);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.combatManager.handleDeath(event);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.combatManager.handleQuit(event);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!this.combatManager.isTagged(event.getPlayer())) {
            return;
        }
        if (this.commandRestrictionManager.shouldBlock(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(this.commandRestrictionManager.getBlockedMessage());
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        this.safezoneIntegrationManager.onPlayerMove(event);
    }
}
