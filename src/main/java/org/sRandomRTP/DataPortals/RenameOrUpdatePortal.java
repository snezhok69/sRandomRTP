package org.sRandomRTP.DataPortals;

import org.sRandomRTP.DifferentMethods.Variables;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RenameOrUpdatePortal {

    public static CompletableFuture<Void> renameOrUpdatePortal(String playerName, String portalName, String worldName, String x, String y, String z) {
        return CompletableFuture.runAsync(() -> {
            try {
                Variables.connectionSQLPortal = SQLManagerPortals.getConnectionSQL();
                SQLManagerPortals.createTableSQL().get();
                PreparedStatement checkStatement = Variables.connectionSQLPortal.prepareStatement(
                        "SELECT * FROM PlayerPortals WHERE player_name COLLATE BINARY = ?"
                );
                checkStatement.setString(1, playerName);
                ResultSet resultSet = checkStatement.executeQuery();
                if (resultSet.next()) {
                    String existingPortalNames = resultSet.getString("portal_names");
                    String existingPortalData = resultSet.getString("portal_data");
                    String[] portalNames = existingPortalNames.split(", ");
                    String[] portalData = existingPortalData.split(" \\| ");
                    StringBuilder newPortalNames = new StringBuilder();
                    StringBuilder newPortalData = new StringBuilder();
                    boolean portalFound = false;
                    for (int i = 0; i < portalNames.length; i++) {
                        if (portalNames[i].equals(portalName)) {
                            portalData[i] = worldName + ", " + x + ", " + y + ", " + z + " -> " + portalName;
                            portalFound = true;
                        }
                        if (newPortalNames.length() > 0) {
                            newPortalNames.append(", ");
                            newPortalData.append(" | ");
                        }
                        newPortalNames.append(portalNames[i]);
                        newPortalData.append(portalData[i]);
                    }
                    if (!portalFound) {
                        if (newPortalNames.length() > 0) {
                            newPortalNames.append(", ");
                            newPortalData.append(" | ");
                        }
                        newPortalNames.append(portalName);
                        newPortalData.append(worldName + ", " + x + ", " + y + ", " + z + " -> " + portalName);
                    }
                    PreparedStatement updateStatement = Variables.connectionSQLPortal.prepareStatement(
                            "UPDATE PlayerPortals SET portal_names = ?, portal_data = ? WHERE player_name COLLATE BINARY = ?"
                    );
                    updateStatement.setString(1, newPortalNames.toString());
                    updateStatement.setString(2, newPortalData.toString());
                    updateStatement.setString(3, playerName);
                    updateStatement.executeUpdate();
                    updateStatement.close();

                } else {
                    String portalDataString = worldName + ", " + x + ", " + y + ", " + z + " -> " + portalName;
                    PreparedStatement insertStatement = Variables.connectionSQLPortal.prepareStatement(
                            "INSERT INTO PlayerPortals (player_name, portal_names, portal_data) VALUES (?, ?, ?)"
                    );
                    insertStatement.setString(1, playerName);
                    insertStatement.setString(2, portalName);
                    insertStatement.setString(3, portalDataString);
                    insertStatement.executeUpdate();
                    insertStatement.close();
                }
                resultSet.close();
                checkStatement.close();
            } catch (SQLException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}