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
 * Tablero de juego. Contiene todos los elementos del juego: serpientes,
 * ratones, obstáculos, turbos y teletransportadores.
 *
 * Actualización vs referencia (main/):
 * - Se reemplazó {@code synchronized} por {@link ReentrantReadWriteLock}
 *   para permitir lecturas concurrentes del EDT mientras los runners
 *   se turnan para escribir (R-1 en README, sección 4).
 * - getters (mice, obstacles, turbo, teleports) → readLock
 * - step() → writeLock
 * - randomEmpty() sin cambios: sigue siendo privado y llamado solo
 *   desde el constructor o step() (CR-3, CR-4 en README).
 *
 * @see Snake
 * @see co.eci.snake.concurrency.SnakeRunner
 * @see co.eci.snake.ui.legacy.SnakeApp.GamePanel
 * @see <a href="file:../../../../../../README.md">README.md — Parte II, sección 2</a>
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
   * Retorna copia defensiva de los ratones.
   * Adquiere readLock para permitir lecturas concurrentes.
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
   * Retorna copia defensiva de los obstáculos.
   * Adquiere readLock para permitir lecturas concurrentes.
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
   * Retorna copia defensiva de los turbos.
   * Adquiere readLock para permitir lecturas concurrentes.
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
   * Retorna copia defensiva de los teletransportadores.
   * Adquiere readLock para permitir lecturas concurrentes.
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
   * Ejecuta un paso de una serpiente en el tablero.
   *
   * Adquiere writeLock, por lo que es exclusivo: ningún otro runner
   * puede ejecutar step() ni ningún lector puede leer mientras esté
   * en ejecución. Esto garantiza la consistencia de todas las colecciones
   * del tablero.
   *
   * @param snake la serpiente que avanza
   * @return MoveResult indicando qué ocurrió en este paso
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
   * Encuentra una posición aleatoria vacía en el tablero.
   * NOTA: No adquiere locks propios. Solo debe llamarse desde el
   * constructor (single-thread) o desde step() (que ya tiene writeLock).
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
