/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.boss.BossBar
 *  org.bukkit.entity.Player
 *  org.bukkit.scheduler.BukkitTask
 */
package com.combatcore.data;

import com.combatcore.data.CombatCause;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class CombatProfile {
    private final Player player;
    private final UUID attackerUuid;
    private final CombatCause cause;
    private final Instant taggedAt;
    private long expiresAt;
    private BukkitTask expirationTask;
    private BossBar bossBar;

    public CombatProfile(Player player, UUID attackerUuid, CombatCause cause, long expirationTick) {
        this.player = player;
        this.attackerUuid = attackerUuid;
        this.cause = cause;
        this.taggedAt = Instant.now();
        this.expiresAt = System.currentTimeMillis() + expirationTick * 50L;
    }

    public Player getPlayer() {
        return this.player;
    }

    public UUID getAttackerUuid() {
        return this.attackerUuid;
    }

    public CombatCause getCause() {
        return this.cause;
    }

    public long getRemainingMillis() {
        return Math.max(0L, this.expiresAt - System.currentTimeMillis());
    }

    public int getRemainingSeconds() {
        return (int)Math.ceil((double)this.getRemainingMillis() / 1000.0);
    }

    public void extend(long extraTicks) {
        this.expiresAt = Math.max(this.expiresAt, System.currentTimeMillis() + extraTicks * 50L);
    }

    public void setExpirationTask(BukkitTask expirationTask) {
        if (this.expirationTask != null) {
            this.expirationTask.cancel();
        }
        this.expirationTask = expirationTask;
    }

    public void cancelExpirationTask() {
        if (this.expirationTask != null) {
            this.expirationTask.cancel();
            this.expirationTask = null;
        }
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public BossBar getBossBar() {
        return this.bossBar;
    }
}
