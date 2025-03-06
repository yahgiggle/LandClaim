package landclaim;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.TextAnchor;
import java.sql.SQLException;
import net.risingworld.api.utils.Vector3i;

public class PlayerUIMenu {
    private final Player player;
    private UIElement menuBase;
    private UIElement settingsMenu; // New Settings UI
    private UILabel claimButton;
    private UILabel removeButton;
    private UILabel exitButton;
    private UILabel settingsButton; // Renamed to match camelCase convention
    private UILabel settingsExitButton;
    
    private UILabel showMyAreasLabel;
    private UILabel showAllAreasLabel;
    private boolean isVisible;
    private boolean showingMyAreas = false;
    private boolean showingAllAreas = false;
    private LandClaim plugin;

    public PlayerUIMenu(Player player, LandClaim plugin) {
        this.player = player;
        this.plugin = plugin;
        this.isVisible = false;
        setupBaseMenu();
        setupSettingsMenu(); // Initialize the Settings UI
    }

    private void setupBaseMenu() {
        menuBase = new UIElement();
        player.addUIElement(menuBase);
        menuBase.setSize(206, 320, false);
        menuBase.setClickable(false);
        menuBase.setPosition(45, 45, true);
        menuBase.setBorderEdgeRadius(5.0f, false);
        menuBase.setBorder(3);
        menuBase.setBorderColor(888);
        menuBase.setBackgroundColor(0.1f, 0.1f, 0.1f, 0.9f);
        menuBase.setVisible(false);

        initClaimButton();
        initRemoveButton();
        initExitButton();
        initSettingsButton();
        initShowMyAreasLabel();
        initShowAllAreasLabel();
    }

    private void setupSettingsMenu() {
        settingsMenu = new UIElement();
        player.addUIElement(settingsMenu);
        settingsMenu.setSize(1280, 800, false);
        settingsMenu.setClickable(false);
        settingsMenu.setPosition(10, 10, true); // Centered-ish on screen
        settingsMenu.setBorderEdgeRadius(5.0f, false);
        settingsMenu.setBorder(3);
        settingsMenu.setBorderColor(888);
        settingsMenu.setBackgroundColor(0.1f, 0.1f, 0.1f, 0.95f);
        settingsMenu.setVisible(false);

        // Add Exit button for Settings menu
        
        initSettingsExitButton();
                
       // settingsMenu.addChild(settingsExitButton);

        // Store the settings exit button in player attributes for click detection
        player.setAttribute("settingsExitButton", settingsExitButton);
    }

    public UILabel createButton(int x, int y, int width, int height, String text) {
        UILabel button = new UILabel();
        button.setClickable(true);
        button.setText(text);
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

        menuBase.addChild(button);
        return button;
    }

    public void initClaimButton() {
        claimButton = createButton(10, 10, 180, 45, "<b>Claim Area</b>");
        claimButton.setVisible(true);
    }

    public void initRemoveButton() {
        removeButton = createButton(10, 60, 180, 45, "<b>Remove Area</b>");
        removeButton.setVisible(true);
    }

    public void initShowMyAreasLabel() {
        showMyAreasLabel = createButton(10, 110, 180, 45, "Show My Areas\nOff");
        showMyAreasLabel.setVisible(true);
    }

    public void initShowAllAreasLabel() {
        showAllAreasLabel = createButton(10, 160, 180, 45, "Show All Areas\nOff");
        showAllAreasLabel.setVisible(true);
    }

    public void initExitButton() {
        exitButton = createButton(10, 210, 180, 45, "<b>Exit</b>");
        exitButton.setVisible(true);
    }

    public void initSettingsButton() {
        settingsButton = createButton(10, 260, 180, 45, "<b>Settings</b>");
        settingsButton.setVisible(true);
    }
    
    
    
    
    public void initSettingsExitButton() {
        settingsExitButton = createButton(10, 210, 180, 45, "<b>X</b>");
        settingsExitButton.setClickable(true);
        settingsExitButton.setFont(Font.Medieval);
        settingsExitButton.style.textAlign.set(TextAnchor.MiddleCenter);
        settingsExitButton.setFontColor(1.0f, 0.0f, 0.0f, 1.0f); // Red for exit
        settingsExitButton.setFontSize(18);
        settingsExitButton.setSize(45, 45, false);
        settingsExitButton.setBorder(2);
        settingsExitButton.setBorderColor(999);
        settingsExitButton.setBackgroundColor(500);
        settingsExitButton.setPosition(1230, 5, false); // Top-right corner
        settingsExitButton.style.borderBottomWidth.set(2);
        settingsExitButton.hoverStyle.backgroundColor.set(0.2f, 0.2f, 0.2f, 0.9f);
        settingsExitButton.hoverStyle.borderBottomWidth.set(2);
        settingsExitButton.hoverStyle.borderBottomColor.set(0.1f, 0.1f, 0.9f, 0.9f);
        settingsExitButton.setVisible(true);
        settingsMenu.addChild(settingsExitButton);
    }
    
    
    
    
    
    
    

    public UILabel getClaimButton() {
        return claimButton;
    }

    public UILabel getUnclaimButton() {
        return removeButton;
    }

    public UILabel getExitButton() {
        return exitButton;
    }

    public UILabel getSettingsButton() {
        return settingsButton;
    }

    public UILabel getShowMyAreasLabel() {
        return showMyAreasLabel;
    }

    public UILabel getShowAllAreasLabel() {
        return showAllAreasLabel;
    }
    
     public UILabel getSettingsExitButton() {
        return settingsExitButton;
    }
    
    

    public void showSettingsMenu() throws SQLException {
        
        player.sendYellMessage("Click Test", 3, true);
        // Check if player is in their own area
        Vector3i chunk = player.getChunkPosition();
        int areaX = chunk.x;
        int areaY = chunk.y;
        int areaZ = chunk.z;
        String key = plugin.getKeyFromCoords(areaX, areaY, areaZ);
        LandClaim.ClaimedArea area = plugin.claimedAreas.get(key);

        if (area == null) {
            player.sendYellMessage("This area is not claimed!", 3, true);
            return;
        }

        if (!area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only open Settings in your own area!", 3, true);
            return;
        }

        // Hide the main menu and show the settings menu
        menuBase.setVisible(false);
        settingsMenu.setVisible(true);
        player.setMouseCursorVisible(true);
    }

    
    
    
    public void closeSettingsMenu() {
        // Hide the settings menu and show the main menu
        settingsMenu.setVisible(false);
        menuBase.setVisible(true);
        player.setMouseCursorVisible(true); // Keep cursor visible since we're back in the main menu
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
        if (showingMyAreas) {
            showingAllAreas = false; // Turn off Show All Areas
            plugin.showMyAreas(true, player);
            plugin.showAllAreas(false, player);
            plugin.showMyAreas(showingMyAreas, player);
        } else {
            plugin.showMyAreas(false, player);
        }
        updateLabels();
    }

    public void toggleAllAreas() {
        showingAllAreas = !showingAllAreas;
        if (showingAllAreas) {
            showingMyAreas = false; // Turn off Show My Areas
            plugin.showAllAreas(true, player);
            plugin.showMyAreas(false, player);
            plugin.showAllAreas(showingAllAreas, player);
        } else {
            plugin.showAllAreas(false, player);
        }
        updateLabels();
    }

    private void updateLabels() {
        showMyAreasLabel.setText("Show My Areas\n" + (showingMyAreas ? "On" : "Off"));
        showAllAreasLabel.setText("Show All Areas\n" + (showingAllAreas ? "On" : "Off"));
    }
}
