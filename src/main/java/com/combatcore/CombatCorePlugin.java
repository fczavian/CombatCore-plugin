/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.combatcore;

import com.combatcore.command.CombatCoreCommand;
import com.combatcore.config.ConfigManager;
import com.combatcore.listener.CombatListener;
import com.combatcore.manager.CombatManager;
import com.combatcore.manager.CommandRestrictionManager;
import com.combatcore.manager.DatabaseManager;
import com.combatcore.manager.DiscordWebhookManager;
import com.combatcore.manager.PlaceholderManager;
import com.combatcore.manager.PunishmentManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatCorePlugin
extends JavaPlugin {
    private ConfigManager configManager;
    private CombatManager combatManager;
    private DatabaseManager databaseManager;
    private CommandRestrictionManager commandRestrictionManager;
    private PunishmentManager punishmentManager;
    private PlaceholderManager placeholderManager;
    private DiscordWebhookManager discordWebhookManager;
    private final Map<UUID, Boolean> playerToggles = new HashMap<UUID, Boolean>();
    private final Map<String, Double> bounties = new HashMap<String, Double>();

    public void onEnable() {
        this.saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this, this.configManager.getCombatConfig());
        this.punishmentManager = new PunishmentManager(this.configManager.getCombatConfig(), this.databaseManager);
        this.placeholderManager = new PlaceholderManager(this);
        this.commandRestrictionManager = new CommandRestrictionManager(this.configManager.getCombatConfig());
        this.combatManager = new CombatManager(this, this.configManager.getCombatConfig(), this.punishmentManager, this.databaseManager, this.placeholderManager);
        this.discordWebhookManager = new DiscordWebhookManager(this.configManager.getCombatConfig().discordWebhookUrl);
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents((Listener)new CombatListener(this, this.combatManager, this.commandRestrictionManager), (Plugin)this);
        if (this.getCommand("combatcore") != null) {
            this.getCommand("combatcore").setExecutor((CommandExecutor)new CombatCoreCommand(this));
        }
        this.getLogger().info("CombatCore enabled with advanced PvP combat tagging and logging.");
    }

    public void onDisable() {
        if (this.combatManager != null) {
            this.combatManager.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }
        this.getLogger().info("CombatCore disabled.");
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public CombatManager getCombatManager() {
        return this.combatManager;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public PunishmentManager getPunishmentManager() {
        return this.punishmentManager;
    }

    public CommandRestrictionManager getCommandRestrictionManager() {
        return this.commandRestrictionManager;
    }

    public DiscordWebhookManager getDiscordWebhookManager() {
        return this.discordWebhookManager;
    }

    public boolean togglePlayer(UUID playerUuid) {
        boolean newState = this.playerToggles.getOrDefault(playerUuid, false) == false;
        this.playerToggles.put(playerUuid, newState);
        return newState;
    }

    public boolean isPlayerToggled(UUID playerUuid) {
        return this.playerToggles.getOrDefault(playerUuid, false);
    }

    public void addBounty(String target, double amount) {
        this.bounties.put(target.toLowerCase(), this.bounties.getOrDefault(target.toLowerCase(), 0.0) + amount);
    }

    public void removeBounty(String target) {
        this.bounties.remove(target.toLowerCase());
    }

    public Map<String, Double> getBounties() {
        return this.bounties;
    }
}
