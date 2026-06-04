package co.eci.snake.concurrency;

import co.eci.snake.core.Snake;

import java.util.List;

/**
 * Cooperative pause monitor for SnakeRunners.
 *
 * New class, did not exist in the reference version (main/).
 *
 * Identical pattern to Control.java from the Wait-notify project:
 * - checkPause()          → called by each runner in every iteration
 * - pauseAndWaitForAll()  → called by the UI; blocks until ALL
 *                           runners are in wait()
 * - resume()              → called by the UI to resume the game
 *
 * Uses synchronized + wait() / notifyAll() to avoid busy-waiting.
 *
 * Concurrency fixes (README, section 2):
 * 1. DL-1: pauseAndWaitForAll() recalculates alive in each while
 *    iteration instead of once at the start. Thus if a runner
 *    finishes before signaling, the while does not wait forever.
 * 2. DL-2: the while also checks paused, so if the user presses
 *    Resume during collection, the condition becomes false and
 *    the loop exits, avoiding a deadlock.
 * 3. checkPause() uses try-finally to guarantee waitingCount-- even
 *    if the runner receives InterruptedException inside wait().
 *
 * @see co.eci.snake.concurrency.SnakeRunner
 * @see <a href="file:../../../../../../README.md">README.md — Part II, section 2</a>
 */
public final class PauseControl {

    private boolean paused = false;
    private int waitingCount = 0;
    private final List<Snake> snakes;

    public PauseControl(List<Snake> snakes) {
        this.snakes = snakes;
    }

    /**
     * Called by each SnakeRunner at the start of every iteration.
     * If the game is paused, blocks the thread until resume() is called.
     * Uses while (paused) to protect against spurious wakeups.
     *
     * waitingCount is incremented before wait() and decremented in a
     * finally block to avoid leaks if InterruptedException occurs.
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
     * Called by the UI (in a virtual thread, not the EDT) to pause.
     * Blocks until ALL runners have entered wait(),
     * guaranteeing the game state is completely stable.
     *
     * The while condition checks both paused and waitingCount:
     * - paused: if the user presses Resume while we are collecting,
     *   paused=false and we exit the loop (DL-2).
     * - waitingCount < snakes.size(): recalculates each iteration so
     *   that if a runner finishes between calculation and signal, no deadlock (DL-1).
     */
    public synchronized void pauseAndWaitForAll() throws InterruptedException {
        paused = true;
        while (paused && waitingCount < snakes.size()) {
            wait();
        }
    }

    /** Wakes up all runners blocked in checkPause(). */
    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    public synchronized boolean isPaused() { return paused; }
}
