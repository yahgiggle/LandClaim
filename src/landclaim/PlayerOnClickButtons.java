package landclaim;

import java.sql.SQLException;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;

import java.util.HashMap;
import java.util.Map;

public class PlayerOnClickButtons implements Listener {
    private final LandClaim plugin;
    private boolean isRegistered = false;
    private final Map<Player, Map<UILabel, Long>> lastClickTimes = new HashMap<>();
    private static final long CLICK_COOLDOWN_MS = 300;

    public PlayerOnClickButtons(LandClaim plugin) {
        this.plugin = plugin;
    }

    @EventMethod
    public void onUIElementClick(PlayerUIElementClickEvent event) throws SQLException {
        Player player = event.getPlayer();
        UIElement element = event.getUIElement();
        if (!(element instanceof UILabel)) return;

        UILabel clickedElement = (UILabel) element;
        long currentTime = System.currentTimeMillis();
        Map<UILabel, Long> playerClickTimes = lastClickTimes.computeIfAbsent(player, k -> new HashMap<>());
        long lastClickTime = playerClickTimes.getOrDefault(clickedElement, 0L);

        if (currentTime - lastClickTime < CLICK_COOLDOWN_MS) {
            System.out.println("[PlayerOnClickButtons] Ignored rapid click for " + player.getName() + " on " + clickedElement.getText());
            return;
        }
        playerClickTimes.put(clickedElement, currentTime);

        PlayerUIMenu menu = plugin.playerMenus.get(player);
        PlayerTools tools = plugin.getPlayerTools().get(player);
        if (tools == null || menu == null) return;

        System.out.println("[PlayerOnClickButtons] Processing click for " + player.getName() + " on " + clickedElement.getText());

        if (clickedElement == tools.getClaimButton()) {
            player.sendTextMessage("Claiming area...");
            plugin.startClaimMode(player);
        } else if (clickedElement == tools.getUnclaimButton()) {
            player.sendTextMessage("Unclaiming area...");
            plugin.unclaimArea(player);
        } else if (clickedElement == tools.getExitButton()) {
            menu.closeMenu();
        } else {
            handleMenuButtons(player, menu, clickedElement);
        }
    }

    private void handleMenuButtons(Player player, PlayerUIMenu menu, UILabel clickedElement) {
        if (clickedElement == menu.getShowMyAreasLabel()) {
            menu.toggleMyAreas();
            player.sendTextMessage("Toggled My Areas visibility.");
        } else if (clickedElement == menu.getShowAllAreasLabel()) {
            menu.toggleAllAreas();
            player.sendTextMessage("Toggled All Areas visibility.");
        } else if (clickedElement == menu.getSettingsButton()) {
            try {
                menu.showSettingsMenu();
            } catch (Exception e) {
                player.sendTextMessage("Error opening settings: " + e.getMessage());
            }
        } else if (clickedElement == menu.getSettingsExitButton()) {
            menu.closeSettingsMenu();
        } else if (clickedElement == player.getAttribute("nextPlayerButton")) {
            menu.nextPlayer();
        } else if (clickedElement == player.getAttribute("backPlayerButton")) {
            menu.backPlayer();
        } else if (clickedElement == player.getAttribute("addGuestButton")) {
            try {
                menu.addGuest();
            } catch (Exception e) {
                player.sendTextMessage("Error adding guest: " + e.getMessage());
            }
        } else if (clickedElement == player.getAttribute("removeGuestButton")) {
            try {
                menu.removeGuest();
            } catch (Exception e) {
                player.sendTextMessage("Error removing guest: " + e.getMessage());
            }
        } else {
            menu.permissionButtons.entrySet().stream()
                .filter(entry -> clickedElement == entry.getValue())
                .findFirst()
                .ifPresent(entry -> {
                    try {
                        menu.togglePermission(entry.getKey());
                    } catch (Exception e) {
                        player.sendTextMessage("Error toggling permission: " + e.getMessage());
                    }
                });
        }
    }

    public void register() {
        if (!isRegistered) {
            plugin.registerEventListener(this);
            isRegistered = true;
            System.out.println("[PlayerOnClickButtons] Registered listener.");
        }
    }
}