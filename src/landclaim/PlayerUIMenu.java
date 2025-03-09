package landclaim;

import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.utils.Vector3i;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import landclaim.LandClaim.ClaimedArea;

public class PlayerUIMenu {
    private final Player player;
    private UIElement menuBase;
    private UIElement settingsMenu;
    private UILabel claimButton;
    private UILabel removeButton;
    private UILabel exitButton;
    private UILabel settingsButton;
    private UILabel settingsExitButton;
    private UILabel showMyAreasLabel;
    private UILabel showAllAreasLabel;
    private UILabel infoLabel;
    private UILabel nextPlayerButton;
    private UILabel backPlayerButton;
    private UILabel addGuestButton;
    private UILabel removeGuestButton;
    private UILabel shootingTargetUseObjectButton;
    private UILabel signUseObjectButton;
    private UILabel spinningWheelUseObjectButton;
    private UILabel storageUseObjectButton;
    private UILabel tanningRackUseObjectButton;
    private UILabel technicalUseObjectButton;
    private UILabel torchUseObjectButton;
    private UILabel trashcanUseObjectButton;
    private UILabel workbenchUseObjectButton;
    private UILabel hitAnimalNPCButton;
    private UILabel hitHumanNPCButton;
    private UILabel hitMountNPCButton;
    private UILabel rideMountNPCButton;
    private UILabel placeBlueprintsButton;
    private UILabel npcAddSaddleButton;
    private UILabel npcRemoveSaddleButton;
    private UILabel npcAddSaddleBagButton;
    private UILabel npcRemoveSaddleBagButton;
    private UILabel npcAddClothesButton;
    private UILabel npcRemoveClothesButton;
    private UILabel changeConstructionColorButton;
    private UILabel changeObjectColorButton;
    private UILabel changeObjectInfoButton;
    private UILabel creativePlaceVegetationButton;
    private UILabel creativeRemoveConstructionButton;
    private UILabel creativeRemoveObjectButton;
    private UILabel creativeRemoveVegetationButton;
    private UILabel creativeTerrainEditButton;
    private UILabel destroyConstructionButton;
    private UILabel destroyObjectButton;
    private UILabel destroyTerrainButton;
    private UILabel editConstructionButton;
    private UILabel hitConstructionButton;
    private UILabel hitObjectButton;
    private UILabel hitVegetationButton;
    private UILabel hitWaterButton;
    private UILabel placeConstructionButton;
    private UILabel placeGrassButton;
    private UILabel placeObjectButton;
    private UILabel placeTerrainButton;
    private UILabel placeVegetationButton;
    private UILabel removeConstructionButton;
    private UILabel removeGrassButton;
    private UILabel removeObjectButton;
    private UILabel removeVegetationButton;
    private UILabel removeWaterButton;
    private UILabel worldEditButton;
    private UILabel clockUseObjectButton;
    private UILabel doorUseObjectButton;
    private UILabel dryingRackUseObjectButton;
    private UILabel fireUseObjectButton;
    private UILabel furnaceUseObjectButton;
    private UILabel grillUseObjectButton;
    private UILabel grinderUseObjectButton;
    private UILabel grindstoneUseObjectButton;
    private UILabel ladderUseObjectButton;
    private UILabel lampUseObjectButton;
    private UILabel musicPlayerUseObjectButton;
    private UILabel ovenUseObjectButton;
    private UILabel paperPressUseObjectButton;
    private UILabel pianoUseObjectButton;
    private UILabel posterUseObjectButton;
    private UILabel scaffoldingUseObjectButton;
    private UILabel seatingUseObjectButton;
    private boolean isVisible;
    private boolean showingMyAreas = false;
    private boolean showingAllAreas = false;
    private LandClaim plugin;
    private List<Player> worldPlayers;
    private int currentPlayerIndex = -1;

    private int currentAreaId = -1;
    final Map<String, UILabel> permissionButtons = new HashMap<>();

    public PlayerUIMenu(Player player, LandClaim plugin) {
        this.player = player;
        this.plugin = plugin;
        this.isVisible = false;
        this.worldPlayers = new ArrayList<>();
        setupBaseMenu();
        setupSettingsMenu();
    }

    private void setupBaseMenu() {
        menuBase = new UIElement();
        player.addUIElement(menuBase);
        menuBase.setSize(276, 320, false);
        menuBase.setClickable(false);
        menuBase.setPosition(45, 45, true);
        menuBase.setBorderEdgeRadius(5.0f, false);
        menuBase.setBorder(3);
        menuBase.setBorderColor(888);
        menuBase.setBackgroundColor(0.1f, 0.1f, 0.1f, 0.9f);
        menuBase.setVisible(false);

        initClaimButton();
        initRemoveButton();
        initExitButton();
        initSettingsButton();
        initShowMyAreasLabel();
        initShowAllAreasLabel();
    }

