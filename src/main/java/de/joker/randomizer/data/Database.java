package de.joker.randomizer.data;

import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private final String url;

    public Database(Plugin plugin) {
        this.url = "jdbc:sqlite:" + plugin.getDataFolder() + "/data.db";
    }

    public void init() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    name TEXT,
                    island_x INTEGER,
                    island_z INTEGER,
                    max_distance INTEGER DEFAULT 0
                );
            """);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }
}