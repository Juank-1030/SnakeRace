package co.eci.snake.core;

/**
 * Position on the board with wrap-around.
 *
 * No changes from the reference version (main/).
 * It is a Java record, so it is immutable and thread-safe.
 *
 * The {@link #wrap(int, int)} method applies non-negative modulo
 * so the board "repeats" at the edges (game rule: wrap-around).
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
