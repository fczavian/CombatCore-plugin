/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.combatcore.libs.gson.Gson
 *  com.combatcore.libs.gson.GsonBuilder
 */
package com.combatcore.manager;

import com.combatcore.CombatCorePlugin;
import com.combatcore.config.CombatConfig;
import com.combatcore.data.CombatCause;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final CombatCorePlugin plugin;
    private final CombatConfig config;
    private String jdbcUrl;
    private String dbType;
    private File localStatsFile;
    private File localOffensesFile;
    private File localBountiesFile;
    private final Map<UUID, String[]> pendingPunishmentsCache = new HashMap<UUID, String[]>();

    public DatabaseManager(CombatCorePlugin plugin, CombatConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.init();
    }

    private void init() {
        String type;
        this.dbType = type = this.plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        if (type.equals("mysql")) {
            String host = this.plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = this.plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = this.plugin.getConfig().getString("database.mysql.database", "combatcore");
            String username = this.plugin.getConfig().getString("database.mysql.username", "root");
            String password = this.plugin.getConfig().getString("database.mysql.password", "");
            this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
            this.plugin.getLogger().info("CombatCore using MySQL/MariaDB backend.");
        } else if (type.equals("mongodb")) {
            String uri = this.plugin.getConfig().getString("database.mongodb.uri", "").trim();
            if (uri.isEmpty()) {
                this.plugin.getLogger().severe("CombatCore: 'database.type' is set to 'mongodb' but 'database.mongodb.uri' is empty. Falling back to SQLite.");
                String file = this.plugin.getConfig().getString("database.sqlite.file", "combatcore.db");
                this.jdbcUrl = "jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + "/" + file;
                this.dbType = "sqlite";
                this.plugin.getLogger().info("CombatCore using SQLite backend as fallback.");
            } else {
                if (!uri.startsWith("mongodb://") && !uri.startsWith("mongodb+srv://")) {
                    uri = "mongodb://" + uri;
                }
                try {
                    URI parsed = new URI(uri);
                    String path = parsed.getPath();
                    if (path == null || path.isEmpty() || path.equals("/")) {
                        uri = uri.endsWith("/") ? uri + "combatcore" : uri + "/combatcore";
                    }
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Could not fully parse MongoDB URI; using as-is: " + ex.getMessage());
                }
                this.jdbcUrl = uri;
                this.plugin.getLogger().info("CombatCore configured to use MongoDB backend (driver optional).");
            }
        } else if (type.equals("local")) {
            File data = this.plugin.getDataFolder();
            if (!data.exists()) {
                data.mkdirs();
            }
            this.localStatsFile = new File(data, "combat_stats.json");
            this.localOffensesFile = new File(data, "combat_offenses.json");
            this.localBountiesFile = new File(data, "combat_bounties.json");
            try {
                if (!this.localStatsFile.exists()) {
                    this.localStatsFile.createNewFile();
                }
                if (!this.localOffensesFile.exists()) {
                    this.localOffensesFile.createNewFile();
                }
                if (!this.localBountiesFile.exists()) {
                    this.localBountiesFile.createNewFile();
                }
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("Could not create local storage files: " + ex.getMessage());
            }
            this.plugin.getLogger().info("CombatCore using local JSON backend.");
        } else {
            String file = this.plugin.getConfig().getString("database.sqlite.file", "combatcore.db");
            this.jdbcUrl = "jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + "/" + file;
            this.plugin.getLogger().info("CombatCore using SQLite backend.");
        }
        this.initTables();
    }

    private Connection openConnection() throws SQLException {
        if (this.jdbcUrl.startsWith("jdbc:sqlite:")) {
            return DriverManager.getConnection(this.jdbcUrl);
        }
        return DriverManager.getConnection(this.jdbcUrl, this.plugin.getConfig().getString("database.mysql.username", ""), this.plugin.getConfig().getString("database.mysql.password", ""));
    }

    private void initTables() {
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 Statement statement = connection.createStatement();){
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS combat_stats (attacker_uuid VARCHAR(36), victim_uuid VARCHAR(36), cause VARCHAR(32), timestamp BIGINT)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS combat_offenses (player_uuid VARCHAR(36) PRIMARY KEY, offense_count INTEGER)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS combat_assists (player_uuid VARCHAR(36), assists INTEGER, PRIMARY KEY(player_uuid))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS combat_bounties (target TEXT PRIMARY KEY, amount DOUBLE)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS pending_punishments (player_uuid VARCHAR(36) PRIMARY KEY, freeze INTEGER DEFAULT 0, potion_effects TEXT DEFAULT '')");
            }
            catch (SQLException ex) {
                this.plugin.getLogger().severe("Failed to initialize CombatCore database: " + ex.getMessage());
            }
        });
    }

    public void savePendingPunishments(UUID playerUuid, boolean freeze, String potionEffects) {
        if ("local".equals(this.dbType) || "mongodb".equals(this.dbType)) {
            this.pendingPunishmentsCache.put(playerUuid, new String[]{String.valueOf(freeze), potionEffects});
            return;
        }
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO pending_punishments (player_uuid, freeze, potion_effects) VALUES (?, ?, ?) ON CONFLICT(player_uuid) DO UPDATE SET freeze = excluded.freeze, potion_effects = excluded.potion_effects");){
                statement.setString(1, playerUuid.toString());
                statement.setInt(2, freeze ? 1 : 0);
                statement.setString(3, potionEffects);
                statement.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().warning("Could not store pending punishment: " + ex.getMessage());
            }
        });
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public String[] loadPendingPunishments(UUID playerUuid) {
        if ("local".equals(this.dbType)) return this.pendingPunishmentsCache.getOrDefault(playerUuid, null);
        if ("mongodb".equals(this.dbType)) {
            return this.pendingPunishmentsCache.getOrDefault(playerUuid, null);
        }
        try (Connection connection = this.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT freeze, potion_effects FROM pending_punishments WHERE player_uuid = ?");){
            statement.setString(1, playerUuid.toString());
            try (ResultSet rs = statement.executeQuery();){
                if (!rs.next()) return null;
                String[] stringArray = new String[]{String.valueOf(rs.getInt("freeze") != 0), rs.getString("potion_effects")};
                return stringArray;
            }
        }
        catch (SQLException ex) {
            this.plugin.getLogger().warning("Could not read pending punishment: " + ex.getMessage());
        }
        return null;
    }

    public void clearPendingPunishments(UUID playerUuid) {
        this.pendingPunishmentsCache.remove(playerUuid);
        if ("local".equals(this.dbType) || "mongodb".equals(this.dbType)) {
            return;
        }
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM pending_punishments WHERE player_uuid = ?");){
                statement.setString(1, playerUuid.toString());
                statement.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().warning("Could not clear pending punishment: " + ex.getMessage());
            }
        });
    }

    public void recordCombatDeath(UUID attackerUuid, UUID victimUuid, CombatCause cause) {
        if ("local".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    List<Map<String, Object>> list = this.readLocalStats();
                    HashMap<String, Object> entry = new HashMap<String, Object>();
                    entry.put("attacker_uuid", attackerUuid.toString());
                    entry.put("victim_uuid", victimUuid.toString());
                    entry.put("cause", cause.name());
                    entry.put("timestamp", System.currentTimeMillis());
                    list.add(entry);
                    this.writeLocalStats(list);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Could not persist local combat death: " + ex.getMessage());
                }
            });
            return;
        }
        if ("mongodb".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                    Method create = mongoClients.getMethod("create", String.class);
                    Object client = create.invoke(null, this.jdbcUrl);
                    Method getDatabase = client.getClass().getMethod("getDatabase", String.class);
                    String dbName = this.plugin.getConfig().getString("database.mongodb.database", "combatcore");
                    Object db = getDatabase.invoke(client, dbName);
                    Method getCollection = db.getClass().getMethod("getCollection", String.class);
                    Object coll = getCollection.invoke(db, "combat_stats");
                    Class<?> docClass = Class.forName("org.bson.Document");
                    Constructor<?> ctor = docClass.getConstructor(new Class[0]);
                    Object doc = ctor.newInstance(new Object[0]);
                    Method put = docClass.getMethod("put", Object.class, Object.class);
                    put.invoke(doc, "attacker_uuid", attackerUuid.toString());
                    put.invoke(doc, "victim_uuid", victimUuid.toString());
                    put.invoke(doc, "cause", cause.name());
                    put.invoke(doc, "timestamp", System.currentTimeMillis());
                    Method insertOne = coll.getClass().getMethod("insertOne", docClass);
                    insertOne.invoke(coll, doc);
                    Method close = client.getClass().getMethod("close", new Class[0]);
                    close.invoke(client, new Object[0]);
                }
                catch (ClassNotFoundException ex) {
                    this.plugin.getLogger().warning("MongoDB driver not found; cannot persist to MongoDB.\n" + ex.getMessage());
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Failed to write to MongoDB: " + ex.getMessage());
                }
            });
            return;
        }
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO combat_stats (attacker_uuid, victim_uuid, cause, timestamp) VALUES (?, ?, ?, ?)");){
                statement.setString(1, attackerUuid.toString());
                statement.setString(2, victimUuid.toString());
                statement.setString(3, cause.name());
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().warning("Could not persist combat death: " + ex.getMessage());
            }
        });
    }

    public int incrementOffenseCount(UUID playerUuid) {
        int offenses = this.getOffenseCount(playerUuid) + 1;
        if ("local".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Map<String, Integer> map = this.readLocalOffenses();
                    map.put(playerUuid.toString(), offenses);
                    this.writeLocalOffenses(map);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Could not store local offense count: " + ex.getMessage());
                }
            });
            return offenses;
        }
        if ("mongodb".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                    Method create = mongoClients.getMethod("create", String.class);
                    Object client = create.invoke(null, this.jdbcUrl);
                    Method getDatabase = client.getClass().getMethod("getDatabase", String.class);
                    String dbName = this.plugin.getConfig().getString("database.mongodb.database", "combatcore");
                    Object db = getDatabase.invoke(client, dbName);
                    Method getCollection = db.getClass().getMethod("getCollection", String.class);
                    Object coll = getCollection.invoke(db, "combat_offenses");
                    Class<?> docClass = Class.forName("org.bson.Document");
                    Constructor<?> ctor = docClass.getConstructor(String.class, Object.class);
                    Object doc = ctor.newInstance("player_uuid", playerUuid.toString());
                    Method put = docClass.getMethod("put", Object.class, Object.class);
                    put.invoke(doc, "offense_count", offenses);
                    Method replaceOne = coll.getClass().getMethod("replaceOne", docClass, docClass, Map.class);
                    HashMap<String, Boolean> opts = new HashMap<String, Boolean>();
                    opts.put("upsert", Boolean.TRUE);
                    Class<?> bsonDocClass2 = Class.forName("org.bson.Document");
                    Constructor<?> ctor2 = bsonDocClass2.getConstructor(String.class, Object.class);
                    Object queryDoc = ctor2.newInstance("player_uuid", playerUuid.toString());
                    replaceOne.invoke(coll, queryDoc, doc, opts);
                    Method close = client.getClass().getMethod("close", new Class[0]);
                    close.invoke(client, new Object[0]);
                }
                catch (ClassNotFoundException ex) {
                    this.plugin.getLogger().warning("MongoDB driver not found; cannot persist offense to MongoDB.");
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Failed to write offense to MongoDB: " + ex.getMessage());
                }
            });
            return offenses;
        }
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO combat_offenses (player_uuid, offense_count) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET offense_count = excluded.offense_count");){
                statement.setString(1, playerUuid.toString());
                statement.setInt(2, offenses);
                statement.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().warning("Could not store offense count: " + ex.getMessage());
            }
        });
        return offenses;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public int getOffenseCount(UUID playerUuid) {
        if ("local".equals(this.dbType)) {
            try {
                Map<String, Integer> map = this.readLocalOffenses();
                return map.getOrDefault(playerUuid.toString(), 0);
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("Could not read local offense count: " + ex.getMessage());
                return 0;
            }
        }
        if ("mongodb".equals(this.dbType)) {
            try {
                Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                Method create = mongoClients.getMethod("create", String.class);
                Object client = create.invoke(null, this.jdbcUrl);
                Method getDatabase = client.getClass().getMethod("getDatabase", String.class);
                String dbName = this.plugin.getConfig().getString("database.mongodb.database", "combatcore");
                Object db = getDatabase.invoke(client, dbName);
                Method getCollection = db.getClass().getMethod("getCollection", String.class);
                Object coll = getCollection.invoke(db, "combat_offenses");
                Class<?> docClass = Class.forName("org.bson.Document");
                Constructor<?> ctor = docClass.getConstructor(String.class, Object.class);
                Object query = ctor.newInstance("player_uuid", playerUuid.toString());
                Method find = coll.getClass().getMethod("find", docClass);
                Object iterable = find.invoke(coll, query);
                Method first = iterable.getClass().getMethod("first", new Class[0]);
                Object doc = first.invoke(iterable, new Object[0]);
                if (doc == null) {
                    Method close = client.getClass().getMethod("close", new Class[0]);
                    close.invoke(client, new Object[0]);
                    return 0;
                }
                Method getInt = doc.getClass().getMethod("getInteger", Object.class);
                Integer val = (Integer)getInt.invoke(doc, "offense_count");
                Method close = client.getClass().getMethod("close", new Class[0]);
                close.invoke(client, new Object[0]);
                if (val == null) {
                    return 0;
                }
                int n = val;
                return n;
            }
            catch (ClassNotFoundException ex) {
                this.plugin.getLogger().warning("MongoDB driver not found; cannot query offenses from MongoDB.");
                return 0;
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("Failed to read offense from MongoDB: " + ex.getMessage());
            }
            return 0;
        }
        try (Connection connection = this.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT offense_count FROM combat_offenses WHERE player_uuid = ?");){
            statement.setString(1, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery();){
                if (!resultSet.next()) return 0;
                int n = resultSet.getInt("offense_count");
                return n;
            }
        }
        catch (SQLException ex) {
            this.plugin.getLogger().warning("Could not read offense count: " + ex.getMessage());
        }
        return 0;
    }

    public void recordOffense(UUID playerUuid) {
        int offenses = this.getOffenseCount(playerUuid) + 1;
        if ("local".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Map<String, Integer> map = this.readLocalOffenses();
                    map.put(playerUuid.toString(), offenses);
                    this.writeLocalOffenses(map);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Could not store local offense count: " + ex.getMessage());
                }
            });
            return;
        }
        if ("mongodb".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    this.incrementOffenseCount(playerUuid);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Could not store offense count in MongoDB: " + ex.getMessage());
                }
            });
            return;
        }
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO combat_offenses (player_uuid, offense_count) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET offense_count = excluded.offense_count");){
                statement.setString(1, playerUuid.toString());
                statement.setInt(2, offenses);
                statement.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().warning("Could not store offense count: " + ex.getMessage());
            }
        });
    }

    public Map<String, Double> loadBounties() {
        if ("local".equals(this.dbType)) {
            try {
                return this.readLocalBounties();
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("Could not read local bounties: " + ex.getMessage());
                return new HashMap<String, Double>();
            }
        }
        if ("mongodb".equals(this.dbType)) {
            try {
                Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                Method create = mongoClients.getMethod("create", String.class);
                Object client = create.invoke(null, this.jdbcUrl);
                Method getDatabase = client.getClass().getMethod("getDatabase", String.class);
                String dbName = this.plugin.getConfig().getString("database.mongodb.database", "combatcore");
                Object db = getDatabase.invoke(client, dbName);
                Method getCollection = db.getClass().getMethod("getCollection", String.class);
                Object coll = getCollection.invoke(db, "combat_bounties");
                Method find = coll.getClass().getMethod("find", new Class[0]);
                Object iterable = find.invoke(coll, new Object[0]);
                Method iterator = iterable.getClass().getMethod("iterator", new Class[0]);
                Object cursor = iterator.invoke(iterable, new Object[0]);
                Method hasNext = cursor.getClass().getMethod("hasNext", new Class[0]);
                Method next = cursor.getClass().getMethod("next", new Class[0]);
                HashMap<String, Double> result = new HashMap<String, Double>();
                while (((Boolean)hasNext.invoke(cursor, new Object[0])).booleanValue()) {
                    Object doc = next.invoke(cursor, new Object[0]);
                    Method getString = doc.getClass().getMethod("getString", Object.class);
                    Method getDouble = doc.getClass().getMethod("getDouble", Object.class);
                    String target = (String)getString.invoke(doc, "target");
                    Double amount = (Double)getDouble.invoke(doc, "amount");
                    result.put(target.toLowerCase(), amount == null ? 0.0 : amount);
                }
                Method close = client.getClass().getMethod("close", new Class[0]);
                close.invoke(client, new Object[0]);
                return result;
            }
            catch (ClassNotFoundException ex) {
                this.plugin.getLogger().warning("MongoDB driver not found; cannot load bounties from MongoDB.");
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("Failed to read bounties from MongoDB: " + ex.getMessage());
            }
            return new HashMap<String, Double>();
        }
        HashMap<String, Double> result = new HashMap<String, Double>();
        try (Connection connection = this.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT target, amount FROM combat_bounties");
             ResultSet resultSet = statement.executeQuery();){
            while (resultSet.next()) {
                result.put(resultSet.getString("target").toLowerCase(), resultSet.getDouble("amount"));
            }
        }
        catch (SQLException ex) {
            this.plugin.getLogger().warning("Could not read bounties: " + ex.getMessage());
        }
        return result;
    }

    public void setBounty(String target, double amount) {
        if ("local".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Map<String, Double> map = this.readLocalBounties();
                    map.put(target, amount);
                    this.writeLocalBounties(map);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Could not store local bounty: " + ex.getMessage());
                }
            });
            return;
        }
        if ("mongodb".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                    Method create = mongoClients.getMethod("create", String.class);
                    Object client = create.invoke(null, this.jdbcUrl);
                    Method getDatabase = client.getClass().getMethod("getDatabase", String.class);
                    String dbName = this.plugin.getConfig().getString("database.mongodb.database", "combatcore");
                    Object db = getDatabase.invoke(client, dbName);
                    Method getCollection = db.getClass().getMethod("getCollection", String.class);
                    Object coll = getCollection.invoke(db, "combat_bounties");
                    Class<?> docClass = Class.forName("org.bson.Document");
                    Constructor<?> ctor = docClass.getConstructor(String.class, Object.class);
                    Object query = ctor.newInstance("target", target);
                    Object doc = ctor.newInstance("target", target);
                    Method put = docClass.getMethod("put", Object.class, Object.class);
                    put.invoke(doc, "amount", amount);
                    Method replaceOne = coll.getClass().getMethod("replaceOne", docClass, docClass, Map.class);
                    HashMap<String, Boolean> opts = new HashMap<String, Boolean>();
                    opts.put("upsert", Boolean.TRUE);
                    replaceOne.invoke(coll, query, doc, opts);
                    Method close = client.getClass().getMethod("close", new Class[0]);
                    close.invoke(client, new Object[0]);
                }
                catch (ClassNotFoundException ex) {
                    this.plugin.getLogger().warning("MongoDB driver not found; cannot store bounty to MongoDB.");
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Failed to store bounty to MongoDB: " + ex.getMessage());
                }
            });
            return;
        }
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO combat_bounties (target, amount) VALUES (?, ?) ON CONFLICT(target) DO UPDATE SET amount = excluded.amount");){
                statement.setString(1, target);
                statement.setDouble(2, amount);
                statement.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().warning("Could not store bounty: " + ex.getMessage());
            }
        });
    }

    public void deleteBounty(String target) {
        if ("local".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Map<String, Double> map = this.readLocalBounties();
                    map.remove(target);
                    this.writeLocalBounties(map);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Could not remove local bounty: " + ex.getMessage());
                }
            });
            return;
        }
        if ("mongodb".equals(this.dbType)) {
            this.runAsync(() -> {
                try {
                    Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                    Method create = mongoClients.getMethod("create", String.class);
                    Object client = create.invoke(null, this.jdbcUrl);
                    Method getDatabase = client.getClass().getMethod("getDatabase", String.class);
                    String dbName = this.plugin.getConfig().getString("database.mongodb.database", "combatcore");
                    Object db = getDatabase.invoke(client, dbName);
                    Method getCollection = db.getClass().getMethod("getCollection", String.class);
                    Object coll = getCollection.invoke(db, "combat_bounties");
                    Class<?> docClass = Class.forName("org.bson.Document");
                    Constructor<?> ctor = docClass.getConstructor(String.class, Object.class);
                    Object query = ctor.newInstance("target", target);
                    Method deleteOne = coll.getClass().getMethod("deleteOne", docClass);
                    deleteOne.invoke(coll, query);
                    Method close = client.getClass().getMethod("close", new Class[0]);
                    close.invoke(client, new Object[0]);
                }
                catch (ClassNotFoundException ex) {
                    this.plugin.getLogger().warning("MongoDB driver not found; cannot remove bounty from MongoDB.");
                }
                catch (Exception ex) {
                    this.plugin.getLogger().warning("Failed to remove bounty from MongoDB: " + ex.getMessage());
                }
            });
            return;
        }
        this.runAsync(() -> {
            try (Connection connection = this.openConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM combat_bounties WHERE target = ?");){
                statement.setString(1, target);
                statement.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().warning("Could not delete bounty: " + ex.getMessage());
            }
        });
    }

    public void shutdown() {
    }

    private synchronized List<Map<String, Object>> readLocalStats() throws Exception {
        if (this.localStatsFile == null) {
            return new ArrayList<Map<String, Object>>();
        }
        try (FileReader fr = new FileReader(this.localStatsFile);){
            Gson gson = new Gson();
            List list = (List)gson.fromJson((Reader)fr, List.class);
            List list2 = list == null ? new ArrayList() : list;
            return list2;
        }
    }

    private synchronized void writeLocalStats(List<Map<String, Object>> list) throws Exception {
        if (this.localStatsFile == null) {
            return;
        }
        try (FileWriter fw = new FileWriter(this.localStatsFile, false);){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(list, (Appendable)fw);
        }
    }

    private synchronized Map<String, Integer> readLocalOffenses() throws Exception {
        if (this.localOffensesFile == null) {
            return new HashMap<String, Integer>();
        }
        try (FileReader fr = new FileReader(this.localOffensesFile);){
            Gson gson = new Gson();
            Map<?, ?> raw = (Map<?, ?>)gson.fromJson((Reader)fr, Map.class);
            HashMap<String, Integer> result = new HashMap<String, Integer>();
            if (raw != null) {
                for (Map.Entry<?, ?> entry : raw.entrySet()) {
                    result.put((String)entry.getKey(), ((Number)entry.getValue()).intValue());
                }
            }
            HashMap<String, Integer> hashMap = result;
            return hashMap;
        }
    }

    private synchronized void writeLocalOffenses(Map<String, Integer> map) throws Exception {
        if (this.localOffensesFile == null) {
            return;
        }
        try (FileWriter fw = new FileWriter(this.localOffensesFile, false);){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(map, (Appendable)fw);
        }
    }

    private synchronized Map<String, Double> readLocalBounties() throws Exception {
        if (this.localBountiesFile == null) {
            return new HashMap<String, Double>();
        }
        try (FileReader fr = new FileReader(this.localBountiesFile);){
            Gson gson = new Gson();
            Map raw = (Map)gson.fromJson((Reader)fr, Map.class);
            Map map = raw == null ? new HashMap() : raw;
            return map;
        }
    }

    private synchronized void writeLocalBounties(Map<String, Double> map) throws Exception {
        if (this.localBountiesFile == null) {
            return;
        }
        try (FileWriter fw = new FileWriter(this.localBountiesFile, false);){
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(map, (Appendable)fw);
        }
    }

    private String getMongoDatabaseFromUri() {
        try {
            URI uri = new URI(this.jdbcUrl);
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                return path.substring(1);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "combatcore";
    }

    private void runAsync(Runnable task) {
        CompletableFuture.runAsync(task);
    }
}
