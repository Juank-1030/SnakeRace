# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.

---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

> Objetivo didáctico: practicar suspensión/continuación **sin** espera activa y consolidar el modelo de monitores en Java.

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.
- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.
  - **Colecciones** o estructuras **no seguras** en contexto concurrente.
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---

## Criterios de evaluación (10)

- (3) **Concurrencia correcta**: sin data races; sincronización bien localizada.
- (2) **Pausa/Reanudar**: consistencia visual y de estado.
- (2) **Robustez**: corre **con N alto** y sin excepciones de concurrencia.
- (1.5) **Calidad**: estructura clara, nombres, comentarios; sin _code smells_ obvios.
- (1.5) **Documentación**: **`reporte de laboratorio`** claro, reproducible;

---

## Tips y configuración útil

- **Número de serpientes**: `-Dsnakes=N` al ejecutar.
- **Tamaño del tablero**: cambiar el constructor `new Board(width, height)`.
- **Teleports / Turbo**: editar `Board.java` (métodos de inicialización y reglas en `step(...)`).
- **Velocidad**: ajustar `GameClock` (tick) o el `sleep` del `SnakeRunner` (incluye modo turbo).

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

## Créditos

Este laboratorio es una adaptación modernizada del ejercicio **SnakeRace** de ARSW. El enunciado de actividades se conserva para mantener los objetivos pedagógicos del curso.

**Base construida por el Ing. Javier Toquica.**

---

# Informe

## Parte I — Cambios realizados en el proyecto Wait-notify

### Resumen

El proyecto original era un buscador de números primos multihilo que **no tenía implementada** la lógica de pausa/reanudación con `wait()`/`notify()`. Se realizaron tres tipos de cambios:

1. Corrección de compatibilidad de versión Java en `pom.xml`
2. Refactorización de `PrimeFinderThread` para soportar pausas coordinadas
3. Implementación completa de la lógica de control en `Control`

---

### 1. `pom.xml` — Corrección de versión de Java

#### Problema
Al ejecutar `mvn compile exec:java` con Java 21, el compilador moderno de Maven arrojaba:

```
[ERROR] Source option 7 is no longer supported. Use 8 or later.
[ERROR] Target option 7 is no longer supported. Use 8 or later.
```

El `pom.xml` original declaraba Java 1.7 como versión de compilación, pero las versiones del compilador `maven-compiler-plugin 3.15.0` con JDK 21 ya no soportan generar bytecode para Java 7.

#### Cambio aplicado

```xml
<!-- ANTES -->
<maven.compiler.source>1.7</maven.compiler.source>
<maven.compiler.target>1.7</maven.compiler.target>

<!-- DESPUÉS -->
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>

**Por qué Java 21:** Es la versión LTS más reciente, alineada con el JDK instalado (Java 21). El proyecto Snake ya usa Java 21, y mantener la misma versión evita conflictos en entornos multimódulo. Las características de Java 21 (virtual threads, records, etc.) están disponibles para evolucionar el código si se requiere.

---

### 2. `PrimeFinderThread.java` — Soporte de pausas

#### Estado original

```java
public PrimeFinderThread(int a, int b) { ... }

public void run() {
    for (int i = a; i < b; i++) {
        if (isPrime(i)) {
            primes.add(i);
            System.out.println(i);   // imprimía cada primo
        }
    }
}
```

El hilo era autónomo: no tenía ningún mecanismo para pausarse ni referencia a un objeto coordinador.

#### Cambios realizados

**a) Nuevo campo `control` y actualización del constructor**

```java
// Campo agregado
private final Control control;

