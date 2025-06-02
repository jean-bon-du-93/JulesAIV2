import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
// Assuming Snake, Food, State, AIPlayer classes are in the default package

public class GamePanel extends JPanel {

    // --- Constants ---
    static final int SCREEN_WIDTH = 600;
    static final int SCREEN_HEIGHT = 600;
    static final int UNIT_SIZE = 25;
    static final int GAME_UNITS_X = SCREEN_WIDTH / UNIT_SIZE;
    static final int GAME_UNITS_Y = SCREEN_HEIGHT / UNIT_SIZE;
    
    private static final int MANUAL_DELAY = 150; // Game speed for manual/watch mode
    private static final int TRAINING_DELAY = 1;  // Faster speed for AI training
    private static final int AI_SCORE_WINDOW = 100; // For calculating average score

    // --- Rewards for AI ---
    private static final double FOOD_REWARD = 50.0;
    private static final double GAMEOVER_REWARD = -100.0;
    private static final double STEP_REWARD = -1.0; // Small penalty per step

    // --- Game Mode ---
    public enum GameMode { MANUAL, TRAIN_AI, WATCH_AI }
    private GameMode currentMode = GameMode.MANUAL;

    // --- Game State Variables ---
    private Snake snake;
    private Food food;
    private Timer gameTimer;
    private boolean running = false;
    private int score = 0;
    private int bestScore = 0; 

    // --- AI Related Fields ---
    private AIPlayer aiPlayer;
    private long gamesPlayedAI = 0;
    private double averageScoreAI = 0.0;
    private List<Integer> recentScoresAI = new ArrayList<>();
    
    // --- Input Handling ---
    private MyKeyAdapter keyAdapter;

    public GamePanel() {
        this.keyAdapter = new MyKeyAdapter();
        this.aiPlayer = new AIPlayer(); // Initialize AI Player (loads Q-table)

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.black);
        this.setFocusable(true);
        this.addKeyListener(keyAdapter);
        
