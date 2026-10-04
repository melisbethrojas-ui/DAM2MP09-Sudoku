package com.clientFX;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

public class CtrlSudoku implements Initializable {
    private int[][] sudoku = {
    {0, 0, 3, 5, 0, 0, 0, 8, 7},
    {0, 0, 0, 3, 0, 0, 0, 4, 0},
    {0, 0, 0, 0, 0, 7, 5, 0, 0},

    {5, 3, 0, 0, 9, 6, 0, 0, 0},
    {0, 0, 2, 1, 8, 0, 4, 5, 0},
    {6, 1, 8, 0, 0, 2, 7, 3, 0},

    {0, 6, 0, 0, 0, 8, 3, 0, 0},
    {0, 8, 1, 0, 0, 5, 0, 0, 2},
    {0, 0, 0, 0, 4, 0, 0, 0, 0}
    };

    @FXML
    private Label txtPlayer;

    @FXML
    private GridPane gridSudoku;

    @FXML
    private VBox boxPlayers;

    private List<String> players;
    private List<Integer> scores;
    private TextField[][] casillas = new TextField[9][9];
    private boolean actualizando = false;
    

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        for (int fila = 0; fila < 9; fila++) {
            for (int columna = 0; columna < 9; columna++) {

                TextField casilla = new TextField();
                casillas[fila][columna] = casilla;
                
                final int filaActual = fila;    
                final int columnaActual = columna;
                
                casilla.setPrefWidth(50);
                casilla.setPrefHeight(50);

                if (sudoku[fila][columna] != 0) {
                    casilla.setText(String.valueOf(sudoku[fila][columna]));
                    casilla.setEditable(false);
                }

                casilla.textProperty().addListener((observable, oldValue, newValue) -> {

                    if (actualizando) {
                        return;
                    }

                    if (!newValue.matches("[1-9]?")) {
                        actualizando = true;
                        casilla.setText(oldValue);
                        actualizando = false;
                        return;
                    }

                    if (newValue.matches("[1-9]")) {
                        JSONObject obj = new JSONObject();
                        obj.put("type", "move");
                        obj.put("row", filaActual);
                        obj.put("column", columnaActual);
                        obj.put("value", Integer.parseInt(newValue));

                        Main.wsClient.safeSend(obj.toString());
                    }
                });
                

                casilla.setStyle(
                    "-fx-alignment: center;" +
                    "-fx-font-size: 18px;" +
                    "-fx-border-color: black;" +
                    "-fx-border-width: " +
                    (fila % 3 == 0 ? "2" : "1") + " " +
                    (columna == 8 ? "2" : (columna % 3 == 2 ? "2" : "1")) + " " +
                    (fila == 8 ? "2" : (fila % 3 == 2 ? "2" : "1")) + " " +
                    (columna % 3 == 0 ? "2" : "1") + ";"
                );

                gridSudoku.add(casilla, columna, fila);

            }
        }
    }
    public void resetGame() {

        for (int fila = 0; fila < 9; fila++) {
            for (int columna = 0; columna < 9; columna++) {

                if (sudoku[fila][columna] == 0) {

                    actualizando = true;
                    casillas[fila][columna].setText("");
                    actualizando = false;

                    casillas[fila][columna].setEditable(true);

                    casillas[fila][columna].setStyle(
                        "-fx-alignment: center;" +
                        "-fx-font-size: 18px;" +
                        "-fx-border-color: black;" +
                        "-fx-border-width: " +
                        (fila % 3 == 0 ? "2" : "1") + " " +
                        (columna == 8 ? "2" : (columna % 3 == 2 ? "2" : "1")) + " " +
                        (fila == 8 ? "2" : (fila % 3 == 2 ? "2" : "1")) + " " +
                        (columna % 3 == 0 ? "2" : "1") + ";"
                    );
                }
            }
        }
    }

    public void setPlayerName(String name) {
        txtPlayer.setText("Juga: " + name);
    }

    public void setPlayers(List<String> players) {

        this.players = players;
        this.scores = new ArrayList<>();

        for (int i = 0; i < players.size(); i++) {
            scores.add(0);
        }

        boxPlayers.getChildren().clear();

        for (String player : players) {
            Label label = new Label(player + " - 0 punts");
            boxPlayers.getChildren().add(label);
        }
    }

    public void updateScore(String player, int score) {

        for (int i = 0; i < players.size(); i++) {

            if (players.get(i).equals(player)) {
                scores.set(i, score);
            }
        }

        for (int i = 0; i < scores.size() - 1; i++) {

            for (int j = i + 1; j < scores.size(); j++) {

                if (scores.get(j) > scores.get(i)) {

                    int tempScore = scores.get(i);
                    scores.set(i, scores.get(j));
                    scores.set(j, tempScore);

                    String tempPlayer = players.get(i);
                    players.set(i, players.get(j));
                    players.set(j, tempPlayer);
                }
            }
        }

        boxPlayers.getChildren().clear();

        for (int i = 0; i < players.size(); i++) {

            Label label = new Label(
                players.get(i) + " - " + scores.get(i) + " punts"
            );

            boxPlayers.getChildren().add(label);
        }
    }

    public void correctCell(int row, int column, int value) {

        TextField casilla = casillas[row][column];        
        
        actualizando = true;
        casilla.setText(String.valueOf(value));
        actualizando = false;

        casilla.setStyle(
            "-fx-alignment: center;" +
            "-fx-font-size: 18px;" +
            "-fx-border-color: black;" +
            "-fx-border-width: 2;" +
            "-fx-background-color: lightgreen;"
        );

        casilla.setEditable(false);
    }

    public void incorrectCell(int row, int column, int value) {

        TextField casilla = casillas[row][column];
        actualizando = true;
        casilla.setText(String.valueOf(value));
        actualizando = false;

        casilla.setStyle(
            "-fx-alignment: center;" +
            "-fx-font-size: 18px;" +
            "-fx-border-color: black;" +
            "-fx-border-width: 2;" +
            "-fx-background-color: lightcoral;"
        );

        casilla.setEditable(true);
    }

    public void showRanking() {

        CtrlRanking ctrlRanking =
            (CtrlRanking) UtilsViews.getController("ViewRanking");

        ctrlRanking.setRanking(players, scores);

        UtilsViews.setViewAnimating("ViewRanking");
    }
}