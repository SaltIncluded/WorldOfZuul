package sdu.student;

import javafx.animation.FadeTransition;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.concurrent.*;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import  javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import sdu.student.editor.BlockEditor;
import sdu.student.editor.DoorEditor;
import sdu.student.editor.FieldEditor;
import worldofzuul.Game;
import worldofzuul.item.*;
import worldofzuul.util.CustomPrintStream;
import worldofzuul.util.Vector;
import worldofzuul.world.*;

import java.io.PrintStream;
import java.io.IOException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static worldofzuul.util.Data.*;
import static worldofzuul.util.Drawing.*;
import static worldofzuul.util.Math.*;

public class FXMLController implements Initializable {
    private final CustomPrintStream printStream = new CustomPrintStream(System.out);
    private final PrintStream systemPrintStream = System.out;
    private static final String configFileName = "gameConfig.json";
    private static final String spriteDirectory = "sprites";
    private int gameTileDim = 16;
    private int backgroundScaling = 6;
    private static final double paneTransDelayCoefficient = 1.2;
    private static final int updateDelay = 60;
    private static final double nightChangeOpacity = 0.6;
    private static final int nightChangeFadeDelay = 6000;
    private static final int textDisplayDeletionDelay = 8000;
    private static final int textDisplayFadeDelay = 1500;



    public StackPane gameContainerPane;
    public VBox textDisplayBox;
    public StackPane mainPane;
    public Pane environmentLayerPane;
    public Pane nightLayerPane;
    public Pane rainImagePane;
    public TableView<Item> inventoryTableView;
    public Label currentlySelectedItemLabel;
    public Label playerBalanceLabel;
    public Label timeLabel;

    @FXML
    private Pane roomPane;
    @FXML
    private ImageView imageView;
    public Button marketButton;

    private TranslateTransition paneTranslation;
    private HashMap<String, Image> loadedImages;
    public Game model;
    private ScheduledExecutorService scheduledThreadPool;
    private Vector selectedGamePosition;
    private Node instanceOfInventory;
    

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        loadedImages = getImages(spriteDirectory, getClass());

        loadGame();

        bindProperties();
        examplePlayAnimation();
        enableGameUpdater();


        gameTileDim = model.getRoom().getRoomTileDim();
        backgroundScaling = model.getRoom().getRoomBGScale();

