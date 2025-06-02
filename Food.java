import java.awt.Point;
import java.util.Random;
import java.util.List; // Required if directly accessing List<Point> from Snake

// No package declaration, assuming default package for Snake and other classes

public class Food {
    private Point position;
    private Random random;
    private int boardWidth;  // Game units (e.g., number of columns)
    private int boardHeight; // Game units (e.g., number of rows)

    public Food(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.random = new Random();
        // Position is not set here; it should be set by an explicit call to spawn()
        // This ensures that the food is placed considering the snake's initial position if necessary.
    }

    /**
     * Spawns the food at a new random location on the board, ensuring it does not overlap with the snake.
     * @param snake The snake instance to check for collisions. Can be null, but food might spawn on snake.
     *              For robust behavior, it's best if the snake is always provided after its initialization.
     */
    public void spawn(Snake snake) {
        boolean onSnake;
        int x, y;
        
        List<Point> snakeBody = null;
        if (snake != null) {
            snakeBody = snake.getBody();
        }

        do {
            x = random.nextInt(boardWidth);  // Generates 0 to boardWidth-1
            y = random.nextInt(boardHeight); // Generates 0 to boardHeight-1
            this.position = new Point(x, y);
            
            onSnake = false;
            if (snakeBody != null && !snakeBody.isEmpty()) {
                for (Point segment : snakeBody) {
                    if (segment.equals(this.position)) {
                        onSnake = true;
                        break; 
                    }
                }
            }
        } while (onSnake);
    }

    public Point getPosition() {
        return position;
    }

    // Optional: Set position directly - generally not recommended as spawn() handles collision logic.
    // public void setPosition(Point position) {
    //    this.position = position;
    // }

    // Optional: A way to spawn food without a snake, for example, at the very beginning of the game.
    // However, it's usually better to initialize snake first, then food.
    public void spawnWithoutCollisionCheck() {
        int x = random.nextInt(boardWidth);
        int y = random.nextInt(boardHeight);
        this.position = new Point(x,y);
    }
}