// Constructor actualizado: recibe el monitor Control
public PrimeFinderThread(int a, int b, Control control) {
    super();
    this.primes = new LinkedList<>();
    this.a = a;
    this.b = b;
    this.control = control;
}
```

**Por qué:** Para que cada hilo pueda consultar al `Control` si debe pausarse, necesita una referencia al monitor compartido. Se pasa en el constructor para garantizar que siempre esté disponible desde el inicio.

**b) Llamada a `checkPause()` en cada iteración del loop**

```java
public void run() {
    for (int i = a; i < b; i++) {
        try {
            control.checkPause();   // punto de pausa cooperativa
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;                 // termina limpiamente si es interrumpido
        }
        if (isPrime(i)) {
            primes.add(i);
            // Se eliminó el System.out.println por número (demasiado ruido)
        }
    }
}
```

**Por qué `checkPause()` en cada iteración:** Es un patrón de *pausa cooperativa*. El hilo verifica voluntariamente si debe bloquearse. Esto es preferible a suspender hilos externamente (deprecado desde Java 1.2 por ser inseguro).

**Por qué manejar `InterruptedException` con `return`:** Si el hilo es interrumpido mientras espera en `wait()`, debe restaurar la bandera de interrupción y terminar limpiamente.

**Por qué se eliminó `System.out.println(i)`:** Imprimir 30 millones de números genera ruido innecesario y degrada drásticamente el rendimiento.

---

### 3. `Control.java` — Implementación completa del coordinador

#### Estado original

```java
public class Control extends Thread {
    private static final int NTHREADS = 3;
    private static final int MAXVALUE = 30000000;
    private static final int TMILISECONDS = 5000;  // existía pero no se usaba

    private PrimeFinderThread pft[];

    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();   // solo iniciaba los hilos, sin ningún control
        }
    }
}
```

La constante `TMILISECONDS` estaba definida pero **sin usar**. No había pausa, ni muestra de resultados, ni espera de ENTER.

#### Cambios realizados

**a) Variables de estado del monitor**

```java
private boolean paused = false;
private int waitingCount = 0;
```

| Variable | Propósito |
|---|---|
| `paused` | Bandera que indica a los hilos trabajadores si deben bloquearse |
| `waitingCount` | Contador de cuántos hilos ya están dentro del `wait()` |

`waitingCount` es clave para que `Control` sepa cuándo **todos** los hilos están efectivamente pausados antes de imprimir el reporte.

**b) Método `checkPause()` — el corazón del mecanismo**

```java
public synchronized void checkPause() throws InterruptedException {
    if (paused) {
        waitingCount++;
        notifyAll();         // avisa a Control que este hilo está entrando a wait
        while (paused) {
            wait();          // libera el monitor y duerme
        }
        waitingCount--;      // al reanudar, decrementa el contador
    }
}
```

**Por qué `while (paused)` y no `if (paused)`:** Los *spurious wakeups* (despertares espurios) pueden ocurrir sin razón aparente en la JVM. El `while` garantiza que el hilo re-evalúe la condición antes de continuar.

**c) Lógica principal en `run()`**

```java
while (anyAlive()) {
    Thread.sleep(TMILISECONDS);    // espera 5 segundos

    int alive = aliveCount();
    synchronized (this) {
        paused = true;
        while (waitingCount < alive) {
            wait();                // espera a que TODOS los hilos estén en wait()
        }
    }

    // Todos los hilos pausados: lectura segura sin sync adicional
    int total = 0;
    for (PrimeFinderThread t : pft) {
        total += t.getPrimes().size();
    }
    System.out.println("Primos encontrados hasta ahora: " + total);
    scanner.nextLine();            // bloquea hasta que el usuario presione ENTER

    synchronized (this) {
        paused = false;
        notifyAll();               // despierta a todos los hilos pausados
    }
}
```

**Por qué `while (waitingCount < alive)` con `wait()`:** Control necesita estar seguro de que **todos** los hilos vivos están en `wait()` antes de leer los resultados. Sin esta espera se generaría una condición de carrera al leer `primes`. Usar `wait()` evita el *busy-waiting*.

**Por qué `aliveCount()` antes de pausar:** Si un hilo terminó su rango justo antes de la pausa, no estará en `wait()`. Comparar contra los hilos **vivos** en ese momento evita que `Control` espere infinitamente a un hilo que ya terminó.

---

### Diagrama del flujo de sincronización

```
Control.run()                           PrimeFinderThread.run()
──────────────────────────────────      ───────────────────────────────────
inicia 3 hilos →                        loop: for i = a..b
                                          checkPause()   ← cada número
sleep(5000ms)
                                            synchronized(control):
paused = true                               if paused:
                                              waitingCount++
                                              notifyAll()  ──→ despierta Control
