import java.io.Serializable;
import java.util.Objects; // Retained for consistency, though not strictly needed by the record for its core features.

/**
 * Represents the discrete state of the game from the AI's perspective.
 * This is used as a key in the Q-table for Q-learning.
 *
 * - foodDeltaXSign: Indicates if the food is to the left (-1), same column (0), or right (1) of the snake's head.
 * - foodDeltaYSign: Indicates if the food is above (-1), same row (0), or below (1) of the snake's head.
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
) implements Serializable { // Added implements Serializable

    // Good practice for serializable classes/records to control versioning.
    private static final long serialVersionUID = 1L;

    // Canonical constructor, getters, equals, hashCode, toString are auto-generated for records.
    // All fields of a record are implicitly final.
    // Since int, boolean, and char are all serializable, the record State is serializable.
}