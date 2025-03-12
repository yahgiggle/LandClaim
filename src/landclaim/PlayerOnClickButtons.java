package landclaim;

import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent; // Corrected import
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import java.sql.SQLException;
import java.util.Map;

public class PlayerOnClickButtons implements Listener {
    private final LandClaim plugin;

    public PlayerOnClickButtons(LandClaim plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.registerEventListener(this);
    }

    @EventMethod
    public void onClick(PlayerUIElementClickEvent event) { // Correct event class
        Player player = event.getPlayer();
        UILabel clickedLabel = (UILabel) event.getUIElement();

        PlayerUIMenu menu = plugin.playerMenus.get(player);
        if (menu == null) {
            System.out.println("[PlayerOnClickButtons] No menu found for player: " + player.getName());
            return;
        }

        System.out.println("[PlayerOnClickButtons] Clicked label text: " + clickedLabel.getText() + " by " + player.getName());

        if (clickedLabel == menu.getClaimButton()) {
            System.out.println("[PlayerOnClickButtons] Claim button clicked by " + player.getName());
            plugin.startClaimMode(player);
        } else if (clickedLabel == menu.getUnclaimButton()) {
            System.out.println("[PlayerOnClickButtons] Unclaim button clicked by " + player.getName());
            plugin.unclaimArea(player);
        } else if (clickedLabel == menu.getExitButton()) {
            System.out.println("[PlayerOnClickButtons] Exit button clicked by " + player.getName());
            menu.closeMenu();
        } else if (clickedLabel == menu.getSettingsButton()) {
            System.out.println("[PlayerOnClickButtons] Settings button clicked by " + player.getName());
            try {
                menu.showSettingsMenu();
            } catch (SQLException e) {
                plugin.showMessage(player, "Error opening settings: " + e.getMessage(), 5.0f);
            }
        } else if (clickedLabel == menu.getShowMyAreasLabel()) {
            System.out.println("[PlayerOnClickButtons] Show My Areas button clicked by " + player.getName());
            menu.toggleMyAreas();
        } else if (clickedLabel == menu.getShowAllAreasLabel()) {
            System.out.println("[PlayerOnClickButtons] Show All Areas button clicked by " + player.getName());
            menu.toggleAllAreas();
        } else if (clickedLabel == menu.getSettingsExitButton()) {
            System.out.println("[PlayerOnClickButtons] Settings Exit button clicked by " + player.getName());
            menu.closeSettingsMenu();
        } else if (clickedLabel == menu.getBuyAreaButton()) {
            System.out.println("[PlayerOnClickButtons] Buy Area button clicked by " + player.getName());
            plugin.buyAreaAllocation(player);
        } else if (clickedLabel == (UILabel) player.getAttribute("nextPlayerButton")) {
            System.out.println("[PlayerOnClickButtons] Next Player button clicked by " + player.getName());
            menu.nextPlayer();
        } else if (clickedLabel == (UILabel) player.getAttribute("backPlayerButton")) {
            System.out.println("[PlayerOnClickButtons] Back Player button clicked by " + player.getName());
            menu.backPlayer();
        } else if (clickedLabel == (UILabel) player.getAttribute("addGuestButton")) {
            System.out.println("[PlayerOnClickButtons] Add Guest button clicked by " + player.getName());
            menu.addGuest();
        } else if (clickedLabel == (UILabel) player.getAttribute("removeGuestButton")) {
            System.out.println("[PlayerOnClickButtons] Remove Guest button clicked by " + player.getName());
            menu.removeGuest();
        } else if (clickedLabel == (UILabel) player.getAttribute("changeAreaNameButton")) {
            System.out.println("[PlayerOnClickButtons] Change Area Name button clicked by " + player.getName());
            menu.changeAreaName();
        } else if (clickedLabel == (UILabel) player.getAttribute("renameButton")) {
            System.out.println("[PlayerOnClickButtons] Rename button clicked by " + player.getName());
            menu.performRename();
        } else if (clickedLabel == (UILabel) player.getAttribute("cancelRenameButton")) {
            System.out.println("[PlayerOnClickButtons] Cancel Rename button clicked by " + player.getName());
            menu.cancelRename();
        } else {
            for (Map.Entry<String, UILabel> entry : menu.permissionButtons.entrySet()) {
                if (clickedLabel == entry.getValue()) {
                    System.out.println("[PlayerOnClickButtons] Permission button clicked: " + entry.getKey() + " by " + player.getName());
                    menu.togglePermission(entry.getKey());
                    break;
                }
            }
        }
    }
}