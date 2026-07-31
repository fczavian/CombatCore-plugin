/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.combatcore.command;

import com.combatcore.CombatCorePlugin;
import com.combatcore.config.ConfigManager;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CombatCoreCommand
implements CommandExecutor {
    private final CombatCorePlugin plugin;

    public CombatCoreCommand(CombatCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("combatcore.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "CombatCore commands: reload, debug, toggle, stats");
            return true;
        }
        String sub = args[0].toLowerCase();
        ConfigManager configManager = this.plugin.getConfigManager();
        block8 : switch (sub) {
            case "reload": {
                configManager.reload();
                sender.sendMessage(ChatColor.GREEN + "CombatCore configuration reloaded.");
                break;
            }
            case "debug": {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    player.sendMessage(ChatColor.YELLOW + "CombatCore debug: " + this.plugin.getConfigManager().getCombatConfig().tagDuration + "s tag.");
                    break;
                }
                sender.sendMessage(ChatColor.YELLOW + "CombatCore debug is only available for players.");
                break;
            }
            case "toggle": {
                if (sender instanceof Player) {
                    Player player;
                    boolean newState = this.plugin.togglePlayer((player = (Player)sender).getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "CombatCore toggle: " + (newState ? "ON" : "OFF"));
                    break;
                }
                sender.sendMessage(ChatColor.YELLOW + "Toggle is a player-only command.");
                break;
            }
            case "stats": {
                if (args.length > 1 && sender.hasPermission("combatcore.admin")) {
                    String targetName = args[1];
                    sender.sendMessage(ChatColor.GREEN + "Stats for " + targetName + " are not available in this stub build.");
                    break;
                }
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    int offenses = this.plugin.getDatabaseManager().getOffenseCount(player.getUniqueId());
                    sender.sendMessage(ChatColor.GREEN + "CombatCore is active; your offense count: " + offenses);
                    break;
                }
                sender.sendMessage(ChatColor.GREEN + "CombatCore is active; database type: " + this.plugin.getConfig().getString("database.type", "sqlite"));
                break;
            }
            case "bounty": {
                String action;
                if (!sender.hasPermission("combatcore.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to manage bounties.");
                    break;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /combatcore bounty <add|remove|list> ...");
                    break;
                }
                switch (action = args[1].toLowerCase()) {
                    case "add": {
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.YELLOW + "Usage: /combatcore bounty add <player> <amount>");
                            break;
                        }
                        String target = args[2];
                        try {
                            double amount = Double.parseDouble(args[3]);
                            this.plugin.addBounty(target, amount);
                            sender.sendMessage(ChatColor.GREEN + "Added bounty of " + amount + " on " + target + ".");
                        }
                        catch (NumberFormatException ex) {
                            sender.sendMessage(ChatColor.RED + "Invalid amount.");
                        }
                        break;
                    }
                    case "remove": {
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.YELLOW + "Usage: /combatcore bounty remove <player>");
                            break;
                        }
                        String target = args[2];
                        this.plugin.removeBounty(target);
                        sender.sendMessage(ChatColor.GREEN + "Removed bounty on " + target + ".");
                        break;
                    }
                    case "list": {
                        if (this.plugin.getBounties().isEmpty()) {
                            sender.sendMessage(ChatColor.YELLOW + "No active bounties.");
                            break;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Active bounties:");
                        for (Map.Entry<String, Double> e : this.plugin.getBounties().entrySet()) {
                            sender.sendMessage(ChatColor.GRAY + " - " + e.getKey() + ": " + e.getValue());
                        }
                        break block8;
                    }
                    default: {
                        sender.sendMessage(ChatColor.RED + "Unknown bounty action.");
                        break;
                    }
                }
                break;
            }
            default: {
                sender.sendMessage(ChatColor.RED + "Unknown CombatCore subcommand.");
            }
        }
        return true;
    }
}