wait() hasta waitingCount==N  ←────────────  wait()         (hilo bloqueado)

todos pausados → lee getPrimes()
muestra total
espera ENTER del usuario

paused = false
notifyAll()  ─────────────────────────→   sale del while(paused)
                                          waitingCount--
                                          continúa el loop
```

---

### Tabla resumen de cambios

| Archivo | Cambio | Razón |
|---|---|---|
| `pom.xml` | `1.7` → `21` en `maven.compiler.source/target` | Java 7 no es soportado por compiladores modernos |
| `PrimeFinderThread` | Nuevo campo `Control control` + constructor actualizado | Necesita referencia al monitor para llamar `checkPause()` |
| `PrimeFinderThread` | `checkPause()` llamado en cada iteración | Pausa cooperativa sin busy-waiting |
| `PrimeFinderThread` | Eliminado `System.out.println(i)` | Evita inundar la consola con 30M de líneas |
| `Control` | Campos `paused` y `waitingCount` | Estado compartido del monitor |
| `Control` | Método `checkPause()` con `wait()`/`notifyAll()` | Punto de bloqueo real para los hilos trabajadores |
| `Control` | Bucle con `sleep` + pausa + ENTER + reanudación | Implementa el ciclo de pausa cada 5 segundos |
| `Control` | Métodos `anyAlive()` y `aliveCount()` | Detectar hilos terminados para evitar esperas infinitas |
| `Control` | `t.join()` + reporte final | Asegura que todos los hilos terminaron antes de mostrar el total |

---

## Parte II — SnakeRace concurrente

### 1) Problemas de concurrencia identificados en la versión original

---

#### CR-1: `Snake.body` (ArrayDeque) — acceso sin sincronización entre hilos

`Snake` usa `ArrayDeque<Position>` como estructura interna. El `SnakeRunner` (virtual thread) escribe el cuerpo mediante `advance()`, mientras que el EDT (Swing) lo lee mediante `snapshot()` en cada repaint. `ArrayDeque` **no es thread-safe**; una lectura concurrente con una escritura puede producir un `ArrayIndexOutOfBoundsException` o un snapshot corrupto.

**Riesgo:** visual inconsistente o excepción en el hilo de Swing.

---

#### CR-2: `Snake.direction` — `turn()` no es atómico

`turn()` leía `direction` para validar que no sea un giro de 180°, y luego escribía la nueva dirección. Entre la lectura y la escritura, otro hilo (el runner o el teclado) podía modificar `direction`, causando que la serpiente se doble sobre sí misma.

**Riesgo:** serpiente se mueve en dirección opuesta (glitch de movimiento).

---

#### CR-3: `Board.randomEmpty()` — lock implícito del llamador

`randomEmpty()` accede a `mice`, `obstacles`, `turbo` y `teleports` sin adquirir ningún lock propio. Solo es seguro porque es llamado exclusivamente desde `step()`, que sí sostiene el lock. Si en el futuro se invoca desde otro contexto, las colecciones quedarían expuestas.

**Riesgo:** latente, podría causar carreras si se reusa el método fuera de `step()`.

---

#### CR-4: Colecciones del `Board` — `HashSet`/`HashMap` no thread-safe

`Board` almacena el estado del juego en `HashSet` y `HashMap`. Sin sincronización externa, el EDT lee estas colecciones en `paintComponent()` mientras los runners las modifican en `step()`.

**Riesgo:** `ConcurrentModificationException` o lecturas inconsistentes en la UI.

---

#### BW-1: `GameClock` — busy-wait durante pausa

```java
scheduler.scheduleAtFixedRate(() -> {
    if (state.get() == GameState.RUNNING) tick.run();  // polling de estado
}, 0, periodMillis, TimeUnit.MILLISECONDS);
```

El scheduler sigue despertando cada 60ms incluso con el juego pausado, solo para descubrir que no debe hacer nada. Es **busy-wait disfrazado**: consumo innecesario de CPU.

**Riesgo:** consumo de CPU en pausa, batería en laptops.

---

#### DR-1: `SnakeRunner` — giros aleatorios en serpientes de jugador

`maybeTurn()` aplicaba a **todas** las serpientes, incluyendo las controladas por teclado (flechas, WASD). Cada ~80ms había 10% de probabilidad de que la serpiente cambiara de dirección aleatoriamente, pisando la entrada del jugador.

**Riesgo:** jugador pierde el control de su serpiente, experiencia de juego frustrante.

---

#### DL-1: `PauseControl` — deadlock al pausar con serpientes muertas

Cuando una serpiente moría al chocar con un obstáculo, su runner terminaba el loop. Si el usuario presionaba *Pause* después, `pauseAndWaitForAll()` calculaba una vez cuántos runners vivos esperar, pero ese número incluía al runner ya terminado, por lo que `waitingCount` nunca lo alcanzaba y el hilo de la UI se bloqueaba para siempre.

**Riesgo:** UI congelada, usuario obligado a cerrar la ventana.

---

#### DL-2: `PauseControl` — deadlock al reanudar durante la recolección

Si el usuario presionaba *Resume* mientras `pauseAndWaitForAll()` aún recolectaba runners (antes de que todos llegaran a `wait()`), el while no verificaba `paused`, por lo que aunque `paused = false` y ya no llegarían más señales, el hilo de la UI seguía esperando.

**Riesgo:** UI congelada al hacer clic muy rápido en pausa/reanudar.

---

#### Tabla resumen de hallazgos

| ID | Archivo | Tipo | Descripción | Severidad |
|---|---|---|---|---|
| CR-1 | `Snake.java` | Data race | `ArrayDeque.snapshot()` vs `advance()` sin sync | Alta |
| CR-2 | `Snake.java` | Data race | `turn()` lee y escribe `direction` no atómicamente | Alta |
| CR-3 | `Board.java` | Riesgo | `randomEmpty()` asume lock del llamador | Baja |
| CR-4 | `Board.java` | Colección no segura | `HashSet`/`HashMap` sin sync | Media |
| BW-1 | `GameClock.java` | Busy-wait | Scheduler sondea estado durante pausa | Media |
| DR-1 | `SnakeRunner.java` | Diseño | `maybeTurn()` interfiere con serpientes de jugador | Alta |
| DL-1 | `PauseControl.java` | Deadlock | `alive` no se recalcula tras muerte de runner | Alta |
| DL-2 | `PauseControl.java` | Deadlock | No verifica `paused` en while de recolección | Alta |

---

### 2) Correcciones aplicadas — solución por cada problema

---

#### CR-1: `synchronized` en `Snake.body`

Se marcaron como `synchronized` todos los métodos que acceden a `body`: `advance()`, `snapshot()`, `head()`. Esto garantiza que el runner (escritura) y el EDT (lectura) nunca accedan al `ArrayDeque` concurrentemente.

```java
public synchronized Deque<Position> snapshot() { return new ArrayDeque<>(body); }
public synchronized void advance(Position newHead, boolean grow) { ... }
public synchronized Position head() { return body.peekFirst(); }
```

**Región crítica mínima:** solo los accesos a `body` están sincronizados. El cálculo de `maxLength` dentro de `advance()` también queda protegido por estar en el mismo método sincronizado.

---

#### CR-2: `synchronized` en `Snake.turn()` y `direction()`

Se eliminó `volatile` de `direction` y se marcaron `turn()` y `direction()` como `synchronized`. Ahora la validación y la escritura de la nueva dirección ocurren en una sola región crítica atómica, bajo el mismo monitor de `Snake`.

```java
public synchronized void turn(Direction dir) { ... }
public synchronized Direction direction() { return direction; }
```

Esto también garantiza que `advance()` (que lee `direction` indirectamente a través de `Board.step()`) vea un estado consistente de dirección junto con el cuerpo.

---

#### CR-3 y CR-4: `ReentrantReadWriteLock` en `Board`

Se reemplazó `synchronized` por un `ReentrantReadWriteLock`:

- **Lectores** (`mice()`, `obstacles()`, `turbo()`, `teleports()`): adquieren el `readLock`, permitiendo que múltiples hilos (EDT + runners en pausa) lean simultáneamente.
- **Escritor** (`step()`): adquiere el `writeLock`, exclusivo contra cualquier otro lector o escritor.

```java
public Set<Position> mice() {
    rwLock.readLock().lock();
    try { return new HashSet<>(mice); }
    finally { rwLock.readLock().unlock(); }
}