    private void setupSettingsMenu() {
        settingsMenu = new UIElement();
        player.addUIElement(settingsMenu);
        settingsMenu.setSize(1566, 850, false);
        settingsMenu.setClickable(false);
        settingsMenu.setPosition(5, 5, true);
        settingsMenu.setBorderEdgeRadius(5.0f, false);
        settingsMenu.setBorder(3);
        settingsMenu.setBorderColor(888);
        settingsMenu.setBackgroundColor(0.8f, 0.8f, 0.8f, 0.95f);
        settingsMenu.setVisible(false);

        initSettingsExitButton();
        initInfoLabel();
        initNextPlayerButton();
        initBackPlayerButton();
        initAddGuestButton();
        initRemoveGuestButton();

        initShootingTargetUseObjectButton();
        initSignUseObjectButton();
        initSpinningWheelUseObjectButton();
        initStorageUseObjectButton();
        initTanningRackUseObjectButton();
        initTechnicalUseObjectButton();
        initTorchUseObjectButton();
        initTrashcanUseObjectButton();
        initWorkbenchUseObjectButton();
        initHitAnimalNPCButton();
        initHitHumanNPCButton();
        initHitMountNPCButton();
        initRideMountNPCButton();
        initPlaceBlueprintsButton();
        initNpcAddSaddleButton();
        initNpcRemoveSaddleButton();
        initNpcAddSaddleBagButton();
        initNpcRemoveSaddleBagButton();
        initNpcAddClothesButton();
        initNpcRemoveClothesButton();
        initChangeConstructionColorButton();
        initChangeObjectColorButton();
        initChangeObjectInfoButton();
        initCreativePlaceVegetationButton();
        initCreativeRemoveConstructionButton();
        initCreativeRemoveObjectButton();
        initCreativeRemoveVegetationButton();
        initCreativeTerrainEditButton();
        initDestroyConstructionButton();
        initDestroyObjectButton();
        initDestroyTerrainButton();
        initEditConstructionButton();
        initHitConstructionButton();
        initHitObjectButton();
        initHitVegetationButton();
        initHitWaterButton();
        initPlaceConstructionButton();
        initPlaceGrassButton();
        initPlaceObjectButton();
        initPlaceTerrainButton();
        initPlaceVegetationButton();
        initRemoveConstructionButton();
        initRemoveGrassButton();
        initRemoveObjectButton();
        initRemoveVegetationButton();
        initRemoveWaterButton();
        initWorldEditButton();
        initClockUseObjectButton();
        initDoorUseObjectButton();
        initDryingRackUseObjectButton();
        initFireUseObjectButton();
        initFurnaceUseObjectButton();
        initGrillUseObjectButton();
        initGrinderUseObjectButton();
        initGrindstoneUseObjectButton();
        initLadderUseObjectButton();
        initLampUseObjectButton();
        initMusicPlayerUseObjectButton();
        initOvenUseObjectButton();
        initPaperPressUseObjectButton();
        initPianoUseObjectButton();
        initPosterUseObjectButton();
        initScaffoldingUseObjectButton();
        initSeatingUseObjectButton();

        player.setAttribute("settingsExitButton", settingsExitButton);
        player.setAttribute("nextPlayerButton", nextPlayerButton);
        player.setAttribute("backPlayerButton", backPlayerButton);
        player.setAttribute("addGuestButton", addGuestButton);
        player.setAttribute("removeGuestButton", removeGuestButton);
    }

    private UILabel createDefaultButton(int x, int y, String text, UIElement parent) {
        UILabel button = new UILabel();
        button.setClickable(true);
        button.setText("<b>" + text + "</b>");
        button.setFont(Font.Medieval);
        button.style.textAlign.set(TextAnchor.MiddleCenter);
        button.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        button.setFontSize(16);
        // Adjust width based on parent: 250px for menuBase, 300px for settingsMenu
        int width = (parent == menuBase) ? 250 : 300;
        button.setSize(width, 45, false);
        button.setBorder(2);
        button.setBorderColor(999);
        button.setBackgroundColor(500);
        button.setPosition(x, y, false);
        button.style.borderBottomWidth.set(5);
        button.hoverStyle.backgroundColor.set(0.2f, 0.2f, 0.2f, 0.9f);
        button.hoverStyle.borderBottomWidth.set(5);
        button.hoverStyle.borderBottomColor.set(0.1f, 0.1f, 0.9f, 0.9f);
        parent.addChild(button);
        return button;
    }

    private void initClaimButton() {
        claimButton = createDefaultButton(10, 10, "Claim Area", menuBase);
        claimButton.setVisible(true);
    }

