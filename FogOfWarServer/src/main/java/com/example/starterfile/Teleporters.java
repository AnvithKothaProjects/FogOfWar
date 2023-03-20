package com.example.starterfile;

public class Teleporters extends Troop {
    public Teleporters() {
        setVars();
    }

    public Teleporters(int x, int y) {
        setVars();

        this.x = x;
        this.y = y;
    }

    private void setVars() {
        name = "Teleporter";
        color = "847582";
        number = 1;
        range = 5;
        description = "Teleports troops that enter it to its pair teleporter";
        maxHealth = rNum(70,80);
        currentHealth = maxHealth;
        cost = 5000;
    }
}
