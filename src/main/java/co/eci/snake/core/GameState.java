package co.eci.snake.core;

/**
 * GameClock states.
 *
 * No changes from the reference version (main/).
 * - STOPPED: before start() or after stop()
 * - RUNNING: the clock is active, the scheduler executes ticks
 * - PAUSED: the clock is paused, the ScheduledFuture is canceled
 *
 * @see co.eci.snake.core.engine.GameClock
 */
public enum GameState { STOPPED, RUNNING, PAUSED }
