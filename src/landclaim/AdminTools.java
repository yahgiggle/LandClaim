package landclaim;

import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AdminTools {
    private final LandClaim plugin;
    private volatile boolean isMigrating = false; // Thread-safe flag
    private volatile String lastMigrationResult = "Migration queued..."; // Track result for callback

    public AdminTools(LandClaim plugin) {
        this.plugin = plugin;
    }

    public boolean isAdmin(Player player) {
        return player.isAdmin();
    }

    public String migrateDatabase(Player player) {
        if (!isAdmin(player)) {
            return "Migration failed: Only admins can perform database migration!";
        }
        if (isMigrating) {
            return "Migration already in progress, please wait...";
        }

        UILabel feedbackPanel = (UILabel) player.getAttribute("FeedBackinfoPanel");
        if (feedbackPanel != null) {
            feedbackPanel.setText("Migration started...");
        }

        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    isMigrating = true;
                    System.out.println("[LandClaim] Starting migration from WorldProtection...");
                    LandClaimDatabase db = plugin.getDatabase();
                    if (db == null) {
                        lastMigrationResult = "Migration failed: Database not initialized.";
                        return;
                    }

                    db.migrateAreasFromWorldProtection(player);

                    // Count areas per player
                    Map<String, Integer> playerAreaCounts = new HashMap<>();
                    String countQuery = "SELECT PlayerUID, COUNT(*) as areaCount FROM `Areas` GROUP BY PlayerUID";
                    System.out.println("[LandClaim] Executing count query: " + countQuery);
                    ResultSet rs = db.getDb().executeQuery(countQuery);
                    while (rs.next()) {
                        playerAreaCounts.put(rs.getString("PlayerUID"), rs.getInt("areaCount"));
                    }
                    rs.close();

                    // Update PlayerAreaStats
                    for (Map.Entry<String, Integer> entry : playerAreaCounts.entrySet()) {
                        String playerUID = entry.getKey();
                        int currentAreas = entry.getValue();
                        int newMaxAreas = currentAreas + 2;

                        String checkQuery = "SELECT PlayerUID FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'";
                        ResultSet checkRs = db.getDb().executeQuery(checkQuery);
                        String updateQuery = checkRs.next() ?
                            "UPDATE `PlayerAreaStats` SET AreaCount = " + currentAreas + ", MaxAreaAllocation = " + newMaxAreas + " WHERE PlayerUID = '" + playerUID + "'" :
                            "INSERT INTO `PlayerAreaStats` (PlayerUID, AreaCount, MaxAreaAllocation) VALUES ('" + playerUID + "', " + currentAreas + ", " + newMaxAreas + ")";
                        checkRs.close();
                        db.getDb().executeUpdate(updateQuery);

                        Player owner = Server.getPlayerByUID(playerUID); // Use Server static method
                        if (owner != null) {
                            owner.sendTextMessage("Your area limit has been updated to " + newMaxAreas + " areas due to migration.");
                        }
                    }

                    lastMigrationResult = "Migration successful!";
                } catch (SQLException e) {
                    lastMigrationResult = "Migration failed: " + e.getMessage();
                    System.out.println("[LandClaim] Migration error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    isMigrating = false;
                }
            },
            () -> {
                // Reload areas on main thread
                plugin.claimedAreasByChunk.clear();
                try {
                    plugin.loadClaimedAreas();
                } catch (SQLException e) {
                    System.out.println("[LandClaim] Reload error: " + e.getMessage());
                    lastMigrationResult = "Migration completed with errors: " + e.getMessage();
                }
                if (feedbackPanel != null) {
                    feedbackPanel.setText(lastMigrationResult);
                }
            }
        );

        return "Migration queued...";
    }

    public String getLastMigrationResult() {
        return lastMigrationResult;
    }
}