public MoveResult step(Snake snake) {
    rwLock.writeLock().lock();
    try { /* modificar colecciones */ }
    finally { rwLock.writeLock().unlock(); }
}
```

`randomEmpty()` sigue siendo privado y sin lock propio; se llama exclusivamente desde `step()` (writeLock) y el constructor (single-thread), por lo que su acceso a las colecciones está siempre protegido.

**Beneficio:** con N=20 runners, la contención se reduce drásticamente porque el EDT puede leer el tablero mientras ningún runner está en la fase de escritura.

---

#### BW-1: Cancelación del scheduler en `GameClock`

En lugar de mantener el scheduler sondeando `state` cada 60ms, se guarda la referencia al `ScheduledFuture<?>` devuelto por `scheduleAtFixedRate()`:

- `pause()` llama a `tickTask.cancel(false)` — el scheduler deja de despertar por completo durante la pausa.
- `resume()` reprograma el tick desde cero.

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

**Beneficio:** cero consumo de CPU durante la pausa. El `cancel(false)` espera a que el tick en curso termine antes de cancelar, evitando dejar la UI en estado inconsistente.

---

#### DR-1: `autoPilot` en `SnakeRunner` — giros solo para IA

Se agregó el parámetro `boolean autoPilot` al constructor de `SnakeRunner`. Solo las serpientes con `autoPilot = true` ejecutan `maybeTurn()` en cada iteración. En `SnakeApp`, las serpientes 0 y 1 (controladas por jugador) se crean con `autoPilot = false`; las serpientes 2+ (IA) con `autoPilot = true`.

```java
// SnakeApp.java
for (int i = 0; i < snakes.size(); i++) {
    boolean autoPilot = i >= 2;
    exec.submit(new SnakeRunner(snakes.get(i), board, pauseControl, autoPilot));
}

