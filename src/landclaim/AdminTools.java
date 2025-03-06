package landclaim;

import net.risingworld.api.objects.Player;
import net.risingworld.api.Plugin;

public class AdminTools {
    private final LandClaim plugin;

    public AdminTools(LandClaim plugin) {
        this.plugin = plugin;
    }

    // Check if a player is an admin
    public boolean isAdmin(Player player) {
        return player.isAdmin();
    }

    // Migrate only Areas table from WorldProtection for testing
    public void migrateDatabase(Player player) {
        LandClaimDatabase db = plugin.getDatabase();
        if (db == null) {
            System.out.println("[LandClaim] Database not initialized for migration.");
            
            return;
        }

        db.migrateAreasFromWorldProtection(player);
    }
}


