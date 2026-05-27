package fr.iatic.theisland;

import fr.iatic.theisland.ui.WelcomeFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public final class App {

    private App() {
       
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Si Nimbus n'est pas disponible, Swing utilise le thème par défaut.
        }

        SwingUtilities.invokeLater(() -> {
            WelcomeFrame frame = new WelcomeFrame();
            frame.setVisible(true);
        });
    }
}
