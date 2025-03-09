package landclaim;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;

public class PlayerOnClickButtons implements Listener {
    private final LandClaim plugin;
    private boolean isRegistered = false;
    private Map<Player, Map<UILabel, Long>> lastClickTimes = new HashMap<>();

    public PlayerOnClickButtons(LandClaim plugin) {
        this.plugin = plugin;
    }

    @EventMethod
    public void onUIElementClick(PlayerUIElementClickEvent event) throws SQLException {
        Player player = event.getPlayer();
        PlayerUIMenu menu = plugin.playerMenus.get(player);
        PlayerTools tools = plugin.getPlayerTools().get(player);
        if (tools == null || menu == null) return;

        UIElement element = event.getUIElement();
        if (!(element instanceof UILabel)) return;
        UILabel clickedElement = (UILabel) element;

        long currentTime = System.currentTimeMillis();
        Map<UILabel, Long> playerClickTimes = lastClickTimes.computeIfAbsent(player, k -> new HashMap<>());
        long lastClickTime = playerClickTimes.getOrDefault(clickedElement, 0L);

        if (currentTime - lastClickTime < 300) {
            System.out.println("[PlayerOnClickButtons] Ignored rapid click on " + clickedElement + " for " + player.getName() +
                              ", too soon after last click (" + (currentTime - lastClickTime) + "ms)");
            return;
        }
        playerClickTimes.put(clickedElement, currentTime);

        System.out.println("[PlayerOnClickButtons] Processing click for " + player.getName() + " on UIElement: " + clickedElement);

        if (clickedElement == tools.getClaimButton()) {
            System.out.println("[PlayerOnClickButtons] Claim button clicked");
            player.sendTextMessage("Claiming area...");
            plugin.startClaimMode(player);
        } else if (clickedElement == tools.getUnclaimButton()) {
            System.out.println("[PlayerOnClickButtons] Unclaim button clicked");
            player.sendTextMessage("Unclaiming area...");
            plugin.unclaimArea(player);
        } else if (clickedElement == tools.getExitButton()) {
            System.out.println("[PlayerOnClickButtons] Exit button clicked");
            menu.closeMenu();
        } else if (menu != null) {
            if (clickedElement == menu.getShowMyAreasLabel()) {
                System.out.println("[PlayerOnClickButtons] Show My Areas button clicked");
                menu.toggleMyAreas();
                player.sendTextMessage("Toggled My Areas visibility.");
            } else if (clickedElement == menu.getShowAllAreasLabel()) {
                System.out.println("[PlayerOnClickButtons] Show All Areas button clicked");
                menu.toggleAllAreas();
                player.sendTextMessage("Toggled All Areas visibility.");
            } else if (clickedElement == menu.getSettingsButton()) {
                System.out.println("[PlayerOnClickButtons] Settings button clicked");
                menu.showSettingsMenu();
            } else if (clickedElement == menu.getSettingsExitButton()) {
                System.out.println("[PlayerOnClickButtons] Settings Exit button clicked");
                menu.closeSettingsMenu();
            } else if (clickedElement == player.getAttribute("nextPlayerButton")) {
                System.out.println("[PlayerOnClickButtons] Next Player button clicked");
                menu.nextPlayer();
            } else if (clickedElement == player.getAttribute("backPlayerButton")) {
                System.out.println("[PlayerOnClickButtons] Back Player button clicked");
                menu.backPlayer();
            } else if (clickedElement == player.getAttribute("addGuestButton")) {
                System.out.println("[PlayerOnClickButtons] Add Guest button clicked");
                menu.addGuest();
            } else if (clickedElement == player.getAttribute("removeGuestButton")) {
                System.out.println("[PlayerOnClickButtons] Remove Guest button clicked");
                menu.removeGuest();
            } else {
                // Handle all permission buttons
                for (Map.Entry<String, UILabel> entry : menu.permissionButtons.entrySet()) {
                    if (clickedElement == entry.getValue()) {
                        System.out.println("[PlayerOnClickButtons] Permission button clicked: " + entry.getKey());
                        menu.togglePermission(entry.getKey());
                        break;
                    }
                }
            }
        }
    }

    public void register() {
        if (!isRegistered) {
            plugin.registerEventListener(this);
            isRegistered = true;
            System.out.println("[PlayerOnClickButtons] Registered listener for " + this);
        } else {
            System.out.println("[PlayerOnClickButtons] Already registered, skipping.");
        }
    }
}

