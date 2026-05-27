package fr.iatic.theisland.model;

public class Creature {
    private CreatureType type;
    private HexCoordinate position;

    public Creature(CreatureType type, HexCoordinate position) {
        this.type = type;
        this.position = position;
    }

    public CreatureType getType() {
        return type;
    }

    public HexCoordinate getPosition() {
        return position;
    }

    public void setPosition(HexCoordinate position) {
        this.position = position;
    }

    public int getMaxMoves() {
        return switch (type) {
            case SERPENT -> 1;
            case REQUIN -> 2;
            case BALEINE -> 3;
        };
    }

    @Override
    public String toString() {
        return type + " à " + position;
    }
}