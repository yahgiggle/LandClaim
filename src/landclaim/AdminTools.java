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
    private volatile boolean isMigrating = false;
    private volatile String lastMigrationResult = "Migration queued...";

    public AdminTools(LandClaim plugin) {
        this.plugin = plugin;
    }

    public boolean isAdmin(Player player) {
        return player.isAdmin();
    }

    public String migrateDatabase(Player player) {
        if (!isAdmin(player)) return "Only admins can migrate!";
        if (isMigrating) return "Migration in progress...";

        UILabel feedback = (UILabel) player.getAttribute("FeedBackinfoPanel");
        if (feedback != null) feedback.setText("Migration started...");

        plugin.getTaskQueue().queueTask(
            () -> {
                isMigrating = true;
                try {
                    LandClaimDatabase db = plugin.getDatabase();
                    if (db == null) {
                        lastMigrationResult = "Database not initialized!";
                        return;
                    }
                    db.migrateAreasFromWorldProtection(player);

                    Map<String, Integer> counts = new HashMap<>();
                    ResultSet rs = db.getDb().executeQuery("SELECT PlayerUID, COUNT(*) as count FROM `Areas` GROUP BY PlayerUID");
                    while (rs.next()) counts.put(rs.getString("PlayerUID"), rs.getInt("count"));

                    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                        String uid = entry.getKey();
                        int count = entry.getValue();
                        int max = count + 2; // Initial max allocation
                        ResultSet existingStats = db.getDb().executeQuery("SELECT PlayerUID FROM `PlayerAreaStats` WHERE PlayerUID = '" + uid + "'");
                        String sql;
                        if (existingStats.next()) {
                            sql = "UPDATE `PlayerAreaStats` SET AreaCount = " + count + ", MaxAreaAllocation = " + max + " WHERE PlayerUID = '" + uid + "'";
                        } else {
                            sql = "INSERT INTO `PlayerAreaStats` (PlayerUID, AreaCount, MaxAreaAllocation) VALUES ('" + uid + "', " + count + ", " + max + ")";
                        }
                        db.getDb().executeUpdate(sql);

                        // Ensure Points row exists for migrated players
                        ResultSet pointsRs = db.getDb().executeQuery("SELECT ID FROM `Points` WHERE PlayerUID = '" + uid + "'");
                        if (!pointsRs.next()) {
                            db.getDb().executeUpdate(
                                "INSERT INTO `Points` (PlayerUID, UserName, Points, TotalPlaytimeHours) " +
                                "VALUES ('" + uid + "', 'MigratedUser', 0, 0.0)"
                            );
                            System.out.println("[LandClaim] Initialized Points row for migrated PlayerUID: " + uid);
                        }

                        Player[] allPlayers = Server.getAllPlayers();
                        for (Player owner : allPlayers) {
                            if (owner.getUID().equals(uid)) {
                                owner.sendTextMessage("Area limit updated to " + max + " due to migration.");
                                break;
                            }
                        }
                    }
                    lastMigrationResult = "Migration successful! Migrated " + counts.size() + " players.";
                } catch (SQLException e) {
                    lastMigrationResult = "Migration failed: " + e.getMessage();
                    System.out.println("[LandClaim] Migration error: " + e.getMessage());
                } finally {
                    isMigrating = false;
                }
            },
            () -> {
                plugin.claimedAreasByChunk.clear();
                try {
                    plugin.loadClaimedAreas();
                } catch (SQLException e) {
                    lastMigrationResult = "Migration completed with errors: " + e.getMessage();
                }
                if (feedback != null) feedback.setText(lastMigrationResult);
            }
        );
        return "Migration queued...";
    }

    public String getLastMigrationResult() {
        return lastMigrationResult;
    }
}

