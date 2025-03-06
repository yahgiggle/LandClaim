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
    
    public HashMap<Player, PlayerUIMenu> playerMenus = new HashMap<>();
    private HashMap<Player, PlayerTools> playerTools = new HashMap<>();
    private HashMap<Player, Vector3i> playerChunks = new HashMap<>();
    private HashMap<String, ClaimedArea> claimedAreas = new HashMap<>();
    protected static ArrayList<Area3D> LandClaimedAreas = new ArrayList<>();

    @Override
    public void onEnable()  {
        String worldName = World.getName();
        System.out.println("-- LandClaim PLUGIN ENABLED --");
        database = new LandClaimDatabase(this);
        database.initialize();
        adminTools = new AdminTools(this);
        adminUIMenu = new AdminUIMenu(this, adminTools);
        adminClicks = new AdminOnClickButtons(this, adminTools);
        
        registerEventListener(this);
        LandClaimedAreas.clear();
        System.out.println("Cleared LandClaimedAreas list on plugin enable.");
        try {
            loadClaimedAreas();
        } catch (SQLException ex) {
            Logger.getLogger(LandClaim.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            loadAreaVisuals(null); // Pass null as default, since no player context in onEnable
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

    private Timer timer = new Timer();

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
        PlayerOnClickButtons clickHandler = new PlayerOnClickButtons(this, tools);
        clickHandler.register();
        
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
        showAreas(true, player); // Show areas on connect with correct colors
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

        ClaimedArea area = claimedAreas.get(getKeyFromCoords(areaX, areaY, areaZ));
        if (area != null) {
            player.sendTextMessage("This area is already claimed as " + area.areaName + "!");
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
        database.addClaimedArea(newArea);
        claimedAreas.put(getKeyFromCoords(areaX, areaY, areaZ), newArea);
        addAreaVisual(areaX, areaY, areaZ, areaName, player, true); // true for player-claimed (slight green)
        player.sendTextMessage("Area claimed successfully as " + areaName + "!");
        updateAreaInfoLabel(player, chunk);
        showAreas(true, player);
    }

    public void unclaimArea(Player player) throws SQLException {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            player.sendTextMessage("Error: No chunk data available! Try moving to update your position.");
            return;
        }

        int areaX = chunk.x;
        int areaY = chunk.y;
        int areaZ = chunk.z;

        String key = getKeyFromCoords(areaX, areaY, areaZ);
        ClaimedArea area = claimedAreas.remove(key);
        if (area != null) {
            database.getDb().executeUpdate("DELETE FROM `Areas` WHERE AreaX = " + areaX + " AND AreaY = " + areaY + " AND AreaZ = " + areaZ);
            removeAreaVisual(areaX, areaY, areaZ);
            player.sendTextMessage("Area unclaimed successfully!");
        } else {
            player.sendTextMessage("This area is not claimed!");
        }
        updateAreaInfoLabel(player, chunk);
        showAreas(true, player);
    }

    private void loadClaimedAreas() throws SQLException {
        ResultSet rs = database.getAllClaimedAreas();
        while (rs.next()) {
            String playerUID = rs.getString("PlayerUID");
            String areaOwnerName = rs.getString("AreaOwnerName");
            String areaName = rs.getString("AreaName");
            int areaX = rs.getInt("AreaX");
            int areaY = rs.getInt("AreaY");
            int areaZ = rs.getInt("AreaZ");
            System.out.println("Loading area: UID=" + playerUID + ", Name=" + areaName + ", Raw Chunk Position=(" + areaX + "," + areaY + "," + areaZ + ")");
            ClaimedArea area = new ClaimedArea(playerUID, areaOwnerName, areaName, areaX, areaY, areaZ);
            claimedAreas.put(getKeyFromCoords(areaX, areaY, areaZ), area);
        }
        System.out.println("Loaded " + claimedAreas.size() + " claimed areas from database.");
    }

    private void loadAreaVisuals(Player player) throws SQLException {
        ResultSet rs = database.getAllClaimedAreas();
        while (rs.next()) {
            int areaX = rs.getInt("AreaX");
            int areaY = rs.getInt("AreaY");
            int areaZ = rs.getInt("AreaZ");
            String areaName = rs.getString("AreaName");
            String playerUID = rs.getString("PlayerUID");
            System.out.println("Attempting to load area visual for " + areaName + " at chunk (" + areaX + "," + areaY + "," + areaZ + ")");
            boolean isPlayerClaimed = playerUID != null && !playerUID.equals("") && (player != null && playerUID.equals(player.getUID())); // Check if claimed by current player
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
            // Very light green, slightly transparent (not too dark, just a hint of green)
            areaVisual.setColor(0.0f, 0.2f, 0.0f, 0.3f); // Very pale green, maintaining transparency
            areaVisual.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f); // Light green frame for player's areas
        } else {
            // Blue for other users' areas, maintaining current transparency
            areaVisual.setColor(0.0f, 0.0f, 1.0f, 0.3f); // Blue, semi-transparent
            areaVisual.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f); // Blue frame for other users' areas
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
        if (visible) {
            String playerUID = player.getUID();
            for (Area3D area : LandClaimedAreas) {
                for (ClaimedArea claimedArea : claimedAreas.values()) {
                    if (isAreaVisualForClaimedArea(area, claimedArea)) {
                        if (claimedArea.playerUID.equals(playerUID)) {
                            // Player's own areas: slight green
                            area.setColor(0.0f, 0.2f, 0.0f, 0.3f);
                            area.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f);
                        } else {
                            // Other users' areas: blue
                            area.setColor(0.0f, 0.0f, 1.0f, 0.3f);
                            area.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f);
                        }
                        player.addGameObject(area);
                        break; // Avoid processing the same area multiple times
                    }
                }
            }
        } else {
            for (Area3D area : LandClaimedAreas) {
                player.removeGameObject(area);
            }
        }
    }

    public void showMyAreas(boolean visible, Player player) {
        String playerUID = player.getUID();
        for (Area3D area : LandClaimedAreas) {
            for (ClaimedArea claimedArea : claimedAreas.values()) {
                if (isAreaVisualForClaimedArea(area, claimedArea) && claimedArea.playerUID.equals(playerUID)) {
                    if (visible) {
                        area.setColor(0.0f, 0.2f, 0.0f, 0.3f);
                        area.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f);
                        player.addGameObject(area);
                    } else {
                        player.removeGameObject(area);
                    }
                    break;
                }
            }
        }
    }

    public void showAllAreas(boolean visible, Player player) {
        if (visible) {
            String playerUID = player.getUID();
            for (Area3D area : LandClaimedAreas) {
                for (ClaimedArea claimedArea : claimedAreas.values()) {
                    if (isAreaVisualForClaimedArea(area, claimedArea)) {
                        if (claimedArea.playerUID.equals(playerUID)) {
                            // Player's own areas: slight green
                            area.setColor(0.0f, 0.2f, 0.0f, 0.3f);
                            area.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f);
                        } else {
                            // Other users' areas: blue
                            area.setColor(0.0f, 0.0f, 1.0f, 0.3f);
                            area.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f);
                        }
                        player.addGameObject(area);
                        break; // Avoid processing the same area multiple times
                    }
                }
            }
        } else {
            for (Area3D area : LandClaimedAreas) {
                player.removeGameObject(area);
            }
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
        areaLabel.setPosition(10, 10, true);
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

    private String getKeyFromCoords(int x, int y, int z) {
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

    private Vector3f getGlobalPosition(Vector3i chunk, Vector3i block) {
        return new Vector3f(
            chunk.x * 32 + block.x,
            chunk.y * 64 + block.y,
            chunk.z * 32 + block.z
        );
    }

    private Vector3i getChunkPosition(Vector3f position) {
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