package com.example.starterfile;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import socketfx.Constants;
import socketfx.FxSocketServer;
import socketfx.SocketListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HelloController {
    private FxSocketServer socket;
    private HashMap<Integer, String> colorCoder = new HashMap<>();

    private int x = 46;
    private int y = 25;
    private int totalBudget = 50000;
    private int serverMoney = 50000;
    private int clientMoney = 50000;
    private int turns = 7;
    private int smokeLength = 0;
    private int numServerTeleporters = 0;
    private int numCLientTeleporters = 0;

    private Button[][] btns = new Button[x][y];
    private int[][] gameGrid = new int[x][y];
    private int[][] environment = new int[x][y];

    private ArrayList<Troop> serverTroops = new ArrayList<>();
    private ArrayList<Troop> clientTroops = new ArrayList<>();
    private ArrayList<Troop> troopInfo = new ArrayList<>();
    private ArrayList<Troop> troopsInMotion = new ArrayList<>();

    private ArrayList<Integer> indexes = new ArrayList<>();

    private boolean serverReady = false;
    private boolean clientReady = false;
    private boolean serverReadyForFighting = false;
    private boolean clientReadyForFighting = false;
    private boolean placingTroops = false;
    private boolean serverTurn;
    private boolean firstTime = true;
    private boolean opened = false;
    private boolean serverNeedsTeleporter = false;
    private boolean clientNeedsTeleporter = false;

    private Troop troopChosenServer;
    private Troop lastTroopSelectedServer;
    private Troop troopChosenClient;
    private Troop lastTroopSelectedClient;
    private String lastDirection;
    private Troop troopTargetedServer;
    private Troop troopTargetedClient;
    private Troop serverLastTeleporter;
    private Troop clientLastTeleporter;

    private static final long second = 1000000000;
    long counter = System.nanoTime();

    @FXML
    private ProgressBar moneyLeftBar;
    @FXML
    private Label troopNameLbl, troopHealthLbl,moneyLeftLabel, turnLabel, troopAttackedLbl, chanceOfHittingLabel;
    @FXML
    private GridPane gPane;
    @FXML
    private Button battleBtn, startFightingBtn, removeTroopBtn, breakRock, placeRock, shootBtn;
    @FXML
    private ListView availableTroops, updates;
    @FXML
    private Rectangle troopInfoRect, shootingInfoRect;
    @FXML
    private TabPane troopDescriptions;
    @FXML
    private TextArea troopDescriptionBox;
    @FXML
    private Slider budgetSlider, movesSlider;
    @FXML
    private TextField directionBox;

    public HelloController() {
        colorCoder.put(0,"00FF00");
        colorCoder.put(1,"000000");
        colorCoder.put(2,"911A1A");
        troopInfo.add(new Teleporters());
        troopInfo.add(new Infantry());
        troopInfo.add(new Tank());
        connect();
    }

    private void connect() {
        socket = new FxSocketServer(new FxSocketListener(), 2016, Constants.instance().DEBUG_NONE);
        socket.connect();
    }

    @FXML
    private void battleStart() {
        battleBtn.setVisible(false);
        serverReady = true;
        startBattle();
    }

    @FXML
    private void startFighting() {
        serverReadyForFighting = true;
        startFightingBtn.setVisible(false);
        moneyLeftBar.setVisible(false);
        moneyLeftLabel.setVisible(false);
        availableTroops.setVisible(false);
        removeTroopBtn.setVisible(false);
        breakRock.setVisible(false);

//        serverTurn = ((int) (Math.random()*2) == 0);
        serverTurn = true;
        if (clientReadyForFighting) fightingStart();
    }

    private int[] giveDirectionsFromString(String str) {
        switch (str.toUpperCase()) {
            case "N": {
                return new int[] {0,-1};
            }
            case "S": {
                return new int[] {0,1};
            }
            case "W": {
                return new int[] {-1,0};
            }
            case "E": {
                return new int[] {1,0};
            }
            case "NW": {
                return new int[] {-1,-1};
            }
            case "NE": {
                return new int[] {1,-1};
            }
            case "SW": {
                return new int[] {-1,1};
            }
            case "SE": {
                return new int[] {1,1};
            }
            default: {
                return null;
            }
        }
    }

    @FXML
    private void shootTroop() {
        if (lastTroopSelectedServer != null && troopTargetedServer != null) {
            troopShot(lastTroopSelectedServer, troopTargetedServer, true);
        }
    }

    @FXML
    private void placeRock() {
        if (lastTroopSelectedServer == null || !spaceForRocks(lastTroopSelectedServer) || lastTroopSelectedServer.getNumRocks() == 0) return;
        String direction = directionBox.getText();
        int[] dirCoords = giveDirectionsFromString(direction);
        if (dirCoords == null) {
            updates.getItems().add(0,"Invalid direction");
            return;
        }
        int xChange = dirCoords[0];
        int yChange = dirCoords[1];
        int newX = lastTroopSelectedServer.getX() + xChange;
        int newY = lastTroopSelectedServer.getY() + yChange;
        if (!inBounds(newX,newY)) {
            updates.getItems().add(0,"Coords out of bounds");
            return;
        } else if (gameGrid[newX][newY] != 0 || environment[newX][newY] != 0) {
            updates.getItems().add(0,"Can't place a rock there");
            return;
        }
        environment[newX][newY] = 1;
        lastTroopSelectedServer.numRocks --;
        turns--;
        changeTurn();
    }

    @FXML
    private void breakRock() {
        if (lastTroopSelectedServer == null) return;
        troopBreakRock(true);
    }

    private void troopBreakRock(boolean server) {
        Troop chosenTroop;
        if (server) chosenTroop = lastTroopSelectedServer;
        else chosenTroop = lastTroopSelectedClient;
        for (int i = chosenTroop.getX()-1; i < chosenTroop.getX()+2; i++) {
            boolean done = false;
            for (int j = chosenTroop.getY()-1; j < chosenTroop.getY()+2; j++) {
                if (inBounds(i,j) && environment[i][j] == 1) {
                    environment[i][j] = 0;
                    chosenTroop.numRocks ++;
                    done = true;
                    turns--;
                    changeTurn();
                    break;
                }
            }
            if (done) break;
        }
    }

    private boolean rocksInArea(Troop troop) {
        for (int i = troop.getX()-1; i < troop.getX()+2; i++) {
            for (int j = troop.getY()-1; j < troop.getY()+2; j++) {
                if (inBounds(i,j) && environment[i][j] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private void troopShot(Troop attacker, Troop target, boolean serverShoooting) {
        double hitChance = attacker.getHitChance(target,gameGrid,environment);
        if (Math.random() < hitChance) {
            target.currentHealth -= 10;
            if (serverShoooting) updates.getItems().add(0,"Dealt 10 Damage");
            else socket.sendMessage("updateDealt 10 Damage");
            if (serverShoooting) die(target,clientTroops);
            else die(target,serverTroops);
            resetGame();
        } else {
            if (serverShoooting) updates.getItems().add(0,"Missed Shot");
            else socket.sendMessage("updateMissed Shot");
        }
        turns--;
        changeTurn();
    }

    private boolean spaceForRocks(Troop troop) {
        for (int i = troop.getX()-1; i < troop.getX()+2; i++) {
            for (int j = troop.getY()-1; j < troop.getY()+2; j++) {
                if (inBounds(i,j) && environment[i][j] != 1 && gameGrid[i][j] == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleBtnClick(int x, int y, boolean throughBtn) {
        //*
        Troop troopByCoords = TroopHelper.getTroopByCoords(x, y, serverTroops);
        if (troopByCoords != null) {
            lastTroopSelectedServer = TroopHelper.getTroopByCoords(x,y,serverTroops);
        }
        if (placingTroops) {
            Object troopFromList = availableTroops.getSelectionModel().getSelectedItem();
            if (troopFromList == null) {
                updates.getItems().add(0,"Chose a troop first");
                return;
            }
            String troopName = troopFromList.toString();
            troopChosenServer = TroopHelper.getTroopByName(troopName, troopInfo);
            if (gameGrid[x][y] == 0 && (x >= btns.length/2 || environment[x][y] != 0)) {
                updates.getItems().add(0,"Can't place a troop there");
            } else if (gameGrid[x][y] == 0) {
                //**
                if (!troopChosenServer.getName().equals("Teleporter")) {
                    if (serverNeedsTeleporter) {
                        updates.getItems().add(0,"You need to place another teleporter first!");
                        return;
                    }
                    if (troopChosenServer.getCost() > serverMoney) {
                        updates.getItems().add(0, "Not enough money!");
                        return;
                    }
                    gameGrid[x][y] = troopChosenServer.getNumber();
                    addBasedOnTroop(serverTroops, x, y, troopChosenServer.getName(), true);
                    serverMoney -= serverTroops.get(serverTroops.size() - 1).getCost();
                } else {
                    if (numServerTeleporters%2 == 0) {
                        if (troopChosenServer.getCost()*2 > serverMoney) {
                            updates.getItems().add(0, "Not enough money!");
                            return;
                        }
                        gameGrid[x][y] = troopChosenServer.getNumber();
                        addBasedOnTroop(serverTroops, x, y, troopChosenServer.getName(), true);
                        serverMoney -= serverTroops.get(serverTroops.size() - 1).getCost();
                        updates.getItems().add(0,"Need to add another teleporter");
                        startFightingBtn.setDisable(true);
                        serverNeedsTeleporter = true;
                        numServerTeleporters ++;
                        serverLastTeleporter = serverTroops.get(serverTroops.size()-1);
                    } else {
                        gameGrid[x][y] = troopChosenServer.getNumber();
                        addBasedOnTroop(serverTroops, x, y, troopChosenServer.getName(), true);
                        serverMoney -= serverTroops.get(serverTroops.size() - 1).getCost();
                        serverLastTeleporter.pair = serverTroops.get(serverTroops.size()-1);
                        serverTroops.get(serverTroops.size()-1).pair = serverLastTeleporter;
                        serverLastTeleporter = serverTroops.get(serverTroops.size()-1);
                        startFightingBtn.setDisable(false);
                        serverNeedsTeleporter = false;
                        numServerTeleporters++;
                    }
                }
            }
            return;
        }
        if (!serverTurn) {
            troopChosenServer = TroopHelper.getTroopByCoords(x,y,serverTroops);
            if (troopChosenServer == null && throughBtn) {
                updates.getItems().add(0,"It's not your turn!");
            }
            return;
        }
        troopChosenServer = TroopHelper.getTroopByCoords(x,y,serverTroops);
        if (troopChosenServer != null) {
            lastTroopSelectedServer = troopChosenServer;
            resetOpacity();
            int[][] distances = lastTroopSelectedServer.djistraAlgorithm(troopChosenServer.getX(), troopChosenServer.getY(),gameGrid,environment,serverTroops);
            if (!lastTroopSelectedServer.getName().equals("Teleporter")) lowerOpacities(lastTroopSelectedServer,distances,true);
            return;
        }
        if (btns[x][y].getOpacity() < 1 && gameGrid[x][y] == 0 && environment[x][y] != -1) {
            if (troopsInMotion.size() > 0) {
                updates.getItems().add(0,"Can't move when other troops are in motion!");
                return;
            }
            moveTroop(lastTroopSelectedServer,true,x,y,serverTroops);
            resetOpacity();
            turns--;
            changeTurn();
            return;
        }
        if (!serverTurn && throughBtn) {
            updates.getItems().add(0,"It's not your turn!");
            return;
        }
        Troop troopToAttack = TroopHelper.getTroopByCoords(x, y, clientTroops);
        if (troopToAttack != null && serverTurn && lastTroopSelectedServer != null) {
            troopTargetedServer = troopToAttack;
        }
        printScreen();
    }

    private int findLastTeleporterIndex(List<Troop> troops) {
        int lastIndex = -1;
        for (int i=0; i<troops.size(); i++) {
            if (troops.get(i).getName().equals("Teleporter")) {
                lastIndex = i;
            }
        }
        return lastIndex;
    }

    private void lowerOpacities(Troop troopChosen, int[][] distances, boolean forServerTroop) {
        for (int i = 0; i < gameGrid.length; i++) {
            for (int j = 0; j < gameGrid[0].length; j++) {
                if (distances[i][j] != -1 && troopChosen.getRange() >= distances[i][j] && gameGrid[i][j] == 0 && environment[i][j] != 1) {
                    if (forServerTroop) btns[i][j].setOpacity(.5);
                    else socket.sendMessage("lowerOpacity " + i + " " + j);
                }
            }
        }
    }

    private int round(double num) {
        return (int) (num + .5);
    }

    private void fightingStart() {
        placingTroops = false;
        socket.sendMessage("startFight");
        turnLabel.setVisible(true);
        printScreen();
        resetGame();

        movesSlider.setDisable(true);
    }

    private void addSmoke() {
        for (int i = 0; i < btns[0].length; i++) {
            environment[smokeLength][i] = 2;
            environment[environment.length-1-smokeLength][i] = 2;
        }
        for (int i = 0; i < btns.length; i++) {
            environment[i][smokeLength] = 2;
            environment[i][environment[0].length-1-smokeLength] = 2;
        }
    }

    private void clearGame() {
        resetOpacity();
        updates.setVisible(false);
        moneyLeftBar.setVisible(false);
        moneyLeftLabel.setVisible(false);
        availableTroops.setVisible(false);
        startFightingBtn.setVisible(false);
        gPane.setGridLinesVisible(false);
        gPane.setVisible(false);
        battleBtn.setVisible(true);
        updates.setVisible(false);
        availableTroops.setVisible(false);
        moneyLeftBar.setVisible(false);
        moneyLeftLabel.setVisible(false);
        startFightingBtn.setVisible(false);
        turnLabel.setVisible(false);
        troopInfoRect.setVisible(false);
        troopHealthLbl.setVisible(false);
        breakRock.setVisible(false);
        troopNameLbl.setVisible(false);
        placeRock.setVisible(false);
        directionBox.setVisible(false);
        removeTroopBtn.setVisible(false);
        shootingInfoRect.setVisible(false);
        troopAttackedLbl.setVisible(false);
        chanceOfHittingLabel.setVisible(false);
        shootBtn.setVisible(false);

        budgetSlider.setDisable(false);
        movesSlider.setDisable(false);

        serverTroops.clear();
        clientTroops.clear();
        troopsInMotion.clear();

        serverReady = false;
        clientReady = false;
        serverReadyForFighting = false;
        clientReadyForFighting = false;
        placingTroops = false;
        firstTime = true;
        serverNeedsTeleporter = false;
        clientNeedsTeleporter = false;

        troopChosenServer = null;
        lastTroopSelectedServer = null;
        troopChosenClient = null;
        lastTroopSelectedClient = null;
        troopTargetedServer = null;
        troopTargetedClient = null;
        serverLastTeleporter = null;
        clientLastTeleporter = null;

        serverMoney = totalBudget;
        clientMoney = totalBudget;
        smokeLength = 0;
        numServerTeleporters = 0;
        numCLientTeleporters = 0;

        socket.sendMessage("clearGame");
    }

    @FXML
    private void setUpSettings() throws InterruptedException {
        if (opened) return;
        Thread.sleep(1000);
        for (Troop troop : troopInfo) {
            troopDescriptions.getTabs().add(new Tab(troop.getName()));
            socket.sendMessage("troopList" + troop.getName());
        }
        opened = true;
    }

    @FXML
    private void troopSelectionChanged() {
        if (troopDescriptions.getSelectionModel().getSelectedItem() == null) return;
        Troop troopSelected = TroopHelper.getTroopByName(troopDescriptions.getSelectionModel().getSelectedItem().getText(),troopInfo);
        troopDescriptionBox.setText(troopSelected.getDescription());
    }

    @FXML
    private void removeTroop() {
        if (lastTroopSelectedServer == null) return;
        removeATroop(lastTroopSelectedServer,serverTroops,true);
    }

    //If both players are ready
    private void startBattle() throws ClassCastException{
        if (serverReady && clientReady) {
            gameGrid = new int[x][y];
            environment = new int[x][y];
            generateEnvironment(environment);
            placingTroops = true;
            socket.sendMessage("startBattle");
            gPane.getChildren().clear();

            updates.setVisible(true);
            moneyLeftBar.setVisible(true);
            moneyLeftLabel.setVisible(true);
            availableTroops.setVisible(true);
            startFightingBtn.setVisible(true);

            budgetSlider.setDisable(true);

            for (int i = 0; i < btns.length; i++) {
                for (int j = 0; j < btns[0].length; j++) {
                    btns[i][j] = new Button();
                    btns[i][j].setStyle("-fx-background-color:#d3d3d3");
                    btns[i][j].setPrefSize(22.5, 22.5);
                    btns[i][j].setOnMouseClicked(btnClicked);
                    btns[i][j].setOnMouseEntered(btnEntered);
                    btns[i][j].setOnMouseExited(btnExited);

                    gPane.add(btns[i][j], i, j);
                }
            }

            gPane.setGridLinesVisible(true);
            gPane.setVisible(true);
            battleBtn.setVisible(false);
            printScreen();
            availableTroops.getItems().clear();
            for (Troop troop : troopInfo) {
                socket.sendMessage("add"+troop.getName());
                availableTroops.getItems().add(troop.getName());
            }

            serverMoney = totalBudget;
            clientMoney = totalBudget;
            moneyLeftBar.setProgress((double) serverMoney / totalBudget);
            long counter = System.nanoTime();
            firstTime = false;
            start();
        }
    }

//    private

    private void findIndexes(String str, Character character) {
        indexes.clear();
        for (int i=0; i<str.length(); i++) {
            if (str.charAt(i) == character) indexes.add(i);
        }
    }

    private void generateEnvironment(int[][] grid) {
        int numRocks = rNum((grid.length * grid[0].length)/10-10,(grid.length * grid[0].length)/10+10);
        for (int i=0; i<numRocks; i++) {
            boolean worked = false;
            while (!worked) {
                int randX = rNum(0, environment.length - 1);
                int randY = rNum(0, environment[0].length - 1);
                if (environment[randX][randY] != 1) {
                    environment[randX][randY] = 1;
                    worked = true;
                }
            }
        }
    }

    private void die(Troop troop, ArrayList<Troop> troops) { //This function checks whether to kill the troop first
        if (troop.getCurrentHealth() <= 0) {
            if (troop.pair != null) {
                Troop pair = troop.getPair();
                gameGrid[pair.getX()][pair.getY()] = 0;
                troops.remove(pair);
            }
            troops.remove(troop);
            gameGrid[troop.getX()][troop.getY()] = 0;
            if (lastTroopSelectedClient!= null && lastTroopSelectedClient.equals(troop)) {
                socket.sendMessage("resetOpacity");
                lastTroopSelectedClient = null;
            } else if (lastTroopSelectedServer!= null && lastTroopSelectedServer.equals(troop)) {
                resetOpacity();
                lastTroopSelectedServer = null;
            }
            if (troopTargetedServer != null && troopTargetedServer.equals(troop)) {
                troopTargetedServer = null;
            } else if (troopTargetedClient != null && troopTargetedClient.equals(troop)) {
                troopTargetedServer = null;
            }
            printScreen();
        }
        if (!placingTroops) {
            resetGame();
        }
    }

    private void resetGame() { //This function checks whether to reset the game first
        boolean gameOver = false;
        if (serverTroops.size() == 0 && clientTroops.size() == 0) {
            updates.getItems().add(0,"You Tied");
            socket.sendMessage("updateYou Tied");
            gameOver = true;
        } else if (clientTroops.size() == 0) {
            updates.getItems().clear();
            updates.getItems().add(0,"You Win!");
            socket.sendMessage("updateYou Lose!");
            gameOver = true;
        } else if (serverTroops.size() == 0) {
            updates.getItems().clear();
            updates.getItems().add(0,"You Lose!");
            socket.sendMessage("updateYou Win!");
            gameOver = true;
        }
        if (gameOver) {
            clearGame();
        }
    }

    private void start() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now-counter > second/5) {
                    counter = System.nanoTime();
                    for (int i=0; i<troopsInMotion.size(); i++) {
                        Troop troopMoving = troopsInMotion.get(i);
                        if (troopMoving.getPath().size() == 0) {
                            troopsInMotion.get(i).inMotion = false;
                            troopsInMotion.remove(i);
                        }
                        else {
                            int pathLength = troopMoving.getPath().size();
                            int[] nextMove = troopMoving.getPath().get(pathLength - 1);
                            troopMoving.move(gameGrid, nextMove[0], nextMove[1]);
                            troopMoving.getPath().remove(pathLength - 1);
                        }
                    }
                    if (lastTroopSelectedServer != null) {
                        handleBtnClick(lastTroopSelectedServer.getX(),lastTroopSelectedServer.getY(),false);
                    } if (lastTroopSelectedClient != null) {
                        if (placingTroops) handleClientPlace(lastTroopSelectedClient.getX(), lastTroopSelectedClient.getY(), "");
                        else handleClientChoice(lastTroopSelectedClient.getX(),lastTroopSelectedClient.getY(), false);
                    }
                    printScreen();
                    updateGameGrid();
                }
            }
        }.start();
    }

    EventHandler<MouseEvent> btnClicked = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            //Uses listview selection to tell which troop to place
            int x = GridPane.getColumnIndex((Button) event.getSource());
            int y = GridPane.getRowIndex((Button) event.getSource());
            handleBtnClick(x,y,true);
        }
    };

    private void updateGameGrid() {
        for (Troop troop : serverTroops) {
            gameGrid[troop.getX()][troop.getY()] = troop.getNumber();
        }
        for (Troop troop : clientTroops) {
            gameGrid[troop.getX()][troop.getY()] = troop.getNumber();
        }
    }

    private void showTroopInfo(Troop troop) {
        troopInfoRect.setVisible(true);
        troopNameLbl.setVisible(true);
        troopHealthLbl.setVisible(true);

        troopNameLbl.setText(troop.getName());
        troopHealthLbl.setText("Health: " + troop.getCurrentHealth() + "/" + troop.getMaxHealth());
    }

    EventHandler<MouseEvent> btnEntered = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) throws ClassCastException{
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

            printSquare(x,y);
        }
    };

    private void addBasedOnTroop(ArrayList<Troop> troops, int x, int y, String troopChosen, boolean serverOrNot) {
        switch (troopChosen) {
            case "Teleporter": addTroop(new Teleporters(x,y),troops);
            case "Bomber Plane": addTroop(new BomberPlanes(x,y),troops);
            case "Infantry": addTroop(new Infantry(x,y),troops);
            case "Tank": addTroop(new Tank(x,y),troops);
        }
    }

    private boolean teleportersWorked(ArrayList<Troop> troops) {
        for (Troop troop : troops) {
            if (troop.getName().equals("Teleporter")) {
                if (troop.getPair() == null) return false;
            }
        }
        return true;
    }

    private void addTroop(Troop troopToAdd, ArrayList<Troop> troops) {
        if (TroopHelper.getTroopByCoords(troopToAdd.getX(),troopToAdd.getY(),troops) == null) troops.add(troopToAdd);
    }

    private void printScreen() {
        //Prints environment and troops
        for (int i=0; i<gameGrid.length; i++) {
            for (int j=0; j<gameGrid[0].length; j++) {
                printSquare(i,j);
                setSquareText(i,j,placingTroops);
            }
        }

        if (lastTroopSelectedServer != null) {
            troopNameLbl.setText(lastTroopSelectedServer.getName());
            troopHealthLbl.setText("Health: " + lastTroopSelectedServer.getCurrentHealth() + "/" + lastTroopSelectedServer.getMaxHealth());
        }

        if (lastTroopSelectedClient != null) {
            socket.sendMessage("troopHealth" + lastTroopSelectedClient.getMaxHealth() + " " + lastTroopSelectedClient.getCurrentHealth());
        }

        if (serverTurn) {
            turnLabel.setText("Your Turns: " + turns);
            socket.sendMessage("turnsLblOpponent's Turns: " + turns);
        } else {
            turnLabel.setText("Opponent's Turns: " + turns);
            socket.sendMessage("turnsLblYour Turns: " + turns);
        }

        if (placingTroops) {
            moneyLeftBar.setProgress((double) serverMoney/totalBudget);
            socket.sendMessage("setMoney" + (double) clientMoney/totalBudget);
        }

        changeTroopUI();
        changeRockUI();
        changeShootUI();
    }

    private void changeShootUI() {
        if (serverTurn && !placingTroops && lastTroopSelectedServer != null && troopTargetedServer != null && !lastTroopSelectedServer.getName().equals("Teleporter")
        && !lastTroopSelectedServer.getInMotion() && !troopTargetedServer.getInMotion()) {
            visShootingUI(true, true);
            troopAttackedLbl.setText("Troop Attacked: " + troopTargetedServer.getName());
            int percentChanceOfHit = (int) (lastTroopSelectedServer.getHitChance(troopTargetedServer,gameGrid,environment)*100);
            chanceOfHittingLabel.setText("Hit Chance: " + percentChanceOfHit + "%");
        } else {
            visShootingUI(false, true);
        }
        if (!serverTurn && !placingTroops && lastTroopSelectedClient != null && troopTargetedClient != null && !lastTroopSelectedClient.getName().equals("Teleporter")
        && !lastTroopSelectedClient.getInMotion() && !troopTargetedClient.getInMotion()) {
            visShootingUI(true, false);
            socket.sendMessage("setAttackedLblTroop Attacked: " + troopTargetedClient.getName());
            int percentChanceOfHit = (int) (lastTroopSelectedClient.getHitChance(troopTargetedClient,gameGrid,environment)*100);
            socket.sendMessage("setChanceLblHit Chance: " + percentChanceOfHit + "%");
        } else {
            visShootingUI(false,false);
        }
    }

    private void visShootingUI(boolean isVis, boolean serverTurn) {
        if (serverTurn) {
            shootingInfoRect.setVisible(isVis);
            troopAttackedLbl.setVisible(isVis);
            chanceOfHittingLabel.setVisible(isVis);
            shootBtn.setVisible(isVis);
        } else {
            if (isVis) {
                socket.sendMessage("shootingInfoVistrue");
            }
            else socket.sendMessage("shootingInfoVisfalse");
        }
    }

    @FXML
    private void restartGame() {
        serverTroops.clear();
        clientTroops.clear();
        resetGame();
    }

    @FXML
    private void sliderMoved() {
        turns = (int) movesSlider.getValue();
        totalBudget = (int) budgetSlider.getValue();
        clientMoney = totalBudget;
        serverMoney = totalBudget;
    }

    private void setSquareText(int i, int j, boolean hasFog) {
        if (hasFog) return;
        boolean hasText = false;
        if (gameGrid[i][j] != 0) {
            Troop troopChosenClient = TroopHelper.getTroopByCoords(i,j,clientTroops);
            Troop troopChosenServer = TroopHelper.getTroopByCoords(i,j,serverTroops);
            if (troopChosenClient != null) {
                btns[i][j].setText("C");
                socket.sendMessage("setBtnC " + i + " " + j);
                hasText = true;
            }
            if (troopChosenServer != null) {
                btns[i][j].setText("S");
                socket.sendMessage("setBtnS " + i + " " + j);
                hasText = true;
            }
        }
        if (!hasText) {
            btns[i][j].setText("");
            socket.sendMessage("setBtn " + i + " " + j);
        }
    }

    private void printSquare(int i, int j) throws ClassCastException{
        String serverColor;
        String clientColor;
        String defaultColor = "-fx-background-color:#" + colorCoder.get(environment[i][j]);
        if (gameGrid[i][j] != 0) {
            String troopColor = TroopHelper.getTroopByNumber(gameGrid[i][j],troopInfo).getColor();
            defaultColor = "-fx-background-color:" + troopColor;
            Troop currentTroop = TroopHelper.getTroopByCoords(i,j,serverTroops);
            if (currentTroop == null) {
                currentTroop = TroopHelper.getTroopByCoords(i,j,clientTroops);
            }
            if (currentTroop != null) {
                if (currentTroop.getCurrentHealth() <= 2.0/3 * currentTroop.getMaxHealth()) {
                    defaultColor = "-fx-background-color: linear-gradient(#b50e2d, #b50e2d, #" + troopColor + ")";
                }
                if (currentTroop.getCurrentHealth() <= 1.0/3 * currentTroop.getMaxHealth()) {
                    defaultColor = "-fx-background-color: linear-gradient(#b50e2d, #" + troopColor + ", #" + troopColor + ")";
                }
            }
        }
        serverColor = defaultColor;
        clientColor = defaultColor;

        if (placingTroops) {
            if (i > btns.length/2) serverColor = "-fx-background-color:#c7c3b9";
            else clientColor = "-fx-background-color:#c7c3b9";
        }

        btns[i][j].setStyle(serverColor);
        socket.sendMessage("fillSquare " + i + " " + j + " " + clientColor);
        //Sends each square to client
    }

    private void resetOpacity() {
        for (int i=0; i<btns.length; i++) {
            for (int j=0; j<btns[0].length; j++) {
                btns[i][j].setOpacity(1);
            }
        }
    }

    private void changeTurn() {
        if (turns == 0) {
            turns = 7;
            smokeDamage();
            addSmoke();
            smokeLength++;
            serverTurn = !serverTurn;
            resetOpacity();
            socket.sendMessage("resetOpacity");
            resetGame();
        }
    }

    private void smokeDamage() {
        for (Troop troop : serverTroops) {
            if (environment[troop.getX()][troop.getY()] == 2) {
                troop.currentHealth -= 5;
                die(troop,serverTroops);
            }
        }
        for (Troop troop : clientTroops) {
            if (environment[troop.getX()][troop.getY()] == 2) {
                troop.currentHealth -= 5;
                die(troop,clientTroops);
            }
        }
        resetGame();
    }

    private boolean inBounds(int x, int y) {
        if (x>=0 && x<gameGrid.length && y>= 0 && y< gameGrid[0].length){
            return true;
        }
        return false;
    }

    private int rNum(int start, int end) {
        return start + (int) (Math.random() * (end-start+1));
    }

    private void showClientTroopInfo(Troop troop) {
        socket.sendMessage("troopInfoVis");
        socket.sendMessage("troopName" + troop.getName());
        socket.sendMessage("troopHealth" + troop.getMaxHealth() + " " + troop.getCurrentHealth());
    }

    private void handleClientPlace(int x, int y, String troopName) {
        //*
        if (!placingTroops) return;
        if (x <= btns.length/2 || environment[x][y] != 0) {
            socket.sendMessage("updateCan't place a troop there!");
            return;
        }
        if (gameGrid[x][y] == 0) {
            if (!troopName.equals("Teleporter")) {
                if (TroopHelper.getTroopByName(troopName, troopInfo).getCost() > clientMoney) {
                    socket.sendMessage("updateNot enough money!");
                    return;
                }
                if (clientNeedsTeleporter) {
                    socket.sendMessage("updateNeed to place teleporter first!");
                    return;
                }
                gameGrid[x][y] = TroopHelper.getTroopByName(troopName, troopInfo).getNumber();
                addBasedOnTroop(clientTroops, x, y, troopName, false);
                clientMoney -= clientTroops.get(clientTroops.size() - 1).getCost();
            } else {
                //**
                troopChosenClient = TroopHelper.getTroopByName(troopName,troopInfo);
                if (numCLientTeleporters%2 == 0) {
                    if (troopChosenClient.getCost()*2 > clientMoney) {
                        socket.sendMessage("updateNot Enough Money!");
                        return;
                    }
                    gameGrid[x][y] = troopChosenClient.getNumber();
                    addBasedOnTroop(clientTroops, x, y, troopChosenClient.getName(), true);
                    clientMoney -= clientTroops.get(clientTroops.size() - 1).getCost();
                    socket.sendMessage("updateNeed to add another teleporter");
                    socket.sendMessage("fightBtntrue");
                    clientNeedsTeleporter = true;
                    numCLientTeleporters ++;
                    clientLastTeleporter = clientTroops.get(clientTroops.size()-1);
                } else {
                    gameGrid[x][y] = troopChosenClient.getNumber();
                    addBasedOnTroop(clientTroops, x, y, troopChosenClient.getName(), true);
                    clientMoney -= clientTroops.get(clientTroops.size() - 1).getCost();
                    clientLastTeleporter.pair = clientTroops.get(clientTroops.size()-1);
                    clientTroops.get(clientTroops.size()-1).pair = clientLastTeleporter;
                    clientLastTeleporter = clientTroops.get(clientTroops.size()-1);
                    socket.sendMessage("fightBtnfalse");
                    clientNeedsTeleporter = false;
                    numCLientTeleporters++;
                }
            }
        } else if (TroopHelper.getTroopByCoords(x,y,clientTroops) != null) {
            if (!clientReadyForFighting) socket.sendMessage("showRemoveBtn");
            lastTroopSelectedClient = TroopHelper.getTroopByCoords(x,y,clientTroops);
        }
        printScreen();
    }

    private void changeTroopUI() {
        if (lastTroopSelectedServer != null) {
            showTroopInfo(lastTroopSelectedServer);
            if (!serverReadyForFighting && placingTroops && !serverNeedsTeleporter) {
                removeTroopBtn.setVisible(true);
            } else {
                removeTroopBtn.setVisible(false);
            }
        } else {
            troopInfoRect.setVisible(false);
            troopNameLbl.setVisible(false);
            troopHealthLbl.setVisible(false);
            removeTroopBtn.setVisible(false);
        }

        if (lastTroopSelectedClient != null) {
            showClientTroopInfo(lastTroopSelectedClient);
            if (placingTroops && !clientReadyForFighting && !clientNeedsTeleporter) {
                socket.sendMessage("showRemoveBtn");
            } else {
                socket.sendMessage("hideRemoveBtn");
            }
        } else {
            socket.sendMessage("hideTroopInfo");
        }
    }

    private void changeRockUI() {
        if (serverTurn && !placingTroops && lastTroopSelectedServer != null) {
            if (rocksInArea(lastTroopSelectedServer) && lastTroopSelectedServer.getName().equals("Infantry")) breakRock.setVisible(true);
            else breakRock.setVisible(false);
            if (lastTroopSelectedServer.getNumRocks() > 0 && spaceForRocks(lastTroopSelectedServer) && !lastTroopSelectedServer.getInMotion() &&
                    lastTroopSelectedServer.getName().equals("Infantry")) {
                placeRock.setVisible(true);
                directionBox.setVisible(true);
            } else {
                placeRock.setVisible(false);
                directionBox.setVisible(false);
            }
        } else {
            placeRock.setVisible(false);
            directionBox.setVisible(false);
            breakRock.setVisible(false);
        }

        if (!serverTurn && !placingTroops && lastTroopSelectedClient != null) {
            if (rocksInArea(lastTroopSelectedClient) && lastTroopSelectedClient.getName().equals("Infantry")) socket.sendMessage("breakingtrue");
            else socket.sendMessage("breakingfalse");
            if (lastTroopSelectedClient.getNumRocks() > 0 && spaceForRocks(lastTroopSelectedClient) && !lastTroopSelectedClient.getInMotion()  &&
                    lastTroopSelectedClient.getName().equals("Infantry")) {
                socket.sendMessage("placingtrue");
            } else {
                socket.sendMessage("placingfalse");
            }
        } else {
            socket.sendMessage("breakingfalse");
            socket.sendMessage("placingfalse");
        }
    }

    private void handleClientChoice(int x, int y, boolean throughBtn) {
        //*
        troopChosenClient = TroopHelper.getTroopByCoords(x,y,clientTroops);
        if (troopChosenClient != null) {
            lastTroopSelectedClient = troopChosenClient;
            showClientTroopInfo(troopChosenClient);
            if (!serverTurn && (!clientReadyForFighting || !placingTroops)) {
                socket.sendMessage("resetOpacity");
                int[][] distances = lastTroopSelectedClient.djistraAlgorithm(troopChosenClient.getX(), troopChosenClient.getY(),gameGrid,environment,clientTroops);
                if (!lastTroopSelectedClient.getName().equals("Teleporter")) lowerOpacities(lastTroopSelectedClient, distances, false);
            }
            return;
        }

        Troop troopToAttack = TroopHelper.getTroopByCoords(x,y,serverTroops);
        if (serverTurn && throughBtn) {
            socket.sendMessage("updateIt's not your turn!");
            return;
        }
        if (troopToAttack != null) {
            if (lastTroopSelectedClient == null) {
                socket.sendMessage("Pick your troop first!");
                return;
            }
            if (serverTurn) return;
            troopTargetedClient = troopToAttack;
            turns--;
            changeTurn();
            die(troopToAttack, serverTroops);
            resetGame();
        }
    }

    private void moveTroop(Troop troopToMove, boolean serverTurn, int x, int y, ArrayList<Troop> friendlies) {
        int[][] distances = troopToMove.djistraAlgorithm(troopToMove.getX(), troopToMove.getY(),gameGrid,environment,serverTroops);
        if (!serverTurn) distances = troopToMove.djistraAlgorithm(troopToMove.getX(), troopToMove.getY(),gameGrid,environment,clientTroops);
        if (troopToMove.coordsList(distances, x, y, troopToMove.getX(), troopToMove.getY(), gameGrid, friendlies) == null) {
            troopToMove.move(gameGrid,x,y);
            if (!serverTurn) socket.sendMessage("updateA troop was struck by lightning and gained 2 health!");
            else  updates.getItems().add(0,"A troop was struck by lightning and gained 2 health!");
            troopToMove.currentHealth = Math.max(troopToMove.getCurrentHealth()+2,troopToMove.getMaxHealth());
        } else {
            troopToMove.path = troopToMove.coordsList(distances, x, y, troopToMove.getX(), troopToMove.getY(), gameGrid, friendlies);
            troopToMove.inMotion = true;
            troopsInMotion.add(troopToMove);
        }
    }

    private void removeATroop(Troop troopToRemove, ArrayList<Troop> list, boolean serverOrNot) {
        if (!serverOrNot) {
            clientMoney += troopToRemove.getCost();
            if (troopToRemove.getPair() != null) clientMoney += troopToRemove.getCost(); //Removing 2 troops
            lastTroopSelectedClient = null;
        } else {
            serverMoney += troopToRemove.getCost();
            if (troopToRemove.getPair() != null) serverMoney += troopToRemove.getCost();
            lastTroopSelectedServer = null;
        }
        troopToRemove.currentHealth = 0;
        die(troopToRemove,list);
    }

    class FxSocketListener implements SocketListener {
        @Override
        public void onMessage(String line) {
            if (line.equals("clientReady")) {
                clientReady = true;
                startBattle();
            } else if (line.startsWith("place")) {
                findIndexes(line, ' ');
                int size = indexes.size();
                String troopName = line.substring(indexes.get(0)+1, indexes.get(size-2));
                int x = Integer.parseInt(line.substring(indexes.get(size-2)+1, indexes.get(size-1)));
                int y = Integer.parseInt(line.substring(indexes.get(size-1)+1));
                if (line.indexOf("null") != -1) {
                    socket.sendMessage("updateChose a troop first!");
                    return;
                }
                handleClientPlace(x,y,troopName);
            } else if (line.startsWith("chose")) {
                findIndexes(line,' ');
                int x = Integer.parseInt(line.substring(indexes.get(0)+1, indexes.get(1)));
                int y = Integer.parseInt(line.substring(indexes.get(1)+1));
                handleClientChoice(x, y, true);

            } else if (line.startsWith("moveTo")) {
                if (!serverTurn) {
                    if (troopsInMotion.size() > 0) {
                        socket.sendMessage("updatesCan't move when troops are in motion!");
                        return;
                    }
                    findIndexes(line, ' ');
                    int x = Integer.parseInt(line.substring(indexes.get(0) + 1, indexes.get(1)));
                    int y = Integer.parseInt(line.substring(indexes.get(1) + 1));
                    moveTroop(lastTroopSelectedClient,false, x, y, clientTroops);
                    resetOpacity();
                    turns--;
                    changeTurn();
                } else {
                    socket.sendMessage("updateIt's not your turn!");
                }
            } else if (line.equals("clientReadyToFight")) {
                clientReadyForFighting = true;
                socket.sendMessage("hideList");
                if (serverReadyForFighting) fightingStart();
            } else if (line.startsWith("btnExit")) {
                findIndexes(line, ' ');
                int x = Integer.parseInt(line.substring(indexes.get(0)+1, indexes.get(1)));
                int y = Integer.parseInt(line.substring(indexes.get(1)+1).trim());
                printSquare(x,y);
            } else if (line.startsWith("getTroopDes")) {
                String troopName = line.substring(11);
                socket.sendMessage("setTroopInfo" + TroopHelper.getTroopByName(troopName,troopInfo).getDescription());
            } else if (line.equals("removeTroop")) {
                if (lastTroopSelectedClient == null) return;
                removeATroop(lastTroopSelectedClient,clientTroops,false);
            } else if (line.equals("breakRock")) {
                if (lastTroopSelectedClient == null || !rocksInArea(lastTroopSelectedClient)) return;
                troopBreakRock(false);
            } else if (line.equals("putRock")) {
                if (lastTroopSelectedClient == null || !spaceForRocks(lastTroopSelectedClient) || lastTroopSelectedClient.getNumRocks() == 0) return;
                int[] dirCoords = giveDirectionsFromString(lastDirection);
                if (dirCoords == null) {
                    socket.sendMessage("updateInvalid direction");
                    return;
                }
                int xChange = dirCoords[0];
                int yChange = dirCoords[1];
                int newX = lastTroopSelectedClient.getX() + xChange;
                int newY = lastTroopSelectedClient.getY() + yChange;
                if (!inBounds(newX,newY)) {
                    socket.sendMessage("updateCoords out of bounds");
                    return;
                } else if (gameGrid[newX][newY] != 0 || environment[newX][newY] != 0) {
                    socket.sendMessage("updateCan't place a rock there");
                    return;
                }
                environment[newX][newY] = 1;
                lastTroopSelectedClient.numRocks --;
                turns--;
                changeTurn();
            } else if (line.startsWith("dir")) {
                lastDirection = line.substring(3);
            } else if (line.equals("shootTroop")) {
                if (lastTroopSelectedClient != null && troopTargetedClient != null) {
                    troopShot(lastTroopSelectedClient, troopTargetedClient, false);
                }
            }
        }
        @Override
        public void onClosedStatus(boolean isClosed) {

        }
    }
}