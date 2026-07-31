/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.combatcore.manager;

import com.combatcore.CombatCorePlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class LogoutNpcManager {
    private final CombatCorePlugin plugin;
    private final Map<UUID, Long> activeNpcs = new HashMap<UUID, Long>();

    public LogoutNpcManager(CombatCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnLogoutNpc(Player player, int lifetimeSeconds) {
        if (lifetimeSeconds <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        this.activeNpcs.put(playerId, System.currentTimeMillis() + (long)lifetimeSeconds * 1000L);
        this.plugin.getLogger().info("[CombatCore] Logout NPC placeholder created for " + player.getName() + " at " + player.getLocation() + " for " + lifetimeSeconds + " seconds.");
        this.scheduleNpcExpiration(playerId, lifetimeSeconds);
    }

    private void scheduleNpcExpiration(UUID playerId, int lifetimeSeconds) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep((long)lifetimeSeconds * 1000L);
            }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            this.activeNpcs.remove(playerId);
            this.plugin.getLogger().info("[CombatCore] Logout NPC placeholder expired for " + playerId);
        });
    }

    public void shutdown() {
        this.activeNpcs.clear();
    }
}
