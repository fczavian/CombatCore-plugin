/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 */
package com.combatcore.manager;

import com.combatcore.config.CombatConfig;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandRestrictionManager {
    private final CombatConfig config;
    private final Set<Pattern> regexPatterns;

    public CommandRestrictionManager(CombatConfig config) {
        this.config = config;
        this.regexPatterns = config.regexBlocked.stream().map(pattern -> Pattern.compile(pattern, 2)).collect(Collectors.toSet());
    }

    public boolean shouldBlock(Player player, String message) {
        if (!this.config.commandBlockDuringCombat) {
            return false;
        }
        if (!message.startsWith("/")) {
            return false;
        }
        String command = message.substring(1).split(" ")[0].toLowerCase();
        if (this.config.allowedCommands.contains(command)) {
            return false;
        }
        if (player.hasPermission("combatcore.commandblock.bypass")) {
            return false;
        }
        if (this.config.blockedCommands.contains(command)) {
            return true;
        }
        return this.regexPatterns.stream().anyMatch(pattern -> pattern.matcher(command).matches());
    }

    public String getBlockedMessage() {
        return ChatColor.RED + "You cannot use this command while in combat.";
    }
}
