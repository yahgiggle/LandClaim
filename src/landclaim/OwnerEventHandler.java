package landclaim;

import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.world.*;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.worldelements.Area3D;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OwnerEventHandler implements Listener {
    private final LandClaim plugin;

    public OwnerEventHandler(LandClaim plugin) {
        this.plugin = plugin;
    }

    private LandClaim.ClaimedArea getClaimFromArea3D(Area3D area, Vector3i chunkPos) {
        for (LandClaim.ClaimedArea claim : plugin.claimedAreas.values()) {
            Vector3f start = area.getArea().getStartPosition();
            Vector3i startChunk = plugin.getChunkPosition(start);
            if (startChunk.x == claim.areaX && startChunk.y == claim.areaY && startChunk.z == claim.areaZ) {
                boolean matches = (claim.areaX == chunkPos.x && claim.areaY == chunkPos.y && claim.areaZ == chunkPos.z);
                Logger.getLogger(OwnerEventHandler.class.getName()).info("Matched claim at chunk (" + claim.areaX + ", " + claim.areaY + ", " + claim.areaZ + 
                        ") owned by " + claim.areaOwnerName + " (UID: " + claim.playerUID + "), matches position: " + matches);
                return claim;
            }
        }
        Logger.getLogger(OwnerEventHandler.class.getName()).info("No matching claim found for area at chunk (" + chunkPos.x + ", " + chunkPos.y + ", " + chunkPos.z + ")");
        return null;
    }

    private boolean isGuest(Player player, LandClaim.ClaimedArea claim) {
        try {
            String query = "SELECT PlayerUID FROM `Guests` WHERE AreaID = (SELECT ID FROM `Areas` WHERE AreaX = " + claim.areaX +
                           " AND AreaY = " + claim.areaY + " AND AreaZ = " + claim.areaZ + ")";
            ResultSet rs = plugin.getDatabase().getDb().executeQuery(query);
            String playerUID = player.getUID();
            while (rs.next()) {
                String guestUID = rs.getString("PlayerUID");
                if (guestUID != null && guestUID.equals(playerUID)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            Logger.getLogger(OwnerEventHandler.class.getName()).log(Level.SEVERE, "Failed to check guest status", ex);
            return false;
        }
    }

    private boolean hasPermission(Player player, LandClaim.ClaimedArea claim, Vector3i chunkPos, String permission) {
        if (claim == null || !(claim.areaX == chunkPos.x && claim.areaY == chunkPos.y && claim.areaZ == chunkPos.z)) {
            return true; // No claim or mismatch, allow action
        }
        String playerUID = player.getUID();
        Logger.getLogger(OwnerEventHandler.class.getName()).info("Checking permission '" + permission + "' for player UID: " + playerUID + 
                " at chunk (" + claim.areaX + ", " + claim.areaY + ", " + claim.areaZ + ")");

        // Owners always have permission
        if (claim.playerUID.equals(playerUID)) {
            Logger.getLogger(OwnerEventHandler.class.getName()).info("Permission granted: Player is the owner.");
            return true;
        }

        // Check if player is a guest and get the specific permission
        if (isGuest(player, claim)) {
            try {
                int areaId = plugin.getDatabase().getAreaIdFromCoords(claim.areaX, claim.areaY, claim.areaZ);
                boolean allowed = plugin.getDatabase().getGuestPermission(areaId, permission);
                Logger.getLogger(OwnerEventHandler.class.getName()).info("Guest permission '" + permission + "' for area " + areaId + ": " + allowed);
                return allowed;
            } catch (SQLException ex) {
                Logger.getLogger(OwnerEventHandler.class.getName()).log(Level.SEVERE, "Failed to check guest permission '" + permission + "'", ex);
                return false; // Deny if we can't check
            }
        }

        Logger.getLogger(OwnerEventHandler.class.getName()).info("Permission denied: Player is neither owner nor guest.");
        return false;
    }

    @EventMethod
    public void onPlayerConnect(PlayerConnectEvent event) {
        Player player = event.getPlayer();
        player.sendYellMessage("LandClaim: Protection active!", 3, true);
    }

    @EventMethod
    public void onPlayerEnterChunk(PlayerEnterChunkEvent event) throws SQLException {
        Player player = event.getPlayer();
        Vector3i chunkPos = event.getNewChunkCoordinates();
        Vector3f playerPos = event.getNewPlayerPosition();
        player.setAttribute("PlayersChunkPositionX", chunkPos.x);
        player.setAttribute("PlayersChunkPositionY", chunkPos.y);
        player.setAttribute("PlayersChunkPositionZ", chunkPos.z);

        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(playerPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null) {
                        player.sendYellMessage("Entered claimed area: " + claim.areaName + " owned by " + claim.areaOwnerName, 3, true);
                    } else {
                        player.sendYellMessage("Entered unclaimed area", 3, true);
                    }
                    break;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerSpawn(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !isGuest(player, claim) && !claim.playerUID.equals(player.getUID())) {
                        player.sendYellMessage("You spawned in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerChangeConstructionColor(PlayerChangeConstructionColorEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerChangeConstructionColor")) {
                        player.sendYellMessage("You can’t change construction colors in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerChangeObjectColor(PlayerChangeObjectColorEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerChangeObjectColor")) {
                        player.sendYellMessage("You can’t change object colors in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerChangeObjectInfo(PlayerChangeObjectInfoEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerChangeObjectInfo")) {
                        player.sendYellMessage("You can’t change object info in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerConstructionPlace(PlayerPlaceConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerPlaceConstruction")) {
                        player.sendYellMessage("You can’t place construction in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerCreativePlaceVegetation(PlayerCreativePlaceVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerCreativePlaceVegetation")) {
                        player.sendYellMessage("You can’t place vegetation in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerCreativeRemoveConstruction(PlayerCreativeRemoveConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerCreativeRemoveConstruction")) {
                        player.sendYellMessage("You can’t remove construction in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerCreativeRemoveObject(PlayerCreativeRemoveObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerCreativeRemoveObject")) {
                        player.sendYellMessage("You can’t remove objects in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerCreativeRemoveVegetation(PlayerCreativeRemoveVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerCreativeRemoveVegetation")) {
                        player.sendYellMessage("You can’t remove vegetation in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerCreativeTerrainEdit(PlayerCreativeTerrainEditEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerCreativeTerrainEdit")) {
                        player.sendYellMessage("You can’t edit terrain in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerDestroyConstruction(PlayerDestroyConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerDestroyConstruction")) {
                        player.sendYellMessage("You can’t destroy construction in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerDestroyObject(PlayerDestroyObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerDestroyObject")) {
                        player.sendYellMessage("You can’t destroy objects in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerDestroyTerrain(PlayerDestroyTerrainEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerDestroyTerrain")) {
                        player.sendYellMessage("You can’t destroy terrain in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerDestroyVegetation(PlayerDestroyVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerDestroyVegetation")) {
                        player.sendYellMessage("You can’t destroy vegetation in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerEditConstruction(PlayerEditConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerEditConstruction")) {
                        player.sendYellMessage("You can’t edit construction in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerHitConstruction(PlayerHitConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerHitConstruction")) {
                        player.sendYellMessage("You can’t hit construction in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerHitObject(PlayerHitObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerHitObject")) {
                        player.sendYellMessage("You can’t hit objects in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerHitTerrain(PlayerHitTerrainEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerHitTerrain")) {
                        player.sendYellMessage("You can’t hit terrain in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerHitVegetation(PlayerHitVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerHitVegetation")) {
                        player.sendYellMessage("You can’t hit vegetation in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerHitWater(PlayerHitWaterEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerHitWater")) {
                        player.sendYellMessage("You can’t interact with water in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerPlaceBlueprint(PlayerPlaceBlueprintEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerPlaceBluePrints")) {
                        player.sendYellMessage("You can’t place blueprints in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerPlaceConstruction(PlayerPlaceConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerPlaceConstruction")) {
                        player.sendYellMessage("You can’t place construction in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerPlaceGrass(PlayerPlaceGrassEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerPlaceGrass")) {
                        player.sendYellMessage("You can’t place grass in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerPlaceObject(PlayerPlaceObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerPlaceObject")) {
                        player.sendYellMessage("You can’t place objects in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerPlaceTerrain(PlayerPlaceTerrainEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerPlaceTerrain")) {
                        player.sendYellMessage("You can’t place terrain in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerPlaceVegetation(PlayerPlaceVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerPlaceVegetation")) {
                        player.sendYellMessage("You can’t place vegetation in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerRemoveConstruction(PlayerRemoveConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerRemoveConstruction")) {
                        player.sendYellMessage("You can’t remove construction in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerRemoveGrass(PlayerRemoveGrassEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerRemoveGrass")) {
                        player.sendYellMessage("You can’t remove grass in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerRemoveObject(PlayerRemoveObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerRemoveObject")) {
                        player.sendYellMessage("You can’t remove objects in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerRemoveVegetation(PlayerRemoveVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerRemoveVegetation")) {
                        player.sendYellMessage("You can’t remove vegetation in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerRemoveWater(PlayerRemoveWaterEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerRemoveWater")) {
                        player.sendYellMessage("You can’t remove water in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventMethod
    public void onPlayerWorldEdit(PlayerWorldEditEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        ArrayList<Area3D> copyAllAreas = new ArrayList<>(plugin.LandClaimedAreas);
        if (copyAllAreas != null) {
            for (Area3D area : copyAllAreas) {
                if (area.getArea().isPointInArea(blockPos)) {
                    LandClaim.ClaimedArea claim = getClaimFromArea3D(area, chunkPos);
                    if (claim != null && !hasPermission(player, claim, chunkPos, "PlayerWorldEdit")) {
                        player.sendYellMessage("You can’t edit the world in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }
} 

