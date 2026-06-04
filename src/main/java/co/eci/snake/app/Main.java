package co.eci.snake.app;

import co.eci.snake.ui.legacy.SnakeApp;

/**
 * Punto de entrada de la aplicación SnakeRace.
 *
 * Lee la propiedad de sistema {@code snakes} para determinar cuántas
 * serpientes crear (valor por defecto: 2). La propiedad se pasa vía
 * {@code -Dsnakes=N} en la línea de comandos de Maven.
 *
 * Sin cambios con respecto a la versión de referencia (main/).
 *
 * @see SnakeApp
 */
public final class Main {
  private Main() {}
  public static void main(String[] args) {
    SnakeApp.launch();
  }
}
