import java.util.*;
import java.io.*;
import java.awt.Point;

public class AIPlayer {

    // --- Q-learning Constants ---
    private static final double ALPHA = 0.1; // Learning rate
    private static final double GAMMA = 0.9; // Discount factor
    private double epsilon = 1.0;      // Exploration rate
    private static final double MIN_EPSILON = 0.01;
    private static final double EPSILON_DECAY_RATE = 0.999; // For multiplicative decay

    // --- Q-Table ---
    // Stores Q-values for (State, Action) pairs. Action: 0=left, 1=straight, 2=right
    private Map<State, double[]> qTable;

    // --- Other Fields ---
    private Random random;
    private static final String Q_TABLE_FILE = "q_table.dat";

    public AIPlayer() {
        this.qTable = new HashMap<>();
        this.random = new Random();
        loadQTable(); // Load Q-table from file if it exists
    }

    // --- State Calculation ---

    /**
     * Calculates the current state of the game from the AI's perspective.
     *
     * @param snake The current snake object.
     * @param food The current food object.
     * @param boardWidth The width of the game board in game units.
     * @param boardHeight The height of the game board in game units.
     * @return The current State record.
     */
    public State getCurrentState(Snake snake, Food food, int boardWidth, int boardHeight) {
        Point head = snake.getHead();
        Point foodPos = food.getPosition();
        char currentDirection = snake.getDirection();
        List<Point> snakeBody = snake.getBody();

        if (head == null || foodPos == null) {
            // This case should ideally not be reached if game is active.
            // Return a default or "error" state, or handle as appropriate.
            // For now, creating a dummy state to avoid null pointers downstream.
            System.err.println("Warning: Snake head or food position is null. Returning default state.");
            return new State(0, 0, true, true, true, 'U'); 
        }

        // 1. Food relative position
        int foodDeltaXSign = Integer.compare(foodPos.x, head.x);
        int foodDeltaYSign = Integer.compare(foodPos.y, head.y);

        // 2. Obstacle detection (relative to snake's current direction)
        Point pointStraight = getRelativePoint(head, currentDirection, 0); // 0 for straight
        Point pointLeft = getRelativePoint(head, currentDirection, -1);   // -1 for left turn
        Point pointRight = getRelativePoint(head, currentDirection, 1);    // 1 for right turn

        boolean obsStraight = isObstacleAt(pointStraight, snakeBody, boardWidth, boardHeight);
        boolean obsLeft = isObstacleAt(pointLeft, snakeBody, boardWidth, boardHeight);
        boolean obsRight = isObstacleAt(pointRight, snakeBody, boardWidth, boardHeight);
        
        return new State(foodDeltaXSign, foodDeltaYSign, obsLeft, obsStraight, obsRight, currentDirection);
    }

    /**
     * Helper to determine the coordinates of a point relative to the snake's head and direction.
     * @param head The snake's current head position.
     * @param currentSnakeDirection The snake's current absolute direction.
     * @param relativeAction -1 for turning left, 0 for straight, 1 for turning right.
     * @return The Point representing the potential next position.
     */
    private Point getRelativePoint(Point head, char currentSnakeDirection, int relativeAction) {
        Point p = new Point(head);
        char actionDirection = currentSnakeDirection; // Default to straight

        if (relativeAction == -1) { // Turn Left
            switch (currentSnakeDirection) {
                case 'U': actionDirection = 'L'; break;
                case 'D': actionDirection = 'R'; break;
                case 'L': actionDirection = 'D'; break;
                case 'R': actionDirection = 'U'; break;
            }
        } else if (relativeAction == 1) { // Turn Right
            switch (currentSnakeDirection) {
                case 'U': actionDirection = 'R'; break;
                case 'D': actionDirection = 'L'; break;
                case 'L': actionDirection = 'U'; break;
                case 'R': actionDirection = 'D'; break;
            }
        }
        // If relativeAction == 0, actionDirection remains currentSnakeDirection (straight)

        switch (actionDirection) {
            case 'U': p.y--; break;
            case 'D': p.y++; break;
            case 'L': p.x--; break;
            case 'R': p.x++; break;
        }
        return p;
    }

