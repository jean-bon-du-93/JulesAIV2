import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameFrame extends JFrame {

    private GamePanel gamePanel;

    public GameFrame() {
        setTitle("Snake Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // setResizable(false); // Decided to keep it resizable for now, can be changed later if needed.

        // Menu Panel
        JPanel menuPanel = new JPanel();
        // Using FlowLayout by default for the menu buttons, could change to GridLayout if specific ordering is paramount.
        // menuPanel.setLayout(new GridLayout(1, 4)); // Example for horizontal layout

        JButton playButton = new JButton("Play Manually");
        JButton trainButton = new JButton("Train AI");
        JButton watchButton = new JButton("Watch AI Play");
        JButton exitButton = new JButton("Exit");

        playButton.addActionListener(e -> {
            System.out.println("Play Manually clicked");
            gamePanel.startManualGame(); 
        });
        trainButton.addActionListener(e -> {
            System.out.println("Train AI clicked");
            gamePanel.startTrainAI();
        });
        watchButton.addActionListener(e -> {
            System.out.println("Watch AI Play clicked");
            gamePanel.startWatchAI();
        });
        exitButton.addActionListener(e -> System.exit(0)); // Exits the application

        menuPanel.add(playButton);
        menuPanel.add(trainButton);
        menuPanel.add(watchButton);
        menuPanel.add(exitButton);

        // Game Panel
        gamePanel = new GamePanel(); // GamePanel should have its own preferred size

        setLayout(new BorderLayout());
        add(menuPanel, BorderLayout.NORTH); // Menu at the top
        add(gamePanel, BorderLayout.CENTER); // Game area in the center

        pack(); // Sizes the frame so all contents are at or above their preferred sizes
        setLocationRelativeTo(null); // Centers the window on the screen
        // setVisible(true); // Visibility is handled by Main.java creating an instance of GameFrame.
                           // If GameFrame was run standalone, this would be needed.
                           // The previous version had it, Main.java also has it.
                           // Let's rely on Main.java to make it visible.
    }

    // Optional main method for testing GameFrame independently
    /*
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GameFrame().setVisible(true); // Make sure to set visible if running standalone
        });
    }
    */
}
