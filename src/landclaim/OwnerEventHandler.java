package landclaim;

import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.world.PlayerChangeConstructionColorEvent;
import net.risingworld.api.events.player.world.PlayerChangeObjectColorEvent;
import net.risingworld.api.events.player.world.PlayerChangeObjectInfoEvent;
import net.risingworld.api.events.player.world.PlayerCreativePlaceVegetationEvent;
import net.risingworld.api.events.player.world.PlayerCreativeRemoveConstructionEvent;
import net.risingworld.api.events.player.world.PlayerCreativeRemoveObjectEvent;
import net.risingworld.api.events.player.world.PlayerCreativeRemoveVegetationEvent;
import net.risingworld.api.events.player.world.PlayerCreativeTerrainEditEvent;
import net.risingworld.api.events.player.world.PlayerWorldEditEvent; // Reintroduced
import net.risingworld.api.events.player.world.PlayerDestroyConstructionEvent;
import net.risingworld.api.events.player.world.PlayerDestroyObjectEvent;
import net.risingworld.api.events.player.world.PlayerDestroyTerrainEvent;
import net.risingworld.api.events.player.world.PlayerDestroyVegetationEvent;
import net.risingworld.api.events.player.world.PlayerEditConstructionEvent;
import net.risingworld.api.events.player.world.PlayerHitConstructionEvent;
import net.risingworld.api.events.player.world.PlayerHitObjectEvent;
import net.risingworld.api.events.player.world.PlayerHitTerrainEvent;
import net.risingworld.api.events.player.world.PlayerHitVegetationEvent;
import net.risingworld.api.events.player.world.PlayerHitWaterEvent;
import net.risingworld.api.events.player.world.PlayerPlaceBlueprintEvent;
import net.risingworld.api.events.player.world.PlayerPlaceConstructionEvent;
import net.risingworld.api.events.player.world.PlayerPlaceGrassEvent;
import net.risingworld.api.events.player.world.PlayerPlaceObjectEvent;
import net.risingworld.api.events.player.world.PlayerPlaceTerrainEvent;
import net.risingworld.api.events.player.world.PlayerPlaceVegetationEvent;
import net.risingworld.api.events.player.world.PlayerRemoveConstructionEvent;
import net.risingworld.api.events.player.world.PlayerRemoveGrassEvent;
import net.risingworld.api.events.player.world.PlayerRemoveObjectEvent;
import net.risingworld.api.events.player.world.PlayerRemoveVegetationEvent;
import net.risingworld.api.events.player.world.PlayerRemoveWaterEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;
import net.risingworld.api.utils.Vector3i;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class OwnerEventHandler implements Listener {
    private final LandClaim plugin;
    private final Map<String, Map<Integer, Boolean>> guestPermissionCache = new HashMap<>(); // UID -> AreaID -> Permission

    public OwnerEventHandler(LandClaim plugin) {
        this.plugin = plugin;
        System.out.println("[LandClaim] Registering OwnerEventHandler...");
    }

    private LandClaim.ClaimedArea getClaimAt(Vector3i chunkPos) {
        return plugin.getClaimedAreaAt(chunkPos);
    }

    private boolean isGuest(Player player, LandClaim.ClaimedArea claim) {
        try {
            String query = "SELECT PlayerUID FROM `Guests` WHERE AreaID = (SELECT ID FROM `Areas` WHERE AreaX = " + claim.areaX +
                           " AND AreaY = " + claim.areaY + " AND AreaZ = " + claim.areaZ + ")";
            ResultSet rs = plugin.getDatabase().getDb().executeQuery(query);
            String playerUID = player.getUID();
            while (rs.next()) {
                if (rs.getString("PlayerUID").equals(playerUID)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            System.out.println("[LandClaim] Error checking guest status: " + e.getMessage());
            return false;
        }
    }

    private boolean hasPermission(Player player, LandClaim.ClaimedArea claim, String permission) {
        if (claim == null) return true;
        String playerUID = player.getUID();
        if (claim.playerUID.equals(playerUID)) return true;

        if (isGuest(player, claim)) {
            Map<Integer, Boolean> areaPermissions = guestPermissionCache.computeIfAbsent(playerUID, k -> new HashMap<>());
            int areaId = -1;
            try {
                areaId = plugin.getDatabase().getAreaIdFromCoords(claim.areaX, claim.areaY, claim.areaZ);
                Boolean cached = areaPermissions.get(areaId);
                if (cached != null) {
                    return cached;
                }
                boolean allowed = plugin.getDatabase().getGuestPermission(areaId, permission);
                areaPermissions.put(areaId, allowed);
                return allowed;
            } catch (SQLException e) {
                System.out.println("[LandClaim] Error checking permission '" + permission + "' for area " + areaId + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    private void checkAndCancel(Player player, Vector3i chunkPos, Vector3f blockPos, String permission, String action, Object event) {
        LandClaim.ClaimedArea claim = getClaimAt(chunkPos);
        if (claim != null && !hasPermission(player, claim, permission)) {
            player.sendYellMessage("You can’t " + action + " in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
            if (event instanceof net.risingworld.api.events.Cancellable) {
                ((net.risingworld.api.events.Cancellable) event).setCancelled(true);
            }
        }
    }

    @EventMethod
    public void onPlayerConnect(PlayerConnectEvent event) {
        Player player = event.getPlayer();
        player.sendYellMessage("LandClaim: Protection active!", 3, true);
    }

    @EventMethod
    public void onPlayerEnterChunk(PlayerEnterChunkEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = event.getNewChunkCoordinates();
        Vector3f playerPos = event.getNewPlayerPosition();
        LandClaim.ClaimedArea claim = getClaimAt(chunkPos);
        player.sendYellMessage(claim != null ? "Entered claimed area: " + claim.areaName + " owned by " + claim.areaOwnerName :
            "Entered unclaimed area", 3, true);
    }

    @EventMethod
    public void onPlayerSpawn(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        LandClaim.ClaimedArea claim = getClaimAt(chunkPos);
        if (claim != null && !isGuest(player, claim) && !claim.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You spawned in " + claim.areaName + " owned by " + claim.areaOwnerName + "!", 3, true);
        }
    }

    @EventMethod
    public void onPlayerChangeConstructionColor(PlayerChangeConstructionColorEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerChangeConstructionColor", "change construction colors", event);
    }

    @EventMethod
    public void onPlayerChangeObjectColor(PlayerChangeObjectColorEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerChangeObjectColor", "change object colors", event);
    }

    @EventMethod
    public void onPlayerChangeObjectInfo(PlayerChangeObjectInfoEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerChangeObjectInfo", "change object info", event);
    }

    @EventMethod
    public void onPlayerCreativePlaceVegetation(PlayerCreativePlaceVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerCreativePlaceVegetation", "place vegetation", event);
    }

    @EventMethod
    public void onPlayerCreativeRemoveConstruction(PlayerCreativeRemoveConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerCreativeRemoveConstruction", "remove construction", event);
    }

    @EventMethod
    public void onPlayerCreativeRemoveObject(PlayerCreativeRemoveObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerCreativeRemoveObject", "remove objects", event);
    }

    @EventMethod
    public void onPlayerCreativeRemoveVegetation(PlayerCreativeRemoveVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerCreativeRemoveVegetation", "remove vegetation", event);
    }

    @EventMethod
    public void onPlayerCreativeTerrainEdit(PlayerCreativeTerrainEditEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerCreativeTerrainEdit", "edit terrain", event);
    }

    @EventMethod
    public void onPlayerWorldEdit(PlayerWorldEditEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = new Vector3i(event.getChunkPositionX(), event.getChunkPositionY(), event.getChunkPositionZ());
        Vector3f blockPos = new Vector3f(event.getBlockPositionX(), event.getBlockPositionY(), event.getBlockPositionZ());
        System.out.println("[LandClaim] PlayerWorldEditEvent triggered at chunk " + chunkPos + ", block " + blockPos);
        checkAndCancel(player, chunkPos, blockPos, "PlayerWorldEdit", "edit the world", event);
    }

    @EventMethod
    public void onPlayerDestroyConstruction(PlayerDestroyConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerDestroyConstruction", "destroy construction", event);
    }

    @EventMethod
    public void onPlayerDestroyObject(PlayerDestroyObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerDestroyObject", "destroy objects", event);
    }

    @EventMethod
    public void onPlayerDestroyTerrain(PlayerDestroyTerrainEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerDestroyTerrain", "destroy terrain", event);
    }

    @EventMethod
    public void onPlayerDestroyVegetation(PlayerDestroyVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerDestroyVegetation", "destroy vegetation", event);
    }

    @EventMethod
    public void onPlayerEditConstruction(PlayerEditConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerEditConstruction", "edit construction", event);
    }

    @EventMethod
    public void onPlayerHitConstruction(PlayerHitConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerHitConstruction", "hit construction", event);
    }

    @EventMethod
    public void onPlayerHitObject(PlayerHitObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerHitObject", "hit objects", event);
    }

    @EventMethod
    public void onPlayerHitTerrain(PlayerHitTerrainEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerHitTerrain", "hit terrain", event);
    }

    @EventMethod
    public void onPlayerHitVegetation(PlayerHitVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerHitVegetation", "hit vegetation", event);
    }

    @EventMethod
    public void onPlayerHitWater(PlayerHitWaterEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerHitWater", "interact with water", event);
    }

    @EventMethod
    public void onPlayerPlaceBlueprint(PlayerPlaceBlueprintEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerPlaceBluePrints", "place blueprints", event);
    }

    @EventMethod
    public void onPlayerPlaceConstruction(PlayerPlaceConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerPlaceConstruction", "place construction", event);
    }

    @EventMethod
    public void onPlayerPlaceGrass(PlayerPlaceGrassEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerPlaceGrass", "place grass", event);
    }

    @EventMethod
    public void onPlayerPlaceObject(PlayerPlaceObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerPlaceObject", "place objects", event);
    }

    @EventMethod
    public void onPlayerPlaceTerrain(PlayerPlaceTerrainEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerPlaceTerrain", "place terrain", event);
    }

    @EventMethod
    public void onPlayerPlaceVegetation(PlayerPlaceVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerPlaceVegetation", "place vegetation", event);
    }

    @EventMethod
    public void onPlayerRemoveConstruction(PlayerRemoveConstructionEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerRemoveConstruction", "remove construction", event);
    }

    @EventMethod
    public void onPlayerRemoveGrass(PlayerRemoveGrassEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerRemoveGrass", "remove grass", event);
    }

    @EventMethod
    public void onPlayerRemoveObject(PlayerRemoveObjectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerRemoveObject", "remove objects", event);
    }

    @EventMethod
    public void onPlayerRemoveVegetation(PlayerRemoveVegetationEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerRemoveVegetation", "remove vegetation", event);
    }

    @EventMethod
    public void onPlayerRemoveWater(PlayerRemoveWaterEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        Vector3f blockPos = player.getPosition();
        checkAndCancel(player, chunkPos, blockPos, "PlayerRemoveWater", "remove water", event);
    }
}