    private void initRemoveButton() {
        removeButton = createDefaultButton(10, 60, "Remove Area", menuBase);
        removeButton.setVisible(true);
    }

    private void initExitButton() {
        exitButton = createDefaultButton(10, 210, "Exit", menuBase);
        exitButton.setVisible(true);
    }

    private void initSettingsButton() {
        settingsButton = createDefaultButton(10, 260, "Settings", menuBase);
        settingsButton.setVisible(true);
    }

    private void initShowMyAreasLabel() {
        showMyAreasLabel = createDefaultButton(10, 110, "Show My Areas\nOff", menuBase);
        showMyAreasLabel.setVisible(true);
    }

    private void initShowAllAreasLabel() {
        showAllAreasLabel = createDefaultButton(10, 160, "Show All Areas\nOff", menuBase);
        showAllAreasLabel.setVisible(true);
    }

    private void initSettingsExitButton() {
        settingsExitButton = new UILabel();
        settingsExitButton.setClickable(true);
        settingsExitButton.setText("<b>X</b>");
        settingsExitButton.setFont(Font.Medieval);
        settingsExitButton.style.textAlign.set(TextAnchor.MiddleCenter);
        settingsExitButton.setFontColor(1.0f, 0.0f, 0.0f, 1.0f);
        settingsExitButton.setFontSize(18);
        settingsExitButton.setSize(45, 45, false);
        settingsExitButton.setBorder(2);
        settingsExitButton.setBorderColor(999);
        settingsExitButton.setBackgroundColor(500);
        settingsExitButton.setPosition(1511, 5, false); // Adjusted for borders
        settingsExitButton.style.borderBottomWidth.set(2);
        settingsExitButton.hoverStyle.backgroundColor.set(0.2f, 0.2f, 0.2f, 0.9f);
        settingsExitButton.hoverStyle.borderBottomWidth.set(2);
        settingsExitButton.hoverStyle.borderBottomColor.set(0.1f, 0.1f, 0.9f, 0.9f);
        settingsExitButton.setVisible(true);
        settingsMenu.addChild(settingsExitButton);
    }

    private void initInfoLabel() {
        infoLabel = new UILabel();
        infoLabel.setClickable(false);
        infoLabel.setText("No Player Selected");
        infoLabel.setFont(Font.Medieval);
        infoLabel.style.textAlign.set(TextAnchor.MiddleCenter);
        infoLabel.setFontColor(9.0f, 9.0f, 9.0f, 1.0f);
        infoLabel.setFontSize(16);
        infoLabel.setSize(1466, 45, false);
        infoLabel.setBorder(2);
        infoLabel.setBorderColor(999);
        infoLabel.setBackgroundColor(500);
        infoLabel.setPosition(10, 5, false);
        settingsMenu.addChild(infoLabel);
        player.setAttribute("infoLabel", infoLabel);
    }

    private void initNextPlayerButton() {
        nextPlayerButton = createDefaultButton(10, 60, "Next Player", settingsMenu);
    }

    private void initBackPlayerButton() {
        backPlayerButton = createDefaultButton(320, 60, "Back Player", settingsMenu);
    }

    private void initAddGuestButton() {
        addGuestButton = createDefaultButton(630, 60, "Add Guest", settingsMenu);
    }

    private void initRemoveGuestButton() {
        removeGuestButton = createDefaultButton(940, 60, "Remove Guest", settingsMenu);
    }

    private void initShootingTargetUseObjectButton() {
        shootingTargetUseObjectButton = createDefaultButton(10, 115, "Shooting Target Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerShootingTargetUseObject", shootingTargetUseObjectButton);
    }

    private void initSignUseObjectButton() {
        signUseObjectButton = createDefaultButton(320, 115, "Sign Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerSignUseObject", signUseObjectButton);
    }

    private void initSpinningWheelUseObjectButton() {
        spinningWheelUseObjectButton = createDefaultButton(630, 115, "Spinning Wheel Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerSpinningWheelUseObject", spinningWheelUseObjectButton);
    }

    private void initStorageUseObjectButton() {
        storageUseObjectButton = createDefaultButton(940, 115, "Storage Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerStorageUseObject", storageUseObjectButton);
    }

    private void initTanningRackUseObjectButton() {
        tanningRackUseObjectButton = createDefaultButton(1250, 115, "Tanning Rack Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerTanningRackUseObject", tanningRackUseObjectButton);
    }

    private void initTechnicalUseObjectButton() {
        technicalUseObjectButton = createDefaultButton(10, 170, "Technical Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerTechnicalUseObject", technicalUseObjectButton);
    }

    private void initTorchUseObjectButton() {
        torchUseObjectButton = createDefaultButton(320, 170, "Torch Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerTorchUseObject", torchUseObjectButton);
    }

