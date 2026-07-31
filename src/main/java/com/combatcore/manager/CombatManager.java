/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.boss.BarColor
 *  org.bukkit.boss.BarFlag
 *  org.bukkit.boss.BarStyle
 *  org.bukkit.boss.BossBar
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityShootBowEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.entity.ProjectileLaunchEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitTask
 */
package com.combatcore.manager;

import com.combatcore.CombatCorePlugin;
import com.combatcore.config.CombatConfig;
import com.combatcore.data.CombatCause;
import com.combatcore.data.CombatProfile;
import com.combatcore.manager.DatabaseManager;
import com.combatcore.manager.DiscordWebhookManager;
import com.combatcore.manager.PlaceholderManager;
import com.combatcore.manager.PunishmentManager;
import com.combatcore.util.TextUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

public class CombatManager {
    private final CombatCorePlugin plugin;
    private final CombatConfig config;
    private final PunishmentManager punishmentManager;
    private final DatabaseManager databaseManager;
    private final PlaceholderManager placeholderManager;
    private final Map<UUID, CombatProfile> combatProfiles = new HashMap<UUID, CombatProfile>();
    private final Set<UUID> recentlyDied = new HashSet<UUID>();
    private final Map<UUID, UUID> projectileShooters = new HashMap<UUID, UUID>();
    private final Map<UUID, UUID> explosiveOwners = new HashMap<UUID, UUID>();
    private final BukkitTask updateTask;

    public CombatManager(CombatCorePlugin plugin, CombatConfig config, PunishmentManager punishmentManager, DatabaseManager databaseManager, PlaceholderManager placeholderManager) {
        this.plugin = plugin;
        this.config = config;
        this.punishmentManager = punishmentManager;
        this.databaseManager = databaseManager;
        this.placeholderManager = placeholderManager;
        this.updateTask = Bukkit.getScheduler().runTaskTimer((Plugin)plugin, this::updateHuds, (long)config.actionBarIntervalTicks, (long)config.actionBarIntervalTicks);
    }

    private void updateHuds() {
        for (CombatProfile profile : this.combatProfiles.values()) {
            this.showCombatHud(profile);
        }
    }

