# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Concurrent programming lab: race conditions, synchronization, and safe collections.

---

## Requirements

- **JDK 21** (Temurin recommended)
- **Maven 3.9+**
- OS: Windows, macOS, or Linux

---

## How to Run

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → starts the game with **N** snakes (default 2).
  - With **N ≥ 2** the second snake (WASD controls) is enabled.
  - Snakes 0 and 1 are player-controlled (keyboard only). Snakes 2+ are AI (autonomous movement).
- **Controls**:
  - **Arrow keys** (←↑↓→): snake **0** (Player 1).
  - **WASD** (W↑ A← S↓ D→): snake **1** (Player 2, only if N ≥ 2).
  - **Space** or **Action** button: Pause / Resume. On pause, a dialog shows stats (each snake's length and the longest one).

---

## Game Rules (Summary)

- **N snakes** run autonomously (each in its own thread).
- **Mice**: when eaten, the snake **grows** and a **new obstacle** appears.
- **Obstacles**: if the head hits an obstacle, the snake **bounces**.
- **Teleporters** (red arrows): entering one **exits through its pair**.
- **Lightning (Turbo)**: stepping on it gives the snake **temporary increased speed**.
- **Wrap-around** movement (the board "repeats" at the edges).

---

## Architecture (Folders)

```
co.eci.snake
├─ app/                 # Application bootstrap (Main)
├─ core/                # Domain: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pause/Resume)
├─ concurrency/         # SnakeRunner (per-snake logic with virtual threads)
└─ ui/legacy/           # Legacy-style UI (Swing) with grid and Action button
```

---

# Lab Activities

## Part I — (Warm-up) `wait/notify` in a multi-threaded program

1. Fork the [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise) project.
2. Modify it so that **every _t_ milliseconds**:
   - All worker threads are **paused**.
   - The number of primes found so far is **displayed**.
   - The program **waits for ENTER** to **resume**.
3. Synchronization must use **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** on the **same monitor** (no _busy-waiting_).
4. Include in the lab report **your observations and/or comments** explaining your synchronization design (which lock, which condition, how you avoid _lost wakeups_).

> Educational goal: practice suspension/resumption **without** active waiting and consolidate the Java monitor model.

---

## Part II — Concurrent SnakeRace (lab core)

### 1) Concurrency analysis

- Explain **how** the code uses threads to give each snake autonomy.
- **Identify** and document in **`the lab report`**:
  - Possible **race conditions**.
  - **Collections** or structures that are **unsafe** in a concurrent context.
  - Occurrences of **busy-waiting** or unnecessary synchronization.

### 2) Minimal fixes and critical regions

- **Eliminate** busy-waiting by replacing it with **signals** / **states** or concurrency library mechanisms.
- Protect **only** the **strictly necessary critical regions** (avoid coarse-grained locking).
- Justify in **`the lab report`** each change: what the risk was and how you solved it.

### 3) Safe execution control (UI)

- Implement the **UI** with **Start / Pause / Resume** (the _Action_ button and `GameClock` already exist).
- On **Pause**, **consistently** display (without _tearing_):
  - The **longest alive snake**.
  - The **worst snake** (the one that **died first**).
- Consider that suspension **is not instantaneous**; coordinate so that the displayed state is not "half-baked."

### 4) Robustness under load

- Run with **high N** (`-Dsnakes=20` or more) and/or increase speed.
- The game **must not break**: no `ConcurrentModificationException`, no inconsistent reads, no _deadlocks_.
- If you enable **teleports** and **turbo**, verify that the rules do not introduce races.

> Detailed deliverables below.

---

## Deliverables

1. **Source code** running on **Java 21**.
2. Everything clearly documented in **`the lab report`** with:
   - Data races found and their solutions.
   - Misused collections and how they were protected (or replaced).
   - Busy-waiting eliminated and the mechanism used.
   - Critical regions defined and justification of their **minimal scope**.
3. UI with **Start / Pause / Resume** and the requested stats on pause.

---

## Evaluation Criteria (10)

- (3) **Correct concurrency**: no data races; well-localized synchronization.
- (2) **Pause/Resume**: visual and state consistency.
- (2) **Robustness**: runs **with high N** and without concurrency exceptions.
- (1.5) **Quality**: clear structure, names, comments; no obvious _code smells_.
- (1.5) **Documentation**: clear, reproducible **`lab report`**.

---

## Tips and Useful Configuration

- **Number of snakes**: `-Dsnakes=N` when running.
- **Board size**: change the constructor `new Board(width, height)`.
- **Teleports / Turbo**: edit `Board.java` (initialization methods and rules in `step(...)`).
- **Speed**: adjust `GameClock` (tick) or the `sleep` in `SnakeRunner` (includes turbo mode).

---

## How to Run Tests

```bash
mvn clean verify
```

Includes compilation and JUnit test execution. If you have static analysis, run it in `verify` or `site` according to your `pom.xml`.

---

## Credits

This lab is a modernized adaptation of the **SnakeRace** exercise from ARSW. The activity descriptions are preserved to maintain the course's pedagogical objectives.

**Base built by Ing. Javier Toquica.**

---

# Report

**Author: Juan Carlos Bohórquez Monroy**

## Part I — Changes made to the Wait-notify project
> Repository: [https://github.com/Juank-1030/Wait-notify-ARSW.git](https://github.com/Juank-1030/Wait-notify-ARSW.git)

### Summary

The original project was a multi-threaded prime number finder that **did not implement** the pause/resume logic with `wait()`/`notify()`. Three types of changes were made:

1. Java version compatibility fix in `pom.xml`
2. Refactoring of `PrimeFinderThread` to support coordinated pauses
3. Full implementation of the control logic in `Control`

---

### 1. `pom.xml` — Java version fix

#### Problem
When running `mvn compile exec:java` with Java 21, the modern Maven compiler threw:

```
[ERROR] Source option 7 is no longer supported. Use 8 or later.
[ERROR] Target option 7 is no longer supported. Use 8 or later.
```

The original `pom.xml` declared Java 1.7 as the compilation version, but `maven-compiler-plugin 3.15.0` with JDK 21 no longer supports generating bytecode for Java 7.

#### Applied change

```xml
<!-- BEFORE -->
<maven.compiler.source>1.7</maven.compiler.source>
<maven.compiler.target>1.7</maven.compiler.target>

<!-- AFTER -->
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>

**Why Java 21:** It is the most recent LTS version, aligned with the installed JDK (Java 21). The Snake project already uses Java 21, and keeping the same version avoids conflicts in multi-module environments. Java 21 features (virtual threads, records, etc.) are available to evolve the code if needed.

---

### 2. `PrimeFinderThread.java` — Pause support

#### Original state

```java
public PrimeFinderThread(int a, int b) { ... }

public void run() {
    for (int i = a; i < b; i++) {
        if (isPrime(i)) {
            primes.add(i);
            System.out.println(i);   // printed each prime
        }
    }
}
```

The thread was autonomous: it had no mechanism to pause itself nor a reference to a coordinator object.

#### Changes made

**a) New `control` field and updated constructor**

```java
// Added field
private final Control control;

// Updated constructor: receives the Control monitor
public PrimeFinderThread(int a, int b, Control control) {
    super();
    this.primes = new LinkedList<>();
    this.a = a;
    this.b = b;
    this.control = control;
}
```

**Why:** So that each thread can query `Control` about whether it should pause, it needs a reference to the shared monitor. It is passed in the constructor to guarantee it is always available from the start.

**b) `checkPause()` call in each loop iteration**

```java
public void run() {
    for (int i = a; i < b; i++) {
        try {
            control.checkPause();   // cooperative pause point
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;                 // cleanly terminates if interrupted
        }
        if (isPrime(i)) {
            primes.add(i);
            // Removed System.out.println per number (too much noise)
        }
    }
}
```

**Why `checkPause()` in each iteration:** It is a *cooperative pause* pattern. The thread voluntarily checks whether it should block. This is preferable to externally suspending threads (deprecated since Java 1.2 for being unsafe).

**Why handle `InterruptedException` with `return`:** If the thread is interrupted while waiting in `wait()`, it must restore the interrupt flag and terminate cleanly.

**Why `System.out.println(i)` was removed:** Printing 30 million numbers generates unnecessary noise and drastically degrades performance.

---

### 3. `Control.java` — Full coordinator implementation

#### Original state

```java
public class Control extends Thread {
    private static final int NTHREADS = 3;
    private static final int MAXVALUE = 30000000;
    private static final int TMILISECONDS = 5000;  // existed but was unused

    private PrimeFinderThread pft[];

    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();   // only started the threads, with no control
        }
    }
}
```

The constant `TMILISECONDS` was defined but **unused**. There was no pause, result display, or ENTER wait.

#### Changes made

**a) Monitor state variables**

```java
private boolean paused = false;
private int waitingCount = 0;
```

| Variable | Purpose |
|---|---|
| `paused` | Flag indicating to worker threads whether they should block |
| `waitingCount` | Counter of how many threads are already inside `wait()` |

`waitingCount` is key for `Control` to know when **all** threads are effectively paused before printing the report.

**b) `checkPause()` method — the heart of the mechanism**

```java
public synchronized void checkPause() throws InterruptedException {
    if (paused) {
        waitingCount++;
        notifyAll();         // notifies Control that this thread is entering wait
        while (paused) {
            wait();          // releases the monitor and sleeps
        }
        waitingCount--;      // on resume, decrements the counter
    }
}
```

**Why `while (paused)` and not `if (paused)`:** *Spurious wakeups* can occur without apparent reason in the JVM. The `while` guarantees the thread re-evaluates the condition before continuing.

**c) Main logic in `run()`**

```java
while (anyAlive()) {
    Thread.sleep(TMILISECONDS);    // waits 5 seconds

    int alive = aliveCount();
    synchronized (this) {
        paused = true;
        while (waitingCount < alive) {
            wait();                // waits until ALL threads are in wait()
        }
    }

    // All threads paused: safe read without additional sync
    int total = 0;
    for (PrimeFinderThread t : pft) {
        total += t.getPrimes().size();
    }
    System.out.println("Primes found so far: " + total);
    scanner.nextLine();            // blocks until user presses ENTER

    synchronized (this) {
        paused = false;
        notifyAll();               // wakes up all paused threads
    }
}
```

**Why `while (waitingCount < alive)` with `wait()`:** Control needs to be sure that **all** alive threads are in `wait()` before reading results. Without this wait, a race condition would occur when reading `primes`. Using `wait()` avoids *busy-waiting*.

**Why `aliveCount()` before pausing:** If a thread finished its range just before the pause, it will not be in `wait()`. Comparing against the **alive** threads at that moment prevents `Control` from waiting indefinitely for a thread that has already finished.

---

### Synchronization flow diagram

```
Control.run()                           PrimeFinderThread.run()
──────────────────────────────────      ───────────────────────────────────
starts 3 threads →                      loop: for i = a..b
                                          checkPause()   ← each number
