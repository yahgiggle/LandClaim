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

    public AdminUIMenu(LandClaim plugin, AdminTools adminTools) {
        this.plugin = plugin;
        this.adminTools = adminTools;
    }

    public void showAdminMenu(Player player) {
        if (!adminTools.isAdmin(player)) {
            player.sendTextMessage("[#FF0000]No admin permission!");
            return;
        }

        UIElement menu = new UIElement();
        player.addUIElement(menu, UITarget.IngameMenu);
        menu.setSize(206, 66, false);
        menu.setClickable(false);
        menu.setPosition(44, 70, true);
        menu.setBorderEdgeRadius(5.0f, false);
        menu.setBorder(3);
        menu.setBorderColor(888);
        menu.setBackgroundColor(0.1f, 0.1f, 0.1f, 0.95f);
        menu.setVisible(true);
        menu.style.borderBottomWidth.set(5);
        

        UILabel button = createButton(menu, 10, 10, "<b>Admin Tools</b>", 180, 45);
        player.setAttribute("adminToolsButton", button);
    }

    public void showAdminToolsPopup(Player player) {
        UIElement popup = new UIElement();
        player.addUIElement(popup, UITarget.IngameMenu);
        popup.setSize(800, 600, false);
        popup.setClickable(false);
        popup.setPosition(30, 20, true);
        popup.setBorderEdgeRadius(5.0f, false);
        popup.setBorder(3);
        popup.setBorderColor(888);
        popup.setBackgroundColor(0.9f, 0.9f, 0.9f, 0.9f);
        popup.setVisible(true);

        UILabel feedback = createLabel(popup, 10, 5, "Info", 730, 45, false);
        player.setAttribute("FeedBackinfoPanel", feedback);

        player.setAttribute("migrateButton", createButton(popup, 10, 60, "<b>Migrate old WP Database</b>", 250, 45));
        player.setAttribute("maxPlusButton", createButton(popup, 270, 60, "<b>+ MaxAreaAllocation</b>", 250, 45));
        player.setAttribute("maxMinusButton", createButton(popup, 530, 60, "<b>- MaxAreaAllocation</b>", 250, 45));
        player.setAttribute("adminPopupExitButton", createButton(popup, 745, 5, "<color=red><b>X</b></color>", 45, 45));
        
        
     //   UILabel exit = createButton(popup, 745, 5, "<b>X</b>", 45, 45);
     //   exit.setFontColor(1.0f, 0.0f, 0.0f, 1.0f);
     //   exit.setFontSize(18);
      //  player.setAttribute("adminPopupExitButton", exit);
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