// SnakeRunner.java
if (autoPilot) maybeTurn();  // ← solo IA gira aleatoriamente
```

**Beneficio:** el jugador mantiene control total sobre su serpiente. Las serpientes IA siguen siendo autónomas.

---

#### DL-1 y DL-2: Corrección de deadlocks en `PauseControl`

**DL-1 — alive recalulado en cada iteración:**

```java
while (paused && waitingCount < snakes.size()) {
    wait();
}
```

`waitingCount` se compara contra `snakes.size()` (el total de serpientes, no solo las "vivas", porque en esta versión ninguna serpiente muere). Si un runner termina entre el cálculo de la condición y la señal, el while sigue esperando hasta que los runners restantes cubran el faltante, o hasta que `paused` sea `false`.

**DL-2 — verificación de `paused` en el while:**

```java
while (paused && waitingCount < snakes.size()) {
    wait();
}
```

Si el usuario presiona *Resume* durante la recolección, `paused` se vuelve `false`, la condición sale del while, y el hilo de la UI continúa (sin intentar mostrar estadísticas).

**Además — fuga de `waitingCount`:**

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
            waitingCount--;  // ← siempre decrementa, incluso con InterruptedException
        }
    }
}
```

El `try-finally` garantiza que `waitingCount` se decremente incluso si el runner recibe `InterruptedException` mientras está en `wait()`.

---

### 3) Arquitectura de concurrencia — estado actual

#### Hilos involucrados

El sistema utiliza cuatro tipos de hilos:

1. **EDT (Event Dispatch Thread)** — Hilo principal de Swing. Crea todos los objetos (`Board`, `SnakeRunner`, `GameClock`, `PauseControl`), procesa los eventos de teclado (flechas para serpiente 0, WASD para serpiente 1) y ejecuta el repintado de la ventana (`paintComponent()`) cada ~60ms mediante el `GameClock`.

2. **N VirtualThreads (SnakeRunner)** — Cada serpiente tiene su propio virtual thread creado con `Executors.newVirtualThreadPerTaskExecutor()`. Cada runner ejecuta un ciclo infinito: `checkPause()` → `maybeTurn()` (solo si `autoPilot=true`) → `board.step()` → `Thread.sleep()`. La serpiente 0 (`autoPilot=false`) solo responde a teclado; la 1 igual; las 2+ (`autoPilot=true`) giran aleatoriamente.

