package com.example.starterfile;

import java.util.ArrayList;

public class Troop {
    protected String name;
    protected String color;
    protected int cost;
    protected int number;
    protected int x;
    protected int y;
    protected int range;
    protected boolean inMotion;
    protected int maxHealth;
    protected int currentHealth;
    protected String description;
    protected int numRocks = 0;
    protected ArrayList<int[]> path;
    protected double chanceLostPerSquare;
    protected int damageDealt;

    //This variable is for teleporters only
    protected Troop pair;

    public Troop() {

    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public String getColor() {return color;}

    public int getCost() {return cost;}

    public int getX() {return x;}

    public int getY() {return y;}

    public int getRange() {return range;}

    public ArrayList<int[]> getPath() {return path;}

    public boolean getInMotion() {return inMotion;}

    public int getMaxHealth() {return maxHealth;}

    public int getCurrentHealth() {return currentHealth;}

    public String getDescription() {return description;}

    public int getNumRocks() {return numRocks;}

    public Troop getPair() {return pair;}

    public double getChanceLostPerSquare() {return chanceLostPerSquare;}

    public void move(int[][] gameGrid, int newX, int newY) {
        gameGrid[x][y] = 0;
        gameGrid[newX][newY] = number;
        x = newX;
        y = newY;
    }

    public int[][] djistraAlgorithm(int initX, int initY, int[][] gameGrid, int[][] environment, ArrayList<Troop> friendlyTroops) {
        int[][] newGrid = new int[gameGrid.length][gameGrid[0].length];

        ArrayList<int[]> queue = new ArrayList<>();
        for (int i=0; i<gameGrid.length; i++) {
            for (int j=0; j<gameGrid[0].length; j++) {
                newGrid[i][j] = -1;
            }
        }

        queue.add(new int[]{initX, initY});
        newGrid[initX][initY] = 0;

        while (queue.size() > 0) {
            int[] nextCoord = queue.get(0);
            int x = nextCoord[0];
            int y = nextCoord[1];
            boolean teleporterHandled = false;
            for (int i=-1; i<2; i++) {
                for (int j=-1; j<2; j++) {
                    int newX = nextCoord[0]+i;
                    int newY = nextCoord[1]+j;
                    if (inBounds(newX,newY,gameGrid) && (newGrid[newX][newY] == -1 || newGrid[newX][newY] > newGrid[x][y]+1) && !(x == newX && y == newY)
                            && gameGrid[newX][newY] == 0 && environment[newX][newY] != 1) {
                        newGrid[newX][newY] = newGrid[x][y]+1;
                        queue.add(new int[] {newX,newY});
                    }
                    if (inBounds(newX,newY,gameGrid) && gameGrid[newX][newY] == 1 && (newGrid[newX][newY] == -1 || newGrid[newX][newY] > newGrid[x][y]+1)
                    && TroopHelper.getTroopByCoords(newX,newY,friendlyTroops) != null) {
                        newGrid[newX][newY] = newGrid[x][y]+1;
                        queue.add(new int[] {newX,newY});
                    }
                    if (gameGrid[x][y] == 1 && !teleporterHandled) {
                        Troop teleporter = TroopHelper.getTroopByCoords(x,y,friendlyTroops);
                        if (teleporter != null) {
                            Troop pair = teleporter.getPair();
                            newX = pair.getX();
                            newY = pair.getY();
                            if ((newGrid[newX][newY] == -1 || newGrid[newX][newY] > newGrid[x][y] + 1)) {
                                newGrid[newX][newY] = newGrid[x][y] + 1;
                                queue.add(new int[]{newX, newY});
                            }
                            teleporterHandled = true;
                        }
                    }
                }
            }
            queue.remove(0);
        }

        return newGrid;
    }

    public ArrayList<int[]> coordsList(int[][] newList, int endX, int endY, int startX, int startY, int[][] gameGrid, ArrayList<Troop> friendlyTroops) {
        int count = 0;
        newList[endX][endY] = 10000000;
        ArrayList<int[]> coords = new ArrayList<>();
        int[] currentCoord = new int[] {endX,endY};
        coords.add(new int[]{endX,endY});
        while (currentCoord[0] != startX || currentCoord[1] != startY) {
            if (count > 1000) return null;
            int x = currentCoord[0];
            int y = currentCoord[1];
            boolean coordFound = false;
            for (int i=-1; i<2; i++) {
                if (coordFound) break;
                for (int j=-1; j<2; j++) {
                    count++;
                    if (count > 1000) return null;
                    int newX = currentCoord[0]+i;
                    int newY = currentCoord[1]+j;
                    if (count > 1000) return null;
                    if (inBounds(newX,newY,newList) && newList[newX][newY] != -1 && newList[newX][newY] < newList[x][y]) {
                        if (count > 1000) return null;
                        coords.add(new int[] {newX,newY});
                        currentCoord = new int[] {newX,newY};
                        coordFound = true;
                        break;
                    }
                }
            }
            if (!coordFound) {
                if (gameGrid[x][y] == 1 && TroopHelper.getTroopByCoords(x,y,friendlyTroops) != null) {
                    Troop teleporter = TroopHelper.getTroopByCoords(x,y,friendlyTroops);
                    Troop pair = teleporter.getPair();
                    int newX = pair.getX();
                    int newY = pair.getY();
                    if (inBounds(newX,newY,newList) && newList[newX][newY] != -1 && newList[newX][newY] < newList[x][y]) {
                        if (count > 1000) return null;
                        coords.add(new int[] {newX,newY});
                        currentCoord = new int[] {newX,newY};
                    }
                }
            }
        }
        return coords;
    }

    private boolean inBounds(int x, int y, int[][] gameGrid) {
        if (x>=0 && x<gameGrid.length && y>= 0 && y< gameGrid[0].length){
            return true;
        }
        return false;
    }

    private int round(double num) {
        return (int) (num + .5);
    }

    public double getHitChance(Troop target, int[][] gameGrid, int[][] environment) {
        double chance = 1;
        double yDist = target.getY()-y;
        double xDist = target.getX()-x;
        ArrayList<int[]> badCoords = new ArrayList<>();
        if (xDist == 0) {
            yDist = .2;
            xDist = 0;
        } else {
            yDist /= (xDist * 5);
            xDist = .2;
        }
        double currentX = x;
        double currentY = y;
        while ((round(currentX) != target.getX() || round(currentY) != target.getY()) && inBounds((int) currentX,(int) currentY,gameGrid)) {
            currentX += xDist;
            currentY += yDist;
            int newX = round(currentX);
            int newY = round(currentY);
            if (inBounds(newX,newY,gameGrid) && (newX != x || newY != y) &&
                    (newX != target.getX() || newY != target.getY()) &&
                    (gameGrid[newX][newY] == 1 || environment[newX][newY] == 1) &&
                !contains(badCoords,new int[] {newX,newY})) {
                chance *= (.85);
                badCoords.add(new int[] {newX,newY});
            }
        }
        chance -= findRange(target.getX(),target.getY(),x,y)*chanceLostPerSquare;
        return Math.max(chance,0);
    }

    private boolean contains(ArrayList<int[]> lists, int[] list) {
        for (int[] newList : lists) {
            if (list[0] == newList[0] && list[1] == newList[1]) return true;
        }
        return false;
    }

    private double findRange(int newX, int newY, int oldX, int oldY) {
        return Math.sqrt(Math.pow(Math.abs(newX-newY),2) + Math.pow(Math.abs(newY-oldY),2));
    }

    protected int rNum(int start, int end) {
        return start + (int) (Math.random() * (end-start+1));
    }
}
