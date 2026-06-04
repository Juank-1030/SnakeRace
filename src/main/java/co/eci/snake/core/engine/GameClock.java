package co.eci.snake.core.engine;

import co.eci.snake.core.GameState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reloj del juego que controla el repintado de la UI.
 *
 * Actualización vs referencia (main/):
 * - Se reemplazó el sondeo de estado (busy-wait) por cancelación del
 *   ScheduledFuture en pause() y reprogramación en resume() (BW-1,
 *   R-3 en README, sección 2 y 4).
 * - tickTask es volatile para visibilidad entre hilos.
 * - start() usa compareAndSet para garantizar inicialización única.
 *
 * @see co.eci.snake.ui.legacy.SnakeApp
 * @see <a href="file:../../../../../../../README.md">README.md — Parte II, sección 2</a>
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

  /** Inicia el reloj. Solo transiciona de STOPPED a RUNNING. */
  public void start() {
    if (state.compareAndSet(GameState.STOPPED, GameState.RUNNING)) {
      tickTask = scheduler.scheduleAtFixedRate(tick, 0, periodMillis, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Pausa el reloj: cancela el ScheduledFuture para que el scheduler
   * deje de despertar. cancel(false) espera a que el tick en curso
   * termine antes de cancelar.
   */
  public void pause() {
    state.set(GameState.PAUSED);
    ScheduledFuture<?> current = tickTask;
    if (current != null)
      current.cancel(false);
  }

  /** Reanuda el reloj: reprograma el tick si el estado es PAUSED. */
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
