package landclaim;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class PlayerTools {
    private final Player player;
    private UILabel claimButton;
    private UILabel unclaimButton;
    private PlayerUIMenu uiMenu; // Store reference to access exit button

    public PlayerTools(Player player) {
        this.player = player;
    }

    public void initTools(PlayerUIMenu uiMenu) {
        this.uiMenu = uiMenu;
        claimButton = uiMenu.createButton(10, 10, 180, 45, "<b>Claim Area</b>");
        claimButton.setVisible(true);

        unclaimButton = uiMenu.createButton(10, 60, 180, 45, "<b>Unclaim Area</b>");
        unclaimButton.setVisible(true);

        uiMenu.initExitButton(); // Initialize exit button
    }

    public UILabel getClaimButton() {
        return claimButton;
    }

    public UILabel getUnclaimButton() {
        return unclaimButton;
    }

    public UILabel getExitButton() {
        return uiMenu.getExitButton();
    }
}