sleep(5000ms)
                                            synchronized(control):
paused = true                               if paused:
                                              waitingCount++
                                              notifyAll()  ──→ wakes Control
wait() until waitingCount==N  ←────────────  wait()         (thread blocked)

all paused → reads getPrimes()
shows total
waits for ENTER from user

paused = false
notifyAll()  ─────────────────────────→   exits while(paused)
                                          waitingCount--
                                          continues the loop
```

---

### Summary table of changes

| File | Change | Reason |
|---|---|---|
| `pom.xml` | `1.7` → `21` in `maven.compiler.source/target` | Java 7 not supported by modern compilers |
| `PrimeFinderThread` | New `Control control` field + updated constructor | Needs monitor reference to call `checkPause()` |
| `PrimeFinderThread` | `checkPause()` called in each iteration | Cooperative pause without busy-waiting |
| `PrimeFinderThread` | Removed `System.out.println(i)` | Avoid flooding the console with 30M lines |
| `Control` | Fields `paused` and `waitingCount` | Shared monitor state |
| `Control` | `checkPause()` method with `wait()`/`notifyAll()` | Actual blocking point for worker threads |
| `Control` | Loop with `sleep` + pause + ENTER + resume | Implements the pause cycle every 5 seconds |
| `Control` | Methods `anyAlive()` and `aliveCount()` | Detect finished threads to avoid infinite waits |
| `Control` | `t.join()` + final report | Ensures all threads finished before showing the total |

---

## Part II — Concurrent SnakeRace

### 1) Concurrency problems identified in the original version

---

#### CR-1: `Snake.body` (ArrayDeque) — unsynchronized access between threads

`Snake` uses `ArrayDeque<Position>` as its internal structure. The `SnakeRunner` (virtual thread) writes to the body via `advance()`, while the EDT (Swing) reads it via `snapshot()` on each repaint. `ArrayDeque` is **not thread-safe**; a concurrent read with a write can produce an `ArrayIndexOutOfBoundsException` or a corrupted snapshot.

**Risk:** inconsistent visual or exception on the Swing thread.

---

#### CR-2: `Snake.direction` — `turn()` is not atomic

`turn()` read `direction` to validate it was not a 180° turn, then wrote the new direction. Between the read and the write, another thread (the runner or keyboard) could modify `direction`, causing the snake to fold onto itself.

**Risk:** snake moves in the opposite direction (movement glitch).

---

#### CR-3: `Board.randomEmpty()` — implicit caller lock

`randomEmpty()` accesses `mice`, `obstacles`, `turbo`, and `teleports` without acquiring any lock of its own. It is only safe because it is called exclusively from `step()`, which does hold the lock. If called from another context in the future, the collections would be exposed.

**Risk:** latent, could cause races if the method is reused outside `step()`.

---

#### CR-4: `Board` collections — `HashSet`/`HashMap` not thread-safe

`Board` stores the game state in `HashSet` and `HashMap`. Without external synchronization, the EDT reads these collections in `paintComponent()` while the runners modify them in `step()`.

**Risk:** `ConcurrentModificationException` or inconsistent reads in the UI.

---

#### BW-1: `GameClock` — busy-wait during pause

```java
scheduler.scheduleAtFixedRate(() -> {
    if (state.get() == GameState.RUNNING) tick.run();  // state polling
}, 0, periodMillis, TimeUnit.MILLISECONDS);
```

The scheduler keeps waking up every 60ms even with the game paused, only to discover it should do nothing. This is **disguised busy-wait**: unnecessary CPU consumption.

**Risk:** CPU consumption during pause, battery drain on laptops.

---

#### DR-1: `SnakeRunner` — random turns on player snakes

`maybeTurn()` applied to **all** snakes, including those controlled by keyboard (arrows, WASD). Every ~80ms there was a 10% chance the snake would randomly change direction, overriding the player's input.

**Risk:** player loses control of their snake, frustrating game experience.

---

#### DL-1: `PauseControl` — deadlock when pausing with dead snakes

When a snake died hitting an obstacle, its runner ended the loop. If the user pressed *Pause* afterwards, `pauseAndWaitForAll()` calculated once how many alive runners to expect, but that number included the already finished runner, so `waitingCount` never reached it and the UI thread blocked forever.

**Risk:** frozen UI, user forced to close the window.

---

#### DL-2: `PauseControl` — deadlock when resuming during collection

If the user pressed *Resume* while `pauseAndWaitForAll()` was still collecting runners (before all reached `wait()`), the while loop did not check `paused`, so even though `paused = false` and no more signals would arrive, the UI thread kept waiting.

**Risk:** frozen UI when clicking pause/resume very quickly.

---

#### Summary table of findings

| ID | File | Type | Description | Severity |
|---|---|---|---|---|
| CR-1 | `Snake.java` | Data race | `ArrayDeque.snapshot()` vs `advance()` without sync | High |
| CR-2 | `Snake.java` | Data race | `turn()` reads/writes `direction` non-atomically | High |
| CR-3 | `Board.java` | Risk | `randomEmpty()` assumes caller's lock | Low |
| CR-4 | `Board.java` | Unsafe collection | `HashSet`/`HashMap` without sync | Medium |
| BW-1 | `GameClock.java` | Busy-wait | Scheduler polls state during pause | Medium |
| DR-1 | `SnakeRunner.java` | Design | `maybeTurn()` interferes with player snakes | High |
| DL-1 | `PauseControl.java` | Deadlock | `alive` not recalculated after runner death | High |
| DL-2 | `PauseControl.java` | Deadlock | Does not check `paused` in collection while | High |

---

### 2) Applied fixes — solution for each problem

---

#### CR-1: `synchronized` on `Snake.body`

All methods that access `body` were marked `synchronized`: `advance()`, `snapshot()`, `head()`. This guarantees that the runner (writer) and the EDT (reader) never access the `ArrayDeque` concurrently.

```java
public synchronized Deque<Position> snapshot() { return new ArrayDeque<>(body); }
public synchronized void advance(Position newHead, boolean grow) { ... }
public synchronized Position head() { return body.peekFirst(); }
```

**Minimum critical region:** only accesses to `body` are synchronized. The `maxLength` calculation inside `advance()` is also protected by being in the same synchronized method.

---

#### CR-2: `synchronized` on `Snake.turn()` and `direction()`

`volatile` was removed from `direction` and both `turn()` and `direction()` were marked `synchronized`. Now the validation and write of the new direction occur in a single atomic critical region, under the same `Snake` monitor.

```java
public synchronized void turn(Direction dir) { ... }
public synchronized Direction direction() { return direction; }
```

This also guarantees that `advance()` (which reads `direction` indirectly through `Board.step()`) sees a consistent direction state together with the body.

---

#### CR-3 and CR-4: `ReentrantReadWriteLock` on `Board`

`synchronized` was replaced with a `ReentrantReadWriteLock`:

- **Readers** (`mice()`, `obstacles()`, `turbo()`, `teleports()`): acquire the `readLock`, allowing multiple threads (EDT + paused runners) to read simultaneously.
- **Writer** (`step()`): acquires the `writeLock`, exclusive against any other reader or writer.

```java
public Set<Position> mice() {
    rwLock.readLock().lock();
    try { return new HashSet<>(mice); }
    finally { rwLock.readLock().unlock(); }
}