    private void initTrashcanUseObjectButton() {
        trashcanUseObjectButton = createDefaultButton(630, 170, "Trashcan Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerTrashcanUseObject", trashcanUseObjectButton);
    }

    private void initWorkbenchUseObjectButton() {
        workbenchUseObjectButton = createDefaultButton(940, 170, "Workbench Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerWorkbenchUseObject", workbenchUseObjectButton);
    }

    private void initHitAnimalNPCButton() {
        hitAnimalNPCButton = createDefaultButton(1250, 170, "Hit Animal NPC: Off", settingsMenu);
        permissionButtons.put("PlayerHitAnimalNPC", hitAnimalNPCButton);
    }

    private void initHitHumanNPCButton() {
        hitHumanNPCButton = createDefaultButton(10, 225, "Hit Human NPC: Off", settingsMenu);
        permissionButtons.put("PlayerHitHumanNPC", hitHumanNPCButton);
    }

    private void initHitMountNPCButton() {
        hitMountNPCButton = createDefaultButton(320, 225, "Hit Mount NPC: Off", settingsMenu);
        permissionButtons.put("PlayerHitMountNPC", hitMountNPCButton);
    }

    private void initRideMountNPCButton() {
        rideMountNPCButton = createDefaultButton(630, 225, "Ride Mount NPC: Off", settingsMenu);
        permissionButtons.put("PlayerRideMountNPC", rideMountNPCButton);
    }

    private void initPlaceBlueprintsButton() {
        placeBlueprintsButton = createDefaultButton(940, 225, "Place Blueprints: Off", settingsMenu);
        permissionButtons.put("PlayerPlaceBluePrints", placeBlueprintsButton);
    }

    private void initNpcAddSaddleButton() {
        npcAddSaddleButton = createDefaultButton(1250, 225, "NPC Add Saddle: Off", settingsMenu);
        permissionButtons.put("PlayerNpcAddSaddle", npcAddSaddleButton);
    }

    private void initNpcRemoveSaddleButton() {
        npcRemoveSaddleButton = createDefaultButton(10, 280, "NPC Remove Saddle: Off", settingsMenu);
        permissionButtons.put("PlayerNpcRemoveSaddle", npcRemoveSaddleButton);
    }

    private void initNpcAddSaddleBagButton() {
        npcAddSaddleBagButton = createDefaultButton(320, 280, "NPC Add Saddle Bag: Off", settingsMenu);
        permissionButtons.put("PlayerNpcAddSaddleBag", npcAddSaddleBagButton);
    }

    private void initNpcRemoveSaddleBagButton() {
        npcRemoveSaddleBagButton = createDefaultButton(630, 280, "NPC Remove Saddle Bag: Off", settingsMenu);
        permissionButtons.put("PlayerNpcRemoveSaddleBag", npcRemoveSaddleBagButton);
    }

    private void initNpcAddClothesButton() {
        npcAddClothesButton = createDefaultButton(940, 280, "NPC Add Clothes: Off", settingsMenu);
        permissionButtons.put("PlayerNpcAddClothes", npcAddClothesButton);
    }

    private void initNpcRemoveClothesButton() {
        npcRemoveClothesButton = createDefaultButton(1250, 280, "NPC Remove Clothes: Off", settingsMenu);
        permissionButtons.put("PlayerNpcRemoveClothes", npcRemoveClothesButton);
    }

    private void initChangeConstructionColorButton() {
        changeConstructionColorButton = createDefaultButton(10, 335, "Change Construction Color: Off", settingsMenu);
        permissionButtons.put("PlayerChangeConstructionColor", changeConstructionColorButton);
    }

    private void initChangeObjectColorButton() {
        changeObjectColorButton = createDefaultButton(320, 335, "Change Object Color: Off", settingsMenu);
        permissionButtons.put("PlayerChangeObjectColor", changeObjectColorButton);
    }

    private void initChangeObjectInfoButton() {
        changeObjectInfoButton = createDefaultButton(630, 335, "Change Object Info: Off", settingsMenu);
        permissionButtons.put("PlayerChangeObjectInfo", changeObjectInfoButton);
    }

    private void initCreativePlaceVegetationButton() {
        creativePlaceVegetationButton = createDefaultButton(940, 335, "Creative Place Vegetation: Off", settingsMenu);
        permissionButtons.put("PlayerCreativePlaceVegetation", creativePlaceVegetationButton);
    }

    private void initCreativeRemoveConstructionButton() {
        creativeRemoveConstructionButton = createDefaultButton(1250, 335, "Creative Remove Construction: Off", settingsMenu);
        permissionButtons.put("PlayerCreativeRemoveConstruction", creativeRemoveConstructionButton);
    }

