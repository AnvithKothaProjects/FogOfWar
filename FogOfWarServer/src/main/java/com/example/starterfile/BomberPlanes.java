package com.example.starterfile;

public class BomberPlanes extends Troop {
    public BomberPlanes() {
        setVars();
    }

    public BomberPlanes(int x, int y) {
        setVars();

        this.x = x;
        this.y = y;
    }

    private void setVars() {
        name = "Battle Plane";
        color = "1a5a87";
        number = 2;
        range = 8;
        description = "A troop with good movement, average damage, average health, and poor range";
        chanceLostPerSquare = .05;
        maxHealth = rNum(40,60);
        currentHealth = maxHealth;
        cost = 7500;
        damageDealt = 15;
    }
}
