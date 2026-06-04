package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Represents a snake on the board.
 *
 * Update vs reference (main/):
 * - All public methods are now synchronized to eliminate
 *   the data race on {@link #body} (CR-1 in README).
 * - {@link #direction} is no longer volatile; it is protected with synchronized
 *   to make the validation+write in {@link #turn} atomic (CR-2).
 *
 * @see co.eci.snake.concurrency.SnakeRunner
 * @see <a href="file:../../../../../../README.md">README.md — Part II, section 2</a>
 */
public final class Snake {
  private final Deque<Position> body = new ArrayDeque<>();
  private Direction direction;
  private int maxLength = 5;

  private Snake(Position start, Direction dir) {
    body.addFirst(start);
    this.direction = dir;
  }

  public static Snake of(int x, int y, Direction dir) {
    return new Snake(new Position(x, y), dir);
  }

  /** Returns the current direction. Synchronized for visibility between threads. */
  public synchronized Direction direction() {
    return direction;
  }

  /**
   * Changes the snake's direction.
   * Synchronized so that the validation (no 180° turn) and the write
   * are atomic. Without this, two threads could pass the validation
   * simultaneously and write opposite directions (CR-2).
   */
  public synchronized void turn(Direction dir) {
    if ((direction == Direction.UP && dir == Direction.DOWN) ||
        (direction == Direction.DOWN && dir == Direction.UP) ||
        (direction == Direction.LEFT && dir == Direction.RIGHT) ||
        (direction == Direction.RIGHT && dir == Direction.LEFT)) {
      return;
    }
    this.direction = dir;
  }

  /** Returns the head position. Synchronized for consistency with body. */
  public synchronized Position head() {
    return body.peekFirst();
  }

  /**
   * Returns a defensive copy of the body so the EDT can
   * read it without risk of ConcurrentModificationException (CR-1).
   */
  public synchronized Deque<Position> snapshot() {
    return new ArrayDeque<>(body);
  }

  /**
   * Advances the snake to the new position.
   * If grow is true, the snake grows (maxLength++).
   * Synchronized to avoid data race with snapshot() (CR-1).
   */
  public synchronized void advance(Position newHead, boolean grow) {
    body.addFirst(newHead);
    if (grow)
      maxLength++;
    while (body.size() > maxLength)
      body.removeLast();
  }
}
