package fr.iatic.theisland.model;

import java.util.Random;

public class Dice {
    private static final Random RANDOM = new Random();

    public static CreatureType roll() {
        int value = RANDOM.nextInt(3);
        return switch (value) {
            case 0 -> CreatureType.SERPENT;
            case 1 -> CreatureType.REQUIN;
            case 2 -> CreatureType.BALEINE;
            default -> CreatureType.SERPENT;
        };
    }
}