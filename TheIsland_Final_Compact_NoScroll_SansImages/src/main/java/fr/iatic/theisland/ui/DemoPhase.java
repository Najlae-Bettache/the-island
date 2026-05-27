package fr.iatic.theisland.ui;

/**
 * Phases du déroulement du jeu — Jeu complet.
 */
public enum DemoPhase {
    SETUP_EXPLORERS,
    SETUP_BOATS,
    PLAY_HAND_TILE,   // mission 3 : jouer une tuile en main (optionnel)
    MOVEMENT,
    TILE_REMOVAL,     // mission 3 : retirer une tuile
    CREATURE_DICE,    // mission 4 : lancer le dé
    CREATURE_MOVE,    // mission 4 : déplacer la créature
    GAME_OVER
}
