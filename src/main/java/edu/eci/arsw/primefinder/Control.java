package edu.eci.arsw.primefinder;

import java.util.Scanner;

public class Control extends Thread {

    private static final int NTHREADS = 3;
    private static final int MAXVALUE = 30000000;
    private static final int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private final PrimeFinderThread[] pft;

    // --- monitor state ---
    private boolean paused = false;
    private int waitingCount = 0;

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];
        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            pft[i] = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, this);
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, this);
    }

    public static Control newControl() {
        return new Control();
    }

    /**
     * Llamado por cada PrimeFinderThread en su loop.
     * Si paused==true, el hilo se bloquea aquí hasta que se reanude.
     */
    public synchronized void checkPause() throws InterruptedException {
        if (paused) {
            waitingCount++;
            notifyAll(); // avisa al Control que un hilo más está esperando
            while (paused) {
                wait();
            }
            waitingCount--;
        }
    }

    @Override
    public void run() {
        // Iniciar los hilos trabajadores
        for (PrimeFinderThread t : pft) {
            t.start();
        }

        Scanner scanner = new Scanner(System.in);

        while (anyAlive()) {
            try {
                Thread.sleep(TMILISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (!anyAlive())
                break;

            // 1. Pausar todos los hilos
            int alive = aliveCount();
            synchronized (this) {
                paused = true;
                // Esperar sin busy-wait hasta que todos los hilos vivos estén en wait()
                while (waitingCount < alive) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // 2. Mostrar cuántos primos se han encontrado (fuera de sync, hilos pausados)
            int total = 0;
            for (PrimeFinderThread t : pft) {
                total += t.getPrimes().size();
            }
            System.out.println("\n--- PAUSA ---");
            System.out.println("Primos encontrados hasta ahora: " + total);
            System.out.print("Presiona ENTER para continuar...");

            // 3. Esperar ENTER
            scanner.nextLine();

            // 4. Reanudar todos los hilos
            synchronized (this) {
                paused = false;
                notifyAll();
            }

            System.out.println("--- REANUDANDO ---\n");
        }

        // Esperar a que todos terminen para mostrar el total final
        for (PrimeFinderThread t : pft) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }

        int total = 0;
        for (PrimeFinderThread t : pft) {
            total += t.getPrimes().size();
        }
        System.out.println("\n=== FIN: total de primos encontrados entre 0 y " + MAXVALUE + ": " + total + " ===");
        scanner.close();
    }

    private boolean anyAlive() {
        for (PrimeFinderThread t : pft) {
            if (t.isAlive())
                return true;
        }
        return false;
    }

    private int aliveCount() {
        int count = 0;
        for (PrimeFinderThread t : pft) {
            if (t.isAlive())
                count++;
        }
        return count;
    }
}
