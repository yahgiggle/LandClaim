package landclaim;

import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PlayerOnClickButtons implements Listener {
    private final LandClaim plugin;
    private final Map<Player, Map<UILabel, Long>> lastClickTimes = new HashMap<>();

    public PlayerOnClickButtons(LandClaim plugin) {
        this.plugin = plugin;
    }

    @EventMethod
    public void onPlayerUIElementClick(PlayerUIElementClickEvent event) {
        Player player = event.getPlayer();
        UILabel element = (UILabel) event.getUIElement();
        if (element == null) {
            System.out.println("[PlayerOnClickButtons] Element is null for " + player.getName());
            return;
        }

        long now = System.currentTimeMillis();
        Map<UILabel, Long> clicks = lastClickTimes.computeIfAbsent(player, k -> new HashMap<>());
        if (now - clicks.getOrDefault(element, 0L) < 300) return;
        clicks.put(element, now);

        PlayerUIMenu menu = plugin.playerMenus.get(player);
        PlayerTools tools = plugin.getPlayerTools().get(player);
        if (menu == null || tools == null) {
            System.out.println("[PlayerOnClickButtons] Menu or Tools null for " + player.getName());
            return;
        }

        System.out.println("[PlayerOnClickButtons] Clicked element: " + element.getText() + " for " + player.getName());

        if (element == tools.getClaimButton()) {
            plugin.showMessage(player, "Claiming area...", 5.0f);
            plugin.startClaimMode(player);
        } else if (element == tools.getUnclaimButton()) {
            plugin.showMessage(player, "Unclaiming area...", 5.0f);
            plugin.unclaimArea(player);
        } else if (element == tools.getExitButton()) {
            menu.closeMenu();
        } else if (element == menu.getSettingsButton()) {
            try {
                menu.showSettingsMenu();
            } catch (SQLException e) {
                plugin.showMessage(player, "Error opening settings: " + e.getMessage(), 5.0f);
            }
        } else if (element == menu.getSettingsExitButton()) {
            System.out.println("[PlayerOnClickButtons] Settings exit clicked for " + player.getName());
            menu.closeSettingsMenu();
        } else if (element == menu.getShowMyAreasLabel()) {
            menu.toggleMyAreas();
        } else if (element == menu.getShowAllAreasLabel()) {
            menu.toggleAllAreas();
        } else if (element == menu.getBuyAreaButton()) {
            System.out.println("[PlayerOnClickButtons] Buy Area clicked for " + player.getName());
            plugin.showMessage(player, "Updating points and processing purchase...", 5.0f);
            plugin.buyAreaAllocation(player);
        } else if (element == player.getAttribute("nextPlayerButton")) {
            menu.nextPlayer();
        } else if (element == player.getAttribute("backPlayerButton")) {
            menu.backPlayer();
        } else if (element == player.getAttribute("addGuestButton")) {
            menu.addGuest();
        } else if (element == player.getAttribute("removeGuestButton")) {
            menu.removeGuest();
        } else {
            for (Map.Entry<String, UILabel> entry : menu.permissionButtons.entrySet()) {
                if (element == entry.getValue()) {
                    menu.togglePermission(entry.getKey());
                    break;
                }
            }
            System.out.println("[PlayerOnClickButtons] Unhandled element: " + element.getText());
        }
    }

    public void register() {
        plugin.registerEventListener(this);
    }
}

