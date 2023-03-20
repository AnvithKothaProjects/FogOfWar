package com.example.starterfile;

public class Infantry extends Troop {
    public Infantry() {
        setVars();
    }

    public Infantry(int x, int y) {
        setVars();

        this.x = x;
        this.y = y;
    }

    private void setVars() {
        name = "Infantry";
        color = "db910f";
        number = 3;
        range = 5;
        description = "An inexpensive troop with low damage, high range, and average movement. It also has the ability to break and place";
        chanceLostPerSquare = 0.01;
        maxHealth = rNum(30,40);
        currentHealth = maxHealth;
        cost = 5000;
        damageDealt = 10;
    }
}
