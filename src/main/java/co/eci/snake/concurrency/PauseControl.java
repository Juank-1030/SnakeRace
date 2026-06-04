package co.eci.snake.concurrency;

import co.eci.snake.core.Snake;

import java.util.List;

/**
 * Monitor de pausa cooperativa para los SnakeRunner.
 *
 * Clase nueva, no existía en la versión de referencia (main/).
 *
 * Patrón idéntico al Control.java del proyecto Wait-notify:
 * - checkPause()          → llamado por cada runner en cada iteración
 * - pauseAndWaitForAll()  → llamado por la UI; bloquea hasta que TODOS
 *                           los runners estén en wait()
 * - resume()              → llamado por la UI para reanudar el juego
 *
 * Se usa synchronized + wait() / notifyAll() para evitar busy-waiting.
 *
 * Correcciones de concurrencia (README, sección 2):
 * 1. DL-1: pauseAndWaitForAll() recalcula alive en cada iteración del
 *    while en lugar de calcularlo una vez al inicio. Así, si un runner
 *    termina antes de señalizar, el while no espera para siempre.
 * 2. DL-2: el while también verifica paused, por lo que si el usuario
 *    presiona Resume durante la recolección, la condición se vuelve
 *    falsa y se sale del bucle evitando un deadlock.
 * 3. checkPause() usa try-finally para garantizar waitingCount-- incluso
 *    si el runner recibe InterruptedException dentro del wait().
 *
 * @see co.eci.snake.concurrency.SnakeRunner
 * @see <a href="file:../../../../../../README.md">README.md — Parte II, sección 2</a>
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
     *
     * waitingCount se incrementa antes de wait() y se decrementa en un
     * finally para evitar fugas si ocurre InterruptedException.
     */
    public synchronized void checkPause() throws InterruptedException {
        if (paused) {
            waitingCount++;
            notifyAll();
            try {
                while (paused) {
                    wait();
                }
            } finally {
                waitingCount--;
            }
        }
    }

    /**
     * Llamado por la UI (en un virtual thread, no en el EDT) para pausar.
     * Bloquea hasta que TODOS los runners hayan entrado en wait(),
     * garantizando que el estado del juego es completamente estable.
     *
     * La condición del while verifica tanto paused como waitingCount:
     * - paused: si el usuario presiona Resume mientras recolectamos,
     *   paused=false y salimos del bucle (DL-2).
     * - waitingCount < snakes.size(): recalcula en cada iteración para
     *   que si un runner termina entre cálculo y señal, no deadlock (DL-1).
     */
    public synchronized void pauseAndWaitForAll() throws InterruptedException {
        paused = true;
        while (paused && waitingCount < snakes.size()) {
            wait();
        }
    }

    /** Despierta a todos los runners bloqueados en checkPause(). */
    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    public synchronized boolean isPaused() { return paused; }
}
