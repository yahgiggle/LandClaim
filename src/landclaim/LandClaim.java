package landclaim;

import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.Timer;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
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
    public HashMap<Player, Long> playerLoginTimes = new HashMap<>();
    HashMap<Vector3i, ClaimedArea> claimedAreasByChunk = new HashMap<>();
    protected static ArrayList<Area3D> AllClaimedAreas = new ArrayList<>();
    protected static HashMap<String, ArrayList<Area3D>> UserClaimedAreas = new HashMap<>();
    private boolean areasLoaded = false;

    private HashMap<Player, Boolean> myAreasVisible = new HashMap<>();
    private HashMap<Player, Boolean> allAreasVisible = new HashMap<>();
    private HashMap<Player, Boolean> isShowingAreas = new HashMap<>();
    HashMap<Player, Area3D> tempClaimVisuals = new HashMap<>(); // New: Track temporary claim visuals
    HashMap<Player, Timer> tempClaimTimers = new HashMap<>();   // New: Track timers for temp visuals

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
        System.out.println("[LandClaim] Points system initialized.");
    }

    @Override
    public void onDisable() {
        System.out.println("-- LandClaim PLUGIN DISABLED --");
        if (database != null) database.close();
        taskQueue.shutdown();
        // Clean up temporary visuals and timers
        for (Player player : tempClaimTimers.keySet()) {
            Timer timer = tempClaimTimers.get(player);
            if (timer != null) timer.kill();
            Area3D visual = tempClaimVisuals.get(player);
            if (visual != null && player.isConnected()) player.removeGameObject(visual);
        }
        tempClaimTimers.clear();
        tempClaimVisuals.clear();
    }

    @EventMethod
    public void onPlayerConnect(PlayerConnectEvent event) {
        Player player = event.getPlayer();
        player.registerKeys(Key.L, Key.LeftShift);
        playerLoginTimes.put(player, System.currentTimeMillis());

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
                    ResultSet rs = database.getDb().executeQuery("SELECT ID FROM `Points` WHERE PlayerUID = '" + escapeSql(uid) + "'");
                    if (!rs.next()) {
                        System.out.println("[LandClaim] Initializing Points row for PlayerUID: " + uid);
                        String sql = "INSERT INTO `Points` (PlayerUID, UserName, Points, TotalPlaytimeHours) " +
                                     "VALUES ('" + escapeSql(uid) + "', '" + escapeSql(player.getName()) + "', 0, 0.0)";
                        database.getDb().executeUpdate(sql);
                    } else {
                        System.out.println("[LandClaim] Points row already exists for PlayerUID: " + uid);
                    }

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
                allAreasVisible.put(player, true);
                isShowingAreas.put(player, false);
                if (!areasLoaded) {
                    try {
                        loadClaimedAreas();
                        areasLoaded = true;
                    } catch (SQLException e) {
                        System.out.println("[LandClaim] Failed to load areas: " + e.getMessage());
                    }
                }
                menu.updateVisibleAreas(initialChunk);
                menu.updateAreaVisibility();
                showAreasOnConnect(player);

                new Timer(60.0f, 0f, 0, () -> {
                    PlayerUIMenu currentMenu = playerMenus.get(player);
                    if (currentMenu != null) {
                        currentMenu.disableAllAreas();
                        allAreasVisible.put(player, false);
                    }
                }).start();
            }
        );
    }

    @EventMethod
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        Player player = event.getPlayer();
        Long loginTime = playerLoginTimes.remove(player);
        if (loginTime == null) {
            System.out.println("[LandClaim] No login time found for Player: " + player.getName());
            return;
        }

        long playtimeMs = System.currentTimeMillis() - loginTime;
        double sessionHours = playtimeMs / 3600000.0;
        System.out.println("[LandClaim] Player " + player.getName() + " disconnected. Session time: " + playtimeMs + "ms (" + sessionHours + " hours)");

        taskQueue.queueTask(
            () -> {
                try {
                    String uid = player.getUID();
                    database.updatePlaytimeAndPoints(uid, sessionHours);
                    int newPoints = database.getPoints(uid);
                    double totalHours = database.getTotalPlaytimeHours(uid);
                    int maxAreaAllocation = database.getMaxAreaAllocation(uid);
                    int pointsEarnedAdjust = database.getPointsEarnedAdjust();
                    int areaCostAdjust = database.getAreaCostAdjust();
                    player.setAttribute("pointsEarned", newPoints);
                    player.setAttribute("totalHours", totalHours);
                    player.setAttribute("maxAreaAllocation", maxAreaAllocation);
                    player.setAttribute("pointsEarnedAdjust", pointsEarnedAdjust);
                    player.setAttribute("areaCostAdjust", areaCostAdjust);
                } catch (SQLException e) {
                    System.out.println("[LandClaim] Error updating playtime/points on disconnect: " + e.getMessage());
                }
            },
            () -> {
                int points = (Integer) player.getAttribute("pointsEarned");
                double totalHours = (Double) player.getAttribute("totalHours");
                int maxAreaAllocation = (Integer) player.getAttribute("maxAreaAllocation");
                int pointsEarnedAdjust = (Integer) player.getAttribute("pointsEarnedAdjust");
                int areaCostAdjust = (Integer) player.getAttribute("areaCostAdjust");
                showMessage(player, "Total playtime: " + String.format("%.3f", totalHours) + " hours, " +
                                    "Points: " + points + ", Max Areas: " + maxAreaAllocation + ", " +
                                    "Points/Hour: " + pointsEarnedAdjust + ", Area Cost: " + areaCostAdjust, 15.0f);
            }
        );
        isShowingAreas.remove(player);
        // Clean up temporary visual and timer
        Timer timer = tempClaimTimers.remove(player);
        if (timer != null) timer.kill();
        Area3D visual = tempClaimVisuals.remove(player);
        if (visual != null && player.isConnected()) player.removeGameObject(visual);
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
        PlayerUIMenu menu = playerMenus.get(player);
        if (menu != null) {
            menu.updateVisibleAreas(chunk);
            menu.updateAreaVisibility();
        }
        System.out.println("[LandClaim] Player " + player.getName() + " entered chunk " + chunk);
    }

    @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) {
        Player player = event.getPlayer();
        if (!adminTools.isAdmin(player)) {
            return;
        }

        String[] args = event.getCommand().split(" ");
        if (args[0].equalsIgnoreCase("/showareas")) {
            boolean show = args.length > 1 ? Boolean.parseBoolean(args[1]) : true;
            if (show) showAreas(player); else hideAreas(player);
            showMessage(player, "Area visuals " + (show ? "shown" : "hidden") + ".", 60.0f);
            event.setCancelled(true);
        } else if (args[0].equalsIgnoreCase("/admin")) {
            if (args.length < 2) {
                showMessage(player, "Usage: /admin [points|cost|alloc] [+|-]", 15.0f);
                event.setCancelled(true);
                return;
            }
            taskQueue.queueTask(
                () -> {
                    try {
                        if (args[1].equalsIgnoreCase("points")) {
                            int current = database.getPointsEarnedAdjust();
                            int adjust = args[2].equals("+") ? 1 : args[2].equals("-") ? -1 : 0;
                            if (adjust == 0) {
                                player.setAttribute("adminResult", "Invalid argument: Use + or -");
                                return;
                            }
                            int newValue = current + adjust;
                            database.setPointsEarnedAdjust(newValue);
                            player.setAttribute("adminResult", "Points per hour set to " + newValue + ". All player points recalculated.");
                        } else if (args[1].equalsIgnoreCase("cost")) {
                            int current = database.getAreaCostAdjust();
                            int adjust = args[2].equals("+") ? 1 : args[2].equals("-") ? -1 : 0;
                            if (adjust == 0) {
                                player.setAttribute("adminResult", "Invalid argument: Use + or -");
                                return;
                            }
                            int newValue = current + adjust;
                            database.setAreaCostAdjust(newValue);
                            player.setAttribute("adminResult", "Area cost set to " + newValue);
                        } else if (args[1].equalsIgnoreCase("alloc")) {
                            String uid = getAreaOwnerUIDFromPosition(player);
                            if (uid == null) {
                                player.setAttribute("adminResult", "Stand in a claimed area!");
                                return;
                            }
                            int current = database.getMaxAreaAllocation(uid);
                            int adjust = args[2].equals("+") ? 1 : args[2].equals("-") ? -1 : 0;
                            if (adjust == 0) {
                                player.setAttribute("adminResult", "Invalid argument: Use + or -");
                                return;
                            }
                            int newMax = current + adjust;
                            if (newMax < 0) {
                                player.setAttribute("adminResult", "Cannot go below 0!");
                                return;
                            }
                            database.setMaxAreaAllocation(uid, newMax);
                            player.setAttribute("adminResult", "Max areas for UID " + uid + " set to " + newMax);
                        } else {
                            player.setAttribute("adminResult", "Unknown subcommand: " + args[1]);
                        }
                    } catch (SQLException e) {
                        player.setAttribute("adminResult", "Error: " + e.getMessage());
                    }
                },
                () -> showMessage(player, (String) player.getAttribute("adminResult"), 15.0f)
            );
            event.setCancelled(true);
        }
    }

    public LandClaimDatabase getDatabase() {
        return database;
    }

    public void startClaimMode(Player player) {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            showMessage(player, "Error: No chunk data!", 15.0f);
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
                        player.setAttribute("claimResult", "You’ve reached your limit of " + max + " areas! Buy more with points.");
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
                    Area3D newVisual = addAreaVisual(chunk.x, chunk.y, chunk.z, area.areaName, area.playerUID, true, player);
                    newVisual.setAlwaysVisible(true); // Set as a fallback
                    showMessage(player, "Claimed " + area.areaName + "!", 15.0f);
                    updateAreaInfoLabel(player, chunk);
                    if (isShowingAreas.getOrDefault(player, false)) {
                        showMessage(player, "Please wait before claiming again!", 15.0f);
                        player.removeGameObject(newVisual);
                        newVisual.setAlwaysVisible(false);
                        return;
                    }
                    // Store the new visual and timer
                    Area3D oldVisual = tempClaimVisuals.remove(player);
                    if (oldVisual != null) player.removeGameObject(oldVisual);
                    Timer oldTimer = tempClaimTimers.remove(player);
                    if (oldTimer != null) oldTimer.kill();
                    tempClaimVisuals.put(player, newVisual);
                    Timer timer = new Timer(60.0f, 0f, 0, () -> {
                        player.removeGameObject(newVisual);
                        newVisual.setAlwaysVisible(false);
                        tempClaimVisuals.remove(player);
                        tempClaimTimers.remove(player);
                    });
                    tempClaimTimers.put(player, timer);
                    timer.start();
                    PlayerUIMenu menu = playerMenus.get(player);
                    if (menu != null) menu.updateAreaVisibility(); // Refresh visibility immediately
                } else {
                    showMessage(player, (String) result, 15.0f);
                }
            }
        );
    }

    public void unclaimArea(Player player) {
        Vector3i chunk = playerChunks.get(player);
        if (chunk == null) {
            showMessage(player, "Error: No chunk data!", 15.0f);
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
                    removeAreaVisual(chunk.x, chunk.y, chunk.z, player.getUID(), player);
                    showMessage(player, "Area unclaimed!", 15.0f);
                    updateAreaInfoLabel(player, chunk);
                    PlayerUIMenu menu = playerMenus.get(player);
                    if (menu != null) menu.updateAreaVisibility(); // Refresh visibility
                } else {
                    showMessage(player, (String) result, 15.0f);
                }
            }
        );
    }

    public void buyAreaAllocation(Player player) {
        taskQueue.queueTask(
            () -> {
                try {
                    String uid = player.getUID();
                    Long loginTime = playerLoginTimes.get(player);
                    if (loginTime == null) {
                        player.setAttribute("buyResult", "Error: No login time recorded!");
                        return;
                    }
                    long sessionMs = System.currentTimeMillis() - loginTime;
                    double sessionHours = sessionMs / 3600000.0;
                    System.out.println("[LandClaim] Buy clicked for " + player.getName() + ", session time: " + sessionHours + " hours");

                    database.updatePlaytimeAndPoints(uid, sessionHours);

                    int areaCost = database.getAreaCostAdjust();
                    int currentPoints = database.getPoints(uid);
                    double currentHours = database.getTotalPlaytimeHours(uid);
                    int pointsEarnedAdjust = database.getPointsEarnedAdjust();

                    if (currentPoints < areaCost) {
                        player.setAttribute("buyResult", "Not enough points! Need " + areaCost + ", have " + currentPoints);
                        playerLoginTimes.put(player, System.currentTimeMillis());
                        return;
                    }

                    double hoursToDeduct = (double) areaCost / pointsEarnedAdjust;
                    if (currentHours < hoursToDeduct) {
                        player.setAttribute("buyResult", "Not enough playtime hours! Need " + hoursToDeduct + ", have " + currentHours);
                        playerLoginTimes.put(player, System.currentTimeMillis());
                        return;
                    }

                    database.deductPoints(uid, areaCost);
                    database.deductHours(uid, hoursToDeduct);

                    int newMax = database.getMaxAreaAllocation(uid) + 1;
                    database.setMaxAreaAllocation(uid, newMax);

                    int newPoints = database.getPoints(uid);
                    double newHours = database.getTotalPlaytimeHours(uid);
                    player.setAttribute("buyResult", "Bought 1 extra area allocation for " + areaCost + " points! " +
                                                    "New max: " + newMax + ", Points left: " + newPoints + ", Hours left: " + String.format("%.3f", newHours));
                    playerLoginTimes.put(player, System.currentTimeMillis());
                } catch (SQLException e) {
                    player.setAttribute("buyResult", "Error: " + e.getMessage());
                    playerLoginTimes.put(player, System.currentTimeMillis());
                }
            },
            () -> {
                showMessage(player, (String) player.getAttribute("buyResult"), 15.0f);
                PlayerUIMenu menu = playerMenus.get(player);
                if (menu != null) {
                    menu.updateBuyAreaButtonText();
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

    public Area3D addAreaVisual(int x, int y, int z, String name, String uid, boolean isPlayer, Player player) {
        Vector3i chunk = new Vector3i(x, y, z);
        Vector3f start = getGlobalPosition(chunk, new Vector3i(0, 0, 0));
        Vector3f end = getGlobalPosition(chunk, new Vector3i(32, 64, 32));
        Area3D visual = new Area3D(new Area(start, end));
        visual.setAlwaysVisible(false); // Default state
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
        return visual;
    }

    private void removeAreaVisual(int x, int y, int z, String uid, Player player) {
        Vector3i chunk = new Vector3i(x, y, z);
        ArrayList<Area3D> areas = UserClaimedAreas.get(uid);
        if (areas != null) {
            areas.removeIf(a -> {
                if (getChunkPosition(a.getArea().getStartPosition()).equals(chunk)) {
                    player.removeGameObject(a);
                    return true;
                }
                return false;
            });
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
                showMessage(player, "Nearby: " + area.areaName + " by " + area.areaOwnerName, 15.0f);
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

    public void hideAreas(Player player) {
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
        UILabel areaInfoLabel = new UILabel();
        areaInfoLabel.setClickable(false);
        areaInfoLabel.setText("");
        areaInfoLabel.setFont(Font.Medieval);
        areaInfoLabel.style.textAlign.set(TextAnchor.MiddleCenter);
        areaInfoLabel.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        areaInfoLabel.setFontSize(22);
        areaInfoLabel.setSize(400, 40, false);
        areaInfoLabel.setBorder(2);
        areaInfoLabel.setBorderColor(1.0f, 1.0f, 1.0f, 0.1f);
        areaInfoLabel.setBackgroundColor(1.0f, 1.0f, 1.0f, 0.05f);
        areaInfoLabel.setPosition(5, 1, true);
        player.addUIElement(areaInfoLabel);
        player.setAttribute("areaInfoLabel", areaInfoLabel);

        UILabel messageLabel = new UILabel();
        messageLabel.setClickable(false);
        messageLabel.setText("");
        messageLabel.setFont(Font.Medieval);
        messageLabel.style.textAlign.set(TextAnchor.MiddleCenter);
        messageLabel.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        messageLabel.setFontSize(22);
        messageLabel.setSize(1000, 40, false);
        messageLabel.setBorder(2);
        messageLabel.setBorderColor(1.0f, 1.0f, 1.0f, 0.1f);
        messageLabel.setBackgroundColor(1.0f, 1.0f, 1.0f, 0.05f);
        messageLabel.setPosition(30, 1, true);
        player.addUIElement(messageLabel);
        player.setAttribute("messageLabel", messageLabel);
    }

    private void updateAreaInfoLabel(Player player, Vector3i chunk) {
        UILabel label = (UILabel) player.getAttribute("areaInfoLabel");
        if (label != null) {
            ClaimedArea area = claimedAreasByChunk.get(chunk);
            label.setText(area != null ? area.areaName : "Area Unclaimed");
        }
    }

    public void showMessage(Player player, String message, float duration) {
        UILabel messageLabel = (UILabel) player.getAttribute("messageLabel");
        if (messageLabel != null) {
            messageLabel.setText(message);
            new Timer(duration, 0f, 0, () -> {
                if (messageLabel.getText().equals(message)) {
                    messageLabel.setText("");
                }
            }).start();
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