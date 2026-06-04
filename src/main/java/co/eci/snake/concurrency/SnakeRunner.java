package co.eci.snake.concurrency;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Snake;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Ciclo de vida de una serpiente. Cada SnakeRunner corre en su propio
 * VirtualThread y ejecuta un bucle infinito: pausa cooperativa →
 * giro aleatorio (solo IA) → step en el tablero → sleep.
 *
 * Actualización vs referencia (main/):
 * - Se agregó {@link PauseControl} para pausa cooperativa sin
 *   busy-wait (DL-1, DL-2 en README, sección 2).
 * - Se agregó {@code autoPilot} para que las serpientes de jugador
 *   (índices 0 y 1) no giren aleatoriamente (DR-1 en README).
 * - Rebote: al chocar con obstáculo gira, no muere (regla preservada
 *   de main/).
 * - turboTicks: variable local del hilo, no requiere sincronización.
 *
 * @see Board#step(Snake)
 * @see PauseControl
 * @see <a href="file:../../../../../../README.md">README.md — Parte II, sección 2</a>
 */
public final class SnakeRunner implements Runnable {
  private final Snake snake;
  private final Board board;
  private final PauseControl pauseControl;
  private final boolean autoPilot;
  private final int baseSleepMs = 80;
  private final int turboSleepMs = 40;
  private int turboTicks = 0;

  /**
   * @param pauseControl monitor compartido de pausa cooperativa
   * @param autoPilot    false para serpientes de jugador (solo teclado),
   *                     true para serpientes IA (giros aleatorios)
   */
  public SnakeRunner(Snake snake, Board board, PauseControl pauseControl, boolean autoPilot) {
    this.snake = snake;
    this.board = board;
    this.pauseControl = pauseControl;
    this.autoPilot = autoPilot;
  }

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pauseControl.checkPause();
        if (autoPilot) maybeTurn();
        var res = board.step(snake);
        if (res == Board.MoveResult.HIT_OBSTACLE) {
          randomTurn();
        } else if (res == Board.MoveResult.ATE_TURBO) {
          turboTicks = 100;
        }
        int sleep = (turboTicks > 0) ? turboSleepMs : baseSleepMs;
        if (turboTicks > 0)
          turboTicks--;
        Thread.sleep(sleep);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  /** Giro aleatorio probabilístico. Solo se ejecuta si autoPilot=true. */
  private void maybeTurn() {
    double p = (turboTicks > 0) ? 0.05 : 0.10;
    if (ThreadLocalRandom.current().nextDouble() < p)
      randomTurn();
  }

  /** Giro aleatorio a una dirección válida cualquiera. */
  private void randomTurn() {
    var dirs = Direction.values();
    snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
  }
}
