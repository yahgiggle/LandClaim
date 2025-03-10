package landclaim;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class PlayerTools {
    private final Player player;
    private PlayerUIMenu uiMenu;
    private UILabel claimButton;
    private UILabel unclaimButton;
    private UILabel exitButton;

    public PlayerTools(Player player) {
        this.player = player;
    }

    public void initTools(PlayerUIMenu uiMenu) {
        if (uiMenu == null) {
            System.out.println("[PlayerTools] Warning: Attempted to initialize with null UI menu for " + player.getName());
            return;
        }
        this.uiMenu = uiMenu;
        this.claimButton = uiMenu.getClaimButton();
        this.unclaimButton = uiMenu.getUnclaimButton();
        this.exitButton = uiMenu.getExitButton();

        if (claimButton == null || unclaimButton == null || exitButton == null) {
            System.out.println("[PlayerTools] Warning: One or more UI buttons not initialized for " + player.getName());
        }
    }

    public UILabel getClaimButton() {
        return claimButton;
    }

    public UILabel getUnclaimButton() {
        return unclaimButton;
    }

    public UILabel getExitButton() {
        return exitButton;
    }
}