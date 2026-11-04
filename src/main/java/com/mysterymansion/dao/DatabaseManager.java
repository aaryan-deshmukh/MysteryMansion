package com.mysterymansion.dao;

import com.mysterymansion.engine.GameEngine;
import com.mysterymansion.model.Difficulty;
import com.mysterymansion.model.Item;
import com.mysterymansion.model.Room;
import com.mysterymansion.util.DbConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** All MySQL access: leaderboard plus save/resume. Credentials come from {@link DbConfig}. */
public class DatabaseManager {
    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "MySQL JDBC driver not found on the classpath", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(DbConfig.url(), DbConfig.user(), DbConfig.password());
        } catch (IllegalStateException e) { // missing configuration
            throw new SQLException(e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Leaderboard
    // ------------------------------------------------------------------
    public static void saveScore(String playerName, int score, String result) {
        String query = "INSERT INTO leaderboard (player_name, score, game_result) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, playerName);
            pstmt.setInt(2, score);
            pstmt.setString(3, result);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Could not save score for " + playerName, e);
        }
    }

    public static List<String[]> getTopScores() {
        List<String[]> scores = new ArrayList<>();
        String query = "SELECT player_name, score, game_result, played_at FROM leaderboard ORDER BY score DESC LIMIT 5";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                scores.add(new String[]{
                    rs.getString("player_name"),
                    String.valueOf(rs.getInt("score")),
                    rs.getString("game_result"),
                    rs.getString("played_at")
                });
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Could not load leaderboard", e);
        }
        return scores;
    }

    // ------------------------------------------------------------------
    // Save and Resume
    // ------------------------------------------------------------------

    /** Inserts or overwrites this player's single save slot. */
    public static void saveGame(GameEngine g) {
        String query = "INSERT INTO saved_games (player_name, difficulty, health, max_health, score, "
                + "moves_remaining, max_moves, current_room, safe_passcode, inventory, room_items, rooms_searched) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE difficulty = VALUES(difficulty), health = VALUES(health), "
                + "max_health = VALUES(max_health), score = VALUES(score), "
                + "moves_remaining = VALUES(moves_remaining), max_moves = VALUES(max_moves), "
                + "current_room = VALUES(current_room), safe_passcode = VALUES(safe_passcode), "
                + "inventory = VALUES(inventory), room_items = VALUES(room_items), "
                + "rooms_searched = VALUES(rooms_searched)";

        // Inventory: "Torch,Code Paper"   Items: "Library=Torch;Basement=Code Paper"   Searched: "Library,Basement"
        String inventory = String.join(",", g.getInventoryNames());

        StringBuilder items = new StringBuilder();
        for (Map.Entry<Room, Item> e : g.getRoomItems().entrySet()) {
            if (items.length() > 0) items.append(';');
            items.append(e.getKey().displayName()).append('=').append(e.getValue().displayName());
        }

        List<String> searched = new ArrayList<>();
        for (Room r : g.getSearchedRooms()) searched.add(r.displayName());

        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(query)) {
            p.setString(1, g.getPlayerName());
            p.setString(2, g.getDifficulty().label());
            p.setInt(3, g.getHealth());
            p.setInt(4, g.getMaxHealth());
            p.setInt(5, g.getScore());
            p.setInt(6, g.getMovesRemaining());
            p.setInt(7, g.getMaxMoves());
            p.setString(8, g.getCurrentRoom().displayName());
            p.setString(9, g.getSafePasscode());
            p.setString(10, inventory);
            p.setString(11, items.toString());
            p.setString(12, String.join(",", searched));
            p.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Could not save game for " + g.getPlayerName(), e);
        }
    }

    /** Returns null if the player has no saved game. */
    public static GameEngine loadGame(String playerName) throws SQLException {
        String query = "SELECT * FROM saved_games WHERE player_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(query)) {
            p.setString(1, playerName);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) return null;

                List<Item> inventory = new ArrayList<>();
                String inv = rs.getString("inventory");
                if (inv != null && !inv.isEmpty()) {
                    for (String name : inv.split(",")) {
                        Item item = Item.fromName(name);
                        if (item != null) inventory.add(item);
                        else LOG.warning("Ignoring unknown inventory item in save: " + name);
                    }
                }

                Map<Room, Item> roomItems = new EnumMap<>(Room.class);
                String items = rs.getString("room_items");
                if (items != null && !items.isEmpty()) {
                    for (String pair : items.split(";")) {
                        String[] kv = pair.split("=", 2);
                        Room room = kv.length == 2 ? Room.fromName(kv[0]) : null;
                        Item item = kv.length == 2 ? Item.fromName(kv[1]) : null;
                        if (room != null && item != null) roomItems.put(room, item);
                        else LOG.warning("Ignoring unreadable room item in save: " + pair);
                    }
                }

                Set<Room> searched = EnumSet.noneOf(Room.class);
                String s = rs.getString("rooms_searched");
                if (s != null && !s.isEmpty()) {
                    for (String name : s.split(",")) {
                        Room room = Room.fromName(name);
                        if (room != null) searched.add(room);
                    }
                }

                Room current = Room.fromName(rs.getString("current_room"));
                if (current == null) current = Room.MAIN_HALL;

                return new GameEngine(
                        rs.getString("player_name"), Difficulty.fromLabel(rs.getString("difficulty")),
                        rs.getInt("health"), rs.getInt("max_health"), rs.getInt("score"),
                        rs.getInt("moves_remaining"), rs.getInt("max_moves"),
                        current, rs.getString("safe_passcode"),
                        inventory, roomItems, searched);
            }
        }
    }

    public static boolean hasSavedGame(String playerName) {
        String query = "SELECT 1 FROM saved_games WHERE player_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(query)) {
            p.setString(1, playerName);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Could not check saved game for " + playerName, e);
            return false;
        }
    }

    /** Called when a run ends so a finished game can't be resumed. */
    public static void deleteSavedGame(String playerName) {
        String query = "DELETE FROM saved_games WHERE player_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(query)) {
            p.setString(1, playerName);
            p.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Could not delete saved game for " + playerName, e);
        }
    }
}
