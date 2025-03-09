package landclaim;

import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerKeyEvent;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.utils.Vector3f;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.utils.Key;
import net.risingworld.api.objects.Area;
import net.risingworld.api.worldelements.Area3D;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LandClaim extends Plugin implements Listener {
    private LandClaimDatabase database;
    private AdminTools adminTools;
    public AdminUIMenu adminUIMenu;
    private AdminOnClickButtons adminClicks;
    private PlayerOnClickButtons clickHandler;
    
    public HashMap<Player, PlayerUIMenu> playerMenus = new HashMap<>();
    private HashMap<Player, PlayerTools> playerTools = new HashMap<>();
    private HashMap<Player, Vector3i> playerChunks = new HashMap<>();
    HashMap<String, ClaimedArea> claimedAreas = new HashMap<>();
    protected static ArrayList<Area3D> LandClaimedAreas = new ArrayList<>();

    private Timer timer = new Timer();

    public HashMap<Player, PlayerTools> getPlayerTools() {
        return playerTools;
    }

    @Override
    public void onEnable() {
        String worldName = World.getName();
        System.out.println("-- LandClaim PLUGIN ENABLED --");
        database = new LandClaimDatabase(this);
        database.initialize();
        adminTools = new AdminTools(this);
        adminUIMenu = new AdminUIMenu(this, adminTools);
        adminClicks = new AdminOnClickButtons(this, adminTools);
        
        registerEventListener(this);
        OwnerEventHandler ownerHandler = new OwnerEventHandler(this);
        registerEventListener(ownerHandler);
        
        clickHandler = new PlayerOnClickButtons(this);
        clickHandler.register();
        
        LandClaimedAreas.clear();
        System.out.println("Cleared LandClaimedAreas list on plugin enable.");
        try {
            loadClaimedAreas();
        } catch (SQLException ex) {
            Logger.getLogger(LandClaim.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            loadAreaVisuals(null);
        } catch (SQLException ex) {
            Logger.getLogger(LandClaim.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Loaded " + LandClaimedAreas.size() + " area visuals from LandClaim.");
    }

    @Override
    public void onDisable() {
        System.out.println("-- LandClaim PLUGIN DISABLED --");
        if (database != null) {
            database.close();
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    @EventMethod
    public void onPlayerConnect(PlayerConnectEvent event) throws SQLException {
        Player player = event.getPlayer();
        
        player.registerKeys(Key.L, Key.LeftShift);
        
        PlayerUIMenu menu = playerMenus.get(player);
        if (menu == null) {
            menu = new PlayerUIMenu(player, this);
            playerMenus.put(player, menu);
        }
        
        PlayerTools tools = new PlayerTools(player);
        tools.initTools(menu);
        playerTools.put(player, tools);
        
        Vector3i initialChunk = player.getChunkPosition();
        playerChunks.put(player, initialChunk);
        System.out.println("Player " + player.getName() + " connected at chunk: " + initialChunk.x + ", " + initialChunk.y + ", " + initialChunk.z);
        
        if (adminTools.isAdmin(player)) {
            adminUIMenu.showAdminMenu(player);
        }
        
        String uid = player.getUID();
        int areaCount = getPlayerAreaCountFromDB(uid);
        if (areaCount == 0) {
            database.setMaxAreaAllocation(uid, 2);
        }
        
        addAreaInfoLabel(player);
        updateAreaInfoLabel(player, initialChunk);
        showMyAreas(true, player);
        showAllAreas(true, player);
        for (int i = 0; i < 10; i++) {
            final int delay = (i + 1) * 500;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    showMyAreas(true, player);
                    System.out.println("[LandClaim] Reapplied showMyAreas for " + player.getName() + " at " + System.currentTimeMillis() + " (delay " + delay + "ms)");
                }
            }, delay);
        }
        scheduleHideAllAreas(player, 60000);
    }

    @EventMethod
    public void onPlayerKey(PlayerKeyEvent event) {
        Player player = event.getPlayer();
        if (event.isPressed() && event.getKey() == Key.L && player.isKeyPressed(Key.LeftShift)) {
            PlayerUIMenu menu = playerMenus.get(player);
            if (menu != null) {
                menu.toggleMenu();
            }
        }
    }

    @EventMethod
    public void onPlayerEnterChunk(PlayerEnterChunkEvent event) {
        Player player = event.getPlayer();
        Vector3i chunk = event.getNewChunkCoordinates();
        playerChunks.put(player, chunk);
        System.out.println("Player " + player.getName() + " entered chunk: " + chunk.x + ", " + chunk.y + ", " + chunk.z);
        updateAreaInfoLabel(player, chunk);
    }

    @EventMethod
    public void onPlayerUIElementClick(PlayerUIElementClickEvent event) throws SQLException {
        adminClicks.onPlayerUIElementClick(event);
    }

    @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) {
        Player player = event.getPlayer();
        if (!adminTools.isAdmin(player)) return;

        String[] args = event.getCommand().split(" ");
        if (args[0].equalsIgnoreCase("/showareas")) {
            boolean show = args.length > 1 ? Boolean.parseBoolean(args[1]) : true;
            showAreas(show, player);
            player.sendTextMessage("Area visuals " + (show ? "shown" : "hidden") + " for you.");
            event.setCancelled(true);
        }
    }

    public LandClaimDatabase getDatabase() {
        return database;
    }

    public void startClaimMode(Player player) throws SQLException {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            player.sendTextMessage("Error: No chunk data available! Try moving a bit to update your position.");
            System.out.println("No chunk data for " + player.getName() + " - playerChunks size: " + playerChunks.size());
            return;
        }

        int areaX = chunk.x;
        int areaY = chunk.y;
        int areaZ = chunk.z;

        String key = getKeyFromCoords(areaX, areaY, areaZ);
        ClaimedArea existingArea = claimedAreas.get(key);
        if (existingArea != null) {
            player.sendTextMessage("This area is already claimed as " + existingArea.areaName + "!");
            return;
        }

        String uid = player.getUID();
        int areaCount = getPlayerAreaCountFromDB(uid);
        int maxAreas = database.getMaxAreaAllocation(uid);
        if (areaCount >= maxAreas) {
            player.sendTextMessage("Sorry, you cannot claim more areas. You have reached your limit of " + maxAreas + "! Ask an admin to increase it via the admin panel.");
            return;
        }

        String areaName = areaCount == 0 ? player.getName() + "'s Home" : player.getName() + "'s Area=" + (areaCount + 1);
        ClaimedArea newArea = new ClaimedArea(uid, player.getName(), areaName, areaX, areaY, areaZ);
        if (database.addClaimedArea(newArea)) {
            claimedAreas.put(key, newArea);
            addAreaVisual(areaX, areaY, areaZ, areaName, player, true);
            player.sendTextMessage("Area claimed successfully as " + areaName + "!");
            updateAreaInfoLabel(player, chunk);
            showAreas(true, player);
        } else {
            player.sendTextMessage("Failed to claim area - you may have reached your limit or an error occurred.");
        }
    }

    public void unclaimArea(Player player) throws SQLException {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            player.sendYellMessage("Error: No chunk data available! Try moving to update your position.", 3, true);
            return;
        }

        int areaX = chunk.x;
        int areaY = chunk.y;
        int areaZ = chunk.z;

        String key = getKeyFromCoords(areaX, areaY, areaZ);
        ClaimedArea area = claimedAreas.get(key);
        if (area == null) {
            player.sendYellMessage("This area is not claimed!", 3, true);
            return;
        }
        String playerUID = player.getUID();
        if (!area.playerUID.equals(playerUID)) {
            player.sendYellMessage("You can only unclaim your own areas!", 3, true);
            return;
        }

        database.removeClaimedArea(areaX, areaY, areaZ, playerUID);
        claimedAreas.remove(key);
        
        removeAreaVisual(areaX, areaY, areaZ);
        player.sendYellMessage("Area unclaimed successfully!", 3, true);
        updateAreaInfoLabel(player, chunk);
        showAreas(true, player);
    }

    public void loadClaimedAreas() throws SQLException {
        ResultSet rs = database.getAllClaimedAreas();
        HashSet<String> loadedChunks = new HashSet<>();
        while (rs.next()) {
            String playerUID = rs.getString("PlayerUID");
            String areaOwnerName = rs.getString("AreaOwnerName");
            String areaName = rs.getString("AreaName");
            int areaX = rs.getInt("AreaX");
            int areaY = rs.getInt("AreaY");
            int areaZ = rs.getInt("AreaZ");
            String key = getKeyFromCoords(areaX, areaY, areaZ);
            if (!loadedChunks.contains(key)) {
                System.out.println("Loading area: UID=" + playerUID + ", Name=" + areaName + ", Chunk=(" + areaX + "," + areaY + "," + areaZ + ")");
                ClaimedArea area = new ClaimedArea(playerUID, areaOwnerName, areaName, areaX, areaY, areaZ);
                claimedAreas.put(key, area);
                loadedChunks.add(key);
            } else {
                System.out.println("Skipping duplicate area for chunk (" + areaX + "," + areaY + "," + areaZ + "), keeping existing entry");
            }
        }
        System.out.println("Loaded " + claimedAreas.size() + " unique claimed areas from database.");
    }

    private void loadAreaVisuals(Player player) throws SQLException {
        ResultSet rs = database.getAllClaimedAreas();
        HashSet<String> loadedChunks = new HashSet<>();
        while (rs.next()) {
            int areaX = rs.getInt("AreaX");
            int areaY = rs.getInt("AreaY");
            int areaZ = rs.getInt("AreaZ");
            String key = getKeyFromCoords(areaX, areaY, areaZ);
            if (loadedChunks.contains(key)) {
                System.out.println("Skipping duplicate area visual for chunk (" + areaX + "," + areaY + "," + areaZ + ")");
                continue;
            }
            loadedChunks.add(key);
            String areaName = rs.getString("AreaName");
            String playerUID = rs.getString("PlayerUID");
            System.out.println("Attempting to load area visual for " + areaName + " at chunk (" + areaX + "," + areaY + "," + areaZ + ")");
            boolean isPlayerClaimed = playerUID != null && !playerUID.equals("") && (player != null && playerUID.equals(player.getUID()));
            addAreaVisual(areaX, areaY, areaZ, areaName, player, isPlayerClaimed);
            System.out.println("Loaded area visual for " + areaName + " at chunk " + areaX + ", " + areaY + ", " + areaZ);
        }
    }

    private void addAreaVisual(int areaX, int areaY, int areaZ, String areaName, Player player, boolean isPlayerClaimed) {
        Vector3i startChunk = new Vector3i(areaX, areaY, areaZ);
        Vector3i endChunk = new Vector3i(areaX, areaY, areaZ);
        Vector3i startBlock = new Vector3i(0, 0, 0);
        Vector3i endBlock = new Vector3i(32, 64, 32);

        Vector3f startPos = getGlobalPosition(startChunk, startBlock);
        Vector3f endPos = getGlobalPosition(endChunk, endBlock);
        Area area = new Area(startPos, endPos);
        Area3D areaVisual = new Area3D(area);
        areaVisual.setAlwaysVisible(false);
        areaVisual.setFrameVisible(true);

        if (isPlayerClaimed) {
            areaVisual.setColor(0.0f, 0.2f, 0.0f, 0.3f); // Green fill
            areaVisual.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f); // Brighter green frame
        } else {
            areaVisual.setColor(0.0f, 0.0f, 1.0f, 0.3f); // Blue fill
            areaVisual.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f); // Blue frame
        }

        LandClaimedAreas.add(areaVisual);

        if (player != null) {
            player.addGameObject(areaVisual);
            scheduleHideArea(areaVisual, player, 60000);
        }
    }

    private void removeAreaVisual(int areaX, int areaY, int areaZ) {
        LandClaimedAreas.removeIf(visual -> {
            Vector3f start = visual.getArea().getStartPosition();
            Vector3f end = visual.getArea().getEndPosition();
            Vector3i startChunk = getChunkPosition(start);
            Vector3i endChunk = getChunkPosition(end);
            return startChunk.x == areaX && startChunk.y == areaY && startChunk.z == areaZ &&
                   endChunk.x == areaX && endChunk.y == areaY && endChunk.z == areaZ;
        });
    }

    public void showAreas(boolean visible, Player player) {
        System.out.println("[LandClaim] showAreas called: visible=" + visible + " for " + player.getName() + " at " + System.currentTimeMillis());
        if (visible) {
            String playerUID = player.getUID();
            int myCount = 0, otherCount = 0;
            for (Area3D area : LandClaimedAreas) {
                for (ClaimedArea claimedArea : claimedAreas.values()) {
                    if (isAreaVisualForClaimedArea(area, claimedArea)) {
                        if (claimedArea.playerUID.equals(playerUID)) {
                            area.setColor(0.0f, 0.2f, 0.0f, 0.3f); // Green for your areas
                            area.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f);
                            myCount++;
                        } else {
                            area.setColor(0.0f, 0.0f, 1.0f, 0.3f); // Blue for others
                            area.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f);
                            otherCount++;
                        }
                        player.addGameObject(area);
                        break;
                    }
                }
            }
            System.out.println("[LandClaim] showAreas processed " + myCount + " my areas, " + otherCount + " other areas at " + System.currentTimeMillis());
        } else {
            int count = 0;
            for (Area3D area : LandClaimedAreas) {
                player.removeGameObject(area);
                count++;
            }
            System.out.println("[LandClaim] showAreas hidden " + count + " areas at " + System.currentTimeMillis());
        }
    }

    public void showMyAreas(boolean visible, Player player) {
        String playerUID = player.getUID();
        System.out.println("[LandClaim] showMyAreas called: visible=" + visible + " for " + player.getName() + " at " + System.currentTimeMillis());
        int count = 0;
        for (Area3D area : LandClaimedAreas) {
            for (ClaimedArea claimedArea : claimedAreas.values()) {
                if (isAreaVisualForClaimedArea(area, claimedArea) && claimedArea.playerUID.equals(playerUID)) {
                    Vector3i chunk = getChunkPosition(area.getArea().getStartPosition());
                    System.out.println("[LandClaim] Matching area at chunk (" + chunk.x + "," + chunk.y + "," + chunk.z + ") for " + player.getName());
                    if (visible) {
                        area.setColor(0.0f, 0.2f, 0.0f, 0.3f); // Green fill
                        area.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f); // Brighter green frame
                        player.addGameObject(area);
                        count++;
                    } else {
                        player.removeGameObject(area);
                    }
                    break;
                }
            }
        }
        System.out.println("[LandClaim] showMyAreas processed " + count + " areas at " + System.currentTimeMillis());
    }

    public void showAllAreas(boolean visible, Player player) {
        System.out.println("[LandClaim] showAllAreas called: visible=" + visible + " for " + player.getName() + " at " + System.currentTimeMillis());
        int count = 0;
        if (visible) {
            String playerUID = player.getUID();
            int myCount = 0, otherCount = 0;
            for (Area3D area : LandClaimedAreas) {
                for (ClaimedArea claimedArea : claimedAreas.values()) {
                    if (isAreaVisualForClaimedArea(area, claimedArea)) {
                        if (claimedArea.playerUID.equals(playerUID)) {
                            area.setColor(0.0f, 0.2f, 0.0f, 0.3f); // Green fill
                            area.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f); // Brighter green frame
                            myCount++;
                        } else {
                            area.setColor(0.0f, 0.0f, 1.0f, 0.3f); // Blue fill
                            area.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f); // Blue frame
                            otherCount++;
                        }
                        player.addGameObject(area);
                        count++;
                        break;
                    }
                }
            }
            System.out.println("[LandClaim] showAllAreas processed " + myCount + " my areas, " + otherCount + " other areas at " + System.currentTimeMillis());
        } else {
            int hideCount = 0;
            for (Area3D area : LandClaimedAreas) {
                player.removeGameObject(area);
                hideCount++;
            }
            System.out.println("[LandClaim] showAllAreas hidden " + hideCount + " areas at " + System.currentTimeMillis());
        }
    }

    private int getPlayerAreaCountFromDB(String playerUID) throws SQLException {
        ResultSet rs = database.getDb().executeQuery("SELECT COUNT(*) as count FROM `Areas` WHERE PlayerUID = '" + escapeSql(playerUID) + "'");
        return rs.next() ? rs.getInt("count") : 0;
    }

    private String escapeSql(String input) {
        if (input == null) return "";
        return input.replace("'", "''");
    }

    private void addAreaInfoLabel(Player player) {
        UILabel areaLabel = new UILabel();
        areaLabel.setClickable(false);
        areaLabel.setText("Area Unclaimed");
        areaLabel.setFont(Font.Medieval);
        areaLabel.style.textAlign.set(TextAnchor.MiddleCenter);
        areaLabel.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        areaLabel.setFontSize(16);
        areaLabel.setSize(300, 50, false);
        areaLabel.setBorder(2);
        areaLabel.setBorderColor(999);
        areaLabel.setBackgroundColor(500);
        areaLabel.setPosition(1, 90, true);
        player.addUIElement(areaLabel);
        player.setAttribute("areaInfoLabel", areaLabel);
    }

    private void updateAreaInfoLabel(Player player, Vector3i chunk) {
        UILabel areaLabel = (UILabel) player.getAttribute("areaInfoLabel");
        if (areaLabel == null) return;

        int areaX = chunk.x;
        int areaY = chunk.y;
        int areaZ = chunk.z;

        ClaimedArea area = claimedAreas.get(getKeyFromCoords(areaX, areaY, areaZ));
        areaLabel.setText(area != null ? area.areaName : "Area Unclaimed");
    }

    String getKeyFromCoords(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    public String getAreaOwnerUIDFromPosition(Player player) throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        int areaX = chunk.x;
        int areaY = chunk.y;
        int areaZ = chunk.z;

        ResultSet rs = database.getDb().executeQuery(
            "SELECT PlayerUID FROM `Areas` WHERE AreaX = " + areaX + " AND AreaY = " + areaY + " AND AreaZ = " + areaZ
        );
        return rs.next() ? rs.getString("PlayerUID") : null;
    }

    public ClaimedArea getClaimedAreaAt(Vector3i chunkPos) {
        String key = getKeyFromCoords(chunkPos.x, chunkPos.y, chunkPos.z);
        Logger.getLogger(LandClaim.class.getName()).info("Looking up claim with key: " + key + " at chunk (" + chunkPos.x + ", " + chunkPos.y + ", " + chunkPos.z + ")");
        ClaimedArea claim = claimedAreas.get(key);
        if (claim != null) {
            Logger.getLogger(LandClaim.class.getName()).info("Found claim at chunk (" + claim.areaX + ", " + claim.areaY + ", " + claim.areaZ + ")");
        } else {
            Logger.getLogger(LandClaim.class.getName()).info("No claim found at this chunk.");
        }
        return claim;
    }

    private Vector3f getGlobalPosition(Vector3i chunk, Vector3i block) {
        return new Vector3f(
            chunk.x * 32 + block.x,
            chunk.y * 64 + block.y,
            chunk.z * 32 + block.z
        );
    }

    Vector3i getChunkPosition(Vector3f position) {
        return new Vector3i(
            (int) (position.x / 32),
            (int) (position.y / 64),
            (int) (position.z / 32)
        );
    }

    private void scheduleHideArea(Area3D area, Player player, long delayMillis) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                player.removeGameObject(area);
            }
        }, delayMillis);
    }

    private void scheduleHideAllAreas(Player player, long delayMillis) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                showAreas(false, player);
            }
        }, delayMillis);
    }

    private boolean isAreaVisualForClaimedArea(Area3D areaVisual, ClaimedArea claimedArea) {
        Vector3f start = areaVisual.getArea().getStartPosition();
        Vector3i startChunk = getChunkPosition(start);
        return startChunk.x == claimedArea.areaX && startChunk.y == claimedArea.areaY && startChunk.z == claimedArea.areaZ;
    }

    class ClaimedArea {
        String playerUID;
        String areaOwnerName;
        String areaName;
        int areaX;
        int areaY;
        int areaZ;

        ClaimedArea(String playerUID, String areaOwnerName, String areaName, int areaX, int areaY, int z) {
            this.playerUID = playerUID;
            this.areaOwnerName = areaOwnerName;
            this.areaName = areaName;
            this.areaX = areaX;
            this.areaY = areaY;
            this.areaZ = z;
        }
    }
}