package co.eci.snake.ui.legacy;

import co.eci.snake.concurrency.PauseControl;
import co.eci.snake.concurrency.SnakeRunner;
import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Position;
import co.eci.snake.core.Snake;
import co.eci.snake.core.engine.GameClock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ventana principal del juego SnakeRace.
 *
 * Actualización vs referencia (main/):
 * - snakes: CopyOnWriteArrayList para iteración segura desde EDT
 *   sin locks (R-2 en README, sección 4).
 * - autoPilot: serpientes 0 y 1 con autoPilot=false, 2+ con true
 *   (DR-1 en README, sección 1).
 * - PauseControl: monitor con wait/notifyAll para pausa cooperativa
 *   (DL-1, DL-2 corregidos en README, sección 2).
 * - Shutdown coordinado: exec.shutdownNow() + clock.close() al cerrar
 *   ventana (README, sección 3).
 * - GameClock con cancel/reschedule (BW-1, R-3 en README).
 *
 * Hilos involucrados:
 * 1. EDT — crea la ventana, procesa teclado (flechas/WASD), repinta UI.
 * 2. N VirtualThreads (SnakeRunner) — uno por serpiente, ciclo autónomo.
 * 3. 1 VirtualThread (pauseAndWaitForAll) — temporal, creado al pausar.
 * 4. SchedulerThread (GameClock) — dispara repaint cada 60ms.
 *
 * @see SnakeRunner
 * @see PauseControl
 * @see GameClock
 * @see Board
 * @see <a href="file:../../../../../../README.md">README.md — Parte II, secciones 1-4</a>
 */
public final class SnakeApp extends JFrame {

  private final Board board;
  private final GamePanel gamePanel;
  private final JButton actionButton;
  private final GameClock clock;
  private final List<Snake> snakes = new CopyOnWriteArrayList<>();
  private ExecutorService exec;
  private PauseControl pauseControl;