    public void handleDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }
    }

    public void handleDirectDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player victim = (Player)entity;
        Player attacker = this.resolveAttacker(event);
        CombatCause cause = this.resolveCause(event, attacker != null);
        if (cause == CombatCause.UNKNOWN || !this.shouldTagForCause(cause)) {
            return;
        }
        if (attacker == null && (attacker = this.getRecentAttacker(victim)) == null) {
            return;
        }
        if (this.config.lockedCombat && !attacker.equals(victim)) {
            CombatProfile victimProfile = this.combatProfiles.get(victim.getUniqueId());
            CombatProfile attackerProfile = this.combatProfiles.get(attacker.getUniqueId());
            if (victimProfile != null && !attacker.getUniqueId().equals(victimProfile.getAttackerUuid())) {
                event.setCancelled(true);
                return;
            }
            if (attackerProfile != null && !victim.getUniqueId().equals(attackerProfile.getAttackerUuid())) {
                event.setCancelled(true);
                return;
            }
        }
        this.tagPlayer(victim, attacker, cause);
        if (!victim.equals(attacker)) {
            this.tagPlayer(attacker, victim, cause);
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        UUID shooterUuid;
        Entity damager = event.getDamager();
        if (damager instanceof Player) {
            Player player = (Player)damager;
            return player;
        }
        EntityType type = damager.getType();
        if ((type == EntityType.ARROW || type == EntityType.TRIDENT || type == EntityType.SPECTRAL_ARROW) && (shooterUuid = this.projectileShooters.remove(damager.getUniqueId())) != null) {
            return this.plugin.getServer().getPlayer(shooterUuid);
        }
        UUID ownerUuid = this.explosiveOwners.remove(damager.getUniqueId());
        if (ownerUuid != null) {
            return this.plugin.getServer().getPlayer(ownerUuid);
        }
        return null;
    }

    public void trackExplosive(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player attacker = (Player)entity;
        EntityType targetType = event.getEntity().getType();
        if (targetType != EntityType.ENDER_CRYSTAL) {
            return;
        }
        UUID crystalId = event.getEntity().getUniqueId();
        UUID attackerId = attacker.getUniqueId();
        this.explosiveOwners.put(crystalId, attackerId);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.explosiveOwners.remove(crystalId), 200L);
    }

    public void trackProjectile(EntityShootBowEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player shooter = (Player)entity;
        Entity projectile = event.getProjectile();
        if (projectile == null) {
            return;
        }
        UUID projectileId = projectile.getUniqueId();
        this.projectileShooters.put(projectileId, shooter.getUniqueId());
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.projectileShooters.remove(projectileId), 600L);
    }

    public void trackProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile == null) {
            return;
        }
        ProjectileSource shooterSource = projectile.getShooter();
        if (!(shooterSource instanceof Player)) {
            return;
        }
        Player shooter = (Player)shooterSource;
        UUID projectileId = projectile.getUniqueId();
        this.projectileShooters.put(projectileId, shooter.getUniqueId());
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.projectileShooters.remove(projectileId), 600L);
    }

    private CombatCause resolveCause(EntityDamageByEntityEvent event, boolean directAttacker) {
        EntityDamageEvent.DamageCause damageCause = event.getCause();
        switch (damageCause) {
            case ENTITY_ATTACK: {
                return CombatCause.MELEE;
            }
            case PROJECTILE: {
                if (!directAttacker) {
                    return CombatCause.UNKNOWN;
                }
                EntityType damagerType = event.getDamager().getType();
                if (damagerType == EntityType.TRIDENT) {
                    return CombatCause.TRIDENT;
                }
                return CombatCause.BOW;
            }
            case ENTITY_EXPLOSION: 
            case BLOCK_EXPLOSION: {
                return CombatCause.TNT;
            }
            case CUSTOM: {
                return CombatCause.UNKNOWN;
            }
        }
        return CombatCause.UNKNOWN;
    }

    private boolean shouldTagForCause(CombatCause cause) {
        return switch (cause) {
            case MELEE -> this.config.enableMelee;
            case BOW -> this.config.enableBow;
            case TNT -> this.config.enableTnt;
            case CRYSTAL -> this.config.enableCrystal;
            case ANCHOR -> this.config.enableAnchor;
            case TRIDENT -> this.config.enableTrident;
            default -> false;
        };
    }

    private Player getRecentAttacker(Player victim) {
        CombatProfile profile = this.combatProfiles.get(victim.getUniqueId());
        if (profile == null) {
            return null;
        }
        UUID attackerUuid = profile.getAttackerUuid();
        return attackerUuid == null ? null : this.plugin.getServer().getPlayer(attackerUuid);
    }

    public void tagPlayer(Player victim, Player attacker, CombatCause cause) {
        int durationSeconds = this.config.tagDuration;
        int durationTicks = durationSeconds * 20;
        CombatProfile existing = this.combatProfiles.get(victim.getUniqueId());
        if (existing != null) {
            if (this.config.extendOnHit) {
                existing.extend(durationTicks);
                this.scheduleExpiration(victim.getUniqueId(), durationTicks);
            }
            this.showCombatHud(existing);
            return;
        }
        CombatProfile profile = new CombatProfile(victim, attacker.getUniqueId(), cause, durationTicks);
        this.combatProfiles.put(victim.getUniqueId(), profile);
        this.scheduleExpiration(victim.getUniqueId(), durationTicks);
        this.showCombatHud(profile);
        if (this.config.enableChatMessages) {
            victim.sendMessage(this.formatMessage(victim, this.config.messages.getOrDefault("tagged", "&cYou are now in combat for {time}s."), Map.of("time", String.valueOf(profile.getRemainingSeconds()))));
        }
    }

    private void scheduleExpiration(UUID target, int ticks) {
        CombatProfile profile = this.combatProfiles.get(target);
        if (profile == null) {
            return;
        }
        profile.setExpirationTask(Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.expireTag(target), (long)ticks));
    }

    private void expireTag(UUID target) {
        CombatProfile profile = this.combatProfiles.remove(target);
        if (profile == null) {
            return;
        }
        profile.cancelExpirationTask();
        Player player = profile.getPlayer();
        if (player != null && player.isOnline()) {
            String expireMsg = this.formatMessage(player, this.config.messages.getOrDefault("combat-ended", "&aYou are no longer in combat now."), Map.of());
            if (this.config.enableChatMessages) {
                player.sendMessage(expireMsg);
            }
            if (this.config.enableActionBar) {
                player.sendActionBar(expireMsg);
            }
            if (this.config.enableBossBar && profile.getBossBar() != null) {
                BossBar bar = profile.getBossBar();
                bar.setTitle(expireMsg);
                bar.setColor(BarColor.GREEN);
                bar.setProgress(1.0);
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> bar.removeAll(), 60L);
            } else if (profile.getBossBar() != null) {
                profile.getBossBar().removeAll();
            }
        } else if (profile.getBossBar() != null) {
            profile.getBossBar().removeAll();
        }
    }

    public void handleDeath(PlayerDeathEvent event) {
        CombatProfile opponentProfile;
        Player dead = event.getEntity();
        CombatProfile deadProfile = this.combatProfiles.get(dead.getUniqueId());
        if (deadProfile == null) {
            return;
        }
        this.recentlyDied.add(dead.getUniqueId());
        UUID opponentUuid = deadProfile.getAttackerUuid();
        CombatProfile removed = this.combatProfiles.remove(dead.getUniqueId());
        if (removed != null) {
            removed.cancelExpirationTask();
            if (removed.getBossBar() != null) {
                removed.getBossBar().removeAll();
            }
        }
        if (opponentUuid != null && (opponentProfile = this.combatProfiles.get(opponentUuid)) != null && dead.getUniqueId().equals(opponentProfile.getAttackerUuid())) {
            this.expireTag(opponentUuid);
        }
    }

    private String formatMessage(Player player, String template, Map<String, String> values) {
        String result = TextUtil.replaceTokens(template, values);
        if (this.placeholderManager != null) {
            result = this.placeholderManager.applyPlaceholders(player, result);
        }
        return TextUtil.color(result);
    }

    public boolean isTagged(Player player) {
        return this.combatProfiles.containsKey(player.getUniqueId());
    }

    public void refreshCombatTag(Player player) {
        CombatProfile profile = this.combatProfiles.get(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.extend((long)this.config.safezoneExtendSeconds * 20L);
        int totalRemainingTicks = (int)Math.ceil((double)profile.getRemainingMillis() / 50.0);
        this.scheduleExpiration(player.getUniqueId(), totalRemainingTicks);
        this.showCombatHud(profile);
    }

    public void handleQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.recentlyDied.remove(player.getUniqueId())) {
            this.combatProfiles.remove(player.getUniqueId());
            return;
        }
        CombatProfile profile = this.combatProfiles.remove(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.cancelExpirationTask();
        if (this.config.punishmentsEnabled) {
            Player attacker = this.plugin.getServer().getPlayer(profile.getAttackerUuid());
            List<String> punishments = this.punishmentManager.applyLogoutPunishment(player, attacker, profile.getCause());
            this.databaseManager.recordCombatDeath(profile.getAttackerUuid(), player.getUniqueId(), profile.getCause());
            this.databaseManager.recordOffense(player.getUniqueId());
            String attackerName = attacker != null ? attacker.getName() : "Attacker";
            DiscordWebhookManager.sendKillWebhook(this.config.discordWebhookUrl, attackerName, player.getName(), profile.getCause().name());
            if (!punishments.isEmpty()) {
                DiscordWebhookManager.sendPunishmentWebhook(this.config.discordWebhookUrl, player.getName(), punishments);
            }
            if (attacker != null && attacker.isOnline()) {
                attacker.sendMessage(this.formatMessage(attacker, this.config.messages.getOrDefault("kill-credit", "&6{attacker} defeated {victim} while tagged."), Map.of("attacker", attacker.getName(), "victim", player.getName())));
            }
        }
    }

    public void showCombatHud(CombatProfile profile) {
        if (this.config.enableActionBar) {
            String text = this.formatMessage(profile.getPlayer(), this.config.actionBarTemplate, Map.of("time", String.valueOf(profile.getRemainingSeconds())));
            profile.getPlayer().sendActionBar(text);
        }
        if (this.config.enableBossBar) {
            String barTitle = this.formatMessage(profile.getPlayer(), this.config.bossBarTemplate, Map.of("time", String.valueOf(profile.getRemainingSeconds())));
            BossBar bossBar = profile.getBossBar();
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar((String)barTitle, (BarColor)BarColor.RED, (BarStyle)BarStyle.SOLID, (BarFlag[])new BarFlag[0]);
                bossBar.addPlayer(profile.getPlayer());
                profile.setBossBar(bossBar);
            }
            bossBar.setTitle(barTitle);
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double)profile.getRemainingMillis() / ((double)this.config.tagDuration * 1000.0))));
        }
    }

    public void shutdown() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
        }
        for (CombatProfile profile : this.combatProfiles.values()) {
            if (profile.getBossBar() != null) {
                profile.getBossBar().removeAll();
            }
            profile.cancelExpirationTask();
        }
        this.combatProfiles.clear();
    }
}
