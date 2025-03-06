package landclaim;

import java.sql.SQLException;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.ui.PlayerUIElementClickEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class PlayerOnClickButtons implements Listener {
    private final PlayerTools tools;
    private final LandClaim plugin;

    public PlayerOnClickButtons(LandClaim plugin, PlayerTools tools) {
        this.tools = tools;
        this.plugin = plugin;
    }

    @EventMethod
    public void onUIElementClick(PlayerUIElementClickEvent event) throws SQLException {
        Player player = event.getPlayer();
        PlayerUIMenu menu = plugin.playerMenus.get(player);

        if (event.getUIElement() == tools.getClaimButton()) {
            player.sendTextMessage("Claiming area...");
            plugin.startClaimMode(player);
        } else if (event.getUIElement() == tools.getUnclaimButton()) {
            player.sendTextMessage("Unclaiming area...");
            plugin.unclaimArea(player);
        } else if (event.getUIElement() == tools.getExitButton()) {
            if (menu != null) {
                menu.closeMenu();
            }
        } else if (menu != null) {
            if (event.getUIElement() == menu.getShowMyAreasLabel()) {
                menu.toggleMyAreas();
                player.sendTextMessage("Toggled My Areas visibility.");
            } else if (event.getUIElement() == menu.getShowAllAreasLabel()) {
                menu.toggleAllAreas();
                player.sendTextMessage("Toggled All Areas visibility.");
            } else if (event.getUIElement() == menu.getSettingsButton()) {
                menu.showSettingsMenu();
            } else if (event.getUIElement() == player.getAttribute("settingsExitButton")) {
                menu.closeSettingsMenu();
            }
        }
    }

    public void register() {
        plugin.registerEventListener(this);
    }
}