    private void initCreativeRemoveObjectButton() {
        creativeRemoveObjectButton = createDefaultButton(10, 390, "Creative Remove Object: Off", settingsMenu);
        permissionButtons.put("PlayerCreativeRemoveObject", creativeRemoveObjectButton);
    }

    private void initCreativeRemoveVegetationButton() {
        creativeRemoveVegetationButton = createDefaultButton(320, 390, "Creative Remove Vegetation: Off", settingsMenu);
        permissionButtons.put("PlayerCreativeRemoveVegetation", creativeRemoveVegetationButton);
    }

    private void initCreativeTerrainEditButton() {
        creativeTerrainEditButton = createDefaultButton(630, 390, "Creative Terrain Edit: Off", settingsMenu);
        permissionButtons.put("PlayerCreativeTerrainEdit", creativeTerrainEditButton);
    }

    private void initDestroyConstructionButton() {
        destroyConstructionButton = createDefaultButton(940, 390, "Destroy Construction: Off", settingsMenu);
        permissionButtons.put("PlayerDestroyConstruction", destroyConstructionButton);
    }

    private void initDestroyObjectButton() {
        destroyObjectButton = createDefaultButton(1250, 390, "Destroy Object: Off", settingsMenu);
        permissionButtons.put("PlayerDestroyObject", destroyObjectButton);
    }

    private void initDestroyTerrainButton() {
        destroyTerrainButton = createDefaultButton(10, 445, "Destroy Terrain: Off", settingsMenu);
        permissionButtons.put("PlayerDestroyTerrain", destroyTerrainButton);
    }

    private void initEditConstructionButton() {
        editConstructionButton = createDefaultButton(320, 445, "Edit Construction: Off", settingsMenu);
        permissionButtons.put("PlayerEditConstruction", editConstructionButton);
    }

    private void initHitConstructionButton() {
        hitConstructionButton = createDefaultButton(630, 445, "Hit Construction: Off", settingsMenu);
        permissionButtons.put("PlayerHitConstruction", hitConstructionButton);
    }

    private void initHitObjectButton() {
        hitObjectButton = createDefaultButton(940, 445, "Hit Object: Off", settingsMenu);
        permissionButtons.put("PlayerHitObject", hitObjectButton);
    }

    private void initHitVegetationButton() {
        hitVegetationButton = createDefaultButton(1250, 445, "Hit Vegetation: Off", settingsMenu);
        permissionButtons.put("PlayerHitVegetation", hitVegetationButton);
    }

    private void initHitWaterButton() {
        hitWaterButton = createDefaultButton(10, 500, "Hit Water: Off", settingsMenu);
        permissionButtons.put("PlayerHitWater", hitWaterButton);
    }

    private void initPlaceConstructionButton() {
        placeConstructionButton = createDefaultButton(320, 500, "Place Construction: Off", settingsMenu);
        permissionButtons.put("PlayerPlaceConstruction", placeConstructionButton);
    }

    private void initPlaceGrassButton() {
        placeGrassButton = createDefaultButton(630, 500, "Place Grass: Off", settingsMenu);
        permissionButtons.put("PlayerPlaceGrass", placeGrassButton);
    }

    private void initPlaceObjectButton() {
        placeObjectButton = createDefaultButton(940, 500, "Place Object: Off", settingsMenu);
        permissionButtons.put("PlayerPlaceObject", placeObjectButton);
    }

    private void initPlaceTerrainButton() {
        placeTerrainButton = createDefaultButton(1250, 500, "Place Terrain: Off", settingsMenu);
        permissionButtons.put("PlayerPlaceTerrain", placeTerrainButton);
    }

    private void initPlaceVegetationButton() {
        placeVegetationButton = createDefaultButton(10, 555, "Place Vegetation: Off", settingsMenu);
        permissionButtons.put("PlayerPlaceVegetation", placeVegetationButton);
    }

    private void initRemoveConstructionButton() {
        removeConstructionButton = createDefaultButton(320, 555, "Remove Construction: Off", settingsMenu);
        permissionButtons.put("PlayerRemoveConstruction", removeConstructionButton);
    }

    private void initRemoveGrassButton() {
        removeGrassButton = createDefaultButton(630, 555, "Remove Grass: Off", settingsMenu);
        permissionButtons.put("PlayerRemoveGrass", removeGrassButton);
    }

    private void initRemoveObjectButton() {
        removeObjectButton = createDefaultButton(940, 555, "Remove Object: Off", settingsMenu);
        permissionButtons.put("PlayerRemoveObject", removeObjectButton);
    }

    private void initRemoveVegetationButton() {
        removeVegetationButton = createDefaultButton(1250, 555, "Remove Vegetation: Off", settingsMenu);
        permissionButtons.put("PlayerRemoveVegetation", removeVegetationButton);
    }

