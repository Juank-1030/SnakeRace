package co.eci.snake.core;

/**
 * Estados del GameClock.
 *
 * Sin cambios con respecto a la versión de referencia (main/).
 * - STOPPED: antes de start() o después de stop()
 * - RUNNING: el reloj está activo, el scheduler ejecuta ticks
 * - PAUSED: el reloj está pausado, el ScheduledFuture está cancelado
 *
 * @see co.eci.snake.core.engine.GameClock
 */
public enum GameState { STOPPED, RUNNING, PAUSED }