3. **1 VirtualThread (PauseAndWait)** — Se crea temporalmente al pausar el juego. Ejecuta `pauseControl.pauseAndWaitForAll()`, que se bloquea en `wait()` hasta que todos los `SnakeRunner` están detenidos. Al reanudar, este hilo termina.

4. **SchedulerThread (GameClock)** — Hilo único del `ScheduledExecutorService`. En estado RUNNING ejecuta `tick` cada 60ms (solo `SwingUtilities.invokeLater(gamePanel::repaint)`). En estado PAUSED el `ScheduledFuture` está cancelado, por lo que este hilo no despierta.

#### Flujo de pausa/reanudación

**Pausar:**
1. Usuario hace clic en *Action* o presiona *Espacio*.
2. `SnakeApp.togglePause()` cambia el texto del botón a *Resume*.
3. `clock.pause()` cancela el `ScheduledFuture` del tick de repintado.
4. Se lanza un **virtual thread** que llama a `PauseControl.pauseAndWaitForAll()`:
   a. Establece `paused = true`.
   b. Entra en `while (paused && waitingCount < snakes.size())`.
   c. Hace `wait()` — libera el monitor de `PauseControl`.
5. Cada `SnakeRunner`, al comenzar su siguiente iteración, llama a `checkPause()`:
   a. Ve `paused = true`, incrementa `waitingCount`.
   b. Llama `notifyAll()` — despierta el hilo de UI para que re-evalúe la condición.
   c. Entra en `while (paused) wait()` — el runner se bloquea de verdad.
6. Cuando `waitingCount == snakes.size()`, el hilo de UI sale del while y termina.
   - En este punto: **todos los runners están bloqueados → estado estable**.

**Reanudar:**
1. Usuario hace clic en *Resume* o presiona *Espacio*.
2. `PauseControl.resume()` establece `paused = false` y llama `notifyAll()`.
3. Todos los runners despiertan, ven `paused = false`, salen de `while (paused)` y continúan su loop.
4. `clock.resume()` reprograma el tick de repintado.
5. El botón vuelve a mostrar *Action*.

#### Mecanismo de sincronización

| Componente | Mecanismo | Propósito |
|---|---|---|
| `Snake` | `synchronized` (monitor intrínseco) | Proteger `body` (ArrayDeque) entre runner (escritura) y EDT (lectura). Un solo lock cubre `direction` + `body` para consistencia. |
| `Board` | `ReentrantReadWriteLock` | Múltiples lectores simultáneos (EDT + stats) vs escritor exclusivo (runner en step). |
| `PauseControl` | `synchronized` + `wait/notifyAll` | Coordinación de pausa cooperativa entre N runners y la UI. Sin busy-wait. |
| `GameClock` | `AtomicReference<GameState>` + `ScheduledFuture.cancel()` | Estado atómico del reloj. Cancelación física del scheduler durante pausa. |
| `SnakeApp.snakes` | `CopyOnWriteArrayList` | Iteración segura desde EDT sin locks. Escritura solo en construcción. |
| `SnakeRunner.turboTicks` | Variable de instancia (un solo hilo) | Solo el propio runner lee/escribe; no requiere sincronización. |

#### Regiones críticas

| Región crítica | Lock | Hilos involucrados |
|---|---|---|
| `Snake.advance()` → modifica `body` | `Snake.this` | 1 runner |
| `Snake.snapshot()` → lee `body` | `Snake.this` | EDT |
| `Snake.turn()` → lee/escribe `direction` | `Snake.this` | EDT o 1 runner |
| `Board.step()` → modifica colecciones | `Board.rwLock.writeLock` | 1 runner |
| `Board.mice/obstacles/turbo/teleports()` → lee colecciones | `Board.rwLock.readLock` | EDT (repaint) |
| `PauseControl.checkPause()` → modifica `waitingCount` | `PauseControl.this` | N runners |
| `PauseControl.pauseAndWaitForAll()` → lee `waitingCount` | `PauseControl.this` | 1 virtual thread (UI) |

