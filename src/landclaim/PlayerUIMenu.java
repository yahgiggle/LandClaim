package landclaim;

import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.utils.Vector3i;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerUIMenu {
    private final Player player;
    private final LandClaim plugin;
    private UIElement menuBase, settingsMenu;
    private UILabel claimButton, removeButton, exitButton, settingsButton, settingsExitButton;
    private UILabel showMyAreasLabel, showAllAreasLabel, infoLabel;
    private UILabel nextPlayerButton, backPlayerButton, addGuestButton, removeGuestButton;
    private UILabel buyAreaButton;
    private boolean isVisible, showingMyAreas = false, showingAllAreas = true; // Default to true for connect
    private List<Player> worldPlayers = new ArrayList<>();
    private int currentPlayerIndex = -1, currentAreaId = -1;
    final Map<String, UILabel> permissionButtons = new HashMap<>();
    private Map<Vector3i, LandClaim.ClaimedArea> visibleAreas = new HashMap<>(); // Tracks areas in 5-chunk radius

    public PlayerUIMenu(Player player, LandClaim plugin) {
        this.player = player;
        this.plugin = plugin;
        setupBaseMenu();
        setupSettingsMenu();
        updateVisibleAreas(player.getChunkPosition()); // Initial area load
        updateLabels(); // Reflect initial "Show All Areas" state
    }

    private void setupBaseMenu() {
        menuBase = new UIElement();
        player.addUIElement(menuBase);
        menuBase.setSize(276, 370, false); // Increased height to accommodate new button
        menuBase.setClickable(false);
        menuBase.setPosition(45, 45, true);
        menuBase.setBorderEdgeRadius(5.0f, false);
        menuBase.setBorder(3);
        menuBase.setBorderColor(888);
        menuBase.setBackgroundColor(0.1f, 0.1f, 0.1f, 0.9f);
        menuBase.setVisible(false);

        claimButton = createButton(menuBase, 10, 10, "Claim Area", 250, 45);
        removeButton = createButton(menuBase, 10, 60, "Remove Area", 250, 45);
        showMyAreasLabel = createButton(menuBase, 10, 110, "Show My Areas\nOff", 250, 45);
        showAllAreasLabel = createButton(menuBase, 10, 160, "Show All Areas\nOn", 250, 45); // Initial state On
        exitButton = createButton(menuBase, 10, 310, "Exit", 250, 45); // exit button
        settingsButton = createButton(menuBase, 10, 260, "Settings", 250, 45);
        
        // Initial buyAreaButton setup
        updateBuyAreaButtonText();
        buyAreaButton = createButton(menuBase, 10, 210, getBuyAreaButtonText(), 250, 45);  // buy button
    }

    private void setupSettingsMenu() {
        settingsMenu = new UIElement();
        player.addUIElement(settingsMenu);
        settingsMenu.setSize(1566, 850, false);
        settingsMenu.setClickable(false);
        settingsMenu.setPosition(5, 5, true);
        settingsMenu.setBorderEdgeRadius(5.0f, false);
        settingsMenu.setBorder(3);
        settingsMenu.setBorderColor(888);
        settingsMenu.setBackgroundColor(0.8f, 0.8f, 0.8f, 0.95f);
        settingsMenu.setVisible(false);

        infoLabel = createLabel(settingsMenu, 10, 5, "No Player Selected", 1466, 45, false);
        player.setAttribute("infoLabel", infoLabel);

        nextPlayerButton = createButton(settingsMenu, 10, 60, "Next Player", 300, 45);
        backPlayerButton = createButton(settingsMenu, 320, 60, "Back Player", 300, 45);
        addGuestButton = createButton(settingsMenu, 630, 60, "Add Guest", 300, 45);
        removeGuestButton = createButton(settingsMenu, 940, 60, "Remove Guest", 300, 45);
        settingsExitButton = createButton(settingsMenu, 1511, 5, "<color=red><b>X</b></color>", 45, 45); 
        player.setAttribute("settingsExitButton", settingsExitButton);

        player.setAttribute("nextPlayerButton", nextPlayerButton);
        player.setAttribute("backPlayerButton", backPlayerButton);
        player.setAttribute("addGuestButton", addGuestButton);
        player.setAttribute("removeGuestButton", removeGuestButton);

        initPermissionButtons();
    }

    private void initPermissionButtons() {
        String[] permissions = {
            "PlayerShootingTargetUseObject", "PlayerSignUseObject", "PlayerSpinningWheelUseObject", "PlayerStorageUseObject",
            "PlayerTanningRackUseObject", "PlayerTechnicalUseObject", "PlayerTorchUseObject", "PlayerTrashcanUseObject",
            "PlayerWorkbenchUseObject", "PlayerHitAnimalNPC", "PlayerHitHumanNPC", "PlayerHitMountNPC", "PlayerRideMountNPC",
            "PlayerPlaceBluePrints", "PlayerNpcAddSaddle", "PlayerNpcRemoveSaddle", "PlayerNpcAddSaddleBag", "PlayerNpcRemoveSaddleBag",
            "PlayerNpcAddClothes", "PlayerNpcRemoveClothes", "PlayerChangeConstructionColor", "PlayerChangeObjectColor",
            "PlayerChangeObjectInfo", "PlayerCreativePlaceVegetation", "PlayerCreativeRemoveConstruction", "PlayerCreativeRemoveObject",
            "PlayerCreativeRemoveVegetation", "PlayerCreativeTerrainEdit", "PlayerDestroyConstruction", "PlayerDestroyObject",
            "PlayerDestroyTerrain", "PlayerEditConstruction", "PlayerHitConstruction", "PlayerHitObject", "PlayerHitVegetation",
            "PlayerHitWater", "PlayerPlaceConstruction", "PlayerPlaceGrass", "PlayerPlaceObject", "PlayerPlaceTerrain",
            "PlayerPlaceVegetation", "PlayerRemoveConstruction", "PlayerRemoveGrass", "PlayerRemoveObject", "PlayerRemoveVegetation",
            "PlayerRemoveWater", "PlayerWorldEdit", "PlayerClockUseObject", "PlayerDoorUseObject", "PlayerDryingRackUseObject",
            "PlayerFireUseObject", "PlayerFurnaceUseObject", "PlayerGrillUseObject", "PlayerGrinderUseObject", "PlayerGrindstoneUseObject",
            "PlayerLadderUseObject", "PlayerLampUseObject", "PlayerMusicPlayerUseObject", "PlayerOvenUseObject", "PlayerPaperPressUseObject",
            "PlayerPianoUseObject", "PlayerPosterUseObject", "PlayerScaffoldingUseObject", "PlayerSeatingUseObject"
        };

        int x = 10, y = 115, perRow = 5, rowHeight = 55;
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            String name = perm.replace("Player", "").replace("UseObject", "").replace("NPC", "");
            UILabel button = createButton(settingsMenu, x + (i % perRow) * 310, y + (i / perRow) * rowHeight, name + ": Off", 300, 45);
            permissionButtons.put(perm, button);
        }
    }

    private UILabel createButton(UIElement parent, int x, int y, String text, int width, int height) {
        UILabel button = new UILabel();
        button.setClickable(true);
        button.setText("<b>" + text + "</b>");
        button.setFont(Font.Medieval);
        button.style.textAlign.set(TextAnchor.MiddleCenter);
        button.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        button.setFontSize(16);
        button.setSize(width, height, false);
        button.setBorder(2);
        button.setBorderColor(999);
        button.setBackgroundColor(500);
        button.setPosition(x, y, false);
        button.style.borderBottomWidth.set(5);
        button.hoverStyle.backgroundColor.set(0.2f, 0.2f, 0.2f, 0.9f);
        button.hoverStyle.borderBottomWidth.set(5);
        button.hoverStyle.borderBottomColor.set(0.1f, 0.1f, 0.9f, 0.9f);
        parent.addChild(button);
        return button;
    }

    private UILabel createLabel(UIElement parent, int x, int y, String text, int width, int height, boolean clickable) {
        UILabel label = new UILabel();
        label.setClickable(clickable);
        label.setText(text);
        label.setFont(Font.Medieval);
        label.style.textAlign.set(TextAnchor.MiddleCenter);
        label.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        label.setFontSize(16);
        label.setSize(width, height, false);
        label.setBorder(2);
        label.setBorderColor(999);
        label.setBackgroundColor(500);
        label.setPosition(x, y, false);
        parent.addChild(label);
        return label;
    }

    private String getBuyAreaButtonText() {
        int areaCost = 1;
        int playerPoints = 0;
        try {
            areaCost = plugin.getDatabase().getAreaCostAdjust();
            playerPoints = plugin.getDatabase().getPoints(player.getUID());
        } catch (SQLException e) {
            System.out.println("[PlayerUIMenu] Error fetching area cost or points: " + e.getMessage());
        }
        return "<b>Buy 1 Area (" + areaCost + " Cost, You: " + playerPoints + ")</b>";
    }

    public void updateBuyAreaButtonText() {
        if (buyAreaButton != null) {
            String newText = getBuyAreaButtonText();
            buyAreaButton.setText(newText);
            System.out.println("[PlayerUIMenu] Updated buyAreaButton text for " + player.getName() + ": " + newText);
        }
    }

    public void showSettingsMenu() throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        LandClaim.ClaimedArea area = plugin.getClaimedAreaAt(chunk);
        if (area == null) {
            player.sendYellMessage("Area not claimed!", 3, true);
            return;
        }
        if (!area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("Only edit your own areas!", 3, true);
            return;
        }
        currentAreaId = plugin.getDatabase().getAreaIdFromCoords(chunk.x, chunk.y, chunk.z);
        if (currentAreaId == -1) {
            player.sendYellMessage("Error: Area ID not found!", 3, true);
            return;
        }
        updatePermissionButtons();
        worldPlayers.clear();
        worldPlayers.addAll(Arrays.asList(Server.getAllPlayers()));
        currentPlayerIndex = -1;
        updateInfoLabel();
        menuBase.setVisible(false);
        settingsMenu.setVisible(true);
        player.setMouseCursorVisible(true);
    }

    private void updatePermissionButtons() throws SQLException {
        if (currentAreaId == -1) return;
        ResultSet rs = plugin.getDatabase().getDb().executeQuery("SELECT * FROM `GuestEventActions` WHERE AreaID = " + currentAreaId);
        if (rs.next()) {
            for (Map.Entry<String, UILabel> entry : permissionButtons.entrySet()) {
                String perm = entry.getKey();
                boolean state = rs.getBoolean(perm);
                String name = entry.getValue().getText().replace("<b>", "").replace(": Off", "").replace(": On", "").trim();
                entry.getValue().setText("<b>" + name + ": " + (state ? "On" : "Off") + "</b>");
            }
        }
    }

    public void togglePermission(String permission) {
        if (currentAreaId == -1 || !plugin.getClaimedAreaAt(player.getChunkPosition()).playerUID.equals(player.getUID())) {
            player.sendYellMessage("Invalid area or permissions!", 3, true);
            return;
        }
        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    boolean state = plugin.getDatabase().getGuestPermission(currentAreaId, permission);
                    plugin.getDatabase().setGuestPermission(currentAreaId, permission, !state);
                    player.setAttribute("permResult", !state);
                } catch (SQLException e) {
                    player.setAttribute("permResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                Object result = player.getAttribute("permResult");
                if (result instanceof Boolean) {
                    boolean newState = (Boolean) result;
                    UILabel button = permissionButtons.get(permission);
                    String name = button.getText().replace("<b>", "").replace(": Off", "").replace(": On", "").trim();
                    button.setText("<b>" + name + ": " + (newState ? "On" : "Off") + "</b>");
                    player.sendTextMessage(name + " set to: " + (newState ? "On" : "Off"));
                } else {
                    player.sendTextMessage((String) result);
                }
            }
        );
    }

    public void closeSettingsMenu() {
        settingsMenu.setVisible(false);
        menuBase.setVisible(true);
        player.setMouseCursorVisible(true); // Keep cursor visible for base menu
        System.out.println("[PlayerUIMenu] Closing settings menu for " + player.getName());
    }

    public void toggleMenu() {
        isVisible = !isVisible;
        menuBase.setVisible(isVisible);
        player.setMouseCursorVisible(isVisible);
        if (isVisible) {
            // Update playtime and points when menu opens
            plugin.getTaskQueue().queueTask(
                () -> {
                    try {
                        Long loginTime = plugin.playerLoginTimes.get(player);
                        if (loginTime != null) {
                            long sessionMs = System.currentTimeMillis() - loginTime;
                            double sessionHours = sessionMs / 3600000.0;
                            plugin.getDatabase().updatePlaytimeAndPoints(player.getUID(), sessionHours);
                            plugin.playerLoginTimes.put(player, System.currentTimeMillis());
                            System.out.println("[PlayerUIMenu] Updated playtime on menu open for " + player.getName() + ": " + sessionHours + " hours");
                        }
                    } catch (SQLException e) {
                        System.out.println("[PlayerUIMenu] Error updating playtime on menu open: " + e.getMessage());
                    }
                },
                () -> {
                    updateBuyAreaButtonText(); // Refresh points display after update
                    updateAreaVisibility(); // Refresh visibility when menu opens
                }
            );
        }
    }

    public void closeMenu() {
        isVisible = false;
        menuBase.setVisible(false);
        player.setMouseCursorVisible(false);
    }

    public void toggleMyAreas() {
        showingMyAreas = !showingMyAreas;
        if (showingMyAreas) showingAllAreas = false;
        updateAreaVisibility();
        updateLabels();
        player.sendTextMessage("My Areas: " + (showingMyAreas ? "On" : "Off"));
        System.out.println("[PlayerUIMenu] Toggle My Areas: " + showingMyAreas + " for " + player.getName());
    }

    public void toggleAllAreas() {
        showingAllAreas = !showingAllAreas;
        if (showingAllAreas) showingMyAreas = false;
        updateAreaVisibility();
        updateLabels();
        player.sendTextMessage("All Areas: " + (showingAllAreas ? "On" : "Off"));
        System.out.println("[PlayerUIMenu] Toggle All Areas: " + showingAllAreas + " for " + player.getName());
    }

    public void disableAllAreas() {
        showingAllAreas = false;
        updateAreaVisibility();
        updateLabels();
        player.sendTextMessage("All Areas: Off (auto-disabled after 60s)");
        System.out.println("[PlayerUIMenu] Auto-disabled All Areas for " + player.getName());
    }

    private void updateLabels() {
        showMyAreasLabel.setText("<b>Show My Areas\n" + (showingMyAreas ? "On" : "Off") + "</b>");
        showAllAreasLabel.setText("<b>Show All Areas\n" + (showingAllAreas ? "On" : "Off") + "</b>");
    }

    private void updateInfoLabel() {
        if (worldPlayers.isEmpty()) {
            infoLabel.setText("No Players in World");
        } else if (currentPlayerIndex == -1) {
            infoLabel.setText("Select a Player");
        } else {
            Player p = worldPlayers.get(currentPlayerIndex);
            String status = currentAreaId != -1 && isGuest(p) ? " (Guest)" : " (Not Guest)";
            infoLabel.setText("Player: " + p.getName() + " (UID: " + p.getUID() + ")" + status);
        }
    }

    private boolean isGuest(Player p) {
        try {
            ResultSet rs = plugin.getDatabase().getDb().executeQuery(
                "SELECT ID FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + p.getUID() + "'"
            );
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public void updateVisibleAreas(Vector3i currentChunk) {
        visibleAreas.clear();
        int radius = 5;
        String uid = player.getUID();
        for (Map.Entry<Vector3i, LandClaim.ClaimedArea> entry : plugin.claimedAreasByChunk.entrySet()) {
            Vector3i areaChunk = entry.getKey();
            if (Math.abs(areaChunk.x - currentChunk.x) <= radius &&
                Math.abs(areaChunk.y - currentChunk.y) <= radius &&
                Math.abs(areaChunk.z - currentChunk.z) <= radius) {
                visibleAreas.put(areaChunk, entry.getValue());
            }
        }
        System.out.println("[PlayerUIMenu] Updated visible areas for " + player.getName() + " at chunk " + currentChunk + 
                           ": " + visibleAreas.size() + " areas found.");
    }

    public void updateAreaVisibility() {
        plugin.hideAreas(player); // Clear existing visuals
        String uid = player.getUID();

        if (showingMyAreas) {
            visibleAreas.entrySet().stream()
                .filter(e -> e.getValue().playerUID.equals(uid))
                .forEach(e -> plugin.addAreaVisual(e.getValue().areaX, e.getValue().areaY, e.getValue().areaZ, 
                                                  e.getValue().areaName, uid, true, player));
        } else if (showingAllAreas) {
            visibleAreas.forEach((chunk, area) -> 
                plugin.addAreaVisual(area.areaX, area.areaY, area.areaZ, area.areaName, area.playerUID, 
                                     area.playerUID.equals(uid), player));
        }
        System.out.println("[PlayerUIMenu] Updated visibility for " + player.getName() + 
                           ": My Areas=" + showingMyAreas + ", All Areas=" + showingAllAreas);
    }

    public void nextPlayer() {
        if (worldPlayers.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex + 1) % worldPlayers.size();
        updateInfoLabel();
    }

    public void backPlayer() {
        if (worldPlayers.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex - 1 + worldPlayers.size()) % worldPlayers.size();
        updateInfoLabel();
    }

    public void addGuest() {
        Vector3i chunk = player.getChunkPosition();
        LandClaim.ClaimedArea area = plugin.getClaimedAreaAt(chunk);
        if (!validateGuestAction(area)) return;

        Player guest = worldPlayers.get(currentPlayerIndex);
        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    ResultSet rs = plugin.getDatabase().getDb().executeQuery(
                        "SELECT ID FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + guest.getUID() + "'"
                    );
                    if (rs.next()) {
                        player.setAttribute("guestResult", guest.getName() + " already a guest!");
                    } else {
                        plugin.getDatabase().getDb().executeUpdate(
                            "INSERT INTO `Guests` (AreaID, GuestName, PlayerUID) VALUES (" + currentAreaId + ", '" + guest.getName() + "', '" + guest.getUID() + "')"
                        );
                        player.setAttribute("guestResult", guest.getName() + " added as guest!");
                    }
                } catch (SQLException e) {
                    player.setAttribute("guestResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                player.sendYellMessage((String) player.getAttribute("guestResult"), 3, true);
                updateInfoLabel();
            }
        );
    }

    public void removeGuest() {
        Vector3i chunk = player.getChunkPosition();
        LandClaim.ClaimedArea area = plugin.getClaimedAreaAt(chunk);
        if (!validateGuestAction(area)) return;

        Player guest = worldPlayers.get(currentPlayerIndex);
        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    ResultSet rs = plugin.getDatabase().getDb().executeQuery(
                        "SELECT ID FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + guest.getUID() + "'"
                    );
                    if (!rs.next()) {
                        player.setAttribute("guestResult", guest.getName() + " not a guest!");
                    } else {
                        plugin.getDatabase().getDb().executeUpdate(
                            "DELETE FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + guest.getUID() + "'"
                        );
                        player.setAttribute("guestResult", "Removed " + guest.getName() + " as guest!");
                    }
                } catch (SQLException e) {
                    player.setAttribute("guestResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                player.sendYellMessage((String) player.getAttribute("guestResult"), 3, true);
                updateInfoLabel();
            }
        );
    }

    private boolean validateGuestAction(LandClaim.ClaimedArea area) {
        if (area == null || !area.playerUID.equals(player.getUID()) || currentPlayerIndex == -1 || worldPlayers.isEmpty()) {
            player.sendYellMessage("Invalid area or no player selected!", 3, true);
            return false;
        }
        return true;
    }

    public UILabel getClaimButton() { return claimButton; }
    public UILabel getUnclaimButton() { return removeButton; }
    public UILabel getExitButton() { return exitButton; }
    public UILabel getSettingsButton() { return settingsButton; }
    public UILabel getShowMyAreasLabel() { return showMyAreasLabel; }
    public UILabel getShowAllAreasLabel() { return showAllAreasLabel; }
    public UILabel getSettingsExitButton() { return settingsExitButton; }
    public UILabel getBuyAreaButton() { return buyAreaButton; }
}