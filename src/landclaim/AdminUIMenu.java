package landclaim;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.TextAnchor;

public class AdminUIMenu {
    private final LandClaim plugin;
    private final AdminTools adminTools;
    private UIElement adminMenu;
    private UILabel adminToolsButton;
    private UIElement popup;
    private UILabel feedbackPanel;
    private UILabel migrateButton;
    private UILabel maxPlusButton;
    private UILabel maxMinusButton;
    private UILabel exitButton;

    public AdminUIMenu(LandClaim plugin, AdminTools adminTools) {
        this.plugin = plugin;
        this.adminTools = adminTools;
    }

    public void showAdminMenu(Player player) {
        if (!adminTools.isAdmin(player)) {
            player.sendTextMessage("[#FF0000]You don’t have permission to access the admin menu!");
            return;
        }

        if (adminMenu == null) {
            createAdminMenu(player);
        }
        adminMenu.setVisible(true);
    }

    public void showAdminToolsPopup(Player player) {
        if (popup == null) {
            createPopup(player);
        }
        popup.setVisible(true);
        player.setMouseCursorVisible(true);
    }

    private void createAdminMenu(Player player) {
        adminMenu = new UIElement();
        player.addUIElement(adminMenu, UITarget.IngameMenu);
        adminMenu.setSize(206, 66, false);
        adminMenu.setClickable(false);
        adminMenu.setPosition(44, 70, true);
        adminMenu.setBorderEdgeRadius(5.0f, false);
        adminMenu.setBorder(3);
        adminMenu.setBorderColor(888);
        adminMenu.setBackgroundColor(0.1f, 0.1f, 0.1f, 0.95f);
        adminMenu.setVisible(true);

        adminToolsButton = createButton(adminMenu, 10, 10, "<b>Admin Tools</b>", 180, 45);
        player.setAttribute("adminToolsButton", adminToolsButton);
    }

    private void createPopup(Player player) {
        popup = new UIElement();
        player.addUIElement(popup, UITarget.IngameMenu);
        popup.setSize(800, 600, false);
        popup.setClickable(false);
        popup.setPosition(30, 20, true);
        popup.setBorderEdgeRadius(5.0f, false);
        popup.setBorder(3);
        popup.setBorderColor(888);
        popup.setBackgroundColor(0.9f, 0.9f, 0.9f, 0.9f);
        popup.setVisible(true);

        feedbackPanel = createLabel(popup, 10, 5, "Info", 730, 45, false);
        player.setAttribute("FeedBackinfoPanel", feedbackPanel);

        migrateButton = createButton(popup, 10, 60, "<b>Migrate old WP Database</b>", 250, 45);
        maxPlusButton = createButton(popup, 270, 60, "<b>+ MaxAreaAllocation</b>", 250, 45);
        maxMinusButton = createButton(popup, 530, 60, "<b>- MaxAreaAllocation</b>", 250, 45);
        exitButton = createButton(popup, 745, 5, "<b>X</b>", 45, 45);
        exitButton.setFontColor(1.0f, 0.0f, 0.0f, 1.0f);
        exitButton.setFontSize(18);
        exitButton.style.borderBottomWidth.set(2);
        exitButton.hoverStyle.borderBottomWidth.set(2);

        player.setAttribute("migrateButton", migrateButton);
        player.setAttribute("maxPlusButton", maxPlusButton);
        player.setAttribute("maxMinusButton", maxMinusButton);
        player.setAttribute("adminPopupExitButton", exitButton);
    }

    private UILabel createButton(UIElement parent, int x, int y, String text, int width, int height) {
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
}