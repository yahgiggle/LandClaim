package landclaim;

import net.risingworld.api.objects.Player;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AdminTools {
    private final LandClaim plugin;
    private static boolean isMigrating = false; // Flag to track migration status

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

        LandClaimDatabase db = plugin.getDatabase();
        if (db == null) {
            return "Migration failed: Database not initialized.";
        }

        String feedback = "Migration in progress...";
        try {
            isMigrating = true; // Set flag at start
            System.out.println("[LandClaim] Starting migration from WorldProtection...");
            db.migrateAreasFromWorldProtection(player);

            // Count areas per player after migration
            Map<String, Integer> playerAreaCounts = new HashMap<>();
            String countQuery = "SELECT PlayerUID, COUNT(*) as areaCount FROM `Areas` GROUP BY PlayerUID";
            System.out.println("[LandClaim] Executing count query: " + countQuery);
            ResultSet rs = db.getDb().executeQuery(countQuery);
            while (rs.next()) {
                String playerUID = rs.getString("PlayerUID");
                int areaCount = rs.getInt("areaCount");
                playerAreaCounts.put(playerUID, areaCount);
            }
            rs.close();
            System.out.println("[LandClaim] Counted areas for " + playerAreaCounts.size() + " players.");

            // Update PlayerAreaStats with new MaxAreaAllocation (current + 2)
            for (Map.Entry<String, Integer> entry : playerAreaCounts.entrySet()) {
                String playerUID = entry.getKey();
                int currentAreas = entry.getValue();
                int newMaxAreas = currentAreas + 2;

                String checkQuery = "SELECT PlayerUID FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'";
                ResultSet checkRs = db.getDb().executeQuery(checkQuery);
                String updateQuery;
                if (checkRs.next()) {
                    updateQuery = "UPDATE `PlayerAreaStats` SET AreaCount = " + currentAreas + ", MaxAreaAllocation = " + newMaxAreas + " WHERE PlayerUID = '" + playerUID + "'";
                } else {
                    updateQuery = "INSERT INTO `PlayerAreaStats` (PlayerUID, AreaCount, MaxAreaAllocation) VALUES ('" + playerUID + "', " + currentAreas + ", " + newMaxAreas + ")";
                }
                checkRs.close();

                System.out.println("[LandClaim] Executing update for PlayerUID " + playerUID + ": " + updateQuery);
                db.getDb().executeUpdate(updateQuery);

                Player owner = findPlayerByUID(playerUID);
                if (owner != null) {
                    owner.sendTextMessage("Your area limit has been updated to " + newMaxAreas + " areas due to migration.");
                }
            }

            // Reload claimed areas into memory
            System.out.println("[LandClaim] Reloading claimed areas...");
            plugin.claimedAreas.clear();
            plugin.loadClaimedAreas();
            System.out.println("[LandClaim] Reloaded " + plugin.claimedAreas.size() + " claimed areas.");

            feedback = "Migration successful!";
        } catch (SQLException e) {
            feedback = "Migration failed: " + e.getMessage();
            System.out.println("[LandClaim] Migration error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isMigrating = false; // Reset flag when done, even on failure
        }
        return feedback;
    }

    private Player findPlayerByUID(String playerUID) {
        Player[] onlinePlayers = Server.getAllPlayers();
        for (Player p : onlinePlayers) {
            if (p.getUID().equals(playerUID)) {
                return p;
            }
        }
        return null;
    }
}