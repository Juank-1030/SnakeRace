package co.eci.snake.concurrency;

import co.eci.snake.core.Snake;

import java.util.List;

/**
 * Monitor de pausa cooperativa para los SnakeRunner.
 *
 * Patrón idéntico al Control.java del proyecto Wait-notify:
 *   - checkPause()          → llamado por cada runner en cada iteración
 *   - pauseAndWaitForAll()  → llamado por la UI; bloquea hasta que TODOS los runners estén en wait()
 *   - resume()              → llamado por la UI para reanudar el juego
 *
 * Se usa synchronized + wait() / notifyAll() para evitar busy-waiting.
 */
public final class PauseControl {

    private boolean paused = false;
    private int waitingCount = 0;
    private final List<Snake> snakes;

    public PauseControl(List<Snake> snakes) {
        this.snakes = snakes;
    }

    /**
     * Llamado por cada SnakeRunner al inicio de cada iteración.
     * Si el juego está pausado, bloquea el hilo hasta que se llame resume().
     * Usa while (paused) para protegerse de spurious wakeups.
     */
    public synchronized void checkPause() throws InterruptedException {
        if (paused) {
            waitingCount++;
            notifyAll();            // avisa a pauseAndWaitForAll() que este runner entró en wait
            try {
                while (paused) {
                    wait();         // bloqueo real, libera el monitor
                }
            } finally {
                waitingCount--;     // garantiza decremento incluso si hay InterruptedException
            }
        }
    }

    /**
     * Llamado por la UI (en un virtual thread, no en el EDT) para pausar.
     * Bloquea hasta que TODOS los runners vivos hayan entrado en wait(),
     * garantizando que el estado del juego es completamente estable para leer.
     */
    public synchronized void pauseAndWaitForAll() throws InterruptedException {
        paused = true;
        // Recalcula alive en cada iteración: si un runner termina entre el
        // cálculo y su señal, el while no se queda esperando para siempre.
        // Además verifica paused: si el usuario presiona Resume mientras
        // aún estamos recolectando, la condición se vuelve falsa y salimos.
        while (paused && waitingCount < snakes.size()) {
            wait();                 // espera la señal de checkPause()
        }
        // En este punto: todos los runners vivos están bloqueados → lectura segura
    }

    /** Despierta a todos los runners bloqueados en checkPause(). */
    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    public synchronized boolean isPaused() { return paused; }
}
