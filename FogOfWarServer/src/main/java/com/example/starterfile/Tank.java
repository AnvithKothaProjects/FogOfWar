package com.example.starterfile;

public class Tank extends Troop {
    public Tank() {
        setVars();
    }

    public Tank(int x, int y) {
        setVars();

        this.x = x;
        this.y = y;
    }

    private void setVars() {
        name = "Tank";
        color = "c8d149";
        number = 4;
        range = 3;
        description = "A troop with high health, high damage, and low movement. It is the most expensive troop.";
        chanceLostPerSquare = .025;
        maxHealth = rNum(90,110);
        currentHealth = maxHealth;
        cost = 15000;
        damageDealt = 20;
    }
}
