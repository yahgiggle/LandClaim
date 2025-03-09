package landclaim;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class PlayerTools {
    private Player player;
    private PlayerUIMenu uiMenu;
    private UILabel claimButton;
    private UILabel unclaimButton;
    private UILabel exitButton;

    public PlayerTools(Player player) {
        this.player = player;
    }

    public void initTools(PlayerUIMenu uiMenu) {
        this.uiMenu = uiMenu;
        this.claimButton = uiMenu.getClaimButton();
        this.unclaimButton = uiMenu.getUnclaimButton();
        this.exitButton = uiMenu.getExitButton();
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


