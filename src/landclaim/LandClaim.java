package landclaim;

import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.Timer;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerKeyEvent;
import net.risingworld.api.objects.Player;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LandClaim extends Plugin implements Listener {
    private LandClaimDatabase database;
    private AdminTools adminTools;
    public AdminUIMenu adminUIMenu;
    private AdminOnClickButtons adminClicks;
    private PlayerOnClickButtons clickHandler;
    private DatabaseTaskQueue taskQueue;

    public HashMap<Player, PlayerUIMenu> playerMenus = new HashMap<>();
    private HashMap<Player, PlayerTools> playerTools = new HashMap<>();
    private HashMap<Player, Vector3i> playerChunks = new HashMap<>();
    HashMap<Vector3i, ClaimedArea> claimedAreasByChunk = new HashMap<>();
    protected static ArrayList<Area3D> AllClaimedAreas = new ArrayList<>();
    protected static HashMap<String, ArrayList<Area3D>> UserClaimedAreas = new HashMap<>();
    private boolean areasLoaded = false;

    private HashMap<Player, Boolean> myAreasVisible = new HashMap<>();
    private HashMap<Player, Boolean> allAreasVisible = new HashMap<>();

    public HashMap<Player, PlayerTools> getPlayerTools() {
        return playerTools;
    }

    public DatabaseTaskQueue getTaskQueue() {
        return taskQueue;
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
        taskQueue = new DatabaseTaskQueue(this);

        registerEventListener(this);
        OwnerEventHandler ownerHandler = new OwnerEventHandler(this);
        registerEventListener(ownerHandler);

        clickHandler = new PlayerOnClickButtons(this);
        clickHandler.register();

        claimedAreasByChunk.clear();
        AllClaimedAreas.clear();
        UserClaimedAreas.clear();
        areasLoaded = false;
        System.out.println("Cleared data structures on enable. Areas will load on first connect.");
    }

    @Override
    public void onDisable() {
        System.out.println("-- LandClaim PLUGIN DISABLED --");
        if (database != null) {
            database.close();
        }
        taskQueue.shutdown();
    }

    @EventMethod
    public void onPlayerConnect(PlayerConnectEvent event) {
        Player player = event.getPlayer();
        player.registerKeys(Key.L, Key.LeftShift);

        PlayerUIMenu menu = playerMenus.computeIfAbsent(player, p -> new PlayerUIMenu(p, this));
        PlayerTools tools = playerTools.computeIfAbsent(player, p -> {
            PlayerTools t = new PlayerTools(p);
            t.initTools(menu);
            return t;
        });

        Vector3i initialChunk = player.getChunkPosition();
        playerChunks.put(player, initialChunk);

        if (adminTools.isAdmin(player)) {
            adminUIMenu.showAdminMenu(player);
        }

        taskQueue.queueTask(
            () -> {
                try {
                    String uid = player.getUID();
                    int areaCount = getPlayerAreaCountFromDB(uid);
                    if (areaCount == 0) {
                        database.setMaxAreaAllocation(uid, 2);
                    }
                } catch (SQLException e) {
                    System.out.println("[LandClaim] Error setting initial allocation: " + e.getMessage());
                }
            },
            () -> {
                addAreaInfoLabel(player);
                updateAreaInfoLabel(player, initialChunk);
                myAreasVisible.put(player, false);
                allAreasVisible.put(player, false);
                if (!areasLoaded) {
                    try {
                        loadClaimedAreas();
                        areasLoaded = true;
                        System.out.println("[LandClaim] Areas loaded successfully for player " + player.getName());
                    } catch (SQLException e) {
                        System.out.println("[LandClaim] Failed to load areas: " + e.getMessage());
                    }
                }
                showAreasOnConnect(player);
            }
        );
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
        updateAreaInfoLabel(player, chunk);
    }

    @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) {
        Player player = event.getPlayer();
        if (!adminTools.isAdmin(player)) return;

        String[] args = event.getCommand().split(" ");
        if (args[0].equalsIgnoreCase("/showareas")) {
            boolean show = args.length > 1 ? Boolean.parseBoolean(args[1]) : true;
            if (show) {
                showAreas(player);
            } else {
                hideAreas(player);
            }
            player.sendTextMessage("Area visuals " + (show ? "shown" : "hidden") + " for you.");
            event.setCancelled(true);
        }
    }

    public LandClaimDatabase getDatabase() {
        return database;
    }

    public void startClaimMode(Player player) {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            player.sendTextMessage("Error: No chunk data available!");
            return;
        }

        taskQueue.queueTask(
            () -> {
                try {
                    ClaimedArea existingArea = claimedAreasByChunk.get(chunk);
                    if (existingArea != null) {
                        player.setAttribute("claimResult", "This area is already claimed as " + existingArea.areaName + "!");
                        return;
                    }

                    String uid = player.getUID();
                    int areaCount = getPlayerAreaCountFromDB(uid);
                    int maxAreas = database.getMaxAreaAllocation(uid);
                    if (areaCount >= maxAreas) {
                        player.setAttribute("claimResult", "Sorry, you cannot claim more areas. Limit: " + maxAreas + "!");
                        return;
                    }

                    String areaName = areaCount == 0 ? player.getName() + "'s Home" : player.getName() + "'s Area=" + (areaCount + 1);
                    ClaimedArea newArea = new ClaimedArea(uid, player.getName(), areaName, chunk.x, chunk.y, chunk.z);
                    if (database.addClaimedArea(newArea)) {
                        player.setAttribute("claimResult", newArea);
                    } else {
                        player.setAttribute("claimResult", "Failed to claim area!");
                    }
                } catch (SQLException e) {
                    player.setAttribute("claimResult", "Error claiming area: " + e.getMessage());
                }
            },
            () -> {
                Object result = player.getAttribute("claimResult");
                if (result instanceof ClaimedArea) {
                    ClaimedArea newArea = (ClaimedArea) result;
                    claimedAreasByChunk.put(chunk, newArea);
                    addAreaVisual(chunk.x, chunk.y, chunk.z, newArea.areaName, newArea.playerUID, true, player);
                    player.sendTextMessage("Area claimed successfully as " + newArea.areaName + "!");
                    updateAreaInfoLabel(player, chunk);
                    showAreas(player);
                } else {
                    player.sendTextMessage((String) result);
                }
            }
        );
    }

    public void unclaimArea(Player player) {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            player.sendYellMessage("Error: No chunk data available!", 3, true);
            return;
        }

        taskQueue.queueTask(
            () -> {
                try {
                    ClaimedArea area = claimedAreasByChunk.get(chunk);
                    if (area == null) {
                        player.setAttribute("unclaimResult", "This area is not claimed!");
                        return;
                    }
                    String playerUID = player.getUID();
                    if (!area.playerUID.equals(playerUID)) {
                        player.setAttribute("unclaimResult", "You can only unclaim your own areas!");
                        return;
                    }

                    database.removeClaimedArea(chunk.x, chunk.y, chunk.z, playerUID);
                    player.setAttribute("unclaimResult", "Area unclaimed successfully!");
                } catch (SQLException e) {
                    player.setAttribute("unclaimResult", "Error unclaiming area: " + e.getMessage());
                }
            },
            () -> {
                String result = (String) player.getAttribute("unclaimResult");
                if ("Area unclaimed successfully!".equals(result)) {
                    claimedAreasByChunk.remove(chunk);
                    removeAreaVisual(chunk.x, chunk.y, chunk.z, player.getUID());
                    player.sendYellMessage(result, 3, true);
                    updateAreaInfoLabel(player, chunk);
                    showAreas(player);
                } else {
                    player.sendYellMessage(result, 3, true);
                }
            }
        );
    }

    public void loadClaimedAreas() throws SQLException {
        System.out.println("[LandClaim] Loading claimed areas from database...");
        ResultSet rs = database.getAllClaimedAreas();
        if (rs == null) {
            System.out.println("[LandClaim] Error: ResultSet is null from getAllClaimedAreas()");
            throw new SQLException("ResultSet is null");
        }

        claimedAreasByChunk.clear();
        while (rs.next()) {
            try {
                String playerUID = rs.getString("PlayerUID");
                String areaOwnerName = rs.getString("AreaOwnerName");
                String areaName = rs.getString("AreaName");
                int areaX = rs.getInt("AreaX");
                int areaY = rs.getInt("AreaY");
                int areaZ = rs.getInt("AreaZ");
                Vector3i chunk = new Vector3i(areaX, areaY, areaZ);
                claimedAreasByChunk.put(chunk, new ClaimedArea(playerUID, areaOwnerName, areaName, areaX, areaY, areaZ));
                System.out.println("[LandClaim] Loaded area: " + areaName + " at " + areaX + "," + areaY + "," + areaZ);
            } catch (SQLException e) {
                System.out.println("[LandClaim] Error reading row: " + e.getMessage());
                throw e; // Re-throw to be caught by the caller
            }
        }
        System.out.println("[LandClaim] Loaded " + claimedAreasByChunk.size() + " claimed areas.");
        rs.close(); // Ensure ResultSet is closed
    }

    private void addAreaVisual(int areaX, int areaY, int areaZ, String areaName, String playerUID, boolean isPlayerClaimed, Player player) {
        Vector3i chunk = new Vector3i(areaX, areaY, areaZ);
        Vector3f startPos = getGlobalPosition(chunk, new Vector3i(0, 0, 0));
        Vector3f endPos = getGlobalPosition(chunk, new Vector3i(32, 64, 32));
        Area area = new Area(startPos, endPos);
        Area3D areaVisual = new Area3D(area);
        areaVisual.setAlwaysVisible(false);
        areaVisual.setFrameVisible(true);

        if (isPlayerClaimed) {
            areaVisual.setColor(0.0f, 0.2f, 0.0f, 0.3f);
            areaVisual.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f);
            UserClaimedAreas.computeIfAbsent(playerUID, k -> new ArrayList<>()).add(areaVisual);
        } else {
            areaVisual.setColor(0.0f, 0.0f, 1.0f, 0.3f);
            areaVisual.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f);
            AllClaimedAreas.add(areaVisual);
        }

        player.addGameObject(areaVisual);
    }

    private void removeAreaVisual(int areaX, int areaY, int areaZ, String playerUID) {
        Vector3i chunk = new Vector3i(areaX, areaY, areaZ);
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(playerUID);
        if (userAreas != null) {
            userAreas.removeIf(visual -> getChunkPosition(visual.getArea().getStartPosition()).equals(chunk));
        }
    }

    public void showAreasOnConnect(Player player) {
        Vector3i playerChunk = player.getChunkPosition();
        int radius = 5;
        String playerUID = player.getUID();

        for (ClaimedArea area : claimedAreasByChunk.values()) {
            Vector3i areaChunk = new Vector3i(area.areaX, area.areaY, area.areaZ);
            if (Math.abs(areaChunk.x - playerChunk.x) <= radius &&
                Math.abs(areaChunk.y - playerChunk.y) <= radius &&
                Math.abs(areaChunk.z - playerChunk.z) <= radius) {
                addAreaVisual(area.areaX, area.areaY, area.areaZ, area.areaName, area.playerUID,
                    area.playerUID.equals(playerUID), player);
                if (area.playerUID.equals(playerUID)) {
                    // Green areas (your claims) vanish after 60 seconds
                    new Timer(60.0f, 0f, 0, () -> {
                        ArrayList<Area3D> userAreas = UserClaimedAreas.getOrDefault(playerUID, new ArrayList<>());
                        Area3D areaVisual = userAreas.stream()
                            .filter(a -> getChunkPosition(a.getArea().getStartPosition()).equals(areaChunk))
                            .findFirst().orElse(null);
                        if (areaVisual != null) {
                            player.removeGameObject(areaVisual);
                        }
                    }).start();
                } else {
                    // Blue areas (others' claims) vanish after 60 seconds
                    Area3D areaVisual = AllClaimedAreas.stream()
                        .filter(a -> getChunkPosition(a.getArea().getStartPosition()).equals(areaChunk))
                        .findFirst().orElse(null);
                    if (areaVisual != null) {
                        new Timer(60.0f, 0f, 0, () -> {
                            player.removeGameObject(areaVisual);
                        }).start();
                    }
                }
            }
        }
    }

    public void showAreas(Player player) {
        hideAreas(player);
        String playerUID = player.getUID();
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(playerUID);
        if (userAreas != null) {
            userAreas.forEach(player::addGameObject);
        }
        AllClaimedAreas.forEach(player::addGameObject);
        myAreasVisible.put(player, true);
        allAreasVisible.put(player, true);
    }

    private void hideAreas(Player player) {
        String playerUID = player.getUID();
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(playerUID);
        if (userAreas != null) {
            userAreas.forEach(player::removeGameObject);
        }
        AllClaimedAreas.forEach(player::removeGameObject);
        myAreasVisible.put(player, false);
        allAreasVisible.put(player, false);
    }

    public void showMyAreas(boolean visible, Player player) {
        Boolean isVisible = myAreasVisible.getOrDefault(player, false);
        if (visible == isVisible) return;

        String playerUID = player.getUID();
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(playerUID);
        if (userAreas == null) return;

        if (visible) {
            userAreas.forEach(player::addGameObject);
            myAreasVisible.put(player, true);
        } else {
            userAreas.forEach(player::removeGameObject);
            myAreasVisible.put(player, false);
        }
    }

    public void showAllAreas(boolean visible, Player player) {
        Boolean isVisible = allAreasVisible.getOrDefault(player, false);
        if (visible == isVisible) return;

        String playerUID = player.getUID();
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(playerUID);

        if (visible) {
            AllClaimedAreas.forEach(player::addGameObject);
            if (userAreas != null) {
                userAreas.forEach(player::addGameObject);
            }
            allAreasVisible.put(player, true);
        } else {
            AllClaimedAreas.forEach(player::removeGameObject);
            if (userAreas != null) {
                userAreas.forEach(player::removeGameObject);
            }
            allAreasVisible.put(player, false);
        }
    }

    private int getPlayerAreaCountFromDB(String playerUID) throws SQLException {
        ResultSet rs = database.getDb().executeQuery("SELECT COUNT(*) as count FROM `Areas` WHERE PlayerUID = '" + escapeSql(playerUID) + "'");
        return rs.next() ? rs.getInt("count") : 0;
    }

    private String escapeSql(String input) {
        return input == null ? "" : input.replace("'", "''");
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
        if (areaLabel != null) {
            ClaimedArea area = claimedAreasByChunk.get(chunk);
            areaLabel.setText(area != null ? area.areaName : "Area Unclaimed");
        }
    }

    public String getAreaOwnerUIDFromPosition(Player player) throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        ResultSet rs = database.getDb().executeQuery(
            "SELECT PlayerUID FROM `Areas` WHERE AreaX = " + chunk.x + " AND AreaY = " + chunk.y + " AND AreaZ = " + chunk.z
        );
        return rs.next() ? rs.getString("PlayerUID") : null;
    }

    public ClaimedArea getClaimedAreaAt(Vector3i chunkPos) {
        return claimedAreasByChunk.get(chunkPos);
    }

    private Vector3f getGlobalPosition(Vector3i chunk, Vector3i block) {
        return new Vector3f(chunk.x * 32 + block.x, chunk.y * 64 + block.y, chunk.z * 32 + block.z);
    }

    Vector3i getChunkPosition(Vector3f position) {
        return new Vector3i((int) (position.x / 32), (int) (position.y / 64), (int) (position.z / 32));
    }

    class ClaimedArea {
        String playerUID;
        String areaOwnerName;
        String areaName;
        int areaX, areaY, areaZ;

        ClaimedArea(String playerUID, String areaOwnerName, String areaName, int areaX, int areaY, int areaZ) {
            this.playerUID = playerUID;
            this.areaOwnerName = areaOwnerName;
            this.areaName = areaName;
            this.areaX = areaX;
            this.areaY = areaY;
            this.areaZ = areaZ;
        }
    }

    class DatabaseTaskQueue {
        private final ExecutorService executor = Executors.newFixedThreadPool(2);
        private final LandClaim plugin;

        DatabaseTaskQueue(LandClaim plugin) {
            this.plugin = plugin;
        }

        public void queueTask(Runnable task, Runnable callback) {
            executor.submit(() -> {
                try {
                    task.run();
                } finally {
                    new Timer(0f, 0f, 0, callback).start();
                }
            });
        }

        public void shutdown() {
            executor.shutdown();
        }
    }
}