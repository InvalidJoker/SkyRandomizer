package de.joker.randomizer.data;

import de.joker.randomizer.SkyRandomizer;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private final String url;

    public Database(Plugin plugin) {
        this.url = "jdbc:sqlite:" + plugin.getDataFolder() + "/" + SkyRandomizer.SEASON_DATABASE_NAME;
    }

    public void init() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS islands (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    island_x INTEGER NOT NULL UNIQUE,
                    max_distance INTEGER NOT NULL DEFAULT 0
                );
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS island_members (
                    uuid TEXT PRIMARY KEY,
                    island_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    FOREIGN KEY (island_id) REFERENCES islands(id) ON DELETE CASCADE
                );
            """);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }
}
