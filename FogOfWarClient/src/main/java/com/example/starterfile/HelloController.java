package com.example.starterfile;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import socketfx.Constants;
import socketfx.FxSocketClient;
import socketfx.SocketListener;

import java.util.ArrayList;
import java.util.List;

public class HelloController {
    private FxSocketClient socket;

    private int x = 46;
    private int y = 25;

    private Button[][] btns = new Button[x][y];

    private ArrayList<Integer> indexes = new ArrayList<>();

    @FXML
    private GridPane gPane;
    @FXML
    private Button battleBtn, startFightingBtn, removeTroopBtn, breakRock, placeRock, shootBtn;
    @FXML
    private ListView availableTroops, updates;
    @FXML
    private Rectangle troopInfoRect, shootingInfoRect;
    @FXML
    private Label troopNameLbl, troopHealthLbl, turnLabel, moneyLeftLabel, troopAttackedLbl, chanceOfHittingLabel;
    @FXML
    private TabPane troopDescriptions;
    @FXML
    private TextArea troopDescriptionBox;
    @FXML
    private ProgressBar moneyLeftBar;
    @FXML
    private TextField directionBox;

    public HelloController() {
        connect();
    }

    private void connect() {
        socket = new FxSocketClient(new FxSocketListener(),
                "localhost", 2016, Constants.instance().DEBUG_NONE);
        socket.connect();
    }

    @FXML
    private void battleStart() {
        battleBtn.setVisible(false);
        socket.sendMessage("clientReady");
    }

    @FXML
    private void startFighting() {
        startFightingBtn.setVisible(false);
        removeTroopBtn.setVisible(false);
        moneyLeftLabel.setVisible(false);
        moneyLeftBar.setVisible(false);
        breakRock.setVisible(false);
        socket.sendMessage("clientReadyToFight");
    }

    @FXML
    private void breakRock() {
        socket.sendMessage("breakRock");
    }

    @FXML
    private void placeRock() {
        socket.sendMessage("dir" + directionBox.getText());
        socket.sendMessage("putRock");
    }

    @FXML
    private void shootTroop() {
        socket.sendMessage("shootTroop");
    }

    private void startBattle() throws ClassCastException{
        availableTroops.setVisible(true);
        updates.setVisible(true);
        startFightingBtn.setVisible(true);
        gPane.getChildren().clear();
        moneyLeftBar.setVisible(true);
        moneyLeftLabel.setVisible(true);

        for (int i=0; i<btns.length; i++) {
            for (int j=0; j<btns[0].length; j++) {
                btns[i][j] = new Button();
                btns[i][j].setStyle("-fx-background-color:#d3d3d3");
                btns[i][j].setPrefSize(22.5,22.5);
                btns[i][j].setOnMouseClicked(btnClicked);
                btns[i][j].setOnMouseEntered(btnEntered);
                btns[i][j].setOnMouseExited(btnExited);

                gPane.add(btns[i][j], i, j);
            }
        }
        gPane.setGridLinesVisible(true);
        gPane.setVisible(true);
        availableTroops.getItems().clear();
    }

    private void resetOpacity() {
        for (int i = 0; i<btns.length; i++) {
            for (int j = 0; j<btns[0].length; j++) {
                btns[i][j].setOpacity(1);
            }
        }
    }

    EventHandler<MouseEvent> btnClicked = new EventHandler<>() {
        @Override
        public void handle(MouseEvent event) {;
            System.out.println("sent");
            int x = GridPane.getColumnIndex((Button) event.getSource());
            int y = GridPane.getRowIndex((Button) event.getSource());
            System.out.println(btns[x][y].getStyle());
            if (availableTroops.isVisible()) {
                socket.sendMessage("place " + availableTroops.getSelectionModel().getSelectedItem() + " " + x + " " + y);
            } else {
                if (btns[x][y].getOpacity() < 1) {
                    socket.sendMessage("moveTo " + x + " " + y);
                    resetOpacity();
                } else {
                    socket.sendMessage("chose " + x + " " + y);
                }
            }
        }
    };

    private void clearGame() {
        resetOpacity();
        gPane.setGridLinesVisible(false);
        gPane.setVisible(false);
        updates.setVisible(false);
        availableTroops.setVisible(false);
        startFightingBtn.setVisible(false);
        battleBtn.setVisible(true);
        updates.setVisible(false);
        availableTroops.setVisible(false);
        startFightingBtn.setVisible(false);
        turnLabel.setVisible(false);
        troopInfoRect.setVisible(false);
        troopNameLbl.setVisible(false);
        troopHealthLbl.setVisible(false);
        breakRock.setVisible(false);
        placeRock.setVisible(false);
        directionBox.setVisible(false);
        removeTroopBtn.setVisible(false);
    }

