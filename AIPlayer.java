import java.util.*;
import java.io.*;
import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
// NotSerializableException and WriteAbortedException are specifically handled.

public class AIPlayer {

    // --- Q-learning Constants ---
    private static final double ALPHA = 0.1; // Learning rate
    private static final double GAMMA = 0.9; // Discount factor
    private double epsilon = 1.0;      // Exploration rate
    private static final double MIN_EPSILON = 0.01;
    private static final double EPSILON_DECAY_RATE = 0.999;

    // --- Q-Table ---
    private Map<State, double[]> qTable;

    // --- Other Fields ---
    private Random random;
    private static final String Q_TABLE_FILE = "q_table.dat";

    public AIPlayer() {
        // qTable is initialized by loadQTable or its fallbacks
        this.random = new Random();
        loadQTable();
    }

    // --- State Calculation ---
    public State getCurrentState(Snake snake, Food food, int boardWidth, int boardHeight) {
        Point head = snake.getHead();
        Point foodPos = food.getPosition();
        char currentDirection = snake.getDirection();
        List<Point> snakeBody = snake.getBody();

        if (head == null || foodPos == null) {
            System.err.println("Warning: Snake head or food position is null in getCurrentState. Returning default state.");
            return new State(0, 0, true, true, true, 'U');
        }

        int foodDeltaXSign = Integer.compare(foodPos.x, head.x);
        int foodDeltaYSign = Integer.compare(foodPos.y, head.y);

        Point pointStraight = getRelativePoint(head, currentDirection, 0);
        Point pointLeft = getRelativePoint(head, currentDirection, -1);
        Point pointRight = getRelativePoint(head, currentDirection, 1);

        boolean obsStraight = isObstacleAt(pointStraight, snakeBody, boardWidth, boardHeight);
        boolean obsLeft = isObstacleAt(pointLeft, snakeBody, boardWidth, boardHeight);
        boolean obsRight = isObstacleAt(pointRight, snakeBody, boardWidth, boardHeight);

        return new State(foodDeltaXSign, foodDeltaYSign, obsLeft, obsStraight, obsRight, currentDirection);
    }

    private Point getRelativePoint(Point head, char currentSnakeDirection, int relativeAction) {
        Point p = new Point(head);
        char actionDirection = currentSnakeDirection;

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

        switch (actionDirection) {
            case 'U': p.y--; break;
            case 'D': p.y++; break;
            case 'L': p.x--; break;
            case 'R': p.x++; break;
        }
        return p;
    }

    private boolean isObstacleAt(Point p, List<Point> snakeBody, int boardWidth, int boardHeight) {
        if (p.x < 0 || p.x >= boardWidth || p.y < 0 || p.y >= boardHeight) {
            return true; // Wall collision
        }
        for (Point segment : snakeBody) {
            if (p.equals(segment)) {
                return true; // Self-collision
            }
        }
        return false;
    }

    // --- Action Selection ---
    public int chooseAction(State state, boolean isTraining) {
        qTable.putIfAbsent(state, new double[3]);

        if (isTraining && random.nextDouble() < epsilon) {
            return random.nextInt(3);
        } else {
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
    public void updateQValue(State state, int action, double reward, State nextState, boolean gameOver) {
        qTable.putIfAbsent(state, new double[3]);
        double oldQValue = qTable.get(state)[action];
        double nextMaxQ = 0.0;

        if (!gameOver && nextState != null) {
            qTable.putIfAbsent(nextState, new double[3]);
            double[] nextQValues = qTable.get(nextState);
            nextMaxQ = Arrays.stream(nextQValues).max().orElse(0.0);
        }

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
    @SuppressWarnings("unchecked") // For the cast of ois.readObject()
    public void loadQTable() {
        File qFile = new File(Q_TABLE_FILE);
        if (!qFile.exists()) {
            System.out.println("Q-table file not found (" + Q_TABLE_FILE + "). Starting with a new table.");
            this.qTable = new HashMap<>();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(qFile))) {
            this.qTable = (Map<State, double[]>) ois.readObject();
            System.out.println("Q-table loaded successfully from " + Q_TABLE_FILE + ". Size: " + (this.qTable != null ? this.qTable.size() : 0));
        } catch (FileNotFoundException e) { // Should be caught by qFile.exists(), but as a safeguard
            System.out.println("Q-table file not found during load attempt (" + Q_TABLE_FILE + "). Starting with a new table.");
            this.qTable = new HashMap<>();
        } catch (WriteAbortedException | NotSerializableException e) { // Specific check for serialization issues
            System.err.println("Serialization compatibility error (e.g., NotSerializableException, WriteAbortedException) loading Q-table: " + e.getMessage());
            handleCorruptedQTable(qFile, e);
        } catch (IOException e) { // General IO issues
            System.err.println("General IOException while loading Q-table: " + e.getMessage());
            handleCorruptedQTable(qFile, e);
        } catch (ClassNotFoundException e) { // Class structure might have changed
            System.err.println("ClassNotFoundException while loading Q-table: " + e.getMessage());
            handleCorruptedQTable(qFile, e);
        } finally {
            if (this.qTable == null) { // Ensure qTable is initialized if any unexpected issue occurred
                System.out.println("Q-table was null after loading attempts. Initializing a new table to be safe.");
                this.qTable = new HashMap<>();
            }
        }
    }

    private void handleCorruptedQTable(File qFile, Exception exception) {
        System.err.println("Handling corrupted or incompatible Q-table file: " + qFile.getName());
        // For debugging, one might want to print the stack trace of the original exception:
        // exception.printStackTrace();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = qFile.getName() + ".corrupted_" + timestamp;

        File parentDir = qFile.getParentFile();
        File backupFile = (parentDir == null) ? new File(backupFileName) : new File(parentDir, backupFileName);

        try {
            if (qFile.exists()) { // Check again, in case it was deleted between initial check and now
                Files.move(qFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Original Q-table file '" + qFile.getName() + "' renamed to: " + backupFile.getName());
            } else {
                System.out.println("Original Q-table file '" + qFile.getName() + "' not found for renaming (might have been moved or deleted already).");
            }
        } catch (IOException moveException) {
            System.err.println("CRITICAL ERROR: Could not rename corrupted Q-table file '" + qFile.getName() + "' to '" + backupFile.getName() + "'. Manual intervention may be required. Error: " + moveException.getMessage());
        }

        System.out.println("Starting with a new empty Q-table due to load failure.");
        this.qTable = new HashMap<>(); // Initialize a new table
    }

    public void saveQTable() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Q_TABLE_FILE))) {
            oos.writeObject(qTable);
            System.out.println("Q-table saved successfully to " + Q_TABLE_FILE + ". Saved " + (qTable != null ? qTable.size() : 0) + " states.");
        } catch (IOException e) {
            System.err.println("Error saving Q-table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getQTableSize() {
        return qTable != null ? qTable.size() : 0;
    }
}