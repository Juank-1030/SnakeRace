package co.eci.snake.core;

/**
 * Posición en el tablero con wrap-around.
 *
 * Sin cambios con respecto a la versión de referencia (main/).
 * Es un record de Java, por lo que es inmutable y thread-safe.
 *
 * El método {@link #wrap(int, int)} aplica la operación módulo
 * no-negativa para que el tablero se "repita" en los bordes
 * (regla del juego: wrap-around).
 *
 * @see Board#step(Snake)
 */
public record Position(int x, int y) {
  public Position wrap(int width, int height) {
    int nx = ((x % width) + width) % width;
    int ny = ((y % height) + height) % height;
    return new Position(nx, ny);
  }
}
