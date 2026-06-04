package co.eci.snake.core;

/**
 * Direcciones cardinales para el movimiento de las serpientes.
 *
 * Sin cambios con respecto a la versión de referencia (main/).
 * Inmutable y thread-safe por ser un enum de Java.
 *
 * @see Snake
 * @see Board#step(Snake)
 */
public enum Direction { UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0);
  public final int dx, dy; Direction(int dx, int dy){ this.dx=dx; this.dy=dy; } }