        // Initialize game timer but don't start it yet. Delay will be set by mode.
        // The initial action listener is a generic one, will be overwritten by specific game loop.
        // Or, we can simply make gameTimer null here and initialize in startGameLogic
        this.gameTimer = new Timer(MANUAL_DELAY, this::actionPerformedGameLoop); 
    }

    // --- Mode Control Methods ---
    public void startManualGame() {
        currentMode = GameMode.MANUAL;
        System.out.println("Starting Manual Game Mode.");
        if (gameTimer != null) gameTimer.setDelay(MANUAL_DELAY);
        startGameLogic();
    }

    public void startTrainAI() {
        currentMode = GameMode.TRAIN_AI;
        System.out.println("Starting AI Training Mode.");
        aiPlayer.setEpsilon(1.0); // Reset epsilon for new training session
        gamesPlayedAI = 0;
        recentScoresAI.clear();
        averageScoreAI = 0.0;
        if (gameTimer != null) gameTimer.setDelay(TRAINING_DELAY); else System.err.println("Timer null in startTrainAI");
        startGameLogic();
    }

    public void startWatchAI() {
        currentMode = GameMode.WATCH_AI;
        System.out.println("Starting Watch AI Mode.");
        aiPlayer.setEpsilon(0.0); // No exploration when watching
        if (gameTimer != null) gameTimer.setDelay(MANUAL_DELAY); // Watch at human speed
        startGameLogic();
    }
    
    // --- Core Game Initialization Logic ---
    private void startGameLogic() {
        running = false; // Stop current game if any
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }

        snake = new Snake(GAME_UNITS_X / 4, GAME_UNITS_Y / 2, 5, 'R', GAME_UNITS_X, GAME_UNITS_Y);
        food = new Food(GAME_UNITS_X, GAME_UNITS_Y);
        food.spawn(snake);

        score = 0;
        running = true;

        if (gameTimer == null) { // Should ideally be initialized in constructor
             System.err.println("CRITICAL: gameTimer was null in startGameLogic. Re-initializing.");
             int delay = (currentMode == GameMode.TRAIN_AI) ? TRAINING_DELAY : MANUAL_DELAY;
             gameTimer = new Timer(delay, this::actionPerformedGameLoop);
        } else { // Ensure correct delay for the current mode
            int delay = (currentMode == GameMode.TRAIN_AI) ? TRAINING_DELAY : MANUAL_DELAY;
            if (currentMode == GameMode.WATCH_AI) delay = MANUAL_DELAY; // Watch AI runs at manual speed
            gameTimer.setDelay(delay);
        }
        
        gameTimer.start();
        this.requestFocusInWindow();
        repaint();
    }

    // --- Main Game Loop ---
    private void actionPerformedGameLoop(ActionEvent e) {
        if (running) {
            State oldStateForAI = null;
            int actionForAI = -1; // 0: left, 1: straight, 2: right

            if (currentMode == GameMode.TRAIN_AI || currentMode == GameMode.WATCH_AI) {
                if (snake == null || food == null) { // Safety check
                    running = false; 
                } else {
                    oldStateForAI = aiPlayer.getCurrentState(snake, food, GAME_UNITS_X, GAME_UNITS_Y);
                    actionForAI = aiPlayer.chooseAction(oldStateForAI, currentMode == GameMode.TRAIN_AI);
                    performAIAction(actionForAI);
                }
            }
            // In MANUAL mode, snake direction is set by MyKeyAdapter

            if (running) { // Check if still running after AI might have found no snake/food
                snake.move();

                boolean gameOver = snake.checkCollisionWithWall() || snake.checkCollisionWithSelf();
                boolean foodEatenThisTick = false;
                double reward = STEP_REWARD; // Default reward for taking a step

                if (!gameOver && snake.getHead().equals(food.getPosition())) {
                    foodEatenThisTick = true;
                    snake.grow();
                    score++;
                    if (score > bestScore) bestScore = score;
                    food.spawn(snake);
                    reward = FOOD_REWARD;
                }

                if (gameOver) {
                    running = false;
                    reward = GAMEOVER_REWARD;
                }

                if (currentMode == GameMode.TRAIN_AI && oldStateForAI != null) {
                    State newStateForAI = gameOver ? null : aiPlayer.getCurrentState(snake, food, GAME_UNITS_X, GAME_UNITS_Y);
                    aiPlayer.updateQValue(oldStateForAI, actionForAI, reward, newStateForAI, gameOver);
                }
            }
        } // end if(running) initial check

        repaint(); // Always repaint

        if (!running) { // Game has just ended or was already not running
            if (gameTimer.isRunning()) {
                gameTimer.stop();
            }
            if (currentMode == GameMode.TRAIN_AI && snake != null) { // snake != null indicates game was actually running
                gamesPlayedAI++;
                recentScoresAI.add(score);
                if (recentScoresAI.size() > AI_SCORE_WINDOW) {
                    recentScoresAI.remove(0);
                }
                if (!recentScoresAI.isEmpty()) {
                    averageScoreAI = recentScoresAI.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                }
                aiPlayer.decayEpsilon();
                if (gamesPlayedAI % 1000 == 0) { // Save Q-table periodically
                    aiPlayer.saveQTable();
                    System.out.println("QTable saved at game: " + gamesPlayedAI + ", Epsilon: " + String.format("%.3f", aiPlayer.getEpsilon()));
                }
                startGameLogic(); // Automatically restart for next training episode
            }
            // For Manual/Watch AI, game over screen shows. MyKeyAdapter handles manual restart.
            // Watch AI currently stops on game over.
        }
    }
    
    private void performAIAction(int relativeAction) { // 0: left, 1: straight, 2: right
        char currentDir = snake.getDirection();
        char newDir = currentDir;

        switch (currentDir) {
            case 'U':
                if (relativeAction == 0) newDir = 'L';      // Relative Left
                else if (relativeAction == 2) newDir = 'R'; // Relative Right
                break;
            case 'D':
                if (relativeAction == 0) newDir = 'R';      // Relative Left
                else if (relativeAction == 2) newDir = 'L'; // Relative Right
                break;
            case 'L':
                if (relativeAction == 0) newDir = 'D';      // Relative Left
                else if (relativeAction == 2) newDir = 'U'; // Relative Right
                break;
            case 'R':
                if (relativeAction == 0) newDir = 'U';      // Relative Left
                else if (relativeAction == 2) newDir = 'D'; // Relative Right
                break;
        }
        snake.setDirection(newDir);
    }

    // --- Drawing Methods ---
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (running) {
            drawGrid(g);
            drawFood(g);
            drawSnake(g);
            drawScores(g); // Also draws AI stats if in TRAIN_AI mode
        } else {
            if (snake != null) { // Game has been run at least once to show "Game Over"
                 drawGameOver(g);
            } else { // Initial state before any game mode starts
                drawScores(g); // Show initial Score: 0, Best: 0
                // Optionally, add a "Select a mode" message
                 g.setColor(Color.WHITE);
                 g.setFont(new Font("SansSerif", Font.BOLD, 30));
                 FontMetrics metrics = getFontMetrics(g.getFont());
                 String welcomeMsg = "Select a mode to start!";
                 g.drawString(welcomeMsg, (SCREEN_WIDTH - metrics.stringWidth(welcomeMsg)) / 2, SCREEN_HEIGHT / 2);
            }
        }
    }

    public void drawGrid(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= GAME_UNITS_X; i++) {
            g.drawLine(i * UNIT_SIZE, 0, i * UNIT_SIZE, SCREEN_HEIGHT);
        }
        for (int i = 0; i <= GAME_UNITS_Y; i++) {
            g.drawLine(0, i * UNIT_SIZE, SCREEN_WIDTH, i * UNIT_SIZE);
        }
    }

    public void drawFood(Graphics g) {
        if (food != null && food.getPosition() != null) {
            g.setColor(Color.red);
            g.fillOval(food.getPosition().x * UNIT_SIZE, food.getPosition().y * UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);
        }
    }

    public void drawSnake(Graphics g) {
        if (snake != null && snake.getBody() != null) {
            List<Point> body = snake.getBody();
            for (int i = 0; i < body.size(); i++) {
                Point segment = body.get(i);
                g.setColor((i == 0) ? Color.green : new Color(45, 180, 0));
                g.fillRect(segment.x * UNIT_SIZE, segment.y * UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);
            }
        }
    }

    public void drawScores(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics metrics = getFontMetrics(g.getFont());
        String scoreText = "Score: " + this.score;
        g.drawString(scoreText, (SCREEN_WIDTH - metrics.stringWidth(scoreText)) / 2, g.getFont().getSize());
        String bestScoreText = "Best: " + this.bestScore;
        g.drawString(bestScoreText, SCREEN_WIDTH - metrics.stringWidth(bestScoreText) - 10, g.getFont().getSize());

        if (currentMode == GameMode.TRAIN_AI) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(Color.CYAN);
            String gamesText = "Games: " + gamesPlayedAI;
            g.drawString(gamesText, 10, SCREEN_HEIGHT - 70);
            String avgScoreText = String.format("Avg Score (last %d): %.2f", AI_SCORE_WINDOW, averageScoreAI);
            g.drawString(avgScoreText, 10, SCREEN_HEIGHT - 50);
            String epsilonText = String.format("Epsilon: %.3f", aiPlayer.getEpsilon());
            g.drawString(epsilonText, 10, SCREEN_HEIGHT - 30);
            if (aiPlayer != null) { // Safety check
                String qTableSizeText = "QTable Size: " + aiPlayer.getQTableSize();
                g.drawString(qTableSizeText, 10, SCREEN_HEIGHT - 10);
            }
        }
    }

    public void drawGameOver(Graphics g) {
        g.setColor(Color.red);
        g.setFont(new Font("SansSerif", Font.BOLD, 65));
        FontMetrics metrics1 = getFontMetrics(g.getFont());
        g.drawString("Game Over", (SCREEN_WIDTH - metrics1.stringWidth("Game Over")) / 2, SCREEN_HEIGHT / 3);

        g.setColor(Color.white);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        String finalScoreMsg = "Final Score: " + score;
        g.drawString(finalScoreMsg, (SCREEN_WIDTH - metrics2.stringWidth(finalScoreMsg)) / 2, SCREEN_HEIGHT / 2);

        if (currentMode == GameMode.MANUAL) {
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            FontMetrics metrics3 = getFontMetrics(g.getFont());
            String restartMsg = "Press Enter to Restart";
            g.drawString(restartMsg, (SCREEN_WIDTH - metrics3.stringWidth(restartMsg)) / 2, SCREEN_HEIGHT - (SCREEN_HEIGHT / 4));
        }
    }

    // --- Input Handling ---
    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();

            if (running) {
                if (currentMode == GameMode.MANUAL && snake != null) {
                    switch (keyCode) {
                        case KeyEvent.VK_LEFT:  if (snake.getDirection() != 'R') snake.setDirection('L'); break;
                        case KeyEvent.VK_RIGHT: if (snake.getDirection() != 'L') snake.setDirection('R'); break;
                        case KeyEvent.VK_UP:    if (snake.getDirection() != 'D') snake.setDirection('U'); break;
                        case KeyEvent.VK_DOWN:  if (snake.getDirection() != 'U') snake.setDirection('D'); break;
                    }
                }
            } else { // If game is not running
                if (keyCode == KeyEvent.VK_ENTER) {
                    if (currentMode == GameMode.MANUAL) {
                        startManualGame();
                    }
                    // For AI modes, Enter currently does nothing on game over screen.
                    // Training auto-restarts. Watch AI stops.
                }
            }
        }
    }
}
