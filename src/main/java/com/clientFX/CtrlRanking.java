package com.clientFX;

import java.util.List;

import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CtrlRanking {

    @FXML
    private VBox boxRanking;

    public void setRanking(List<String> players, List<Integer> scores) {

        boxRanking.getChildren().clear();

        for (int i = 0; i < players.size(); i++) {

            Label label = new Label(
                (i + 1) + ". " + players.get(i) + " - " + scores.get(i) + " punts"
            );

            boxRanking.getChildren().add(label);
        }
    }

    @FXML
    public void playAgain() {

        JSONObject obj = new JSONObject();
        obj.put("type", "restart");

        Main.wsClient.safeSend(obj.toString());

        UtilsViews.setViewAnimating("ViewSudoku");
    }
}