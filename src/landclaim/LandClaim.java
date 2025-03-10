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

public class LandClaim extends Plugin implements Listener {
    private LandClaimDatabase database;
    private AdminTools adminTools;
    private AdminUIMenu adminUIMenu;
    private AdminOnClickButtons adminClicks;
    private PlayerOnClickButtons playerClicks;
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
        System.out.println("-- LandClaim PLUGIN ENABLED --");
        database = new LandClaimDatabase(this);
        database.initialize();
        adminTools = new AdminTools(this);
        adminUIMenu = new AdminUIMenu(this, adminTools);
        adminClicks = new AdminOnClickButtons(this, adminTools);
        playerClicks = new PlayerOnClickButtons(this);
        taskQueue = new DatabaseTaskQueue();

        registerEventListener(this);
        registerEventListener(new OwnerEventHandler(this));
        adminClicks.register();
        playerClicks.register();

        claimedAreasByChunk.clear();
        AllClaimedAreas.clear();
        UserClaimedAreas.clear();
        areasLoaded = false;
        System.out.println("[LandClaim] Initialized and listeners registered.");
    }

    @Override
    public void onDisable() {
        System.out.println("-- LandClaim PLUGIN DISABLED --");
        if (database != null) database.close();
        taskQueue.shutdown();
    }

    @EventMethod
    public void onPlayerConnect(PlayerConnectEvent event) {
        Player player = event.getPlayer();
        player.registerKeys(Key.L, Key.LeftShift);

        PlayerUIMenu menu = new PlayerUIMenu(player, this);
        playerMenus.put(player, menu);
        PlayerTools tools = new PlayerTools(player);
        tools.initTools(menu);
        playerTools.put(player, tools);

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
                    if (areaCount == 0) database.setMaxAreaAllocation(uid, 2);
                } catch (SQLException e) {
                    System.out.println("[LandClaim] Error in player connect DB task: " + e.getMessage());
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
            if (menu != null) menu.toggleMenu();
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
            if (show) showAreas(player); else hideAreas(player);
            player.sendTextMessage("Area visuals " + (show ? "shown" : "hidden") + ".");
            event.setCancelled(true);
        }
    }

    public LandClaimDatabase getDatabase() {
        return database;
    }

    public void startClaimMode(Player player) {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            player.sendTextMessage("Error: No chunk data!");
            return;
        }

        taskQueue.queueTask(
            () -> {
                try {
                    ClaimedArea area = claimedAreasByChunk.get(chunk);
                    if (area != null) {
                        player.setAttribute("claimResult", "Area already claimed as " + area.areaName + "!");
                        return;
                    }
                    String uid = player.getUID();
                    int count = getPlayerAreaCountFromDB(uid);
                    int max = database.getMaxAreaAllocation(uid);
                    if (count >= max) {
                        player.setAttribute("claimResult", "You’ve reached your limit of " + max + " areas!");
                        return;
                    }
                    String name = count == 0 ? player.getName() + "'s Home" : player.getName() + "'s Area=" + (count + 1);
                    ClaimedArea newArea = new ClaimedArea(uid, player.getName(), name, chunk.x, chunk.y, chunk.z);
                    if (database.addClaimedArea(newArea)) {
                        player.setAttribute("claimResult", newArea);
                    } else {
                        player.setAttribute("claimResult", "Failed to claim area!");
                    }
                } catch (SQLException e) {
                    player.setAttribute("claimResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                Object result = player.getAttribute("claimResult");
                if (result instanceof ClaimedArea) {
                    ClaimedArea area = (ClaimedArea) result;
                    claimedAreasByChunk.put(chunk, area);
                    addAreaVisual(chunk.x, chunk.y, chunk.z, area.areaName, area.playerUID, true, player);
                    player.sendTextMessage("Claimed " + area.areaName + "!");
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
            player.sendYellMessage("Error: No chunk data!", 3, true);
            return;
        }

        taskQueue.queueTask(
            () -> {
                try {
                    ClaimedArea area = claimedAreasByChunk.get(chunk);
                    if (area == null) {
                        player.setAttribute("unclaimResult", "Area not claimed!");
                        return;
                    }
                    if (!area.playerUID.equals(player.getUID())) {
                        player.setAttribute("unclaimResult", "You can only unclaim your own areas!");
                        return;
                    }
                    database.removeClaimedArea(chunk.x, chunk.y, chunk.z, player.getUID());
                    player.setAttribute("unclaimResult", area);
                } catch (SQLException e) {
                    player.setAttribute("unclaimResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                Object result = player.getAttribute("unclaimResult");
                if (result instanceof ClaimedArea) {
                    claimedAreasByChunk.remove(chunk);
                    removeAreaVisual(chunk.x, chunk.y, chunk.z, player.getUID());
                    player.sendYellMessage("Area unclaimed!", 3, true);
                    updateAreaInfoLabel(player, chunk);
                    showAreas(player);
                } else {
                    player.sendYellMessage((String) result, 3, true);
                }
            }
        );
    }

    public void loadClaimedAreas() throws SQLException {
        ResultSet rs = database.getAllClaimedAreas();
        claimedAreasByChunk.clear();
        while (rs.next()) {
            String uid = rs.getString("PlayerUID");
            String owner = rs.getString("AreaOwnerName");
            String name = rs.getString("AreaName");
            int x = rs.getInt("AreaX");
            int y = rs.getInt("AreaY");
            int z = rs.getInt("AreaZ");
            claimedAreasByChunk.put(new Vector3i(x, y, z), new ClaimedArea(uid, owner, name, x, y, z));
        }
        System.out.println("[LandClaim] Loaded " + claimedAreasByChunk.size() + " areas.");
    }

    private void addAreaVisual(int x, int y, int z, String name, String uid, boolean isPlayer, Player player) {
        Vector3i chunk = new Vector3i(x, y, z);
        Vector3f start = getGlobalPosition(chunk, new Vector3i(0, 0, 0));
        Vector3f end = getGlobalPosition(chunk, new Vector3i(32, 64, 32));
        Area3D visual = new Area3D(new Area(start, end));
        visual.setAlwaysVisible(false);
        visual.setFrameVisible(true);

        if (isPlayer) {
            visual.setColor(0.0f, 0.2f, 0.0f, 0.3f);
            visual.setFrameColor(0.0f, 0.5f, 0.0f, 1.0f);
            UserClaimedAreas.computeIfAbsent(uid, k -> new ArrayList<>()).add(visual);
        } else {
            visual.setColor(0.0f, 0.0f, 1.0f, 0.3f);
            visual.setFrameColor(0.0f, 0.0f, 1.0f, 1.0f);
            AllClaimedAreas.add(visual);
        }
        player.addGameObject(visual);
    }

    private void removeAreaVisual(int x, int y, int z, String uid) {
        Vector3i chunk = new Vector3i(x, y, z);
        ArrayList<Area3D> areas = UserClaimedAreas.get(uid);
        if (areas != null) {
            areas.removeIf(a -> getChunkPosition(a.getArea().getStartPosition()).equals(chunk));
        }
    }

    public void showAreasOnConnect(Player player) {
        Vector3i chunk = player.getChunkPosition();
        int radius = 5;
        String uid = player.getUID();
        for (ClaimedArea area : claimedAreasByChunk.values()) {
            Vector3i areaChunk = new Vector3i(area.areaX, area.areaY, area.areaZ);
            if (Math.abs(areaChunk.x - chunk.x) <= radius &&
                Math.abs(areaChunk.y - chunk.y) <= radius &&
                Math.abs(areaChunk.z - chunk.z) <= radius) {
                addAreaVisual(area.areaX, area.areaY, area.areaZ, area.areaName, area.playerUID, area.playerUID.equals(uid), player);
                new Timer(60f, 0f, 0, () -> {
                    ArrayList<Area3D> areas = area.playerUID.equals(uid) ? UserClaimedAreas.get(uid) : AllClaimedAreas;
                    Area3D visual = areas.stream()
                        .filter(a -> getChunkPosition(a.getArea().getStartPosition()).equals(areaChunk))
                        .findFirst().orElse(null);
                    if (visual != null) player.removeGameObject(visual);
                }).start();
            }
        }
    }

    public void showAreas(Player player) {
        hideAreas(player);
        String uid = player.getUID();
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(uid);
        if (userAreas != null) userAreas.forEach(player::addGameObject);
        AllClaimedAreas.forEach(player::addGameObject);
        myAreasVisible.put(player, true);
        allAreasVisible.put(player, true);
    }

    private void hideAreas(Player player) {
        String uid = player.getUID();
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(uid);
        if (userAreas != null) userAreas.forEach(player::removeGameObject);
        AllClaimedAreas.forEach(player::removeGameObject);
        myAreasVisible.put(player, false);
        allAreasVisible.put(player, false);
    }

    public void showMyAreas(boolean visible, Player player) {
        if (myAreasVisible.getOrDefault(player, false) == visible) return;
        String uid = player.getUID();
        ArrayList<Area3D> areas = UserClaimedAreas.get(uid);
        if (areas != null) {
            if (visible) areas.forEach(player::addGameObject);
            else areas.forEach(player::removeGameObject);
        }
        myAreasVisible.put(player, visible);
    }

    public void showAllAreas(boolean visible, Player player) {
        if (allAreasVisible.getOrDefault(player, false) == visible) return;
        String uid = player.getUID();
        ArrayList<Area3D> userAreas = UserClaimedAreas.get(uid);
        if (visible) {
            AllClaimedAreas.forEach(player::addGameObject);
            if (userAreas != null) userAreas.forEach(player::addGameObject);
        } else {
            AllClaimedAreas.forEach(player::removeGameObject);
            if (userAreas != null) userAreas.forEach(player::removeGameObject);
        }
        allAreasVisible.put(player, visible);
    }

    private int getPlayerAreaCountFromDB(String uid) throws SQLException {
        ResultSet rs = database.getDb().executeQuery("SELECT COUNT(*) as count FROM `Areas` WHERE PlayerUID = '" + escapeSql(uid) + "'");
        return rs.next() ? rs.getInt("count") : 0;
    }

    private String escapeSql(String input) {
        return input == null ? "" : input.replace("'", "''");
    }

    private void addAreaInfoLabel(Player player) {
        UILabel label = new UILabel();
        label.setClickable(false);
        label.setText("Area Unclaimed");
        label.setFont(Font.Medieval);
        label.style.textAlign.set(TextAnchor.MiddleCenter);
        label.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        label.setFontSize(16);
        label.setSize(300, 50, false);
        label.setBorder(2);
        label.setBorderColor(999);
        label.setBackgroundColor(500);
        label.setPosition(1, 90, true);
        player.addUIElement(label);
        player.setAttribute("areaInfoLabel", label);
    }

    private void updateAreaInfoLabel(Player player, Vector3i chunk) {
        UILabel label = (UILabel) player.getAttribute("areaInfoLabel");
        if (label != null) {
            ClaimedArea area = claimedAreasByChunk.get(chunk);
            label.setText(area != null ? area.areaName : "Area Unclaimed");
        }
    }

    public String getAreaOwnerUIDFromPosition(Player player) throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        ResultSet rs = database.getDb().executeQuery(
            "SELECT PlayerUID FROM `Areas` WHERE AreaX = " + chunk.x + " AND AreaY = " + chunk.y + " AND AreaZ = " + chunk.z
        );
        return rs.next() ? rs.getString("PlayerUID") : null;
    }

    public ClaimedArea getClaimedAreaAt(Vector3i chunk) {
        return claimedAreasByChunk.get(chunk);
    }

    private Vector3f getGlobalPosition(Vector3i chunk, Vector3i block) {
        return new Vector3f(chunk.x * 32 + block.x, chunk.y * 64 + block.y, chunk.z * 32 + block.z);
    }

    Vector3i getChunkPosition(Vector3f pos) {
        return new Vector3i((int) (pos.x / 32), (int) (pos.y / 64), (int) (pos.z / 32));
    }

    class ClaimedArea {
        String playerUID, areaOwnerName, areaName;
        int areaX, areaY, areaZ;

        ClaimedArea(String uid, String owner, String name, int x, int y, int z) {
            this.playerUID = uid;
            this.areaOwnerName = owner;
            this.areaName = name;
            this.areaX = x;
            this.areaY = y;
            this.areaZ = z;
        }
    }

    class DatabaseTaskQueue {
        private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        public void queueTask(Runnable task, Runnable callback) {
            executor.submit(() -> {
                task.run();
                new Timer(0f, 0f, 0, callback).start();
            });
        }

        public void shutdown() {
            executor.shutdown();
        }
    }
}