import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Snake {
    private List<Point> body;
    private char direction; // 'U', 'D', 'L', 'R'
    private boolean growing = false; // Flag to indicate snake should grow
    private int initialSegmentsCount; // To store the initial number of segments for reset

    // Dimensions of the game board in game units
    private int boardWidth;
    private int boardHeight;

    public Snake(int startX, int startY, int initialSegments, char initialDirection, int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.initialSegmentsCount = initialSegments;
        this.body = new ArrayList<>();
        // Initialize is separated for clarity and reuse by reset
        initializeSnake(startX, startY, initialSegments, initialDirection);
    }

    private void initializeSnake(int startX, int startY, int initialSegments, char initialDirection) {
        this.body.clear();
        this.direction = initialDirection;
        this.growing = false;

        // Head is at body.get(0)
        // Segments are added "behind" the head based on initial direction.
        // For example, if initialDirection is 'R', head is (startX, startY),
        // next segment is (startX-1, startY), then (startX-2, startY), etc.
        for (int i = 0; i < initialSegments; i++) {
            int segmentX = startX;
            int segmentY = startY;
            switch (initialDirection) {
                case 'U':
                    segmentY += i;
                    break;
                case 'D':
                    segmentY -= i;
                    break;
                case 'L':
                    segmentX += i;
                    break;
                case 'R':
                    segmentX -= i;
                    break;
            }
            this.body.add(new Point(segmentX, segmentY));
        }
    }

    public void move() {
        if (body.isEmpty()) {
            return; // Should not happen with proper game logic
        }
        Point currentHead = body.get(0);
        Point newHead = new Point(currentHead); // Create a mutable copy

        switch (direction) {
            case 'U': newHead.y--; break;
            case 'D': newHead.y++; break;
            case 'L': newHead.x--; break;
            case 'R': newHead.x++; break;
        }
        body.add(0, newHead); // Add new head

        if (growing) {
            growing = false; // Reset flag, snake has grown
        } else {
            body.remove(body.size() - 1); // Remove tail if not growing
        }
    }

    public void grow() {
        this.growing = true; // Set flag, snake will grow on next move
    }

    public boolean checkCollisionWithWall() {
        Point head = getHead();
        if (head == null) return true; // Or handle error appropriately
        return head.x < 0 || head.x >= boardWidth || head.y < 0 || head.y >= boardHeight;
    }

    public boolean checkCollisionWithSelf() {
        Point head = getHead();
        if (head == null || body.size() <= 1) { // No collision if no body or just a head
            return false;
        }
        for (int i = 1; i < body.size(); i++) {
            if (head.equals(body.get(i))) {
                return true;
            }
        }
        return false;
    }

    public Point getHead() {
        if (body.isEmpty()) {
            return null;
        }
        return body.get(0);
    }

    public List<Point> getBody() {
        return body;
    }

    public void setDirection(char newDirection) {
        // Prevent immediate reversal
        if ((direction == 'U' && newDirection == 'D') ||
            (direction == 'D' && newDirection == 'U') ||
            (direction == 'L' && newDirection == 'R') ||
            (direction == 'R' && newDirection == 'L')) {
            return;
        }
        this.direction = newDirection;
    }

    public char getDirection() {
        return direction;
    }

    public void reset(int startX, int startY, char initialDirection) {
        // Uses the stored initialSegmentsCount
        initializeSnake(startX, startY, this.initialSegmentsCount, initialDirection);
    }
    
    // Overloaded reset if initialSegments needs to change too, though less common for a simple reset
    public void reset(int startX, int startY, int initialSegments, char initialDirection) {
        this.initialSegmentsCount = initialSegments; // Update if needed
        initializeSnake(startX, startY, initialSegments, initialDirection);
    }
}
