/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package com.combatcore.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

public class CombatConfig {
    public int tagDuration;
    public boolean extendOnHit;
    public boolean lockedCombat;
    public boolean punishmentsEnabled;
    public boolean punishmentsDeath;
    public boolean enableMelee;
    public boolean enableBow;
    public boolean enableTnt;
    public boolean enableCrystal;
    public boolean enableAnchor;
    public boolean enableTrident;
    public Set<String> blockedCommands;
    public Set<String> allowedCommands;
    public Set<String> regexBlocked;
    public boolean commandBlockDuringCombat;
    public boolean blockTabComplete;
    public boolean enableActionBar;
    public boolean enableBossBar;
    public boolean enableChatMessages;
    public String actionBarTemplate;
    public String bossBarTemplate;
    public int actionBarIntervalTicks;
    public boolean lightning;
    public boolean inventoryWipe;
    public double economyDeduction;
    public int tempBan;
    public boolean permBan;
    public int tempMute;
    public boolean permMute;
    public boolean freeze;
    public String jailLocation;
    public List<String> commandExecution;
    public List<String> potionEffects;
    public boolean statReduction;
    public boolean progressiveEnabled;
    public double progressiveMultiplier;
    public int repeatOffenderThreshold;
    public String discordWebhookUrl;
    public String safezoneIntegrationMode;
    public boolean safezoneExtendTag;
    public int safezoneExtendSeconds;
    public Map<String, String> messages = new HashMap<String, String>();

    public void loadMessages(FileConfiguration config) {
        this.messages.put("tagged", config.getString("messages.tagged", "&cYou are now in combat for {time}s."));
        this.messages.put("combat-ended", config.getString("messages.combat-ended", "&aYou are no longer in combat now."));
        this.messages.put("combat-logout", config.getString("messages.combat-logout", "&4Combat log! You have been punished."));
        this.messages.put("blocked-command", config.getString("messages.blocked-command", "&cYou cannot use this command while tagged."));
        this.messages.put("kill-credit", config.getString("messages.kill-credit", "&6{attacker} defeated {victim} while tagged."));
        this.messages.put("safezone-deny", config.getString("messages.safezone-deny", "&cYou cannot enter a safe zone while combat tagged."));
        this.messages.put("safezone-extended", config.getString("messages.safezone-extended", "&eYour combat tag has been extended while in a safe zone."));
    }
}
