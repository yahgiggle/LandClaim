package landclaim;

import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.Server;
import java.sql.SQLException;

public class AdminOnClickButtons implements Listener {
    private final LandClaim plugin;
    private final AdminTools adminTools;
    private long lastClickTime = 0;

    public AdminOnClickButtons(LandClaim plugin, AdminTools adminTools) {
        this.plugin = plugin;
        this.adminTools = adminTools;
    }

    @EventMethod
    public void onPlayerUIElementClick(PlayerUIElementClickEvent event) {
        Player player = event.getPlayer();
        UILabel element = (UILabel) event.getUIElement();
        if (element == null || !adminTools.isAdmin(player)) return;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 500) return;
        lastClickTime = now;

        UILabel feedback = (UILabel) player.getAttribute("FeedBackinfoPanel");

        if (element.equals(player.getAttribute("adminToolsButton"))) {
            new AdminUIMenu(plugin, adminTools).showAdminToolsPopup(player);
            player.setMouseCursorVisible(true);
        } else if (element.equals(player.getAttribute("adminPopupExitButton"))) {
            element.getParent().setVisible(false);
            player.setMouseCursorVisible(false);
        } else if (element.equals(player.getAttribute("migrateButton"))) {
            if (feedback != null) feedback.setText("Migration started...");
            plugin.getTaskQueue().queueTask(
                () -> adminTools.migrateDatabase(player),
                () -> { if (feedback != null) feedback.setText(adminTools.getLastMigrationResult()); }
            );
        } else if (element.equals(player.getAttribute("maxPlusButton"))) {
            handleMaxAreaAdjustment(player, 1, feedback);
        } else if (element.equals(player.getAttribute("maxMinusButton"))) {
            handleMaxAreaAdjustment(player, -1, feedback);
        } else if (element.equals(player.getAttribute("pointsPlusButton"))) {
            handlePointsAdjustment(player, 1, feedback);
        } else if (element.equals(player.getAttribute("pointsMinusButton"))) {
            handlePointsAdjustment(player, -1, feedback);
        } else if (element.equals(player.getAttribute("costPlusButton"))) {
            handleCostAdjustment(player, 1, feedback);
        } else if (element.equals(player.getAttribute("costMinusButton"))) {
            handleCostAdjustment(player, -1, feedback);
        }
    }

    private void handleMaxAreaAdjustment(Player player, int adjustment, UILabel feedback) {
        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    String uid = plugin.getAreaOwnerUIDFromPosition(player);
                    if (uid == null) {
                        player.setAttribute("adjustResult", "Stand in a claimed area!");
                        return;
                    }
                    int current = plugin.getDatabase().getMaxAreaAllocation(uid);
                    int newMax = current + adjustment;
                    if (newMax < 0) {
                        player.setAttribute("adjustResult", "Cannot go below 0!");
                        return;
                    }
                    plugin.getDatabase().setMaxAreaAllocation(uid, newMax);
                    Player[] allPlayers = Server.getAllPlayers();
                    for (Player owner : allPlayers) {
                        if (owner.getUID().equals(uid)) {
                            owner.sendTextMessage("Max areas updated to " + newMax + " by admin.");
                            break;
                        }
                    }
                    player.setAttribute("adjustResult", "Max areas for UID " + uid + " set to " + newMax);
                } catch (SQLException e) {
                    player.setAttribute("adjustResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                if (feedback != null) {
                    String result = (String) player.getAttribute("adjustResult");
                    feedback.setText(result != null ? result : "Adjustment failed!");
                }
            }
        );
    }

    private void handlePointsAdjustment(Player player, int adjustment, UILabel feedback) {
        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    int current = plugin.getDatabase().getPointsEarnedAdjust();
                    int newValue = current + adjustment;
                    if (newValue < 0) {
                        player.setAttribute("pointsResult", "Points per hour cannot go below 0!");
                        return;
                    }
                    plugin.getDatabase().setPointsEarnedAdjust(newValue);
                    player.setAttribute("pointsResult", "Points per hour set to " + newValue);
                } catch (SQLException e) {
                    player.setAttribute("pointsResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                if (feedback != null) {
                    String result = (String) player.getAttribute("pointsResult");
                    feedback.setText(result != null ? result : "Points adjustment failed!");
                }
            }
        );
    }

    private void handleCostAdjustment(Player player, int adjustment, UILabel feedback) {
        plugin.getTaskQueue().queueTask(
            () -> {
                try {
                    int current = plugin.getDatabase().getAreaCostAdjust();
                    int newValue = current + adjustment;
                    if (newValue < 1) {
                        player.setAttribute("costResult", "Area cost cannot go below 1!");
                        return;
                    }
                    plugin.getDatabase().setAreaCostAdjust(newValue);
                    player.setAttribute("costResult", "Area cost set to " + newValue);
                } catch (SQLException e) {
                    player.setAttribute("costResult", "Error: " + e.getMessage());
                }
            },
            () -> {
                if (feedback != null) {
                    String result = (String) player.getAttribute("costResult");
                    feedback.setText(result != null ? result : "Cost adjustment failed!");
                }
            }
        );
    }

    public void register() {
        plugin.registerEventListener(this);
    }
}