    EventHandler<MouseEvent> btnEntered = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) throws ClassCastException {
            int x = GridPane.getColumnIndex((Button) event.getSource());
            int y = GridPane.getRowIndex((Button) event.getSource());

            btns[x][y].setStyle("-fx-background-color:#757472");
        }
    };

    EventHandler<MouseEvent> btnExited = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            int x = GridPane.getColumnIndex((Button) event.getSource());
            int y = GridPane.getRowIndex((Button) event.getSource());

            socket.sendMessage("btnExit " + x + " " + y);
        }
    };

    private ArrayList<Integer> findIndexes(String str, Character character) {
       ArrayList<Integer> indexes = new ArrayList<>();
       for (int i=0; i<str.length(); i++) {
           if (str.charAt(i) == character) indexes.add(i);
       }
       return indexes;
    }

    private int rNum(int start, int end) {
        return start + (int) (Math.random() * (end-start+1));
    }

    @FXML
    private void removeTroop() {
        socket.sendMessage("removeTroop");
    }

    @FXML
    private void troopSelectionChanged() {
        if (troopDescriptions.getSelectionModel().getSelectedItem() == null) return;
        socket.sendMessage("getTroopDes" + troopDescriptions.getSelectionModel().getSelectedItem().getText());

    }

    private void setBtnText(int x, int y, String letter) {
        btns[x][y].setText(letter);
    }

    class FxSocketListener implements SocketListener {
        @Override
        public void onMessage(String line) throws ClassCastException{
            if (line.equals("startBattle")) startBattle();
            else if (line.startsWith("fillSquare")) {
                indexes = findIndexes(line,' ');
                int xCoord = Integer.parseInt(line.substring(indexes.get(0)+1, indexes.get(1)));
                int yCoord = Integer.parseInt(line.substring(indexes.get(1)+1, indexes.get(2)));
                btns[xCoord][yCoord].setStyle(line.substring(indexes.get(2)+1));
            } else if (line.startsWith("add")) {
                availableTroops.getItems().add(line.substring(3));
            } else if (line.equals("startFight")) {
                startFightingBtn.setVisible(false);
                removeTroopBtn.setVisible(false);
                turnLabel.setVisible(true);
            } else if (line.equals("hideList")) availableTroops.setVisible(false);
            else if (line.equals("troopInfoVis")) {
                troopInfoRect.setVisible(true);
                troopNameLbl.setVisible(true);
                troopHealthLbl.setVisible(true);
            } else if (line.startsWith("troopName")) troopNameLbl.setText(line.substring(9));
            else if (line.startsWith("troopHealth")){
                indexes = findIndexes(line, ' ');
                int maxHealth = Integer.parseInt(line.substring(11,indexes.get(0)));
                int health = Integer.parseInt(line.substring(indexes.get(0)+1));
                troopHealthLbl.setText("Health: " + health + "/" + maxHealth);
            } else if (line.equals("resetOpacity")) resetOpacity();
            else if (line.startsWith("lowerOpacity")) {
                indexes = findIndexes(line, ' ');
                int x = Integer.parseInt(line.substring(indexes.get(0)+1,indexes.get(1)));
                int y = Integer.parseInt(line.substring(indexes.get(1)+1));
                btns[x][y].setOpacity(.5);
            } else if (line.startsWith("update")) {
                if (line.indexOf("You") != -1) updates.getItems().clear();
                updates.getItems().add(0,line.substring(6));
            } else if (line.equals("clearGame")) {
                clearGame();
            } else if (line.startsWith("turnsLbl")) {
                turnLabel.setText(line.substring(8));
            } else if (line.equals("hideTroopInfo")) {
                troopInfoRect.setVisible(false);
                troopHealthLbl.setVisible(false);
                troopNameLbl.setVisible(false);
                removeTroopBtn.setVisible(false);
            } else if (line.startsWith("troopList")) {
                troopDescriptions.getTabs().add(new Tab(line.substring(9)));
            } else if (line.startsWith("setTroopInfo")) {
                troopDescriptionBox.setText(line.substring(12));
            } else if (line.equals("showRemoveBtn")) {
                removeTroopBtn.setVisible(true);
            } else if (line.startsWith("setMoney")) {
                moneyLeftBar.setProgress(Double.parseDouble(line.substring(8)));
            } else if (line.startsWith("setOpacity")) {
                indexes = findIndexes(line, ' ');
                int x = Integer.parseInt(line.substring(indexes.get(0)+1,indexes.get(1)));
                int y = Integer.parseInt(line.substring(indexes.get(1)+1));
                btns[x][y].setOpacity(1);
            } else if (line.startsWith("breaking")) {
                if (line.indexOf("true") != -1) breakRock.setVisible(true);
                else breakRock.setVisible(false);
            } else if (line.startsWith("placing")) {
                if (line.indexOf("true") != -1) {
                    placeRock.setVisible(true);
                    directionBox.setVisible(true);
                } else {
                    placeRock.setVisible(false);
                    directionBox.setVisible(false);
                }
            } else if (line.equals("hideRemoveBtn")) {
                removeTroopBtn.setVisible(false);
            } else if (line.startsWith("setBtn")) {
                indexes = findIndexes(line, ' ');
                int x = Integer.parseInt(line.substring(indexes.get(0)+1,indexes.get(1)));
                int y = Integer.parseInt(line.substring(indexes.get(1)+1));
                setBtnText(x,y,line.substring(6,indexes.get(0)));
            } else if (line.startsWith("shootingInfoVis")) {
                if (line.indexOf("true") != -1) {
                    shootingInfoRect.setVisible(true);
                    troopAttackedLbl.setVisible(true);
                    chanceOfHittingLabel.setVisible(true);
                    shootBtn.setVisible(true);
                } else {
                    shootingInfoRect.setVisible(false);
                    troopAttackedLbl.setVisible(false);
                    chanceOfHittingLabel.setVisible(false);
                    shootBtn.setVisible(false);
                }
            } else if (line.startsWith("setAttackedLbl")) {
                troopAttackedLbl.setText(line.substring(14));
            } else if (line.startsWith("setChanceLbl")) {
                chanceOfHittingLabel.setText(line.substring(12));
            } else if (line.startsWith("fightBtn")) {
                startFightingBtn.setDisable(line.contains("true"));
            }
        }
        @Override
        public void onClosedStatus(boolean isClosed) {

        }
    }
}