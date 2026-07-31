/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.combatcore.libs.gson.Gson
 *  com.combatcore.libs.gson.GsonBuilder
 *  org.bukkit.ChatColor
 */
package com.combatcore.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Map;
import org.bukkit.ChatColor;

public class TextUtil {
    private static final Gson GSON = new GsonBuilder().create();

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes((char)'&', (String)input);
    }

    public static String replaceTokens(String message, Map<String, String> tokens) {
        if (message == null || tokens == null || tokens.isEmpty()) {
            return message;
        }
        String result = message;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : (CharSequence)entry.getValue());
        }
        return result;
    }

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }
}
