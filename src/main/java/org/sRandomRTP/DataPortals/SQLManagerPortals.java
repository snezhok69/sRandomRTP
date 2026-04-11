package org.sRandomRTP.DataPortals;

import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.PortalRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class SQLManagerPortals {

    /**
     * Called with existing [portal_names, portal_data] (null if no row yet).
     * Return updated [portal_names, portal_data], or null to delete the row entirely.
     */
    @FunctionalInterface
    interface PortalDataTransformer {
        String[] transform(String[] existing) throws SQLException;
    }

    /**
     * Generic SELECT → transform → INSERT/UPDATE/DELETE for the PlayerPortals table.
     * Eliminates the duplicated SELECT * + if/else pattern from Save, Rename, Remove.
     */
    static void upsertPortalData(Connection conn, String playerName,
                                 PortalDataTransformer transformer) throws SQLException {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement sel = conn.prepareStatement(
                    "SELECT portal_names, portal_data FROM PlayerPortals WHERE player_name COLLATE BINARY = ?")) {
                sel.setString(1, playerName);
                try (ResultSet rs = sel.executeQuery()) {
                    String[] existing = rs.next()
                            ? new String[]{rs.getString("portal_names"), rs.getString("portal_data")}
                            : null;
                    String[] updated = transformer.transform(existing);
                    if (updated == null) {
                        try (PreparedStatement del = conn.prepareStatement(
                                "DELETE FROM PlayerPortals WHERE player_name COLLATE BINARY = ?")) {
                            del.setString(1, playerName);
                            del.executeUpdate();
                        }
                    } else if (existing == null) {
                        try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO PlayerPortals (player_name, portal_names, portal_data) VALUES (?, ?, ?)")) {
                            ins.setString(1, playerName);
                            ins.setString(2, updated[0]);
                            ins.setString(3, updated[1]);
                            ins.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement upd = conn.prepareStatement(
                                "UPDATE PlayerPortals SET portal_names = ?, portal_data = ? WHERE player_name COLLATE BINARY = ?")) {
                            upd.setString(1, updated[0]);
                            upd.setString(2, updated[1]);
                            upd.setString(3, playerName);
                            upd.executeUpdate();
                        }
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Runs an async DB operation that returns CompletableFuture&lt;Void&gt;.
     * Handles null-repo guard and SQLException logging so callers don't repeat the boilerplate.
     */
    static CompletableFuture<Void> runDbAsync(String opName, PortalRepository.SqlConsumer action) {
        PortalRepository repo = Variables.getPortalRepository();
        if (repo == null) return CompletableFuture.completedFuture(null);
        return repo.runAsync(conn -> {
            try {
                action.accept(conn);
            } catch (SQLException e) {
                Variables.getInstance().getLogger().severe("Failed to " + opName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Fire-and-forget variant: runs an async DB operation without returning a future.
     * Handles null-repo guard and SQLException logging.
     */
    static void fireAndForgetAsync(String opName, PortalRepository.SqlConsumer action) {
        PortalRepository repo = Variables.getPortalRepository();
        if (repo == null) return;
        repo.runAsync(conn -> {
            try {
                action.accept(conn);
            } catch (SQLException e) {
                Variables.getInstance().getLogger().severe("Failed to " + opName + ": " + e.getMessage());
            }
        });
    }
}
