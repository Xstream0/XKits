package com.xkits.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class MySQLManager {

    private final JavaPlugin plugin;
    private HikariDataSource ds;

    public MySQLManager(JavaPlugin plugin, String host, int port, String database, String user, String pass) {
        this.plugin = plugin;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, database));
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("XKitsPool");

        ds = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public void disconnect() {
        if (ds != null && !ds.isClosed()) ds.close();
    }

    public CompletableFuture<Void> createTablesAsync() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS xkits_kits (
                        name VARCHAR(64) PRIMARY KEY,
                        items TEXT,
                        permission VARCHAR(128),
                        cooldown BIGINT
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """)) {
                    ps.execute();
                }
                try (PreparedStatement ps = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS xkits_player_cooldowns (
                        uuid CHAR(36) PRIMARY KEY,
                        cooldowns TEXT
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """)) {
                    ps.execute();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore creando le tabelle MySQL: " + e.getMessage());
            }
        });
    }
}
