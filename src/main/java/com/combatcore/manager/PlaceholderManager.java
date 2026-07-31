/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.combatcore.manager;

import com.combatcore.CombatCorePlugin;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlaceholderManager {
    private final CombatCorePlugin plugin;
    private final boolean enabled;
    private final Method setPlaceholdersMethod;
    private final Object placeholderApiClass;

    public PlaceholderManager(CombatCorePlugin plugin) {
        this.plugin = plugin;
        boolean available = false;
        Method setPlaceholders = null;
        Class<?> apiClass = null;
        Plugin placeholderPlugin = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderPlugin != null) {
            try {
                Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                setPlaceholders = clazz.getMethod("setPlaceholders", Player.class, String.class);
                apiClass = clazz;
                available = true;
            }
            catch (Exception ignored) {
                available = false;
            }
        }
        this.enabled = available;
        this.setPlaceholdersMethod = setPlaceholders;
        this.placeholderApiClass = apiClass;
    }

    public String applyPlaceholders(Player player, String text) {
        if (text == null) {
            return "";
        }
        if (!this.enabled || player == null || this.setPlaceholdersMethod == null || this.placeholderApiClass == null) {
            return text;
        }
        try {
            Object result = this.setPlaceholdersMethod.invoke(this.placeholderApiClass, player, text);
            return result instanceof String ? (String)result : text;
        }
        catch (Exception ex) {
            this.plugin.getLogger().warning("PlaceholderAPI failed to parse placeholders: " + ex.getMessage());
            return text;
        }
    }
}
