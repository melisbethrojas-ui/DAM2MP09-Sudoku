package com.clientFX;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;

    public static CtrlConfig ctrlConfig;
    public static CtrlSudoku ctrlSudoku;
    

    public static void main(String[] args) {

        // Iniciar app JavaFX   
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {

        final int windowWidth = 900;
        final int windowHeight = 600;

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
        UtilsViews.addView(getClass(), "ViewSudoku", "/assets/viewSudoku.fxml");
        UtilsViews.addView(getClass(), "ViewRanking", "/assets/viewRanking.fxml");

        ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
        ctrlSudoku = (CtrlSudoku) UtilsViews.getController("ViewSudoku");
        // UtilsViews.setView("ViewSudoku");

        Scene scene = new Scene(UtilsViews.parentContainer);
        
        stage.setScene(scene);
        stage.setTitle("JavaFX - NodeJS");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();

        // Add icon only if not Mac
        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image("file:/icons/icon.png");
            stage.getIcons().add(icon);
        }
    }


    @Override
    public void stop() { 
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(0); // Kill all executor services
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    public static <T> List<T> jsonArrayToList(JSONArray array, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            T value = clazz.cast(array.get(i));
            list.add(value);
        }
        return list;
    }

    public static void connectToServer() {

        ctrlConfig.txtMessage.setTextFill(Color.BLACK);
        ctrlConfig.txtMessage.setText("Connecting ...");
    
        pauseDuring(1500, () -> { // Give time to show connecting message ...

            String protocol = ctrlConfig.txtProtocol.getText();
            String host = ctrlConfig.txtHost.getText();
            String port = ctrlConfig.txtPort.getText();
            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);
    
            // Platform.runlater assegura que el codi s'executi 
            // al fil de la UI, per evitar problemes de concurrència amb JavaFX
            wsClient.onOpen((message) -> { JSONObject obj = new JSONObject(); obj.put("type", "name");
                obj.put("name", ctrlConfig.txtName.getText());

                wsClient.safeSend(obj.toString());
            });
            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });
        });
    }
   
    private static void wsMessage(String response) {
        JSONObject msgObj = new JSONObject(response);

        if ("clients".equals(msgObj.getString("type"))) {
            UtilsViews.setViewAnimating("ViewSudoku");
            ctrlSudoku.setPlayerName(ctrlConfig.txtName.getText());

            JSONArray list = msgObj.getJSONArray("list");
            JSONArray scoresList = msgObj.getJSONArray("scores");

            ctrlSudoku.setPlayers(jsonArrayToList(list, String.class));

            for (int i = 0; i < list.length(); i++) {
                String player = list.getString(i);
                int score = scoresList.getInt(i);
                ctrlSudoku.updateScore(player, score);
            }
        }

        if ("score".equals(msgObj.getString("type"))) {

            String player = msgObj.getString("player");
            int score = msgObj.getInt("score");
            boolean correct = msgObj.getBoolean("correct");

            ctrlSudoku.updateScore(player, score);

            if (correct) {
                int row = msgObj.getInt("row");
                int column = msgObj.getInt("column");
                int value = msgObj.getInt("value");
                ctrlSudoku.correctCell(row, column, value);
            } else {
                int row = msgObj.getInt("row");
                int column = msgObj.getInt("column");
                int value = msgObj.getInt("value");

                ctrlSudoku.incorrectCell(row, column, value);
            }
        }

        if ("end".equals(msgObj.getString("type"))) {
            ctrlSudoku.showRanking();
        }
        if ("restart".equals(msgObj.getString("type"))) {
            ctrlSudoku.resetGame();
        }
    }

    private static void wsError(String response) {

        String connectionRefused = "Connection refused";
        if (response.indexOf(connectionRefused) != -1) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText(connectionRefused);
            pauseDuring(1500, () -> {
                ctrlConfig.txtMessage.setText("");
            });
        }
    }
}