#### Orden de locks (importante para evitar deadlocks)

```
SnakeRunner → Board.writeLock → Snake.this  (runner avanzando)
EDT         → Snake.this                    (turn, snapshot)
EDT         → Board.readLock                (paintComponent)
```

No hay dependencia circular: el EDT nunca adquiere `Board.writeLock`, y el runner nunca adquiere otro lock después de soltar `Board.writeLock`. Por lo tanto **no hay posibilidad de deadlock por orden de locks**.

---

### 4) Cambios con respecto a la versión de referencia (`main/`)

El proyecto partió de una implementación de referencia en `main/java/` que tenía la lógica de juego correcta pero sin ninguna corrección de concurrencia. Sobre esa base se aplicaron los siguientes cambios:

| Archivo | main (referencia) | src (versión final) |
|---|---|---|
| `Snake.java` | Sin sincronización, `volatile direction` | Todos los métodos `synchronized`, sin `volatile` |
| `Board.java` | `synchronized` en métodos | `ReentrantReadWriteLock` (lectura/escritura) |
| `GameClock.java` | Scheduler sondea estado cada 60ms (busy-wait) | `ScheduledFuture.cancel()` en pausa, reprograma en resume |
| `SnakeRunner.java` | Sin PauseControl, `maybeTurn()` en todas | `autoPilot` para separar IA/jugador, `checkPause()` cooperativa |
| `PauseControl.java` | No existía | Nueva clase: monitor con `wait/notifyAll`, corrección de deadlocks |
| `SnakeApp.java` | `ArrayList`, `EXIT_ON_CLOSE`, togglePause simple | `CopyOnWriteArrayList`, shutdown coordinado, PauseControl integrado |
| `pom.xml` | Sin configuración Maven | `exec-maven-plugin` con `systemProperties` para `-Dsnakes` |

#### Funcionalidad preservada de `main` (reglas del juego)

- **Rebote en obstáculos**: al chocar, la serpiente gira aleatoriamente (`randomTurn()`) y sigue viva.
- **Sin muerte**: no hay concepto de serpiente muerta; no se usa `markDead()`/`isDead()`/`deathTime()`.
- **Sin estadísticas**: no hay diálogo de estadísticas al pausar.
- **Autonomía de IA**: las serpientes 2+ giran aleatoriamente con probabilidad configurable.
- **Turbo, teletransportadores, ratones**: idéntico comportamiento.

#### Resumen de líneas por archivo

| Archivo | Líneas | Cambios principales |
|---|---|---|
| `pom.xml` | 58 | Propiedad `<snakes>`, `exec-maven-plugin` con systemProperties |
| `Main.java` | 10 | Sin cambios |
| `Snake.java` | 49 | `synchronized` en todos los métodos públicos |
| `Board.java` | 155 | `ReadWriteLock`, copias defensivas |
| `Direction.java` | 4 | Sin cambios |
| `Position.java` | 9 | Sin cambios |
| `GameState.java` | 2 | Sin cambios |
| `GameClock.java` | 59 | `ScheduledFuture` para cancel/reprogram |
| `PauseControl.java` | 70 | Nueva clase: wait/notify, deadlocks corregidos |
| `SnakeRunner.java` | 57 | `autoPilot`, `checkPause()`, rebote en obstáculo |
| `SnakeApp.java` | 263 | `CopyOnWriteArrayList`, shutdown, PauseControl, autoPilot por índice |

---

### 5) Cómo verificar la correcta concurrencia

1. **Ejecutar con N alto**:
   ```bash
   mvn -q -DskipTests exec:java -Dsnakes=20
   ```
   El juego debe correr sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin deadlocks.

2. **Pausar y reanudar repetidamente**:
   Presionar *Espacio* o *Action/Resume* rápidamente varias veces. No debe congelarse la UI ni perderse el control de las serpientes.

3. **Cerrar la ventana**:
   Al cerrar, todos los virtual threads deben terminar limpiamente (sin excepciones en consola).

4. **Control de jugador**:
   Las serpientes 0 (flechas) y 1 (WASD) deben responder exclusivamente al teclado, sin giros aleatorios no solicitados.
