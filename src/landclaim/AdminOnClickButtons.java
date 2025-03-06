package landclaim;

import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.objects.Player;
import java.sql.SQLException;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;

public class AdminOnClickButtons extends LandClaim {
    private final LandClaim plugin;
    private final AdminTools adminTools;

    public AdminOnClickButtons(LandClaim plugin, AdminTools adminTools) {
        this.plugin = plugin;
        this.adminTools = adminTools;
    }

    @EventMethod
    public void onPlayerUIElementClick(PlayerUIElementClickEvent event) throws SQLException {
        Player player = event.getPlayer();
        UILabel element = (UILabel) event.getUIElement();
        UILabel FeedBackinfoPanel = (UILabel) player.getAttribute("FeedBackinfoPanel");

        if (element != null && element.getText().equals("<b>Admin Tools</b>")) {
            AdminUIMenu adminMenu = new AdminUIMenu(this, adminTools);
            adminMenu.showAdminToolsPopup(player);
            player.setMouseCursorVisible(true);
        } else if (element != null && element.equals(player.getAttribute("adminPopupExitButton"))) {
            // Use the specific admin Exit button reference instead of text matching
            UIElement ThisParent = element.getParent();
            ThisParent.setVisible(false); // Only hide the parent (admin popup)
            player.setMouseCursorVisible(false);
        } else if (element != null && element.getText().equals("<b>Migrate old WP Database</b>")) {
            adminTools.migrateDatabase(player);
        } else if (element != null && element.getText().equals("<b>+ MaxAreaAllocation</b>")) {
            handleMaxAreaAdjustment(player, 1, FeedBackinfoPanel);
        } else if (element != null && element.getText().equals("<b>- MaxAreaAllocation</b>")) {
            handleMaxAreaAdjustment(player, -1, FeedBackinfoPanel);
        }
    }

    private void handleMaxAreaAdjustment(Player admin, int adjustment, UILabel FeedBackinfoPanel) throws SQLException {
        String areaOwnerUID = plugin.getAreaOwnerUIDFromPosition(admin);
        if (areaOwnerUID == null) {
            if (FeedBackinfoPanel != null) {
                FeedBackinfoPanel.setText("You must be inside a claimed area to adjust MaxAreaAllocation!");
            }
            return;
        }

        int currentMax = plugin.getDatabase().getMaxAreaAllocation(areaOwnerUID);
        int newMax = currentMax + adjustment;

        if (newMax < 0) {
            if (FeedBackinfoPanel != null) {
                FeedBackinfoPanel.setText("MaxAreaAllocation cannot go below 0!");
            }
            return;
        }

        plugin.getDatabase().setMaxAreaAllocation(areaOwnerUID, newMax);
        if (FeedBackinfoPanel != null) {
            FeedBackinfoPanel.setText("MaxAreaAllocation for player UID " + areaOwnerUID + " updated to " + newMax);
        }
    }

    public void register() {
        plugin.registerEventListener(this);
    }
}