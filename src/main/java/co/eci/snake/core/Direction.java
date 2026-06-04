package co.eci.snake.core;

/**
 * Cardinal directions for snake movement.
 *
 * No changes from the reference version (main/).
 * Immutable and thread-safe as a Java enum.
 *
 * @see Snake
 * @see Board#step(Snake)
 */
public enum Direction { UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0);
  public final int dx, dy; Direction(int dx, int dy){ this.dx=dx; this.dy=dy; } }
