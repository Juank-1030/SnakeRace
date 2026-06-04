package co.eci.snake.app;

import co.eci.snake.ui.legacy.SnakeApp;

/**
 * Entry point for the SnakeRace application.
 *
 * Reads the {@code snakes} system property to determine how many
 * snakes to create (default: 2). The property is passed via
 * {@code -Dsnakes=N} on the Maven command line.
 *
 * No changes from the reference version (main/).
 *
 * @see SnakeApp
 */
public final class Main {
  private Main() {}
  public static void main(String[] args) {
    SnakeApp.launch();
  }
}
