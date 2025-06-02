import java.util.Objects;

/**
 * Represents the discrete state of the game from the AI's perspective.
 * This is used as a key in the Q-table for Q-learning.
 * 
 * - foodDeltaXSign: Indicates if the food is to the left (-1), same column (0), or right (1) of the snake's head.
 * - foodDeltaYSign: Indicates if the food is above (-1), same row (0), or below (1) the snake's head.
 * - isObstacleLeftRelative: True if there's an obstacle (wall or self) if the snake turns left relative to its current direction.
 * - isObstacleStraightRelative: True if there's an obstacle if the snake continues straight relative to its current direction.
 * - isObstacleRightRelative: True if there's an obstacle if the snake turns right relative to its current direction.
 * - currentDirection: The snake's current absolute direction of movement ('U', 'D', 'L', 'R').
 */
public record State(
    int foodDeltaXSign,
    int foodDeltaYSign,
    boolean isObstacleLeftRelative,
    boolean isObstacleStraightRelative,
    boolean isObstacleRightRelative,
    char currentDirection
) {
    // Records automatically provide:
    // 1. A constructor with parameters for all fields (canonical constructor).
    // 2. Getter-like methods for all fields (e.g., foodDeltaXSign()).
    // 3. Implementations of equals(), hashCode(), and toString().
    // 4. Fields are final by default, ensuring immutability.

    // No additional methods are needed here for basic state representation.
}