public MoveResult step(Snake snake) {
    rwLock.writeLock().lock();
    try { /* modify collections */ }
    finally { rwLock.writeLock().unlock(); }
}
```

`randomEmpty()` remains private and without its own lock; it is called exclusively from `step()` (writeLock) and the constructor (single-thread), so its access to collections is always protected.

**Benefit:** with N=20 runners, contention is drastically reduced because the EDT can read the board while no runner is in the writing phase.

---

#### BW-1: Scheduler cancellation in `GameClock`

Instead of keeping the scheduler polling `state` every 60ms, the reference to the `ScheduledFuture<?>` returned by `scheduleAtFixedRate()` is stored:

- `pause()` calls `tickTask.cancel(false)` — the scheduler stops waking entirely during pause.
- `resume()` reschedules the tick from scratch.

```java
public void pause() {
    state.set(GameState.PAUSED);
    ScheduledFuture<?> current = tickTask;
    if (current != null) current.cancel(false);
}

public void resume() {
    if (state.compareAndSet(GameState.PAUSED, GameState.RUNNING)) {
        tickTask = scheduler.scheduleAtFixedRate(tick, 0, periodMillis, TimeUnit.MILLISECONDS);
    }
}
```

**Benefit:** zero CPU consumption during pause. `cancel(false)` waits for the current tick to finish before canceling, avoiding leaving the UI in an inconsistent state.

---

#### DR-1: `autoPilot` in `SnakeRunner` — turns only for AI

The `boolean autoPilot` parameter was added to the `SnakeRunner` constructor. Only snakes with `autoPilot = true` execute `maybeTurn()` in each iteration. In `SnakeApp`, snakes 0 and 1 (player-controlled) are created with `autoPilot = false`; snakes 2+ (AI) with `autoPilot = true`.

```java
// SnakeApp.java
for (int i = 0; i < snakes.size(); i++) {
    boolean autoPilot = i >= 2;
    exec.submit(new SnakeRunner(snakes.get(i), board, pauseControl, autoPilot));
}

