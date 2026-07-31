/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 */
package com.combatcore.config;

import com.combatcore.CombatCorePlugin;
import com.combatcore.config.CombatConfig;
import java.util.HashSet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final CombatCorePlugin plugin;
    private CombatConfig combatConfig;

    public ConfigManager(CombatCorePlugin plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        this.plugin.reloadConfig();
        FileConfiguration config = this.plugin.getConfig();
        this.combatConfig = new CombatConfig();
        this.combatConfig.tagDuration = config.getInt("combat.tag-duration", 20);
        this.combatConfig.extendOnHit = config.getBoolean("combat.extend-on-hit", true);
        this.combatConfig.lockedCombat = config.getBoolean("combat.locked-combat", false);
        this.combatConfig.enableMelee = config.getBoolean("combat.causes.melee", true);
        this.combatConfig.enableBow = config.getBoolean("combat.causes.bow", true);
        this.combatConfig.enableTnt = config.getBoolean("combat.causes.tnt", true);
        this.combatConfig.enableCrystal = config.getBoolean("combat.causes.crystal", true);
        this.combatConfig.enableAnchor = config.getBoolean("combat.causes.anchor", true);
        this.combatConfig.enableTrident = config.getBoolean("combat.causes.trident", true);
        this.combatConfig.blockedCommands = new HashSet<String>(config.getStringList("command-restrictions.blacklist"));
        this.combatConfig.allowedCommands = new HashSet<String>(config.getStringList("command-restrictions.whitelist"));
        this.combatConfig.regexBlocked = new HashSet<String>(config.getStringList("command-restrictions.regex-blacklist"));
        this.combatConfig.commandBlockDuringCombat = config.getBoolean("command-restrictions.enabled", true);
        this.combatConfig.blockTabComplete = config.getBoolean("command-restrictions.block-tab-complete", true);
        this.combatConfig.enableActionBar = config.getBoolean("display.action-bar.enabled", true);
        this.combatConfig.enableBossBar = config.getBoolean("display.boss-bar.enabled", true);
        this.combatConfig.enableChatMessages = config.getBoolean("display.chat.enabled", true);
        this.combatConfig.actionBarTemplate = config.getString("display.action-bar.template", "<red>Combat:</red> <gray>{time}s left to tag");
        this.combatConfig.bossBarTemplate = config.getString("display.boss-bar.template", "<gradient:#ff5f6d:#ffc371>Combat Tag</gradient> - {time}s");
        this.combatConfig.actionBarIntervalTicks = config.getInt("display.action-bar.update-interval", 10);
        ConfigurationSection punish = config.getConfigurationSection("punishments");
        if (punish != null) {
            this.combatConfig.punishmentsEnabled = punish.getBoolean("enabled", true);
            this.combatConfig.punishmentsDeath = punish.getBoolean("death", true);
            this.combatConfig.lightning = punish.getBoolean("lightning", false);
            this.combatConfig.inventoryWipe = punish.getBoolean("inventory-wipe", false);
            this.combatConfig.economyDeduction = punish.getDouble("economy-deduction", 0.0);
            this.combatConfig.tempBan = punish.getInt("temporary-ban-seconds", 0);
            this.combatConfig.permBan = punish.getBoolean("permanent-ban", false);
            this.combatConfig.tempMute = punish.getInt("temporary-mute-seconds", 0);
            this.combatConfig.permMute = punish.getBoolean("permanent-mute", false);
            this.combatConfig.freeze = punish.getBoolean("freeze", false);
            this.combatConfig.jailLocation = punish.getString("jail-location", "");
            this.combatConfig.commandExecution = punish.getStringList("command-execution");
            this.combatConfig.potionEffects = punish.getStringList("potion-effects");
            this.combatConfig.statReduction = punish.getBoolean("stat-reduction", false);
            this.combatConfig.progressiveEnabled = punish.getBoolean("progressive.enabled", true);
            this.combatConfig.progressiveMultiplier = punish.getDouble("progressive.multiplier", 1.25);
            this.combatConfig.repeatOffenderThreshold = punish.getInt("progressive.repeat-offender-threshold", 3);
        } else {
            this.combatConfig.punishmentsEnabled = false;
            this.combatConfig.progressiveEnabled = false;
        }
        this.combatConfig.discordWebhookUrl = config.getString("discord-webhook.url", "");
        this.combatConfig.safezoneIntegrationMode = config.getString("safezone.integration-mode", "deny");
        this.combatConfig.safezoneExtendTag = config.getBoolean("safezone.extend-tag", true);
        this.combatConfig.safezoneExtendSeconds = config.getInt("safezone.extend-seconds", 6);
        this.combatConfig.loadMessages(config);
    }

    public CombatConfig getCombatConfig() {
        return this.combatConfig;
    }
}