    private void initRemoveWaterButton() {
        removeWaterButton = createDefaultButton(10, 610, "Remove Water: Off", settingsMenu);
        permissionButtons.put("PlayerRemoveWater", removeWaterButton);
    }

    private void initWorldEditButton() {
        worldEditButton = createDefaultButton(320, 610, "World Edit: Off", settingsMenu);
        permissionButtons.put("PlayerWorldEdit", worldEditButton);
    }

    private void initClockUseObjectButton() {
        clockUseObjectButton = createDefaultButton(630, 610, "Clock Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerClockUseObject", clockUseObjectButton);
    }

    private void initDoorUseObjectButton() {
        doorUseObjectButton = createDefaultButton(940, 610, "Door Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerDoorUseObject", doorUseObjectButton);
    }

    private void initDryingRackUseObjectButton() {
        dryingRackUseObjectButton = createDefaultButton(1250, 610, "Drying Rack Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerDryingRackUseObject", dryingRackUseObjectButton);
    }

    private void initFireUseObjectButton() {
        fireUseObjectButton = createDefaultButton(10, 665, "Fire Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerFireUseObject", fireUseObjectButton);
    }

    private void initFurnaceUseObjectButton() {
        furnaceUseObjectButton = createDefaultButton(320, 665, "Furnace Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerFurnaceUseObject", furnaceUseObjectButton);
    }

    private void initGrillUseObjectButton() {
        grillUseObjectButton = createDefaultButton(630, 665, "Grill Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerGrillUseObject", grillUseObjectButton);
    }

    private void initGrinderUseObjectButton() {
        grinderUseObjectButton = createDefaultButton(940, 665, "Grinder Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerGrinderUseObject", grinderUseObjectButton);
    }

    private void initGrindstoneUseObjectButton() {
        grindstoneUseObjectButton = createDefaultButton(1250, 665, "Grindstone Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerGrindstoneUseObject", grindstoneUseObjectButton);
    }

    private void initLadderUseObjectButton() {
        ladderUseObjectButton = createDefaultButton(10, 720, "Ladder Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerLadderUseObject", ladderUseObjectButton);
    }

    private void initLampUseObjectButton() {
        lampUseObjectButton = createDefaultButton(320, 720, "Lamp Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerLampUseObject", lampUseObjectButton);
    }

    private void initMusicPlayerUseObjectButton() {
        musicPlayerUseObjectButton = createDefaultButton(630, 720, "Music Player Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerMusicPlayerUseObject", musicPlayerUseObjectButton);
    }

    private void initOvenUseObjectButton() {
        ovenUseObjectButton = createDefaultButton(940, 720, "Oven Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerOvenUseObject", ovenUseObjectButton);
    }

    private void initPaperPressUseObjectButton() {
        paperPressUseObjectButton = createDefaultButton(1250, 720, "Paper Press Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerPaperPressUseObject", paperPressUseObjectButton);
    }

    private void initPianoUseObjectButton() {
        pianoUseObjectButton = createDefaultButton(10, 775, "Piano Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerPianoUseObject", pianoUseObjectButton);
    }

    private void initPosterUseObjectButton() {
        posterUseObjectButton = createDefaultButton(320, 775, "Poster Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerPosterUseObject", posterUseObjectButton);
    }

    private void initScaffoldingUseObjectButton() {
        scaffoldingUseObjectButton = createDefaultButton(630, 775, "Scaffolding Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerScaffoldingUseObject", scaffoldingUseObjectButton);
    }

    private void initSeatingUseObjectButton() {
        seatingUseObjectButton = createDefaultButton(940, 775, "Seating Use Object: Off", settingsMenu);
        permissionButtons.put("PlayerSeatingUseObject", seatingUseObjectButton);
    }

    public UILabel getClaimButton() {
        return claimButton;
    }

    public UILabel getUnclaimButton() {
        return removeButton;
    }

    public UILabel getExitButton() {
        return exitButton;
    }

    public UILabel getSettingsButton() {
        return settingsButton;
    }

    public UILabel getShowMyAreasLabel() {
        return showMyAreasLabel;
    }

    public UILabel getShowAllAreasLabel() {
        return showAllAreasLabel;
    }

    public UILabel getSettingsExitButton() {
        return settingsExitButton;
    }

    public void showSettingsMenu() throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        int areaX = chunk.x;
        int areaY = chunk.y;
        int areaZ = chunk.z;
        String key = plugin.getKeyFromCoords(areaX, areaY, areaZ);
        ClaimedArea area = plugin.claimedAreas.get(key);

        if (area == null) {
            player.sendYellMessage("This area is not claimed!", 3, true);
            return;
        }

        if (!area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only open Settings in your own area!", 3, true);
            return;
        }

