/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 */
package com.combatcore.manager;

import com.combatcore.util.TextUtil;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;

public class DiscordWebhookManager {
    private final String url;

    public DiscordWebhookManager(String url) {
        this.url = url;
    }

    public static void sendKillWebhook(String webhookUrl, String attacker, String victim, String cause) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            HashMap<String, Object> payload = new HashMap<String, Object>();
            payload.put("username", "CombatCore");
            ArrayList embeds = new ArrayList();
            HashMap<String, Object> embed = new HashMap<String, Object>();
            embed.put("title", "\u2694\ufe0f Combat Log - Player Killed");
            embed.put("color", 0xFF0000);
            embed.put("timestamp", Instant.now().toString());
            ArrayList fields = new ArrayList();
            HashMap<String, Object> attackerField = new HashMap<String, Object>();
            attackerField.put("name", "Attacker");
            attackerField.put("value", "`" + attacker + "`");
            attackerField.put("inline", true);
            fields.add(attackerField);
            HashMap<String, Object> victimField = new HashMap<String, Object>();
            victimField.put("name", "Victim");
            victimField.put("value", "`" + victim + "`");
            victimField.put("inline", true);
            fields.add(victimField);
            HashMap<String, Object> causeField = new HashMap<String, Object>();
            causeField.put("name", "Weapon / Cause");
            causeField.put("value", "`" + cause + "`");
            causeField.put("inline", false);
            fields.add(causeField);
            embed.put("fields", fields);
            HashMap<String, String> footer = new HashMap<String, String>();
            footer.put("text", "CombatCore System");
            embed.put("footer", footer);
            embeds.add(embed);
            payload.put("embeds", embeds);
            String body = TextUtil.toJson(payload);
            try (OutputStream out = connection.getOutputStream();){
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                Bukkit.getLogger().warning("Discord webhook returned HTTP " + responseCode + ".");
            }
        }
        catch (Exception ex) {
            Bukkit.getLogger().warning("Failed to send Discord webhook: " + ex.getMessage());
        }
    }

    public static void sendPunishmentWebhook(String webhookUrl, String victim, List<String> punishments) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            HashMap<String, Object> payload = new HashMap<String, Object>();
            payload.put("username", "CombatCore");
            ArrayList embeds = new ArrayList();
            HashMap<String, Object> embed = new HashMap<String, Object>();
            embed.put("title", "\u26a0\ufe0f Combat Log Detected");
            embed.put("color", 16753920);
            embed.put("timestamp", Instant.now().toString());
            ArrayList fields = new ArrayList();
            HashMap<String, Object> victimField = new HashMap<String, Object>();
            victimField.put("name", "Player");
            victimField.put("value", "`" + victim + "`");
            victimField.put("inline", false);
            fields.add(victimField);
            HashMap<String, Object> punField = new HashMap<String, Object>();
            punField.put("name", "Punishments Applied");
            if (punishments.isEmpty()) {
                punField.put("value", "None");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String p : punishments) {
                    sb.append("- ").append(p).append("\n");
                }
                punField.put("value", sb.toString());
            }
            punField.put("inline", false);
            fields.add(punField);
            embed.put("fields", fields);
            HashMap<String, String> footer = new HashMap<String, String>();
            footer.put("text", "CombatCore Anti-Log");
            embed.put("footer", footer);
            embeds.add(embed);
            payload.put("embeds", embeds);
            String body = TextUtil.toJson(payload);
            try (OutputStream out = connection.getOutputStream();){
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                Bukkit.getLogger().warning("Discord webhook returned HTTP " + responseCode + ".");
            }
        }
        catch (Exception ex) {
            Bukkit.getLogger().warning("Failed to send Discord webhook: " + ex.getMessage());
        }
    }
}
