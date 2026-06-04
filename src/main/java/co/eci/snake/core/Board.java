package co.eci.snake.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Game board. Contains all game elements: snakes,
 * mice, obstacles, turbos, and teleporters.
 *
 * Update vs reference (main/):
 * - Replaced {@code synchronized} with {@link ReentrantReadWriteLock}
 *   to allow concurrent reads from the EDT while runners
 *   take turns writing (R-1 in README, section 4).
 * - getters (mice, obstacles, turbo, teleports) → readLock
 * - step() → writeLock
 * - randomEmpty() unchanged: still private and called only
 *   from the constructor or step() (CR-3, CR-4 in README).
 *
 * @see Snake
 * @see co.eci.snake.concurrency.SnakeRunner
 * @see co.eci.snake.ui.legacy.SnakeApp.GamePanel
 * @see <a href="file:../../../../../../README.md">README.md — Part II, section 2</a>
 */
public final class Board {
  private final int width;
  private final int height;

  private final Set<Position> mice = new HashSet<>();
  private final Set<Position> obstacles = new HashSet<>();
  private final Set<Position> turbo = new HashSet<>();
  private final Map<Position, Position> teleports = new HashMap<>();

  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

  public enum MoveResult {
    MOVED, ATE_MOUSE, HIT_OBSTACLE, ATE_TURBO, TELEPORTED
  }

  public Board(int width, int height) {
    if (width <= 0 || height <= 0)
      throw new IllegalArgumentException("Board dimensions must be positive");
    this.width = width;
    this.height = height;
    for (int i = 0; i < 6; i++)
      mice.add(randomEmpty());
    for (int i = 0; i < 4; i++)
      obstacles.add(randomEmpty());
    for (int i = 0; i < 3; i++)
      turbo.add(randomEmpty());
    createTeleportPairs(2);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  /**
   * Returns a defensive copy of the mice.
   * Acquires readLock to allow concurrent reads.
   */
  public Set<Position> mice() {
    rwLock.readLock().lock();
    try {
      return new HashSet<>(mice);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns a defensive copy of the obstacles.
   * Acquires readLock to allow concurrent reads.
   */
  public Set<Position> obstacles() {
    rwLock.readLock().lock();
    try {
      return new HashSet<>(obstacles);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns a defensive copy of the turbos.
   * Acquires readLock to allow concurrent reads.
   */
  public Set<Position> turbo() {
    rwLock.readLock().lock();
    try {
      return new HashSet<>(turbo);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns a defensive copy of the teleporters.
   * Acquires readLock to allow concurrent reads.
   */
  public Map<Position, Position> teleports() {
    rwLock.readLock().lock();
    try {
      return new HashMap<>(teleports);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Executes one step for a snake on the board.
   *
   * Acquires writeLock, so it is exclusive: no other runner
   * can execute step() and no reader can read while it is
   * running. This guarantees consistency of all board collections.
   *
   * @param snake the snake that advances
   * @return MoveResult indicating what happened in this step
   */
  public MoveResult step(Snake snake) {
    Objects.requireNonNull(snake, "snake");
    rwLock.writeLock().lock();
    try {
      var head = snake.head();
      var dir = snake.direction();
      Position next = new Position(head.x() + dir.dx, head.y() + dir.dy).wrap(width, height);

      if (obstacles.contains(next))
        return MoveResult.HIT_OBSTACLE;

      boolean teleported = false;
      if (teleports.containsKey(next)) {
        next = teleports.get(next);
        teleported = true;
      }

      boolean ateMouse = mice.remove(next);
      boolean ateTurbo = turbo.remove(next);

      snake.advance(next, ateMouse);

      if (ateMouse) {
        mice.add(randomEmpty());
        obstacles.add(randomEmpty());
        if (ThreadLocalRandom.current().nextDouble() < 0.2)
          turbo.add(randomEmpty());
      }

      if (ateTurbo)
        return MoveResult.ATE_TURBO;
      if (ateMouse)
        return MoveResult.ATE_MOUSE;
      if (teleported)
        return MoveResult.TELEPORTED;
      return MoveResult.MOVED;
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  private void createTeleportPairs(int pairs) {
    for (int i = 0; i < pairs; i++) {
      Position a = randomEmpty();
      Position b = randomEmpty();
      teleports.put(a, b);
      teleports.put(b, a);
    }
  }

  /**
   * Finds a random empty position on the board.
   * NOTE: Does not acquire its own locks. Should only be called from the
   * constructor (single-thread) or from step() (which already holds writeLock).
   */
  private Position randomEmpty() {
    var rnd = ThreadLocalRandom.current();
    Position p;
    int guard = 0;
    do {
      p = new Position(rnd.nextInt(width), rnd.nextInt(height));
      guard++;
      if (guard > width * height * 2)
        break;
    } while (mice.contains(p) || obstacles.contains(p) || turbo.contains(p) || teleports.containsKey(p));
    return p;
  }
}