        //Configure custom PrintStream
        System.setOut(printStream);
        printStream.printListProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> displayTextMessage(newValue.get(newValue.size() - 1), textDisplayDeletionDelay));
        });


        //timeLabel.setText("" + dateFormat.format(model.getRoom().getEnvironment().getCalendar().getTime()));
    }

    private void enableGameUpdater() {

        scheduledThreadPool = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        long delay = ((long) 1e9) / updateDelay;
        scheduledThreadPool.scheduleAtFixedRate(() -> model.update(), 0, delay, TimeUnit.NANOSECONDS);
    }

    private void loadGame() {

        var gamee = new Game();
        gamee.createRooms();

        var aa = gameToJson(gamee);

        model = jsonToGame(readConfigFile(configFileName));
        if (model != null) {
            model.reconfigureRooms();
        } else {
            System.out.println("Error loading game config");
            model = new Game();
            model.createRooms();
        }

        model.getRoom().setPrintingEnabled(true);
    }

    private void examplePlayAnimation() {

        Image playerSpriteSheet = loadedImages.get("sprites/player/walkCycle.png");
        List<Image[]> spriteAnimations = cutSprites(playerSpriteSheet, 64);

        List<Object> animationKeys = new ArrayList<>();
        animationKeys.add(Direction.NORTH);
        animationKeys.add(Direction.EAST);
        animationKeys.add(Direction.SOUTH);
        animationKeys.add(Direction.WEST);

        model.getPlayer().addAnimation(animationKeys, spriteAnimations);
        model.getPlayer().setImageView(imageView);
        model.getPlayer().setAnimationCycleLengthMillis(400);

        model.getPlayer().display();

        //Example add sprites to fields


        for (Room room : model.getRooms()) {
            if(room != null && room.getRoomGrid() != null){
                Arrays.stream(room.getRoomGrid()).forEach(t -> {
                    Arrays.stream(t).forEach(gameObject -> {
                        if (gameObject instanceof Field) {
                            gameObject.configureImages(loadedImages);
                        }
                    });
                });

            }
        }

         model.getPlayer().getInventory().getItems().forEach(item -> item.configureImages(loadedImages));
        model.getMarket().getStock().forEach(item -> item.configureImages(loadedImages));





        drawRoom();
        setPaneTranslation(model.getPlayer().getPos());
        subscribeToPlayerMovement();

    }

    private void subscribeToPlayerMovement() {
        //Upon player position change, move pane and activate player walk animation
        model.getPlayer().getPos().vectorValueProperty().addListener((observable, oldValue, newValue) -> {
            repositionPlayer(new Vector(oldValue), new Vector(newValue));
        });
    }

    private void repositionPlayer(Vector pos, Vector pos2) {
        setPaneTranslation(pos);
        if (vectorDifference(pos, pos2) > 1) {
            drawRoom();
            model.getPlayer().stopAnimation();
            paneTranslation.stop();
            setPaneTranslation(pos2);
            return;
        }
        repositionPlayer(vectorDirection(pos, pos2));
    }

    private void repositionPlayer(Direction direction) {
        model.getPlayer().playAnimation(1, direction);

        double transX = getBackgroundTileDim();
        double transY = getBackgroundTileDim();

        switch (direction) {
            case SOUTH -> {
                transY *= -1;
                transX = 0;
            }
            case NORTH -> {
                transX = 0;
            }
            case WEST -> {
                transY = 0;
                transX *= -1;
            }
            case EAST -> {
                transY = 0;
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        }

        paneTranslation = translate(roomPane,
                transX,
                transY,
                0,
                (int) (model.getPlayer().getAnimationCycleLengthMillis() * paneTransDelayCoefficient));

        paneTranslation.setDelay(Duration.millis(paneTransDelayCoefficient * model.getPlayer().getAnimationCycleLengthMillis()));

        paneTranslation.play();
    }

    private void setPaneTranslation(Vector pos) {
        double cubeDim = (getBackgroundTileDim());


        double transX = cubeDim * pos.getX();
        double transY = cubeDim * pos.getY();

        transX += cubeDim;
        transY += cubeDim;


        double roomCenterX = roomPane.getMinWidth() / 2;
        double roomCenterY = roomPane.getMinHeight() / 2;


        roomPane.setTranslateX(roomCenterX - transX + cubeDim / 2);
        roomPane.setTranslateY(roomCenterY - transY + cubeDim);

    }

    private void drawRoom() {
        roomPane.getChildren().clear();
        if (model.getRoom().getBackgroundImage() != null && loadedImages.containsKey(model.getRoom().getBackgroundImage())) {
            setBackground(model.getRoom());
        } else {
            setBackground(loadedImages.get("sprites/room/test.png"));
        }
        //drawGrid(roomPane, getBackgroundRowCount());
        drawGameObjects(model.getRoom(), loadedImages, roomPane, getBackgroundTileDim(), getClass(), selectedGamePosition);
    }

    private void bindProperties() {

        inventoryTableView.itemsProperty().bindBidirectional(model.getPlayer().getInventory().itemsProperty());
        subscribeToEnvironmentChanges(model.getRoom().getEnvironment());
        currentlySelectedItemLabel.textProperty().bindBidirectional(model.getPlayer().getInventory().selectedItemNameProperty());

        playerBalanceLabel.textProperty().bindBidirectional(model.getPlayer().balanceProperty(), new NumberStringConverter());

        //Listen to Room change
        model.roomProperty().addListener((observable, oldValue, newValue) -> {
            oldValue.setPrintingEnabled(false);

            unsubscribeToEnvironmentChanges(oldValue.getEnvironment());
            subscribeToEnvironmentChanges(newValue.getEnvironment());

            gameTileDim = newValue.getRoomTileDim();
            backgroundScaling = newValue.getRoomBGScale();
            oldValue.setPrintingEnabled(true);

        });



    }

    private void subscribeToEnvironmentChanges(Environment environment){
        environment.rainStateProperty().addListener((observable1, oldValue1, newValue1) -> {
            changeRainState(newValue1);
        });
        environment.nightStateProperty().addListener((observable1, oldValue1, newValue1) -> {
            changeNightStage(newValue1);
        });
    }

    private void unsubscribeToEnvironmentChanges(Environment environment){
        environment.rainStateProperty().removeListener((observable1, oldValue1, newValue1) -> {
            changeRainState(newValue1);
        });
        environment.nightStateProperty().removeListener((observable1, oldValue1, newValue1) -> {
            changeNightStage(newValue1);
        });
    }

    private void changeRainState(boolean isRaining){
        FadeTransition ft = new FadeTransition(Duration.millis(nightChangeFadeDelay), rainImagePane);
        if(isRaining){
            ft.setFromValue(0.0);
            ft.setToValue(1);

        } else {
            ft.setFromValue(1);
            ft.setToValue(0);
        }
        ft.play();

    }
    private void changeNightStage(boolean isNight){
        FadeTransition ft = new FadeTransition(Duration.millis(nightChangeFadeDelay), nightLayerPane);
        if(isNight){
            ft.setFromValue(0.0);
            ft.setToValue(nightChangeOpacity);

        } else {
            ft.setFromValue(nightChangeOpacity);
            ft.setToValue(0);
        }
        ft.play();
    }

    private void setBackground(Room room) {
        setBackground(loadedImages.get(room.getBackgroundImage()));
    }

    private void setBackground(Image backgroundImage) {
        BackgroundImage myBI = new BackgroundImage(backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                new BackgroundSize(0, 0, false, false, false, true));

        roomPane.setMinSize(myBI.getImage().getWidth() * backgroundScaling, myBI.getImage().getHeight() * backgroundScaling);
        roomPane.setBackground(new Background(myBI));
    }

    public void moveNorth(ActionEvent actionEvent) {
        if (!isPlayerMoving()) {
            model.move(Direction.NORTH);
        }
    }

    public void moveSouth(ActionEvent actionEvent) {
        if (!isPlayerMoving()) {
            model.move(Direction.SOUTH);
        }
    }

    public void moveEast(ActionEvent actionEvent) {
        if (!isPlayerMoving()) {
            model.move(Direction.EAST);
        }
    }

    public void moveWest(ActionEvent actionEvent) {
        if (!isPlayerMoving()) {
            model.move(Direction.WEST);
        }
    }

    public void interact(ActionEvent actionEvent) {
        model.interact();
    }

    public void openMarket(ActionEvent actionEvent) {

        FXMLLoader loader = new FXMLLoader();

        //Defines our controller
        loader.setControllerFactory(aClass -> new sdu.student.SubScene(model));
        //Defines the FXML file
        loader.setLocation(getClass().getResource("subScene.fxml"));

        try {
            gameContainerPane.getChildren().add(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
        try {
            Node subRoot = FXMLLoader.load(getClass().getResource("subScene.fxml"));
            loader.setControllerFactory(aClass -> new FieldInfoBarController(field));
            stackPane.getChildren().add(subRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }


    private boolean isPlayerMoving() {

        if (model.getPlayer().getAnimationTimeline() != null) {
            return model.getPlayer().isAnimationActive();
        }

        return false;
        /*

        //TODO: Solve pane translation flicker caused by animation cancelling
        if (paneTranslation != null) {
            if (paneTranslation.getStatus().equals(Animation.Status.RUNNING)) {
                return true;
            }
        }


        return false;*/
    }

    private double getBackgroundTileDim() {
        return roomPane.getMinWidth() / getBackgroundRowCount();
    }

    private double getBackgroundRowCount() {
        return (roomPane.getMinWidth() / backgroundScaling) / gameTileDim;
    }

    private void displayTextMessage(String text, int deletionDelay){

        Label newLabel = new Label(text);
        newLabel.setId("textDisplayLabel");
        newLabel.setPickOnBounds(false);

        DropShadow effect = new DropShadow(0.67, Color.WHITE);
        effect.setInput(new Glow(0.65));
        newLabel.setEffect(effect);


        textDisplayBox.getChildren().add(newLabel);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run()
            {
                Platform.runLater(() -> { //Platform.runLater is used to avoid cross thread UI invocation exceptions

                    FadeTransition ft = new FadeTransition(Duration.millis(textDisplayFadeDelay), newLabel);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.0);
                    ft.setOnFinished(event -> textDisplayBox.getChildren().remove(newLabel));

                    ft.play();
                });
                this.cancel();
            }
        }, deletionDelay);





    }

    public void roomPaneClicked(MouseEvent mouseEvent) {

        selectedGamePosition = positionClickedOnPane(getBackgroundTileDim(), getBackgroundTileDim(), mouseEvent.getX(), mouseEvent.getY());
        if (selectedGamePosition.getX() < 0 || selectedGamePosition.getY() < 0 || selectedGamePosition.getX() > getBackgroundRowCount() || selectedGamePosition.getY() > getBackgroundRowCount()) {
            return;
        }

        if (vectorDifference(model.getPlayer().getPos(), selectedGamePosition) <= 1) {
            switch (mouseEvent.getButton()) {
                case PRIMARY -> {
                    model.interact(selectedGamePosition, false);
                    faceDirection(vectorDirection(model.getPlayer().getPos(), selectedGamePosition));
                }
                case SECONDARY -> {
                    model.interact(selectedGamePosition, true);
                    faceDirection(vectorDirection(model.getPlayer().getPos(), selectedGamePosition));
                }
            }
        }

    }

    private void faceDirection(Direction vectorDirection) {
        model.getPlayer().setDefaultImage(vectorDirection);
    }


    private void rightClickGameObject(GameObject object){

               /*
               call using
            try {
                rightClickGameObject(model.getRoom().getGridGameObject(selectedGamePosition));
            } catch (ArrayIndexOutOfBoundsException e) { // Handle exceptions caused by non-matching RoomGrid sizes or invalid positions
                System.out.println(e.getMessage());
            }*/

        if(object instanceof Field && selectedGamePosition != null){

            //TODO: Implement on click functionality

        } else {

        }

    }

    public void selectItem(MouseEvent mouseEvent) {
        model.getPlayer().getInventory().setSelectedItem(inventoryTableView.getSelectionModel().getSelectedItem());
    }
}