package landclaim;

import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.database.Database;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
            System.out.println("[LandClaim] Could not connect to WorldProtection database.");
        }

        db = plugin.getSQLiteConnection(plugin.getPath() + "/" + worldName + "/database.db");

        createTables();
        initializeAdminSettings();
    }

    private void createTables() {
        String[] tableScripts = {
            "CREATE TABLE IF NOT EXISTS `Areas` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `AreaOwnerName` VARCHAR(64), `AreaName` VARCHAR(64), `AreaX` INTEGER, `AreaY` INTEGER, `AreaZ` INTEGER, `PlayerUID` BIGINT, `AreaLocked` BOOLEAN DEFAULT 0, `PVPStatus` BOOLEAN DEFAULT 0, `CreationDate` DATE);",
            "CREATE TABLE IF NOT EXISTS `Guests` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `AreaID` INTEGER, `GuestName` VARCHAR(64), `PlayerUID` BIGINT, FOREIGN KEY (AreaID) REFERENCES Areas(ID));",
            "CREATE TABLE IF NOT EXISTS `Points` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `UserName` VARCHAR(64), `Points` INTEGER DEFAULT 0, `PlayerUID` BIGINT, `TotalPlaytimeHours` DOUBLE DEFAULT 0.0);",
            "CREATE TABLE IF NOT EXISTS `AdminSettings` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `PointsEarnedAdjust` INTEGER, `AreaCostAdjust` INTEGER);",
            "CREATE TABLE IF NOT EXISTS `DataBaseVersion` (`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `Version` INTEGER);",
            "CREATE TABLE IF NOT EXISTS `PlayerAreaStats` (`PlayerUID` BIGINT PRIMARY KEY NOT NULL, `AreaCount` INTEGER DEFAULT 0, `MaxAreaAllocation` INTEGER DEFAULT 2);",
            "CREATE TABLE IF NOT EXISTS `GuestEventActions` (" +
                "`AreaID` INTEGER NOT NULL, " +
                String.join(", ", getGuestPermissions().stream().map(p -> "`" + p + "` BOOLEAN DEFAULT 1").toArray(String[]::new)) +
                ", FOREIGN KEY (AreaID) REFERENCES Areas(ID));"
        };

        for (String script : tableScripts) {
            db.execute(script);
        }

        try (ResultSet result = db.executeQuery("SELECT * FROM `DataBaseVersion` WHERE ID = '1'")) {
            if (!result.next()) {
                db.executeUpdate("INSERT INTO `DataBaseVersion` (Version) VALUES ('0');");
            }
        } catch (SQLException e) {
            System.out.println("[LandClaim] Error initializing DataBaseVersion: " + e.getMessage());
        }
    }

    private void initializeAdminSettings() {
        try {
            ResultSet rs = db.executeQuery("SELECT * FROM `AdminSettings` WHERE ID = 1");
            if (!rs.next()) {
                db.executeUpdate("INSERT INTO `AdminSettings` (ID, PointsEarnedAdjust, AreaCostAdjust) VALUES (1, 1, 1)");
                System.out.println("[LandClaim] Initialized AdminSettings with default values: PointsEarnedAdjust=1, AreaCostAdjust=1");
            }
        } catch (SQLException e) {
            System.out.println("[LandClaim] Error initializing AdminSettings: " + e.getMessage());
        }
    }

    public void migrateAreasFromWorldProtection(Player player) {
        UILabel feedbackPanel = (UILabel) player.getAttribute("FeedBackinfoPanel");
        if (feedbackPanel != null) {
            feedbackPanel.setText("Migration of Areas Started");
        }

        if (oldDataBase == null) {
            System.out.println("[LandClaim] WorldProtection database not initialized.");
            if (feedbackPanel != null) {
                feedbackPanel.setText("[LandClaim] WorldProtection database not initialized!!!");
            }
            return;
        }

        Map<String, Integer> migratedAreas = new HashMap<>();
        int migratedGuests = 0;

        try {
            ResultSet columns = oldDataBase.executeQuery("PRAGMA table_info(`Areas`)");
            boolean hasCreationDate = false, hasAreaLocked = false, hasPVPStatus = false, hasAreaGuest = false;
            while (columns.next()) {
                String columnName = columns.getString("name");
                if ("CreationDate".equals(columnName)) hasCreationDate = true;
                if ("AreaLocked".equals(columnName)) hasAreaLocked = true;
                if ("PVPStatus".equals(columnName)) hasPVPStatus = true;
                if ("AreaGuest".equals(columnName)) hasAreaGuest = true;
            }

            String selectSql = "SELECT ID, AreaOwnerName, AreaName, AreaX, AreaY, AreaZ, PlayerUID" +
                (hasCreationDate ? ", CreationDate" : "") +
                (hasAreaLocked ? ", AreaLocked" : "") +
                (hasPVPStatus ? ", PVPStatus" : "") +
                (hasAreaGuest ? ", AreaGuest" : "") +
                " FROM `Areas`";

            ResultSet areasRs = oldDataBase.executeQuery(selectSql);
            while (areasRs.next()) {
                int id = areasRs.getInt("ID");
                String areaOwnerName = areasRs.getString("AreaOwnerName");
                String areaName = areasRs.getString("AreaName");
                int areaX = areasRs.getInt("AreaX");
                int areaY = areasRs.getInt("AreaY");
                int areaZ = areasRs.getInt("AreaZ");
                long playerUID = areasRs.getLong("PlayerUID");
                String areaGuest = hasAreaGuest ? areasRs.getString("AreaGuest") : null;

                String areaKey = areaX + "," + areaY + "," + areaZ;
                if (!migratedAreas.containsKey(areaKey)) {
                    ResultSet existing = db.executeQuery("SELECT ID FROM `Areas` WHERE AreaX = " + areaX + " AND AreaY = " + areaY + " AND AreaZ = " + areaZ);
                    if (!existing.next()) {
                        String insertSql = "INSERT INTO `Areas` (ID, AreaOwnerName, AreaName, AreaX, AreaY, AreaZ, PlayerUID, AreaLocked, PVPStatus, CreationDate) " +
                            "VALUES (" + id + ", '" + escapeSql(areaOwnerName) + "', '" + escapeSql(areaName) + "', " +
                            areaX + ", " + areaY + ", " + areaZ + ", " + playerUID + ", 0, 0, NULL)";
                        db.executeUpdate(insertSql);

                        insertDefaultGuestPermissions(id);
                    }
                    migratedAreas.put(areaKey, id);
                }

                if (hasAreaGuest && areaGuest != null && !areaGuest.trim().equalsIgnoreCase("AreaGuest")) {
                    try {
                        long guestUID = Long.parseLong(areaGuest.trim());
                        if (guestUID != 0 && guestUID != playerUID) {
                            int areaId = migratedAreas.get(areaKey);
                            ResultSet existingGuest = db.executeQuery("SELECT ID FROM `Guests` WHERE AreaID = " + areaId + " AND PlayerUID = " + guestUID);
                            if (!existingGuest.next()) {
                                db.executeUpdate("INSERT INTO `Guests` (AreaID, PlayerUID) VALUES (" + areaId + ", " + guestUID + ")");
                                migratedGuests++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("[LandClaim] Skipping invalid AreaGuest: " + areaGuest);
                    }
                }

                String updateSql = buildUpdateSql(areasRs, migratedAreas.get(areaKey), hasCreationDate, hasAreaLocked, hasPVPStatus);
                if (updateSql != null) {
                    db.executeUpdate(updateSql);
                }
            }

            System.out.println("[LandClaim] Migration completed. Areas: " + migratedAreas.size() + ", Guests: " + migratedGuests);
            if (feedbackPanel != null) {
                feedbackPanel.setText("Migration of Areas from WorldProtection database completed");
            }
        } catch (SQLException e) {
            System.out.println("[LandClaim] Migration failed: " + e.getMessage());
            if (feedbackPanel != null) {
                feedbackPanel.setText("Areas and Guests migration failed!!!");
            }
            e.printStackTrace();
        }
    }

    private void insertDefaultGuestPermissions(int areaId) throws SQLException {
        StringBuilder insertSql = new StringBuilder("INSERT INTO `GuestEventActions` (AreaID, ");
        StringBuilder valuesSql = new StringBuilder("VALUES (" + areaId + ", ");
        java.util.List<String> permissions = getGuestPermissions();
        for (int i = 0; i < permissions.size(); i++) {
            insertSql.append("`").append(permissions.get(i)).append("`");
            valuesSql.append("1");
            if (i < permissions.size() - 1) {
                insertSql.append(", ");
                valuesSql.append(", ");
            }
        }
        insertSql.append(") ").append(valuesSql).append(")");
        db.executeUpdate(insertSql.toString());
    }

    private String buildUpdateSql(ResultSet rs, int areaId, boolean hasCreationDate, boolean hasAreaLocked, boolean hasPVPStatus) throws SQLException {
        StringBuilder updateSql = new StringBuilder("UPDATE `Areas` SET ");
        boolean hasUpdates = false;

        if (hasCreationDate) {
            String creationDate = rs.getString("CreationDate");
            if (creationDate != null) {
                updateSql.append("CreationDate = '").append(creationDate).append("'");
                hasUpdates = true;
            }
        }
        if (hasAreaLocked) {
            if (hasUpdates) updateSql.append(", ");
            updateSql.append("AreaLocked = ").append(rs.getInt("AreaLocked") != 0 ? 1 : 0);
            hasUpdates = true;
        }
        if (hasPVPStatus) {
            if (hasUpdates) updateSql.append(", ");
            updateSql.append("PVPStatus = ").append(rs.getInt("PVPStatus") != 0 ? 1 : 0);
            hasUpdates = true;
        }

        return hasUpdates ? updateSql.append(" WHERE ID = ").append(areaId).toString() : null;
    }

    public void close() {
        if (db != null) db.close();
        if (oldDataBase != null) oldDataBase.close();
    }

    public Database getDb() {
        return db;
    }

    public boolean addClaimedArea(LandClaim.ClaimedArea area) throws SQLException {
        ResultSet rsCheck = db.executeQuery(
            "SELECT ID FROM `Areas` WHERE AreaX = " + area.areaX + " AND AreaY = " + area.areaY + " AND AreaZ = " + area.areaZ
        );
        if (rsCheck.next()) {
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
            insertDefaultGuestPermissions(areaId);

            areaCount++;
            String statsSql = isNewPlayer ?
                "INSERT INTO `PlayerAreaStats` (PlayerUID, AreaCount, MaxAreaAllocation) VALUES ('" + uid + "', " + areaCount + ", 2)" :
                "UPDATE `PlayerAreaStats` SET AreaCount = " + areaCount + " WHERE PlayerUID = '" + uid + "'";
            db.executeUpdate(statsSql);
            return true;
        }
        return false;
    }

    public void removeClaimedArea(int areaX, int areaY, int areaZ, String playerUID) throws SQLException {
        ResultSet rs = db.executeQuery(
            "SELECT ID FROM `Areas` WHERE AreaX = " + areaX + " AND AreaY = " + areaY + " AND AreaZ = " + areaZ + " AND PlayerUID = '" + playerUID + "'"
        );
        if (rs.next()) {
            int areaId = rs.getInt("ID");
            db.executeUpdate("DELETE FROM `GuestEventActions` WHERE AreaID = " + areaId);
            db.executeUpdate("DELETE FROM `Guests` WHERE AreaID = " + areaId);
            db.executeUpdate("DELETE FROM `Areas` WHERE ID = " + areaId);

            ResultSet rsStats = db.executeQuery("SELECT AreaCount FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'");
            if (rsStats.next()) {
                int areaCount = rsStats.getInt("AreaCount") - 1;
                if (areaCount > 0) {
                    db.executeUpdate("UPDATE `PlayerAreaStats` SET AreaCount = " + areaCount + " WHERE PlayerUID = '" + playerUID + "'");
                } else {
                    db.executeUpdate("DELETE FROM `PlayerAreaStats` WHERE PlayerUID = '" + playerUID + "'");
                }
            }
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
    }

    public void setGuestPermission(int areaId, String eventName, boolean value) throws SQLException {
        db.executeUpdate("UPDATE `GuestEventActions` SET `" + eventName + "` = " + (value ? 1 : 0) + " WHERE AreaID = " + areaId);
    }

    public boolean getGuestPermission(int areaId, String eventName) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT `" + eventName + "` FROM `GuestEventActions` WHERE AreaID = " + areaId);
        return rs.next() && rs.getBoolean(eventName);
    }

    public int getAreaIdFromCoords(int x, int y, int z) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT ID FROM `Areas` WHERE AreaX = " + x + " AND AreaY = " + y + " AND AreaZ = " + z);
        return rs.next() ? rs.getInt("ID") : -1;
    }

    public void addPoints(String playerUID, int points) throws SQLException {
        String sql = "UPDATE `Points` SET Points = Points + " + points + " WHERE PlayerUID = '" + escapeSql(playerUID) + "'";
        db.executeUpdate(sql);
    }

    public int getPoints(String playerUID) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT Points FROM `Points` WHERE PlayerUID = '" + escapeSql(playerUID) + "'");
        return rs.next() ? rs.getInt("Points") : 0;
    }

    public void deductPoints(String playerUID, int points) throws SQLException {
        int currentPoints = getPoints(playerUID);
        if (currentPoints < points) {
            throw new SQLException("Points deduction failed: insufficient points! Need " + points + ", have " + currentPoints);
        }
        String sql = "UPDATE `Points` SET Points = Points - " + points + " WHERE PlayerUID = '" + escapeSql(playerUID) + "'";
        db.executeUpdate(sql);
        int newPoints = getPoints(playerUID);
        if (newPoints != currentPoints - points) {
            throw new SQLException("Points deduction failed: update did not apply correctly!");
        }
    }

    public boolean buyAreaAllocation(String playerUID, int areaCost) throws SQLException {
        int currentPoints = getPoints(playerUID);
        if (currentPoints < areaCost) {
            return false;
        }
        deductPoints(playerUID, areaCost);
        int currentMax = getMaxAreaAllocation(playerUID);
        setMaxAreaAllocation(playerUID, currentMax + 1);
        System.out.println("[LandClaim] PlayerUID: " + playerUID + " bought 1 area allocation for " + areaCost + " points. New MaxAreaAllocation: " + (currentMax + 1));
        return true;
    }

    public void updatePlaytimeAndPoints(String playerUID, double sessionHours) throws SQLException {
        if (sessionHours < 1.0 / 3600.0) {
            System.out.println("[LandClaim] Session time too short for PlayerUID: " + playerUID + " (" + sessionHours + " hours), skipping update.");
            return;
        }

        System.out.println("[LandClaim] Updating playtime for PlayerUID: " + playerUID + ", Session Hours: " + sessionHours);
        ResultSet rsBefore = db.executeQuery("SELECT TotalPlaytimeHours, Points FROM `Points` WHERE PlayerUID = '" + escapeSql(playerUID) + "'");
        double previousHours = 0.0;
        int previousPoints = 0;
        boolean playerExists = rsBefore.next();
        if (!playerExists) {
            System.out.println("[LandClaim] No Points row found for PlayerUID: " + playerUID + ", initializing...");
            db.executeUpdate("INSERT INTO `Points` (PlayerUID, UserName, Points, TotalPlaytimeHours) " +
                             "VALUES ('" + escapeSql(playerUID) + "', 'Unknown', 0, 0.0)");
            previousHours = 0.0;
            previousPoints = 0;
        } else {
            previousHours = rsBefore.getDouble("TotalPlaytimeHours");
            previousPoints = rsBefore.getInt("Points");
        }
        System.out.println("[LandClaim] Before update - PlayerUID: " + playerUID + ", TotalPlaytimeHours: " + previousHours + ", Points: " + previousPoints);

        String sql = "UPDATE `Points` SET TotalPlaytimeHours = TotalPlaytimeHours + " + sessionHours +
                     " WHERE PlayerUID = '" + escapeSql(playerUID) + "'";
        db.executeUpdate(sql);
        System.out.println("[LandClaim] Playtime update executed for PlayerUID: " + playerUID);

        int pointsEarnedAdjust = getPointsEarnedAdjust();
        double newTotalHours = previousHours + sessionHours;
        int newPoints = (int) Math.floor(newTotalHours * pointsEarnedAdjust);
        int pointsToAdd = newPoints - previousPoints;
        if (pointsToAdd > 0) {
            System.out.println("[LandClaim] Adding " + pointsToAdd + " points to PlayerUID: " + playerUID + " (Total Hours: " + newTotalHours + ", PointsEarnedAdjust: " + pointsEarnedAdjust + ")");
            addPoints(playerUID, pointsToAdd);
        } else {
            System.out.println("[LandClaim] No new points to add for PlayerUID: " + playerUID + " (Total Hours: " + newTotalHours + ", Current Points: " + previousPoints + ")");
        }
    }

    public double getTotalPlaytimeHours(String playerUID) throws SQLException {
        ResultSet rs = db.executeQuery("SELECT TotalPlaytimeHours FROM `Points` WHERE PlayerUID = '" + escapeSql(playerUID) + "'");
        return rs.next() ? rs.getDouble("TotalPlaytimeHours") : 0.0;
    }

    public int getPointsEarnedAdjust() throws SQLException {
        ResultSet rs = db.executeQuery("SELECT PointsEarnedAdjust FROM `AdminSettings` WHERE ID = 1");
        return rs.next() ? rs.getInt("PointsEarnedAdjust") : 1;
    }

    public void setPointsEarnedAdjust(int value) throws SQLException {
        if (value < 0) value = 0;
        db.executeUpdate("UPDATE `AdminSettings` SET PointsEarnedAdjust = " + value + " WHERE ID = 1");
        System.out.println("[LandClaim] PointsEarnedAdjust set to " + value);
    }

    public int getAreaCostAdjust() throws SQLException {
        ResultSet rs = db.executeQuery("SELECT AreaCostAdjust FROM `AdminSettings` WHERE ID = 1");
        return rs.next() ? Math.max(rs.getInt("AreaCostAdjust"), 1) : 1;
    }

    public void setAreaCostAdjust(int value) throws SQLException {
        if (value < 1) value = 1;
        db.executeUpdate("UPDATE `AdminSettings` SET AreaCostAdjust = " + value + " WHERE ID = 1");
        System.out.println("[LandClaim] AreaCostAdjust set to " + value);
    }

    private String escapeSql(String input) {
        return input == null ? "" : input.replace("'", "''");
    }

    private java.util.List<String> getGuestPermissions() {
        return java.util.Arrays.asList(
            "PlayerDoorObjectStatus", "PlayerDestroyTerrain", "PlayerHitTerrain", "PlayerBedUseObject",
            "PlayerCampfireUseObject", "PlayerStorageObjectStatus", "PlayerTreeDestroyVegetation",
            "PlayerPrivateStatus", "PlayerTrunkDestroyVegetation", "PlayerTreeHitVegetation",
            "PlayerTrunkHitVegetation", "PlayerTreeRemoveVegetation", "PlayerTrunkRemoveVegetation",
            "PlayerCropRemoveVegetation", "PlayerFruitTreeRemoveVegetation", "PlayerPlantRemoveVegetation",
            "PlayerRockDestroyVegetation", "PlayerRockHitVegetation", "PlayerRockRemoveVegetation",
            "PlayerAdminRights", "PlayerClockUseObject", "PlayerDoorUseObject", "PlayerDryingRackUseObject",
            "PlayerFireUseObject", "PlayerFurnaceUseObject", "PlayerGrillUseObject", "PlayerGrinderUseObject",
            "PlayerGrindstoneUseObject", "PlayerLadderUseObject", "PlayerLampUseObject",
            "PlayerMusicPlayerUseObject", "PlayerOvenUseObject", "PlayerPaperPressUseObject",
            "PlayerPianoUseObject", "PlayerPosterUseObject", "PlayerScaffoldingUseObject",
            "PlayerSeatingUseObject", "PlayerShootingTargetUseObject", "PlayerSignUseObject",
            "PlayerSpinningWheelUseObject", "PlayerStorageUseObject", "PlayerTanningRackUseObject",
            "PlayerTechnicalUseObject", "PlayerTorchUseObject", "PlayerTrashcanUseObject",
            "PlayerWorkbenchUseObject", "PlayerHitAnimalNPC", "PlayerHitHumanNPC", "PlayerHitMountNPC",
            "PlayerRideMountNPC", "PlayerPlaceBluePrints", "PlayerNpcAddSaddle", "PlayerNpcRemoveSaddle",
            "PlayerNpcAddSaddleBag", "PlayerNpcRemoveSaddleBag", "PlayerNpcAddClothes",
            "PlayerNpcRemoveClothes", "PlayerChangeConstructionColor", "PlayerChangeObjectColor",
            "PlayerChangeObjectInfo", "PlayerCreativePlaceVegetation", "PlayerCreativeRemoveConstruction",
            "PlayerCreativeRemoveObject", "PlayerCreativeRemoveVegetation", "PlayerCreativeTerrainEdit",
            "PlayerDestroyConstruction", "PlayerDestroyObject", "PlayerEditConstruction",
            "PlayerHitConstruction", "PlayerHitObject", "PlayerHitVegetation", "PlayerHitWater",
            "PlayerPlaceConstruction", "PlayerPlaceGrass", "PlayerPlaceObject", "PlayerPlaceTerrain",
            "PlayerPlaceVegetation", "PlayerRemoveConstruction", "PlayerRemoveGrass", "PlayerRemoveObject",
            "PlayerRemoveVegetation", "PlayerRemoveWater", "PlayerWorldEdit"
        );
    }
}