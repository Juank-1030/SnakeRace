package co.eci.snake.core.engine;

import co.eci.snake.core.GameState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Game clock that controls UI repainting.
 *
 * Update vs reference (main/):
 * - Replaced state polling (busy-wait) with ScheduledFuture
 *   cancellation on pause() and rescheduling on resume() (BW-1,
 *   R-3 in README, sections 2 and 4).
 * - tickTask is volatile for visibility between threads.
 * - start() uses compareAndSet to guarantee single initialization.
 *
 * @see co.eci.snake.ui.legacy.SnakeApp
 * @see <a href="file:../../../../../../../README.md">README.md — Part II, section 2</a>
 */
public final class GameClock implements AutoCloseable {
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final long periodMillis;
  private final Runnable tick;
  private final AtomicReference<GameState> state = new AtomicReference<>(GameState.STOPPED);
  private volatile ScheduledFuture<?> tickTask;

  public GameClock(long periodMillis, Runnable tick) {
    if (periodMillis <= 0)
      throw new IllegalArgumentException("periodMillis must be > 0");
    this.periodMillis = periodMillis;
    this.tick = java.util.Objects.requireNonNull(tick, "tick");
  }

  /** Starts the clock. Only transitions from STOPPED to RUNNING. */
  public void start() {
    if (state.compareAndSet(GameState.STOPPED, GameState.RUNNING)) {
      tickTask = scheduler.scheduleAtFixedRate(tick, 0, periodMillis, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Pauses the clock: cancels the ScheduledFuture so the scheduler
   * stops waking up. cancel(false) waits for the current tick to
   * finish before canceling.
   */
  public void pause() {
    state.set(GameState.PAUSED);
    ScheduledFuture<?> current = tickTask;
    if (current != null)
      current.cancel(false);
  }

  /** Resumes the clock: reschedules the tick if state is PAUSED. */
  public void resume() {
    if (state.compareAndSet(GameState.PAUSED, GameState.RUNNING)) {
      tickTask = scheduler.scheduleAtFixedRate(tick, 0, periodMillis, TimeUnit.MILLISECONDS);
    }
  }

  public void stop() {
    state.set(GameState.STOPPED);
  }

  @Override
  public void close() {
    scheduler.shutdownNow();
  }
}