        ResultSet rs = plugin.getDatabase().getDb().executeQuery(
            "SELECT ID FROM `Areas` WHERE AreaX = " + areaX + " AND AreaY = " + areaY + " AND AreaZ = " + areaZ
        );
        if (rs.next()) {
            currentAreaId = rs.getInt("ID");
            updatePermissionButtons();
        } else {
            player.sendYellMessage("Error: Area ID not found!", 3, true);
            return;
        }

        worldPlayers.clear();
        worldPlayers.addAll(getAllPlayersInWorld());
        currentPlayerIndex = -1;
        updateInfoLabel();

        menuBase.setVisible(false);
        settingsMenu.setVisible(true);
        player.setMouseCursorVisible(true);
    }

    private void updatePermissionButtons() throws SQLException {
        if (currentAreaId == -1) return;
        for (Map.Entry<String, UILabel> entry : permissionButtons.entrySet()) {
            String permission = entry.getKey();
            UILabel button = entry.getValue();
            boolean state = plugin.getDatabase().getGuestPermission(currentAreaId, permission);
            String displayName = button.getText().replace("<b>", "").replace(": Off", "").replace(": On", "").trim();
            button.setText("<b>" + displayName + ": " + (state ? "On" : "Off") + "</b>");
        }
    }

    public void togglePermission(String permission) throws SQLException {
        if (currentAreaId == -1) {
            player.sendYellMessage("No area selected!", 3, true);
            return;
        }
        Vector3i chunk = player.getChunkPosition();
        String key = plugin.getKeyFromCoords(chunk.x, chunk.y, chunk.z);
        ClaimedArea area = plugin.claimedAreas.get(key);
        if (area == null || !area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only modify permissions in your own area!", 3, true);
            return;
        }

        boolean currentState = plugin.getDatabase().getGuestPermission(currentAreaId, permission);
        boolean newState = !currentState;
        plugin.getDatabase().setGuestPermission(currentAreaId, permission, newState);
        UILabel button = permissionButtons.get(permission);
        if (button != null) {
            String displayName = button.getText().replace("<b>", "").replace(": Off", "").replace(": On", "").trim();
            button.setText("<b>" + displayName + ": " + (newState ? "On" : "Off") + "</b>");
            player.sendTextMessage(displayName + " for guests set to: " + (newState ? "On" : "Off"));
        }
    }

    public void closeSettingsMenu() {
        settingsMenu.setVisible(false);
        menuBase.setVisible(true);
        player.setMouseCursorVisible(true);
    }

    public void toggleMenu() {
        isVisible = !isVisible;
        menuBase.setVisible(isVisible);
        player.setMouseCursorVisible(isVisible);
    }

    public void closeMenu() {
        isVisible = false;
        menuBase.setVisible(false);
        player.setMouseCursorVisible(false);
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void toggleMyAreas() {
        if (!showingMyAreas) {
            showingMyAreas = true;
            showingAllAreas = false;
            System.out.println("[PlayerUIMenu] Toggling My Areas: " + showingMyAreas + " for " + player.getName());
            plugin.showAllAreas(false, player);
            plugin.showMyAreas(true, player);
            player.sendTextMessage("My Areas visibility set to: On");
        } else {
            showingMyAreas = false;
            System.out.println("[PlayerUIMenu] Toggling My Areas: " + showingMyAreas + " for " + player.getName());
            plugin.showMyAreas(false, player);
            player.sendTextMessage("My Areas visibility set to: Off");
        }
        updateLabels();
    }

    public void toggleAllAreas() {
        if (!showingAllAreas) {
            showingAllAreas = true;
            showingMyAreas = false;
            System.out.println("[PlayerUIMenu] Toggling All Areas: " + showingAllAreas + " for " + player.getName());
            plugin.showMyAreas(false, player);
            plugin.showAllAreas(true, player);
            player.sendTextMessage("All Areas visibility set to: On");
        } else {
            showingAllAreas = false;
            System.out.println("[PlayerUIMenu] Toggling All Areas: " + showingAllAreas + " for " + player.getName());
            plugin.showAllAreas(false, player);
            player.sendTextMessage("All Areas visibility set to: Off");
        }
        updateLabels();
    }

    private void updateLabels() {
        showMyAreasLabel.setText("<b>Show My Areas\n" + (showingMyAreas ? "On" : "Off") + "</b>");
        showAllAreasLabel.setText("<b>Show All Areas\n" + (showingAllAreas ? "On" : "Off") + "</b>");
    }

    private void updateInfoLabel() {
        if (worldPlayers.isEmpty()) {
            infoLabel.setText("No Players in World");
        } else if (currentPlayerIndex == -1) {
            infoLabel.setText("Select a Player");
        } else {
            Player selectedPlayer = worldPlayers.get(currentPlayerIndex);
            String guestStatus = "";
            if (currentAreaId != -1) {
                try {
                    ResultSet rs = plugin.getDatabase().getDb().executeQuery(
                        "SELECT ID FROM `Guests` WHERE AreaID = " + currentAreaId + " AND PlayerUID = '" + selectedPlayer.getUID() + "'"
                    );
                    guestStatus = rs.next() ? " (Guest)" : " (Not a Guest)";
                } catch (SQLException ex) {
                    System.out.println("[LandClaim] Error checking guest status for " + selectedPlayer.getName() + ": " + ex.getMessage());
                    guestStatus = " (Status Unknown)";
                }
            }
            infoLabel.setText("Player: " + selectedPlayer.getName() + " (UID: " + selectedPlayer.getUID() + ")" + guestStatus);
        }
    }

    public void nextPlayer() {
        if (worldPlayers.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex + 1) % worldPlayers.size();
        updateInfoLabel();
    }

    public void backPlayer() {
        if (worldPlayers.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex - 1 + worldPlayers.size()) % worldPlayers.size();
        updateInfoLabel();
    }

    public void addGuest() throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        String key = plugin.getKeyFromCoords(chunk.x, chunk.y, chunk.z);
        ClaimedArea area = plugin.claimedAreas.get(key);

        if (area == null) {
            player.sendYellMessage("This area is not claimed!", 3, true);
            return;
        }

        if (!area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only add guests to your own area!", 3, true);
            return;
        }

        if (currentPlayerIndex == -1 || worldPlayers.isEmpty()) {
            player.sendYellMessage("No player selected!", 3, true);
            return;
        }

        Player guest = worldPlayers.get(currentPlayerIndex);
        String guestUID = guest.getUID();
        String guestName = guest.getName();

        ResultSet rs = plugin.getDatabase().getDb().executeQuery(
            "SELECT ID FROM `Guests` WHERE AreaID = (SELECT ID FROM `Areas` WHERE AreaX = " + chunk.x +
            " AND AreaY = " + chunk.y + " AND AreaZ = " + chunk.z + ") AND PlayerUID = '" + guestUID + "'"
        );
        if (rs.next()) {
            player.sendYellMessage(guestName + " is already a guest in this area!", 3, true);
            return;
        }

        String insertSql = "INSERT INTO `Guests` (AreaID, GuestName, PlayerUID) VALUES (" +
                          "(SELECT ID FROM `Areas` WHERE AreaX = " + chunk.x + " AND AreaY = " + chunk.y + " AND AreaZ = " + chunk.z + "), " +
                          "'" + guestName + "', '" + guestUID + "')";
        plugin.getDatabase().getDb().executeUpdate(insertSql);
        player.sendYellMessage(guestName + " is now added as a guest to this area!", 3, true);
        updateInfoLabel();
    }

    public void removeGuest() throws SQLException {
        Vector3i chunk = player.getChunkPosition();
        String key = plugin.getKeyFromCoords(chunk.x, chunk.y, chunk.z);
        ClaimedArea area = plugin.claimedAreas.get(key);

        if (area == null) {
            player.sendYellMessage("This area is not claimed!", 3, true);
            return;
        }

        if (!area.playerUID.equals(player.getUID())) {
            player.sendYellMessage("You can only remove guests from your own area!", 3, true);
            return;
        }

        if (currentPlayerIndex == -1 || worldPlayers.isEmpty()) {
            player.sendYellMessage("No player selected!", 3, true);
            return;
        }

        Player guest = worldPlayers.get(currentPlayerIndex);
        String guestUID = guest.getUID();

        ResultSet rs = plugin.getDatabase().getDb().executeQuery(
            "SELECT ID FROM `Guests` WHERE AreaID = (SELECT ID FROM `Areas` WHERE AreaX = " + chunk.x +
            " AND AreaY = " + chunk.y + " AND AreaZ = " + chunk.z + ") AND PlayerUID = '" + guestUID + "'"
        );
        if (!rs.next()) {
            player.sendYellMessage(guest.getName() + " is not a guest!", 3, true);
            return;
        }

        String deleteSql = "DELETE FROM `Guests` WHERE AreaID = (SELECT ID FROM `Areas` WHERE AreaX = " + chunk.x +
                           " AND AreaY = " + chunk.y + " AND AreaZ = " + chunk.z + ") AND PlayerUID = '" + guestUID + "'";
        plugin.getDatabase().getDb().executeUpdate(deleteSql);
        player.sendYellMessage("Removed " + guest.getName() + " as a guest!", 3, true);
        updateInfoLabel();
    }

    private List<Player> getAllPlayersInWorld() {
        return new ArrayList<Player>(Arrays.asList(Server.getAllPlayers()));
    }
}

