package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Representa una serpiente en el tablero.
 *
 * Actualización vs referencia (main/):
 * - Todos los métodos públicos ahora son synchronized para eliminar
 *   la data race en {@link #body} (CR-1 en README).
 * - {@link #direction} ya no es volatile; se protege con synchronized
 *   para hacer atómica la validación+escritura en {@link #turn} (CR-2).
 *
 * @see co.eci.snake.concurrency.SnakeRunner
 * @see <a href="file:../../../../../../README.md">README.md — Parte II, sección 2</a>
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

  /** Retorna la dirección actual. Sincronizado para visibilidad entre hilos. */
  public synchronized Direction direction() {
    return direction;
  }

  /**
   * Cambia la dirección de la serpiente.
   * Sincronizado para que la validación (no giro de 180°) y la escritura
   * sean atómicas. Sin esto, dos hilos podrían pasar la validación
   * simultáneamente y escribir direcciones opuestas (CR-2).
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

  /** Retorna la posición de la cabeza. Sincronizado por consistencia con body. */
  public synchronized Position head() {
    return body.peekFirst();
  }

  /**
   * Retorna una copia defensiva del cuerpo para que el EDT pueda
   * leerlo sin riesgo de ConcurrentModificationException (CR-1).
   */
  public synchronized Deque<Position> snapshot() {
    return new ArrayDeque<>(body);
  }

  /**
   * Avanza la serpiente a la nueva posición.
   * Si grow es true, la serpiente crece (maxLength++).
   * Sincronizado para evitar data race con snapshot() (CR-1).
   */
  public synchronized void advance(Position newHead, boolean grow) {
    body.addFirst(newHead);
    if (grow)
      maxLength++;
    while (body.size() > maxLength)
      body.removeLast();
  }
}
