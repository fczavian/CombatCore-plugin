/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.combatcore.manager;

import com.combatcore.config.CombatConfig;
import com.combatcore.data.CombatCause;
import com.combatcore.manager.DatabaseManager;
import com.combatcore.util.TextUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PunishmentManager {
    private final CombatConfig config;
    private final DatabaseManager databaseManager;

    public PunishmentManager(CombatConfig config, DatabaseManager databaseManager) {
        this.config = config;
        this.databaseManager = databaseManager;
    }

    public List<String> applyLogoutPunishment(Player victim, Player attacker, CombatCause cause) {
        int duration;
        double multiplier;
        ArrayList<String> appliedPunishments = new ArrayList<String>();
        victim.sendMessage(TextUtil.color(this.config.messages.getOrDefault("combat-logout", "&cCombat logging detected! Punishments applied.")));
        int offenses = this.databaseManager.getOffenseCount(victim.getUniqueId()) + 1;
        double d = multiplier = this.config.progressiveEnabled ? Math.pow(this.config.progressiveMultiplier, offenses - 1) : 1.0;
        if (this.config.punishmentsDeath) {
            for (int i = 0; i < 3; ++i) {
                victim.setNoDamageTicks(0);
                if (attacker != null) {
                    victim.damage(victim.getHealth() + 1000.0, (Entity)attacker);
                    continue;
                }
                victim.damage(victim.getHealth() + 1000.0);
            }
            appliedPunishments.add("Death (Totem Bypass)");
        }
        if (this.config.lightning) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
        }
        if (this.config.inventoryWipe) {
            victim.getInventory().clear();
            appliedPunishments.add("Inventory Wiped");
        }
        if (this.config.economyDeduction > 0.0) {
            double deducted = this.config.economyDeduction * multiplier;
            this.doEconomyDeduction(victim, deducted);
            appliedPunishments.add(String.format("Economy Deduction: $%.2f", deducted));
        }
        if (this.config.tempBan > 0) {
            duration = (int)((double)this.config.tempBan * multiplier);
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("tempban " + victim.getName() + " " + duration + "s Combat log offense"));
            appliedPunishments.add("Temp Banned: " + duration + "s (Reason: Combat log offense)");
        }
        if (this.config.permBan) {
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("ban " + victim.getName() + " Permanent combat log punishment"));
            appliedPunishments.add("Permanently Banned (Reason: Combat log punishment)");
        }
        if (this.config.tempMute > 0) {
            duration = (int)((double)this.config.tempMute * multiplier);
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("tempmute " + victim.getName() + " " + duration + "s Combat log mute"));
            appliedPunishments.add("Temp Muted: " + duration + "s");
        }
        if (this.config.permMute) {
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("mute " + victim.getName() + " Permanent combat log mute"));
            appliedPunishments.add("Permanently Muted");
        }
        if (this.config.freeze || !this.config.potionEffects.isEmpty()) {
            String potions;
            String string = potions = this.config.freeze ? "__freeze__" : "";
            if (!this.config.potionEffects.isEmpty()) {
                String joined = String.join((CharSequence)"|", this.config.potionEffects);
                potions = potions.isEmpty() ? joined : potions + "|" + joined;
            }
            this.databaseManager.savePendingPunishments(victim.getUniqueId(), this.config.freeze, potions);
            if (this.config.freeze) {
                appliedPunishments.add("Frozen (Pending Login)");
            }
        }
        if (!this.config.jailLocation.isEmpty()) {
            this.teleportToJail(victim, this.config.jailLocation);
            appliedPunishments.add("Jailed");
        }
        this.applyEffectCommands(victim);
        if (this.config.statReduction) {
            victim.sendMessage(ChatColor.GRAY + "Your combat stats have been reduced due to logging.");
            appliedPunishments.add("Stats Reduced");
        }
        return appliedPunishments;
    }

    public void applyPendingPunishments(Player player) {
        String[] pending = this.databaseManager.loadPendingPunishments(player.getUniqueId());
        if (pending == null) {
            return;
        }
        this.databaseManager.clearPendingPunishments(player.getUniqueId());
        boolean freeze = Boolean.parseBoolean(pending[0]);
        String potionData = pending[1];
        if (freeze) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 600, 6, false, false, false));
            player.sendMessage(ChatColor.RED + "You have been frozen for combat logging.");
        }
        if (potionData != null && !potionData.isEmpty()) {
            for (String raw : potionData.split("\\|")) {
                String[] parts;
                if (raw.equals("__freeze__") || (parts = raw.split(":")).length < 2) continue;
                try {
                    int amplifier;
                    PotionEffectType type = PotionEffectType.getByName((String)parts[0].toUpperCase());
                    int duration = Integer.parseInt(parts[1]) * 20;
                    int n = amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                    if (type == null) continue;
                    player.addPotionEffect(new PotionEffect(type, duration, amplifier));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
    }

    private void doEconomyDeduction(Player victim, double amount) {
        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("eco take " + victim.getName() + " " + amount));
    }

    private void teleportToJail(Player victim, String locationString) {
        String[] parts = locationString.split(",");
        if (parts.length < 4) {
            return;
        }
        try {
            String worldName = parts[0].trim();
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            Location jail = new Location(Bukkit.getWorld((String)worldName), x, y, z);
            victim.teleport(jail);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
    }

    private void applyEffectCommands(Player victim) {
        for (String command : this.config.commandExecution) {
            if (command == null || command.trim().isEmpty()) continue;
            String formatted = command.replace("{player}", victim.getName());
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)formatted);
        }
    }
}