// SnakeRunner.java
if (autoPilot) maybeTurn();  // ← only AI turns randomly
```

**Benefit:** the player maintains full control over their snake. AI snakes remain autonomous.

---

#### DL-1 and DL-2: Deadlock fixes in `PauseControl`

**DL-1 — alive recalculated each iteration:**

```java
while (paused && waitingCount < snakes.size()) {
    wait();
}
```

`waitingCount` is compared against `snakes.size()` (the total number of snakes, not just "alive" ones, because no snake dies in this version). If a runner finishes between the condition calculation and the signal, the while keeps waiting until the remaining runners cover the shortfall, or until `paused` becomes `false`.

**DL-2 — `paused` check in the while:**

```java
while (paused && waitingCount < snakes.size()) {
    wait();
}
```

If the user presses *Resume* during collection, `paused` becomes `false`, the condition exits the while, and the UI thread continues (without attempting to show stats).

**Additionally — `waitingCount` leak:**

```java
public synchronized void checkPause() throws InterruptedException {
    if (paused) {
        waitingCount++;
        notifyAll();
        try {
            while (paused) {
                wait();
            }
        } finally {
            waitingCount--;  // ← always decrements, even with InterruptedException
        }
    }
}
```

The `try-finally` guarantees that `waitingCount` is decremented even if the runner receives `InterruptedException` while in `wait()`.

---

### 3) Concurrency architecture — current state

#### Threads involved

The system uses four types of threads:

1. **EDT (Event Dispatch Thread)** — Swing's main thread. Creates all objects (`Board`, `SnakeRunner`, `GameClock`, `PauseControl`), processes keyboard events (arrows for snake 0, WASD for snake 1) and executes window repainting (`paintComponent()`) every ~60ms via the `GameClock`.

2. **N VirtualThreads (SnakeRunner)** — Each snake has its own virtual thread created with `Executors.newVirtualThreadPerTaskExecutor()`. Each runner executes an infinite loop: `checkPause()` → `maybeTurn()` (only if `autoPilot=true`) → `board.step()` → `Thread.sleep()`. Snake 0 (`autoPilot=false`) only responds to keyboard; snake 1 likewise; snakes 2+ (`autoPilot=true`) turn randomly.

3. **1 VirtualThread (PauseAndWait)** — Created temporarily when pausing the game. Executes `pauseControl.pauseAndWaitForAll()`, which blocks on `wait()` until all `SnakeRunner` instances are stopped. On resume, this thread ends.

4. **SchedulerThread (GameClock)** — Single thread from the `ScheduledExecutorService`. In RUNNING state it executes `tick` every 60ms (only `SwingUtilities.invokeLater(gamePanel::repaint)`). In PAUSED state the `ScheduledFuture` is canceled, so this thread does not wake up.

#### Pause/Resume flow

**Pausing:**
1. User clicks *Action* or presses *Space*.
2. `SnakeApp.togglePause()` changes the button text to *Resume*.
3. `clock.pause()` cancels the `ScheduledFuture` of the repaint tick.
4. A **virtual thread** is launched that calls `PauseControl.pauseAndWaitForAll()`:
   a. Sets `paused = true`.
   b. Enters `while (paused && waitingCount < snakes.size())`.
   c. Calls `wait()` — releases the `PauseControl` monitor.
5. Each `SnakeRunner`, at the start of its next iteration, calls `checkPause()`:
   a. Sees `paused = true`, increments `waitingCount`.
   b. Calls `notifyAll()` — wakes the UI thread to re-evaluate the condition.
   c. Enters `while (paused) wait()` — the runner truly blocks.
6. When `waitingCount == snakes.size()`, the UI thread exits the while and terminates.
   - At this point: **all runners are blocked → stable state**.

**Resuming:**
1. User clicks *Resume* or presses *Space*.
2. `PauseControl.resume()` sets `paused = false` and calls `notifyAll()`.
3. All runners wake up, see `paused = false`, exit `while (paused)` and continue their loop.
4. `clock.resume()` reschedules the repaint tick.
5. The button returns to showing *Action*.

#### Synchronization mechanism

| Component | Mechanism | Purpose |
|---|---|---|
| `Snake` | `synchronized` (intrinsic monitor) | Protect `body` (ArrayDeque) between runner (writer) and EDT (reader). A single lock covers `direction` + `body` for consistency. |
| `Board` | `ReentrantReadWriteLock` | Multiple concurrent readers (EDT + stats) vs exclusive writer (runner in step). |
| `PauseControl` | `synchronized` + `wait/notifyAll` | Cooperative pause coordination between N runners and the UI. No busy-wait. |
| `GameClock` | `AtomicReference<GameState>` + `ScheduledFuture.cancel()` | Atomic clock state. Physical cancellation of the scheduler during pause. |
| `SnakeApp.snakes` | `CopyOnWriteArrayList` | Safe iteration from EDT without locks. Write-only during construction. |
| `SnakeRunner.turboTicks` | Instance variable (single thread) | Only the runner itself reads/writes; no synchronization required. |

#### Critical regions

| Critical Region | Lock | Threads involved |
|---|---|---|
| `Snake.advance()` → modifies `body` | `Snake.this` | 1 runner |
| `Snake.snapshot()` → reads `body` | `Snake.this` | EDT |
| `Snake.turn()` → reads/writes `direction` | `Snake.this` | EDT or 1 runner |
| `Board.step()` → modifies collections | `Board.rwLock.writeLock` | 1 runner |
| `Board.mice/obstacles/turbo/teleports()` → reads collections | `Board.rwLock.readLock` | EDT (repaint) |
| `PauseControl.checkPause()` → modifies `waitingCount` | `PauseControl.this` | N runners |
| `PauseControl.pauseAndWaitForAll()` → reads `waitingCount` | `PauseControl.this` | 1 virtual thread (UI) |

#### Lock ordering (important to avoid deadlocks)

```
SnakeRunner → Board.writeLock → Snake.this  (runner advancing)
EDT         → Snake.this                    (turn, snapshot)
EDT         → Board.readLock                (paintComponent)
```

There is no circular dependency: the EDT never acquires `Board.writeLock`, and the runner never acquires another lock after releasing `Board.writeLock`. Therefore **there is no possibility of deadlock from lock ordering**.

---

### 4) Changes compared to the reference version (`main/`)

The project started from a reference implementation in `main/java/` that had correct game logic but without any concurrency fixes. On that basis, the following changes were applied:

| File | main (reference) | src (final version) |
|---|---|---|
| `Snake.java` | No synchronization, `volatile direction` | All methods `synchronized`, no `volatile` |
| `Board.java` | `synchronized` on methods | `ReentrantReadWriteLock` (read/write) |
| `GameClock.java` | Scheduler polls state every 60ms (busy-wait) | `ScheduledFuture.cancel()` on pause, reschedule on resume |
| `SnakeRunner.java` | No PauseControl, `maybeTurn()` on all | `autoPilot` to separate AI/player, cooperative `checkPause()` |
| `PauseControl.java` | Did not exist | New class: monitor with `wait/notifyAll`, deadlock fixes |
| `SnakeApp.java` | `ArrayList`, `EXIT_ON_CLOSE`, simple togglePause | `CopyOnWriteArrayList`, coordinated shutdown, PauseControl integrated |
| `pom.xml` | No Maven configuration | `exec-maven-plugin` with `systemProperties` for `-Dsnakes` |

#### Functionality preserved from `main` (game rules)

- **Bounce on obstacles**: when hitting, the snake turns randomly (`randomTurn()`) and stays alive.
- **No death**: there is no concept of a dead snake; `markDead()`/`isDead()`/`deathTime()` are not used.
- **AI autonomy**: snakes 2+ turn randomly with configurable probability.
- **Turbo, teleporters, mice**: identical behavior.

#### Added functionality (not present in `main`)

- **Stats on pause**: when pressing *Action* or *Space*, after all runners are blocked, a `JOptionPane` shows the longest snake and individual lengths of each snake. The `pauseControl.isPaused()` check prevents showing the dialog if the user resumed before collection finished.
- **CopyOnWriteArrayList**: replaces `ArrayList` for safe iteration from the EDT without locks.
- **Coordinated shutdown**: on window close, all virtual threads are interrupted and the clock scheduler is shut down.

#### Lines per file summary

| File | Lines | Main changes |
|---|---|---|
| `pom.xml` | 58 | `<snakes>` property, `exec-maven-plugin` with systemProperties |
| `Main.java` | 10 | No changes |
| `Snake.java` | 49 | `synchronized` on all public methods |
| `Board.java` | 155 | `ReadWriteLock`, defensive copies |
| `Direction.java` | 4 | No changes |
| `Position.java` | 9 | No changes |
| `GameState.java` | 2 | No changes |
| `GameClock.java` | 59 | `ScheduledFuture` for cancel/reschedule |
| `PauseControl.java` | 70 | New class: wait/notify, deadlocks fixed |
| `SnakeRunner.java` | 57 | `autoPilot`, `checkPause()`, bounce on obstacle |
| `SnakeApp.java` | 263 | `CopyOnWriteArrayList`, shutdown, PauseControl, autoPilot by index |

---

### 5) How to verify correct concurrency

1. **Run with high N**:
   ```bash
   mvn -q -DskipTests exec:java -Dsnakes=20
   ```
   The game must run without `ConcurrentModificationException`, without inconsistent reads, without deadlocks.

2. **Pause and resume repeatedly**:
   Press *Space* or *Action/Resume* quickly multiple times. The UI must not freeze nor lose control of the snakes.

3. **Close the window**:
   On close, all virtual threads must terminate cleanly (no exceptions on the console).

4. **Player control**:
   Snakes 0 (arrows) and 1 (WASD) must respond exclusively to the keyboard, without unsolicited random turns.
