package landclaim;

import net.risingworld.api.Server;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class AdminOnClickButtons implements Listener {
    private final LandClaim plugin;
    private final AdminTools adminTools;

    public AdminOnClickButtons(LandClaim plugin, AdminTools adminTools) {
        this.plugin = plugin;
        this.adminTools = adminTools;
    }

    @EventMethod
    public void onPlayerUIElementClick(PlayerUIElementClickEvent event) {
        Player player = event.getPlayer();
        UILabel element = (UILabel) event.getUIElement();
        UILabel feedbackPanel = (UILabel) player.getAttribute("FeedBackinfoPanel");

        if (element == null) return;

        String elementText = element.getText();
        if (elementText.equals("<b>Admin Tools</b>")) {
            AdminUIMenu adminMenu = new AdminUIMenu(plugin, adminTools);
            adminMenu.showAdminToolsPopup(player);
            player.setMouseCursorVisible(true);
        } else if (element.equals(player.getAttribute("adminPopupExitButton"))) {
            element.getParent().setVisible(false);
            player.setMouseCursorVisible(false);
        } else if (elementText.equals("<b>Migrate old WP Database</b>")) {
            if (feedbackPanel != null) {
                feedbackPanel.setText("Migration started...");
            }
            plugin.getTaskQueue().queueTask(
                () -> {
                    adminTools.migrateDatabase(player); // No return needed
                },
                () -> {
                    String feedback = adminTools.getLastMigrationResult();
                    if (feedbackPanel != null) {
                        feedbackPanel.setText(feedback);
                    }
                }
            );
        } else if (elementText.equals("<b>+ MaxAreaAllocation</b>")) {
            handleMaxAreaAdjustment(player, 1, feedbackPanel);
        } else if (elementText.equals("<b>- MaxAreaAllocation</b>")) {
            handleMaxAreaAdjustment(player, -1, feedbackPanel);
        }
    }

    private void handleMaxAreaAdjustment(Player admin, int adjustment, UILabel feedbackPanel) {
        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    String areaOwnerUID = plugin.getAreaOwnerUIDFromPosition(admin);
                    if (areaOwnerUID == null) {
                        admin.setAttribute("lastAdjustmentResult", "You must be inside a claimed area to adjust MaxAreaAllocation!");
                        return;
                    }

                    int currentMax = plugin.getDatabase().getMaxAreaAllocation(areaOwnerUID);
                    int newMax = currentMax + adjustment;

                    if (newMax < 0) {
                        admin.setAttribute("lastAdjustmentResult", "MaxAreaAllocation cannot go below 0!");
                        return;
                    }

                    plugin.getDatabase().setMaxAreaAllocation(areaOwnerUID, newMax);
                    Player owner = Server.getPlayerByUID(areaOwnerUID); // Use Server static method
                    if (owner != null) {
                        owner.sendTextMessage("Your MaxAreaAllocation has been updated to " + newMax + " by an admin.");
                    }
                    admin.setAttribute("lastAdjustmentResult", "MaxAreaAllocation for player UID " + areaOwnerUID + " updated to " + newMax);
                } catch (Exception e) {
                    admin.setAttribute("lastAdjustmentResult", "Error adjusting MaxAreaAllocation: " + e.getMessage());
                }
            },
            () -> {
                if (feedbackPanel != null) {
                    String result = (String) admin.getAttribute("lastAdjustmentResult");
                    feedbackPanel.setText(result != null ? result : "Adjustment failed!");
                }
            }
        );
    }

    public void register() {
        plugin.registerEventListener(this);
    }
}