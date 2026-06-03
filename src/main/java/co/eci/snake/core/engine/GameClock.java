package co.eci.snake.core.engine;

import co.eci.snake.core.GameState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class GameClock implements AutoCloseable {
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final long periodMillis;
  private final Runnable tick;
  private final AtomicReference<GameState> state = new AtomicReference<>(GameState.STOPPED);
  // BW-1: referencia al task programado para poder cancelarlo en pause() sin
  // busy-wait
  private volatile ScheduledFuture<?> tickTask;

  public GameClock(long periodMillis, Runnable tick) {
    if (periodMillis <= 0)
      throw new IllegalArgumentException("periodMillis must be > 0");
    this.periodMillis = periodMillis;
    this.tick = java.util.Objects.requireNonNull(tick, "tick");
  }

  public void start() {
    if (state.compareAndSet(GameState.STOPPED, GameState.RUNNING)) {
      // Guarda la referencia al task para poder cancelarlo en pause()
      tickTask = scheduler.scheduleAtFixedRate(tick, 0, periodMillis, TimeUnit.MILLISECONDS);
    }
  }

  public void pause() {
    state.set(GameState.PAUSED);
    // BW-1: cancela el task programado; el scheduler deja de despertar durante la
    // pausa
    // cancel(false): espera a que el tick en curso termine antes de cancelar
    ScheduledFuture<?> current = tickTask;
    if (current != null)
      current.cancel(false);
  }

  public void resume() {
    if (state.compareAndSet(GameState.PAUSED, GameState.RUNNING)) {
      // Reprograma el tick ahora que se reanuda
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