    /**
     * Helper to check if a given point is an obstacle (wall or snake's own body).
     * @param p The point to check.
     * @param snakeBody The list of points representing the snake's body.
     * @param boardWidth Board width in game units.
     * @param boardHeight Board height in game units.
     * @return True if the point is an obstacle, false otherwise.
     */
    private boolean isObstacleAt(Point p, List<Point> snakeBody, int boardWidth, int boardHeight) {
        // Check wall collision
        if (p.x < 0 || p.x >= boardWidth || p.y < 0 || p.y >= boardHeight) {
            return true;
        }
        // Check self-collision
        // An AI might want to know if the target square *will be* occupied.
        // For this state representation, checking current body is standard.
        // The snake's actual move logic handles not dying to its departing tail.
        for (Point segment : snakeBody) {
            if (p.equals(segment)) {
                return true;
            }
        }
        return false;
    }

    // --- Action Selection ---
    /**
     * Chooses an action based on the current state using epsilon-greedy strategy.
     * Actions: 0 (turn left), 1 (go straight), 2 (turn right).
     * @param state The current game state.
     * @param isTraining True if the AI is in training mode (allows exploration).
     * @return The chosen action (0, 1, or 2).
     */
    public int chooseAction(State state, boolean isTraining) {
        qTable.putIfAbsent(state, new double[3]); // Ensure Q-values exist for this state

        if (isTraining && random.nextDouble() < epsilon) {
            return random.nextInt(3); // Explore: choose a random action
        } else {
            // Exploit: choose the action with the highest Q-value
            double[] qValues = qTable.get(state);
            int bestAction = 0;
            for (int i = 1; i < qValues.length; i++) {
                if (qValues[i] > qValues[bestAction]) {
                    bestAction = i;
                }
            }
            return bestAction;
        }
    }

    // --- Q-Value Update ---
    /**
     * Updates the Q-value for a given state-action pair.
     * @param state The state before the action was taken.
     * @param action The action taken (0, 1, or 2).
     * @param reward The reward received after taking the action.
     * @param nextState The state after the action was taken.
     * @param gameOver True if the game ended after this action.
     */
    public void updateQValue(State state, int action, double reward, State nextState, boolean gameOver) {
        qTable.putIfAbsent(state, new double[3]); // Ensure current state Q-values exist
        double oldQValue = qTable.get(state)[action];
        double nextMaxQ = 0.0;

        if (!gameOver && nextState != null) {
            qTable.putIfAbsent(nextState, new double[3]); // Ensure next state Q-values exist
            double[] nextQValues = qTable.get(nextState);
            // Find max Q-value for the next state
            nextMaxQ = Arrays.stream(nextQValues).max().orElse(0.0);
        }

        // Q-learning formula
        double newQValue = oldQValue + ALPHA * (reward + GAMMA * nextMaxQ - oldQValue);
        qTable.get(state)[action] = newQValue;
    }

    // --- Epsilon Management ---
    public void decayEpsilon() {
        epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY_RATE);
    }

    public double getEpsilon() {
        return epsilon;
    }
    
    public void setEpsilon(double newEpsilon) {
        this.epsilon = Math.max(MIN_EPSILON, newEpsilon);
    }


    // --- Persistence ---
    @SuppressWarnings("unchecked")
    public void loadQTable() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Q_TABLE_FILE))) {
            qTable = (Map<State, double[]>) ois.readObject();
            System.out.println("Q-table loaded successfully from " + Q_TABLE_FILE);
            System.out.println("Loaded " + qTable.size() + " states.");
        } catch (FileNotFoundException e) {
            System.out.println("No Q-table file found (" + Q_TABLE_FILE + "). Starting with a new table.");
            qTable = new HashMap<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading Q-table: " + e.getMessage());
            e.printStackTrace();
            qTable = new HashMap<>(); // Start with a fresh table on error
        }
    }

    public void saveQTable() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Q_TABLE_FILE))) {
            oos.writeObject(qTable);
            System.out.println("Q-table saved successfully to " + Q_TABLE_FILE + ". Saved " + qTable.size() + " states.");
        } catch (IOException e) {
            System.err.println("Error saving Q-table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getQTableSize() {
        return qTable.size();
    }
}
