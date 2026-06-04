package co.eci.snake.concurrency;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Snake;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Life cycle of a snake. Each SnakeRunner runs in its own
 * VirtualThread and executes an infinite loop: cooperative pause →
 * random turn (AI only) → step on the board → sleep.
 *
 * Update vs reference (main/):
 * - Added {@link PauseControl} for cooperative pause without
 *   busy-wait (DL-1, DL-2 in README, section 2).
 * - Added {@code autoPilot} so player snakes (indices 0 and 1)
 *   do not turn randomly (DR-1 in README).
 * - Bounce: on obstacle hit it turns, does not die (rule preserved
 *   from main/).
 * - turboTicks: thread-local variable, no synchronization needed.
 *
 * @see Board#step(Snake)
 * @see PauseControl
 * @see <a href="file:../../../../../../README.md">README.md — Part II, section 2</a>
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
   * @param pauseControl shared cooperative pause monitor
   * @param autoPilot    false for player snakes (keyboard only),
   *                     true for AI snakes (random turns)
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

  /** Probabilistic random turn. Only executed if autoPilot=true. */
  private void maybeTurn() {
    double p = (turboTicks > 0) ? 0.05 : 0.10;
    if (ThreadLocalRandom.current().nextDouble() < p)
      randomTurn();
  }

  /** Random turn to any valid direction. */
  private void randomTurn() {
    var dirs = Direction.values();
    snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
  }
}
