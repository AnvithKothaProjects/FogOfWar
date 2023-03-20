package com.example.starterfile;

import java.util.ArrayList;

public class TroopHelper {
    public static Troop getTroopByName(String name, ArrayList<Troop> troopInfo) {
        if (name == null) return null;
        for (Troop troop : troopInfo) if (troop.getName().equals(name)) return troop;
        return null;
    }

    public static Troop getTroopByNumber(int num, ArrayList<Troop> troopInfo) {
        for (Troop troop : troopInfo) if (troop.getNumber() == num) return troop;
        return null;
    }

    public static Troop getTroopByCost(int num, ArrayList<Troop> troopInfo) {
        for (Troop troop : troopInfo) if (troop.getCost() == num) return troop;
        return null;
    }

    public static Troop getTroopByCoords(int x, int y, ArrayList<Troop> troops) {
        for (Troop troop : troops) if (x == troop.getX() && y == troop.getY() && !troop.getInMotion()) return troop;
        return null;
    }

    public static Teleporters getTeleporterByTroop(Troop troop, ArrayList<Troop> troops) {
        for (Troop troop1 : troops) if (troop.equals(troop)) return (Teleporters) troop;
        return null;
    }
}
