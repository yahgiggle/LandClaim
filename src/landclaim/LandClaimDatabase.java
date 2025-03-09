package landclaim;

import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.database.Database;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import landclaim.LandClaim.ClaimedArea;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class LandClaimDatabase {
    private final Plugin plugin;
    private Database db;
    private Database oldDataBase;

    public LandClaimDatabase(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        System.out.println("-- LandClaim Database Loaded --");
        String worldName = World.getName();
        
        oldDataBase = plugin.getSQLiteConnection("Plugins/WorldProtection/" + worldName + "/database.db");
        if (oldDataBase == null) {
            System.out.println("[LandClaim] Could not connect to WorldProtection database at: Plugins/WorldProtection/" + worldName + "/database.db");
            return;
        }

        db = plugin.getSQLiteConnection(plugin.getPath() + "/" + worldName + "/database.db");

        db.execute("CREATE TABLE IF NOT EXISTS `Areas` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `AreaOwnerName` VARCHAR(64), `AreaName` VARCHAR(64), `AreaX` INTEGER, `AreaY` INTEGER, `AreaZ` INTEGER, `PlayerUID` BIGINT, `AreaLocked` BOOLEAN DEFAULT 0, `PVPStatus` BOOLEAN DEFAULT 0, `CreationDate` DATE);");
        db.execute("CREATE TABLE IF NOT EXISTS `Guests` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `AreaID` INTEGER, `GuestName` VARCHAR(64), `PlayerUID` BIGINT, FOREIGN KEY (AreaID) REFERENCES Areas(ID));");
        db.execute("CREATE TABLE IF NOT EXISTS `Points` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `UserName` VARCHAR(64), `Points` INTEGER, `PlayerUID` BIGINT);");
        db.execute("CREATE TABLE IF NOT EXISTS `AdminSettings` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `PointsEarnedAdjust` INTEGER, `AreaCostAdjust` INTEGER);");
        db.execute("CREATE TABLE IF NOT EXISTS `DataBaseVersion` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `Version` INTEGER);");
        db.execute("CREATE TABLE IF NOT EXISTS `PlayerAreaStats` (`PlayerUID` BIGINT PRIMARY KEY NOT NULL, `AreaCount` INTEGER DEFAULT 0, `MaxAreaAllocation` INTEGER DEFAULT 2);");

        db.execute("CREATE TABLE IF NOT EXISTS `GuestEventActions` (" +
            "`AreaID` INTEGER NOT NULL, " +
            "`PlayerDoorObjectStatus` BOOLEAN DEFAULT 1, " +
            "`PlayerDestroyTerrain` BOOLEAN DEFAULT 1, " +
            "`PlayerHitTerrain` BOOLEAN DEFAULT 1, " +
            "`PlayerBedUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerCampfireUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerStorageObjectStatus` BOOLEAN DEFAULT 1, " +
            "`PlayerTreeDestroyVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerPrivateStatus` BOOLEAN DEFAULT 1, " +
            "`PlayerTrunkDestroyVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerTreeHitVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerTrunkHitVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerTreeRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerTrunkRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerCropRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerFruitTreeRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerPlantRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerRockDestroyVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerRockHitVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerRockRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerAdminRights` BOOLEAN DEFAULT 1, " +
            "`PlayerClockUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerDoorUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerDryingRackUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerFireUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerFurnaceUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerGrillUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerGrinderUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerGrindstoneUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerLadderUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerLampUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerMusicPlayerUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerOvenUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerPaperPressUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerPianoUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerPosterUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerScaffoldingUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerSeatingUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerShootingTargetUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerSignUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerSpinningWheelUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerStorageUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerTanningRackUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerTechnicalUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerTorchUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerTrashcanUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerWorkbenchUseObject` BOOLEAN DEFAULT 1, " +
            "`PlayerHitAnimalNPC` BOOLEAN DEFAULT 1, " +
            "`PlayerHitHumanNPC` BOOLEAN DEFAULT 1, " +
            "`PlayerHitMountNPC` BOOLEAN DEFAULT 1, " +
            "`PlayerRideMountNPC` BOOLEAN DEFAULT 1, " +
            "`PlayerPlaceBluePrints` BOOLEAN DEFAULT 1, " +
            "`PlayerNpcAddSaddle` BOOLEAN DEFAULT 1, " +
            "`PlayerNpcRemoveSaddle` BOOLEAN DEFAULT 1, " +
            "`PlayerNpcAddSaddleBag` BOOLEAN DEFAULT 1, " +
            "`PlayerNpcRemoveSaddleBag` BOOLEAN DEFAULT 1, " +
            "`PlayerNpcAddClothes` BOOLEAN DEFAULT 1, " +
            "`PlayerNpcRemoveClothes` BOOLEAN DEFAULT 1, " +
            "`PlayerChangeConstructionColor` BOOLEAN DEFAULT 1, " +
            "`PlayerChangeObjectColor` BOOLEAN DEFAULT 1, " +
            "`PlayerChangeObjectInfo` BOOLEAN DEFAULT 1, " +
            "`PlayerCreativePlaceVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerCreativeRemoveConstruction` BOOLEAN DEFAULT 1, " +
            "`PlayerCreativeRemoveObject` BOOLEAN DEFAULT 1, " +
            "`PlayerCreativeRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerCreativeTerrainEdit` BOOLEAN DEFAULT 1, " +
            "`PlayerDestroyConstruction` BOOLEAN DEFAULT 1, " +
            "`PlayerDestroyObject` BOOLEAN DEFAULT 1, " +
            "`PlayerEditConstruction` BOOLEAN DEFAULT 1, " +
            "`PlayerHitConstruction` BOOLEAN DEFAULT 1, " +
            "`PlayerHitObject` BOOLEAN DEFAULT 1, " +
            "`PlayerHitVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerHitWater` BOOLEAN DEFAULT 1, " +
            "`PlayerPlaceConstruction` BOOLEAN DEFAULT 1, " +
            "`PlayerPlaceGrass` BOOLEAN DEFAULT 1, " +
            "`PlayerPlaceObject` BOOLEAN DEFAULT 1, " +
            "`PlayerPlaceTerrain` BOOLEAN DEFAULT 1, " +
            "`PlayerPlaceVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerRemoveConstruction` BOOLEAN DEFAULT 1, " +
            "`PlayerRemoveGrass` BOOLEAN DEFAULT 1, " +
            "`PlayerRemoveObject` BOOLEAN DEFAULT 1, " +
            "`PlayerRemoveVegetation` BOOLEAN DEFAULT 1, " +
            "`PlayerRemoveWater` BOOLEAN DEFAULT 1, " +
            "`PlayerWorldEdit` BOOLEAN DEFAULT 1, " +
            "FOREIGN KEY (AreaID) REFERENCES Areas(ID));");

        try (ResultSet result = db.executeQuery("SELECT * FROM `DataBaseVersion` WHERE ID = '1'")) {
            if (!result.next()) {
                db.executeUpdate("INSERT INTO `DataBaseVersion` (Version) VALUES ('0');");
            }
        } catch (SQLException ex) {
            Logger.getLogger(LandClaimDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void migrateAreasFromWorldProtection(Player player) {
        UILabel FeedBackinfoPanel = (UILabel)player.getAttribute("FeedBackinfoPanel");
        FeedBackinfoPanel.setText("Migration of Areas Started");
        
        if (oldDataBase == null) {
            System.out.println("[LandClaim] WorldProtection database not initialized for migration.");
            FeedBackinfoPanel.setText("[LandClaim] WorldProtection database not initialized for migration!!!");
            return;
        }

        Map<String, Integer> migratedAreas = new HashMap<>(); // Track areas by coords to avoid duplicates
        int migratedGuests = 0;

        try {
            boolean hasCreationDate = false, hasAreaLocked = false, hasPVPStatus = false, hasAreaGuest = false;
            ResultSet columns = oldDataBase.executeQuery("PRAGMA table_info(`Areas`)");
            while (columns.next()) {
                String columnName = columns.getString("name");
                if ("CreationDate".equals(columnName)) hasCreationDate = true;
                if ("AreaLocked".equals(columnName)) hasAreaLocked = true;
                if ("PVPStatus".equals(columnName)) hasPVPStatus = true;
                if ("AreaGuest".equals(columnName)) hasAreaGuest = true;
            }

            String selectSql = "SELECT ID, AreaOwnerName, AreaName, AreaX, AreaY, AreaZ, PlayerUID";
            if (hasCreationDate) selectSql += ", CreationDate";
            if (hasAreaLocked) selectSql += ", AreaLocked";
            if (hasPVPStatus) selectSql += ", PVPStatus";
            if (hasAreaGuest) selectSql += ", AreaGuest";
            selectSql += " FROM `Areas`";

            ResultSet areasRs = oldDataBase.executeQuery(selectSql);
            while (areasRs.next()) {
                int id = areasRs.getInt("ID");
                String areaOwnerName = areasRs.getString("AreaOwnerName");
                String areaName = areasRs.getString("AreaName");
                int areaX = areasRs.getInt("AreaX");
                int areaY = areasRs.getInt("AreaY");
                int areaZ = areasRs.getInt("AreaZ");
                long playerUID = areasRs.getLong("PlayerUID");
                String areaGuest = areasRs.getString("AreaGuest");

                // Create a unique key for the area based on coordinates
                String areaKey = areaX + "," + areaY + "," + areaZ;

                // Check if this area (by coords) has been migrated
                if (!migratedAreas.containsKey(areaKey)) {
                    // New area - insert it
                    ResultSet existing = db.executeQuery("SELECT ID FROM `Areas` WHERE AreaX = " + areaX + " AND AreaY = " + areaY + " AND AreaZ = " + areaZ);
                    if (!existing.next()) {
                        String insertSql = "INSERT INTO `Areas` (ID, AreaOwnerName, AreaName, AreaX, AreaY, AreaZ, PlayerUID, AreaLocked, PVPStatus, CreationDate) " +
                            "VALUES (" + id + ", '" + escapeSql(areaOwnerName) + "', '" + escapeSql(areaName) + "', " +
                            areaX + ", " + areaY + ", " + areaZ + ", " + playerUID + ", 0, 0, NULL)";
                        db.executeUpdate(insertSql);

                        // Insert default GuestEventActions row
                        String insertPermissionsSql = "INSERT INTO `GuestEventActions` (AreaID, " +
                            "PlayerDoorObjectStatus, PlayerDestroyTerrain, PlayerHitTerrain, PlayerBedUseObject, " +
                            "PlayerCampfireUseObject, PlayerStorageObjectStatus, PlayerTreeDestroyVegetation, " +
                            "PlayerPrivateStatus, PlayerTrunkDestroyVegetation, PlayerTreeHitVegetation, " +
                            "PlayerTrunkHitVegetation, PlayerTreeRemoveVegetation, PlayerTrunkRemoveVegetation, " +
                            "PlayerCropRemoveVegetation, PlayerFruitTreeRemoveVegetation, PlayerPlantRemoveVegetation, " +
                            "PlayerRockDestroyVegetation, PlayerRockHitVegetation, PlayerRockRemoveVegetation, " +
                            "PlayerAdminRights, PlayerClockUseObject, PlayerDoorUseObject, PlayerDryingRackUseObject, " +
                            "PlayerFireUseObject, PlayerFurnaceUseObject, PlayerGrillUseObject, PlayerGrinderUseObject, " +
                            "PlayerGrindstoneUseObject, PlayerLadderUseObject, PlayerLampUseObject, " +
                            "PlayerMusicPlayerUseObject, PlayerOvenUseObject, PlayerPaperPressUseObject, " +
                            "PlayerPianoUseObject, PlayerPosterUseObject, PlayerScaffoldingUseObject, " +
                            "PlayerSeatingUseObject, PlayerShootingTargetUseObject, PlayerSignUseObject, " +
                            "PlayerSpinningWheelUseObject, PlayerStorageUseObject, PlayerTanningRackUseObject, " +
                            "PlayerTechnicalUseObject, PlayerTorchUseObject, PlayerTrashcanUseObject, " +
                            "PlayerWorkbenchUseObject, PlayerHitAnimalNPC, PlayerHitHumanNPC, PlayerHitMountNPC, " +
                            "PlayerRideMountNPC, PlayerPlaceBluePrints, PlayerNpcAddSaddle, PlayerNpcRemoveSaddle, " +
                            "PlayerNpcAddSaddleBag, PlayerNpcRemoveSaddleBag, PlayerNpcAddClothes, " +
                            "PlayerNpcRemoveClothes, PlayerChangeConstructionColor, PlayerChangeObjectColor, " +
                            "PlayerChangeObjectInfo, PlayerCreativePlaceVegetation, PlayerCreativeRemoveConstruction, " +
                            "PlayerCreativeRemoveObject, PlayerCreativeRemoveVegetation, PlayerCreativeTerrainEdit, " +
                            "PlayerDestroyConstruction, PlayerDestroyObject, PlayerEditConstruction, " +
                            "PlayerHitConstruction, PlayerHitObject, PlayerHitVegetation, PlayerHitWater, " +
                            "PlayerPlaceConstruction, PlayerPlaceGrass, PlayerPlaceObject, PlayerPlaceTerrain, " +
                            "PlayerPlaceVegetation, PlayerRemoveConstruction, PlayerRemoveGrass, PlayerRemoveObject, " +
                            "PlayerRemoveVegetation, PlayerRemoveWater, PlayerWorldEdit) " +
                            "VALUES (" + id + ", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, " +
                            "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, " +
                            "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, " +
                            "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)";
                        db.executeUpdate(insertPermissionsSql);
                        System.out.println("[LandClaim] Added GuestEventActions row for migrated AreaID: " + id);
                    }
                    migratedAreas.put(areaKey, id); // Mark this area as migrated
                }

                // Handle guest if AreaGuest is a valid UID (not the placeholder "AreaGuest")
                if (hasAreaGuest && areaGuest != null && !areaGuest.trim().equalsIgnoreCase("AreaGuest")) {
                    try {
                        long guestUID = Long.parseLong(areaGuest.trim());
                        if (guestUID != 0 && guestUID != playerUID) { // Avoid adding owner as guest
                            int areaId = migratedAreas.get(areaKey);
                            ResultSet existingGuest = db.executeQuery("SELECT ID FROM `Guests` WHERE AreaID = " + areaId + " AND PlayerUID = " + guestUID);
                            if (!existingGuest.next()) {
                                db.executeUpdate("INSERT INTO `Guests` (AreaID, PlayerUID) VALUES (" + areaId + ", " + guestUID + ")");
                                System.out.println("[LandClaim] Added guest with UID " + guestUID + " to AreaID " + areaId);
                                migratedGuests++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("[LandClaim] Skipping invalid AreaGuest value '" + areaGuest + "' for AreaID " + id + " - not a valid UID");
                    }
                }

                // Apply updates (CreationDate, AreaLocked, PVPStatus) to the existing or newly inserted area
                String updateSql = "UPDATE `Areas` SET ";
                boolean hasUpdates = false;
                if (hasCreationDate) {
                    String creationDate = areasRs.getString("CreationDate");
                    if (creationDate != null) {
                        updateSql += "CreationDate = '" + creationDate + "'";
                        hasUpdates = true;
                    }
                }
                if (hasAreaLocked) {
                    if (hasUpdates) updateSql += ", ";
                    int areaLocked = areasRs.getInt("AreaLocked");
                    updateSql += "AreaLocked = " + (areaLocked != 0);
                    hasUpdates = true;
                }
                if (hasPVPStatus) {
                    if (hasUpdates) updateSql += ", ";
                    int pvpStatus = areasRs.getInt("PVPStatus");
                    updateSql += "PVPStatus = " + (pvpStatus != 0);
                    hasUpdates = true;
                }
                if (hasUpdates) {
                    updateSql += " WHERE ID = " + migratedAreas.get(areaKey);
                    db.executeUpdate(updateSql);
                }
            }

            if (migratedGuests > 0) {
                System.out.println("[LandClaim] Migration of " + migratedGuests + " guests from WorldProtection database completed.");
            } else {
                System.out.println("[LandClaim] No valid guests migrated from WorldProtection.");
            }
            System.out.println("[LandClaim] Migration of Areas from WorldProtection database completed.");
            FeedBackinfoPanel.setText("Migration of Areas from WorldProtection database completed");
            
        } catch (SQLException ex) {
            System.out.println("[LandClaim] Areas and Guests migration failed: " + ex.getMessage());
            FeedBackinfoPanel.setText("Areas and Guests migration failed!!!");
            ex.printStackTrace();
        }
    }

    public void close() {
        if (db != null) {
            db.close();
        }
        if (oldDataBase != null) {
            oldDataBase.close();
        }
    }

    public Database getDb() {
        return db;
    }

    public Database getOldDataBase() {
        return oldDataBase;
    }

    public boolean addClaimedArea(ClaimedArea area) throws SQLException {
        ResultSet rsCheck = db.executeQuery(
            "SELECT ID FROM `Areas` WHERE AreaX = " + area.areaX + " AND AreaY = " + area.areaY + " AND AreaZ = " + area.areaZ
        );
        if (rsCheck.next()) {
            System.out.println("[LandClaim] Area at (" + area.areaX + ", " + area.areaY + ", " + area.areaZ + ") already claimed, skipping.");
            return false;
        }

        String uid = area.playerUID;
        ResultSet rsStats = db.executeQuery("SELECT AreaCount, MaxAreaAllocation FROM `PlayerAreaStats` WHERE PlayerUID = '" + uid + "'");
        int areaCount = 0;
        int maxAreaAllocation = 2;
        boolean isNewPlayer = !rsStats.next();
        if (!isNewPlayer) {
            areaCount = rsStats.getInt("AreaCount");
            maxAreaAllocation = rsStats.getInt("MaxAreaAllocation");
        }
        if (areaCount >= maxAreaAllocation) {
            System.out.println("[LandClaim] Claim denied for UID: " + uid + " - AreaCount (" + areaCount + ") >= MaxAreaAllocation (" + maxAreaAllocation + ")");
            return false;
        }

        String sql = "INSERT INTO `Areas` (AreaOwnerName, AreaName, AreaX, AreaY, AreaZ, PlayerUID, AreaLocked, PVPStatus, CreationDate) " +
                     "VALUES ('" + escapeSql(area.areaOwnerName) + "', '" + escapeSql(area.areaName) + "', " +
                     area.areaX + ", " + area.areaY + ", " + area.areaZ + ", '" + uid + "', 0, 0, CURRENT_TIMESTAMP)";
        db.executeUpdate(sql);

        ResultSet rsArea = db.executeQuery(
            "SELECT ID FROM `Areas` WHERE AreaX = " + area.areaX + " AND AreaY = " + area.areaY + " AND AreaZ = " + area.areaZ
        );
        if (rsArea.next()) {
            int areaId = rsArea.getInt("ID");
            String insertPermissionsSql = "INSERT INTO `GuestEventActions` (AreaID, " +
                "PlayerDoorObjectStatus, PlayerDestroyTerrain, PlayerHitTerrain, PlayerBedUseObject, " +
                "PlayerCampfireUseObject, PlayerStorageObjectStatus, PlayerTreeDestroyVegetation, " +
                "PlayerPrivateStatus, PlayerTrunkDestroyVegetation, PlayerTreeHitVegetation, " +
                "PlayerTrunkHitVegetation, PlayerTreeRemoveVegetation, PlayerTrunkRemoveVegetation, " +
                "PlayerCropRemoveVegetation, PlayerFruitTreeRemoveVegetation, PlayerPlantRemoveVegetation, " +
                "PlayerRockDestroyVegetation, PlayerRockHitVegetation, PlayerRockRemoveVegetation, " +
                "PlayerAdminRights, PlayerClockUseObject, PlayerDoorUseObject, PlayerDryingRackUseObject, " +
                "PlayerFireUseObject, PlayerFurnaceUseObject, PlayerGrillUseObject, PlayerGrinderUseObject, " +
                "PlayerGrindstoneUseObject, PlayerLadderUseObject, PlayerLampUseObject, " +
                "PlayerMusicPlayerUseObject, PlayerOvenUseObject, PlayerPaperPressUseObject, " +
                "PlayerPianoUseObject, PlayerPosterUseObject, PlayerScaffoldingUseObject, " +
                "PlayerSeatingUseObject, PlayerShootingTargetUseObject, PlayerSignUseObject, " +
                "PlayerSpinningWheelUseObject, PlayerStorageUseObject, PlayerTanningRackUseObject, " +
                "PlayerTechnicalUseObject, PlayerTorchUseObject, PlayerTrashcanUseObject, " +
                "PlayerWorkbenchUseObject, PlayerHitAnimalNPC, PlayerHitHumanNPC, PlayerHitMountNPC, " +
                "PlayerRideMountNPC, PlayerPlaceBluePrints, PlayerNpcAddSaddle, PlayerNpcRemoveSaddle, " +
                "PlayerNpcAddSaddleBag, PlayerNpcRemoveSaddleBag, PlayerNpcAddClothes, " +
                "PlayerNpcRemoveClothes, PlayerChangeConstructionColor, PlayerChangeObjectColor, " +
                "PlayerChangeObjectInfo, PlayerCreativePlaceVegetation, PlayerCreativeRemoveConstruction, " +
                "PlayerCreativeRemoveObject, PlayerCreativeRemoveVegetation, PlayerCreativeTerrainEdit, " +
                "PlayerDestroyConstruction, PlayerDestroyObject, PlayerEditConstruction, " +
                "PlayerHitConstruction, PlayerHitObject, PlayerHitVegetation, PlayerHitWater, " +
                "PlayerPlaceConstruction, PlayerPlaceGrass, PlayerPlaceObject, PlayerPlaceTerrain, " +
                "PlayerPlaceVegetation, PlayerRemoveConstruction, PlayerRemoveGrass, PlayerRemoveObject, " +
                "PlayerRemoveVegetation, PlayerRemoveWater, PlayerWorldEdit) " +
                "VALUES (" + areaId + ", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, " +
                "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, " +
                "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, " +
                "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)";
            db.executeUpdate(insertPermissionsSql);
            System.out.println("[LandClaim] Added GuestEventActions row for AreaID: " + areaId);
        } else {
            System.out.println("[LandClaim] Failed to retrieve AreaID for (" + area.areaX + ", " + area.areaY + ", " + area.areaZ + ")");
            return false;
        }

        areaCount += 1;
        if (isNewPlayer) {
            db.executeUpdate("INSERT INTO `PlayerAreaStats` (PlayerUID, AreaCount, MaxAreaAllocation) " +
                             "VALUES ('" + uid + "', " + areaCount + ", 2)");
            System.out.println("[LandClaim] Inserted new PlayerAreaStats for UID: " + uid + " - AreaCount: " + areaCount + ", MaxAreaAllocation: 2");
        } else {
            db.executeUpdate("UPDATE `PlayerAreaStats` SET AreaCount = " + areaCount + " WHERE PlayerUID = '" + uid + "'");
            System.out.println("[LandClaim] Updated PlayerAreaStats for UID: " + uid + " - AreaCount: " + areaCount + ", MaxAreaAllocation: " + maxAreaAllocation);
        }
        return true;
    }

    public void removeClaimedArea(int areaX, int areaY, int areaZ, String playerUID) throws SQLException {
        ResultSet rs = db.executeQuery(
            "SELECT ID FROM `Areas` WHERE AreaX = " + areaX + " AND AreaY = " + areaY + " AND AreaZ = " + areaZ + " AND PlayerUID = '" + playerUID + "'"
        );
        if (rs.next()) {
            int areaId = rs.getInt("ID");
            db.executeUpdate("DELETE FROM `GuestEventActions` WHERE AreaID = " + areaId);
            System.out.println("[LandClaim] Removed GuestEventActions row for AreaID: " + areaId);

            db.executeUpdate("DELETE FROM `Guests` WHERE AreaID = " + areaId);
            System.out.println("[LandClaim] Removed Guests rows for AreaID: " + areaId);

            db.executeUpdate("DELETE FROM `Areas` WHERE ID = " + areaId);
            System.out.println("[LandClaim] Removed area with AreaID: " + areaId);

            ResultSet rsStats = db.executeQuery("SELECT AreaCount FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'");
            if (rsStats.next()) {
                int areaCount = rsStats.getInt("AreaCount") - 1;
                if (areaCount > 0) {
                    db.executeUpdate("UPDATE `PlayerAreaStats` SET AreaCount = " + areaCount + " WHERE PlayerUID = '" + playerUID + "'");
                } else {
                    db.executeUpdate("DELETE FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'");
                }
                System.out.println("[LandClaim] Updated AreaCount for PlayerUID: " + playerUID + " to " + areaCount);
            }
        } else {
            System.out.println("[LandClaim] No area found to unclaim at (" + areaX + ", " + areaY + ", " + areaZ + ") for PlayerUID: " + playerUID);
        }
    }

    public ResultSet getAllClaimedAreas() throws SQLException {
        return db.executeQuery("SELECT PlayerUID, AreaOwnerName, AreaName, AreaX, AreaY, AreaZ FROM `Areas`");
    }

    public int getPlayerAreaCount(String playerUID) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT AreaCount FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'");
        return rs.next() ? rs.getInt("AreaCount") : 0;
    }

    public int getMaxAreaAllocation(String playerUID) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT MaxAreaAllocation FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'");
        return rs.next() ? rs.getInt("MaxAreaAllocation") : 2;
    }

    public void setMaxAreaAllocation(String playerUID, int maxAreas) throws SQLException {
        db.executeUpdate("INSERT OR REPLACE INTO `PlayerAreaStats` (PlayerUID, AreaCount, MaxAreaAllocation) " +
                         "VALUES ('" + playerUID + "', (SELECT AreaCount FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'), " + maxAreas + ")");
        System.out.println("[LandClaim] Set MaxAreaAllocation for PlayerUID: " + playerUID + " to " + maxAreas);
    }

    public void setGuestPermission(int areaId, String eventName, boolean value) throws SQLException {
        String sql = "UPDATE `GuestEventActions` SET `" + eventName + "` = " + (value ? 1 : 0) + " WHERE AreaID = " + areaId;
        db.executeUpdate(sql);
    }

    public boolean getGuestPermission(int areaId, String eventName) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT `" + eventName + "` FROM `GuestEventActions` WHERE AreaID = " + areaId);
        return rs.next() && rs.getBoolean(eventName);
    }

    public int getAreaIdFromCoords(int x, int y, int z) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT ID FROM `Areas` WHERE AreaX = " + x + " AND AreaY = " + y + " AND AreaZ = " + z);
        return rs.next() ? rs.getInt("ID") : -1;
    }

    private String escapeSql(String input) {
        if (input == null) return "";
        return input.replace("'", "''");
    }
}