  public SnakeApp() {
    super("The Snake Race");
    this.board = new Board(35, 28);

    int N = Integer.getInteger("snakes", 2);
    for (int i = 0; i < N; i++) {
      int x = 2 + (i * 3) % board.width();
      int y = 2 + (i * 2) % board.height();
      var dir = Direction.values()[i % Direction.values().length];
      snakes.add(Snake.of(x, y, dir));
    }

    this.gamePanel = new GamePanel(board, () -> snakes);
    this.actionButton = new JButton("Action");

    setLayout(new BorderLayout());
    add(gamePanel, BorderLayout.CENTER);
    add(actionButton, BorderLayout.SOUTH);

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        if (exec != null)
          exec.shutdownNow();
        clock.close();
        dispose();
        System.exit(0);
      }
    });
    pack();
    setLocationRelativeTo(null);

    this.clock = new GameClock(60, () -> SwingUtilities.invokeLater(gamePanel::repaint));

    this.pauseControl = new PauseControl(snakes);
    this.exec = Executors.newVirtualThreadPerTaskExecutor();
    for (int i = 0; i < snakes.size(); i++) {
      boolean autoPilot = i >= 2;
      exec.submit(new SnakeRunner(snakes.get(i), board, pauseControl, autoPilot));
    }

    actionButton.addActionListener((ActionEvent e) -> togglePause());

    gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "pause");
    gamePanel.getActionMap().put("pause", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        togglePause();
      }
    });

    var player = snakes.get(0);
    InputMap im = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap am = gamePanel.getActionMap();
    im.put(KeyStroke.getKeyStroke("LEFT"), "left");
    im.put(KeyStroke.getKeyStroke("RIGHT"), "right");
    im.put(KeyStroke.getKeyStroke("UP"), "up");
    im.put(KeyStroke.getKeyStroke("DOWN"), "down");
    am.put("left", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.LEFT);
      }
    });
    am.put("right", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.RIGHT);
      }
    });
    am.put("up", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.UP);
      }
    });
    am.put("down", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        player.turn(Direction.DOWN);
      }
    });

    if (snakes.size() > 1) {
      var p2 = snakes.get(1);
      im.put(KeyStroke.getKeyStroke('A'), "p2-left");
      im.put(KeyStroke.getKeyStroke('D'), "p2-right");
      im.put(KeyStroke.getKeyStroke('W'), "p2-up");
      im.put(KeyStroke.getKeyStroke('S'), "p2-down");
      am.put("p2-left", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.LEFT);
        }
      });
      am.put("p2-right", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.RIGHT);
        }
      });
      am.put("p2-up", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.UP);
        }
      });
      am.put("p2-down", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          p2.turn(Direction.DOWN);
        }
      });
    }

    setVisible(true);
    clock.start();
  }

  /**
   * Alterna entre pausar y reanudar el juego.
   *
   * Al pausar:
   * 1. Cambia el texto del botón a "Resume".
   * 2. clock.pause() — cancela el ScheduledFuture del repintado.
   * 3. Crea un virtual thread que llama a pauseControl.pauseAndWaitForAll()
   *    para esperar a que todos los runners estén bloqueados.
   *
   * Al reanudar:
   * 1. pauseControl.resume() — despierta todos los runners.
   * 2. clock.resume() — reprograma el tick de repintado.
   * 3. Restaura el texto del botón a "Action".
   */
  private void togglePause() {
    if ("Action".equals(actionButton.getText())) {
      actionButton.setText("Resume");
      clock.pause();
      Thread.ofVirtual().start(() -> {
        try {
          pauseControl.pauseAndWaitForAll();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    } else {
      actionButton.setText("Action");
      pauseControl.resume();
      clock.resume();
    }
  }

  /**
   * Panel de dibujo del juego. Lee el estado del Board y las serpientes
   * para pintar la cuadrícula, obstáculos, ratones, turbos,
   * teletransportadores y serpientes en cada repaint.
   *
   * Concurrencia:
   * - Se ejecuta en el EDT, invocado por GameClock vía
   *   SwingUtilities.invokeLater.
   * - Llama a board.mice(), board.obstacles(), etc. que usan readLock
   *   (múltiples lectores simultáneos permitidos).
   * - Llama a snake.snapshot() que está sincronizado con el monitor de
   *   Snake, garantizando consistencia.
   */
  public static final class GamePanel extends JPanel {
    private final Board board;
    private final Supplier snakesSupplier;
    private final int cell = 20;

    @FunctionalInterface
    public interface Supplier {
      List<Snake> get();
    }

    public GamePanel(Board board, Supplier snakesSupplier) {
      this.board = board;
      this.snakesSupplier = snakesSupplier;
      setPreferredSize(new Dimension(board.width() * cell + 1, board.height() * cell + 40));
      setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      var g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      g2.setColor(new Color(220, 220, 220));
      for (int x = 0; x <= board.width(); x++)
        g2.drawLine(x * cell, 0, x * cell, board.height() * cell);
      for (int y = 0; y <= board.height(); y++)
        g2.drawLine(0, y * cell, board.width() * cell, y * cell);

      g2.setColor(new Color(255, 102, 0));
      for (var p : board.obstacles()) {
        int x = p.x() * cell, y = p.y() * cell;
        g2.fillRect(x + 2, y + 2, cell - 4, cell - 4);
        g2.setColor(Color.RED);
        g2.drawLine(x + 4, y + 4, x + cell - 6, y + 4);
        g2.drawLine(x + 4, y + 8, x + cell - 6, y + 8);
        g2.drawLine(x + 4, y + 12, x + cell - 6, y + 12);
        g2.setColor(new Color(255, 102, 0));
      }

      g2.setColor(Color.BLACK);
      for (var p : board.mice()) {
        int x = p.x() * cell, y = p.y() * cell;
        g2.fillOval(x + 4, y + 4, cell - 8, cell - 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(x + 8, y + 8, cell - 16, cell - 16);
        g2.setColor(Color.BLACK);
      }

      Map<Position, Position> tp = board.teleports();
      g2.setColor(Color.RED);
      for (var entry : tp.entrySet()) {
        Position from = entry.getKey();
        int x = from.x() * cell, y = from.y() * cell;
        int[] xs = { x + 4, x + cell - 4, x + cell - 10, x + cell - 10, x + 4 };
        int[] ys = { y + cell / 2, y + cell / 2, y + 4, y + cell - 4, y + cell / 2 };
        g2.fillPolygon(xs, ys, xs.length);
      }

      g2.setColor(Color.BLACK);
      for (var p : board.turbo()) {
        int x = p.x() * cell, y = p.y() * cell;
        int[] xs = { x + 8, x + 12, x + 10, x + 14, x + 6, x + 10 };
        int[] ys = { y + 2, y + 2, y + 8, y + 8, y + 16, y + 10 };
        g2.fillPolygon(xs, ys, xs.length);
      }

      var snakes = snakesSupplier.get();
      int idx = 0;
      for (Snake s : snakes) {
        var body = s.snapshot().toArray(new Position[0]);
        for (int i = 0; i < body.length; i++) {
          var p = body[i];
          Color base = (idx == 0) ? new Color(0, 170, 0) : new Color(0, 160, 180);
          int shade = Math.max(0, 40 - i * 4);
          g2.setColor(new Color(
              Math.min(255, base.getRed() + shade),
              Math.min(255, base.getGreen() + shade),
              Math.min(255, base.getBlue() + shade)));
          g2.fillRect(p.x() * cell + 2, p.y() * cell + 2, cell - 4, cell - 4);
        }
        idx++;
      }
      g2.dispose();
    }
  }

  public static void launch() {
    SwingUtilities.invokeLater(SnakeApp::new);
  }
}
