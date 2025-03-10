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
    private UIElement menuBase;
    private UIElement settingsMenu;
    private UILabel claimButton, removeButton, exitButton, settingsButton, settingsExitButton;
    private UILabel showMyAreasLabel, showAllAreasLabel, infoLabel;
    private UILabel nextPlayerButton, backPlayerButton, addGuestButton, removeGuestButton;
    private boolean isVisible, showingMyAreas = false, showingAllAreas = false;
    private List<Player> worldPlayers = new ArrayList<>();
    private int currentPlayerIndex = -1;
    private int currentAreaId = -1;
    final Map<String, UILabel> permissionButtons = new HashMap<>();

    public PlayerUIMenu(Player player, LandClaim plugin) {
        this.player = player;
        this.plugin = plugin;
        setupBaseMenu();
        setupSettingsMenu();
    }

    private void setupBaseMenu() {
        menuBase = new UIElement();
        player.addUIElement(menuBase);
        menuBase.setSize(276, 320, false);
        menuBase.setClickable(false);
        menuBase.setPosition(45, 45, true);
        menuBase.setBorderEdgeRadius(5.0f, false);
        menuBase.setBorder(3);
        menuBase.setBorderColor(888);
        menuBase.setBackgroundColor(0.1f, 0.1f, 0.1f, 0.9f);
        menuBase.setVisible(false);

        claimButton = createButton(menuBase, 10, 10, "Claim Area", 250, 45);
        removeButton = createButton(menuBase, 10, 60, "Remove Area", 250, 45);
        exitButton = createButton(menuBase, 10, 210, "Exit", 250, 45);
        settingsButton = createButton(menuBase, 10, 260, "Settings", 250, 45);
        showMyAreasLabel = createButton(menuBase, 10, 110, "Show My Areas\nOff", 250, 45);
        showAllAreasLabel = createButton(menuBase, 10, 160, "Show All Areas\nOff", 250, 45);
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

        settingsExitButton = createButton(settingsMenu, 1511, 5, "X", 45, 45);
        settingsExitButton.setFontColor(1.0f, 0.0f, 0.0f, 1.0f);
        settingsExitButton.setFontSize(18);
        settingsExitButton.style.borderBottomWidth.set(2);
        settingsExitButton.hoverStyle.borderBottomWidth.set(2);

        player.setAttribute("settingsExitButton", settingsExitButton);
        player.setAttribute("nextPlayerButton", nextPlayerButton);
        player.setAttribute("backPlayerButton", backPlayerButton);
        player.setAttribute("addGuestButton", addGuestButton);
        player.setAttribute("removeGuestButton", removeGuestButton);

        initializePermissionButtons();
    }

    private void initializePermissionButtons() {
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

        int x = 10, y = 115, itemsPerRow = 5, rowHeight = 55;
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            String displayName = perm.replace("Player", "").replace("UseObject", "").replace("NPC", "");
            UILabel button = createButton(settingsMenu, x + (i % itemsPerRow) * 310, y + (i / itemsPerRow) * rowHeight,
                displayName + ": Off", 300, 45);
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

    public void showSettingsMenu() throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        LandClaim.ClaimedArea area = plugin.getClaimedAreaAt(chunk);

        if (area == null) {
            player.sendYellMessage("This area is not claimed!", 3, true);
            return;
        }
        if (!area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only open Settings in your own area!", 3, true);
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
                String permission = entry.getKey();
                UILabel button = entry.getValue();
                boolean state = rs.getBoolean(permission);
                String displayName = button.getText().replace("<b>", "").replace(": Off", "").replace(": On", "").trim();
                button.setText("<b>" + displayName + ": " + (state ? "On" : "Off") + "</b>");
            }
        }
    }

    public void togglePermission(String permission) {
        if (currentAreaId == -1) {
            player.sendYellMessage("No area selected!", 3, true);
            return;
        }
        Vector3i chunk = player.getChunkPosition();
        LandClaim.ClaimedArea area = plugin.getClaimedAreaAt(chunk);
        if (area == null || !area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only modify permissions in your own area!", 3, true);
            return;
        }

        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    boolean currentState = plugin.getDatabase().getGuestPermission(currentAreaId, permission);
                    boolean newState = !currentState;
                    plugin.getDatabase().setGuestPermission(currentAreaId, permission, newState);
                    player.setAttribute("toggleResult", newState);
                } catch (SQLException e) {
                    player.setAttribute("toggleResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                Object result = player.getAttribute("toggleResult");
                if (result instanceof Boolean) {
                    boolean newState = (Boolean) result;
                    UILabel button = permissionButtons.get(permission);
                    if (button != null) {
                        String displayName = button.getText().replace("<b>", "").replace(": Off", "").replace(": On", "").trim();
                        button.setText("<b>" + displayName + ": " + (newState ? "On" : "Off") + "</b>");
                        player.sendTextMessage(displayName + " for guests set to: " + (newState ? "On" : "Off"));
                    }
                } else {
                    player.sendTextMessage((String) result);
                }
            }
        );
    }

    public void closeSettingsMenu() {
        settingsMenu.setVisible(false);
        menuBase.setVisible(true);
        player.setMouseCursorVisible(true);
    }

    public void toggleMenu() {
        isVisible = !isVisible;
        menuBase.setVisible(isVisible);
        player.setMouseCursorVisible(isVisible);
    }

    public void closeMenu() {
        isVisible = false;
        menuBase.setVisible(false);
        player.setMouseCursorVisible(false);
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void toggleMyAreas() {
        showingMyAreas = !showingMyAreas;
        if (showingMyAreas) showingAllAreas = false;
        plugin.showAllAreas(false, player);
        plugin.showMyAreas(showingMyAreas, player);
        player.sendTextMessage("My Areas visibility set to: " + (showingMyAreas ? "On" : "Off"));
        updateLabels();
    }

    public void toggleAllAreas() {
        showingAllAreas = !showingAllAreas;
        if (showingAllAreas) showingMyAreas = false;
        plugin.showMyAreas(false, player);
        plugin.showAllAreas(showingAllAreas, player);
        player.sendTextMessage("All Areas visibility set to: " + (showingAllAreas ? "On" : "Off"));
        updateLabels();
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
            Player selectedPlayer = worldPlayers.get(currentPlayerIndex);
            String guestStatus = currentAreaId != -1 && isGuest(selectedPlayer) ? " (Guest)" : " (Not a Guest)";
            infoLabel.setText("Player: " + selectedPlayer.getName() + " (UID: " + selectedPlayer.getUID() + ")" + guestStatus);
        }
    }

    private boolean isGuest(Player selectedPlayer) {
        try {
            ResultSet rs = plugin.getDatabase().getDb().executeQuery(
                "SELECT ID FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + selectedPlayer.getUID() + "'"
            );
            return rs.next();
        } catch (SQLException e) {
            System.out.println("[LandClaim] Error checking guest status: " + e.getMessage());
            return false;
        }
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

        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    Player guest = worldPlayers.get(currentPlayerIndex);
                    String guestUID = guest.getUID();
                    String guestName = guest.getName();
                    ResultSet rs = plugin.getDatabase().getDb().executeQuery(
                        "SELECT ID FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + guestUID + "'"
                    );
                    if (rs.next()) {
                        player.setAttribute("guestActionResult", guestName + " is already a guest!");
                    } else {
                        plugin.getDatabase().getDb().executeUpdate(
                            "INSERT INTO `Guests` (AreaID, GuestName, PlayerUID) VALUES (" + currentAreaId + ", '" + guestName + "', '" + guestUID + "')"
                        );
                        player.setAttribute("guestActionResult", guestName + " added as a guest!");
                    }
                } catch (SQLException e) {
                    player.setAttribute("guestActionResult", "Error adding guest: " + e.getMessage());
                }
            },
            () -> {
                String result = (String) player.getAttribute("guestActionResult");
                player.sendYellMessage(result, 3, true);
                updateInfoLabel();
            }
        );
    }

    public void removeGuest() {
        Vector3i chunk = player.getChunkPosition();
        LandClaim.ClaimedArea area = plugin.getClaimedAreaAt(chunk);
        if (!validateGuestAction(area)) return;

        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    Player guest = worldPlayers.get(currentPlayerIndex);
                    String guestUID = guest.getUID();
                    ResultSet rs = plugin.getDatabase().getDb().executeQuery(
                        "SELECT ID FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + guestUID + "'"
                    );
                    if (!rs.next()) {
                        player.setAttribute("guestActionResult", guest.getName() + " is not a guest!");
                    } else {
                        plugin.getDatabase().getDb().executeUpdate(
                            "DELETE FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + guestUID + "'"
                        );
                        player.setAttribute("guestActionResult", "Removed " + guest.getName() + " as a guest!");
                    }
                } catch (SQLException e) {
                    player.setAttribute("guestActionResult", "Error removing guest: " + e.getMessage());
                }
            },
            () -> {
                String result = (String) player.getAttribute("guestActionResult");
                player.sendYellMessage(result, 3, true);
                updateInfoLabel();
            }
        );
    }

    private boolean validateGuestAction(LandClaim.ClaimedArea area) {
        if (area == null) {
            player.sendYellMessage("This area is not claimed!", 3, true);
            return false;
        }
        if (!area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only manage guests in your own area!", 3, true);
            return false;
        }
        if (currentPlayerIndex == -1 || worldPlayers.isEmpty()) {
            player.sendYellMessage("No player selected!", 3, true);
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
}