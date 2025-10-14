import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * TicTacToeV2 – Swing-Implementierung mit Regel:
 * Jeder Spieler darf maximal 3 Steine gleichzeitig besitzen.
 * Wenn bereits 3 gesetzt sind, entfernt ein neuer Zug automatisch den
 * ältesten eigenen Stein (FIFO) und setzt den neuen auf das gewählte Feld.
 *
 * Weitere Eigenschaften:
 * - Gewinn-/Unentschieden-Erkennung (Unentschieden nur bei voller Belegung ohne 3er-Reihe –
 *   kommt hier praktisch kaum vor, da alte Steine rotieren). Optional erweiterbar um Zuglimit.
 * - Punktestände (X, O, Remis)
 * - Neue Runde / Punkte zurücksetzen / Beenden
 * - Tastenkürzel: N (neu), R (Punkte zurücksetzen), ESC (beenden)
 */
public class TicTacToeV2 extends JFrame {
    private final JButton[][] cells = new JButton[3][3];
    private final char[][] board = new char[3][3];

    private final JLabel status = new JLabel();
    private final JLabel score = new JLabel();

    private char current = 'X';
    private boolean gameOver = false;

    private int scoreX = 0;
    private int scoreO = 0;
    private int scoreDraw = 0;

    // Positionen in Setzreihenfolge (FIFO) je Spieler
    private final Deque<Point> posX = new ArrayDeque<>();
    private final Deque<Point> posO = new ArrayDeque<>();

    public TicTacToeV2() {
        super("TicTacToeV2 – 3-Steine-Regel");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setMinimumSize(new Dimension(380, 480));

        setJMenuBar(createMenuBar());

        JPanel info = new JPanel(new GridLayout(2, 1, 4, 4));
        status.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        score.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        info.add(status);
        info.add(score);
        add(info, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 6, 6));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 48);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton b = new JButton("");
                b.setFont(f);
                b.setFocusPainted(false);
                b.setBackground(Color.WHITE);
                final int rr = r, cc = c;
                b.addActionListener(e -> handleMove(rr, cc));
                cells[r][c] = b;
                boardPanel.add(b);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newGame = new JButton("Neu (N)");
        JButton resetPoints = new JButton("Punkte zurücksetzen (R)");
        JButton exit = new JButton("Beenden (ESC)");
        newGame.addActionListener(e -> newRound());
        resetPoints.addActionListener(e -> resetScores());
        exit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        actions.add(newGame);
        actions.add(resetPoints);
        actions.add(exit);
        add(actions, BorderLayout.SOUTH);

        bindKeyStroke("N", this::newRound);
        bindKeyStroke("R", this::resetScores);
        bindKeyStroke("ESCAPE", () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        updateScoreLabel();
        updateStatus();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu game = new JMenu("Spiel");

        JMenuItem miNew = new JMenuItem("Neu");
        miNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
        miNew.addActionListener(e -> newRound());

        JMenuItem miReset = new JMenuItem("Punkte zurücksetzen");
        miReset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
        miReset.addActionListener(e -> resetScores());

        JMenuItem miExit = new JMenuItem("Beenden");
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        miExit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        game.add(miNew);
        game.add(miReset);
        game.addSeparator();
        game.add(miExit);
        mb.add(game);
        return mb;
    }

    private void bindKeyStroke(String key, Runnable action) {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(key), key);
        am.put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void newRound() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c] = '\0';
                JButton b = cells[r][c];
                b.setText("");
                b.setEnabled(true);
                b.setBackground(Color.WHITE);
            }
        }
        posX.clear();
        posO.clear();
        current = 'X';
        gameOver = false;
        updateStatus();
    }

    private void resetScores() {
        scoreX = scoreO = scoreDraw = 0;
        updateScoreLabel();
        newRound();
    }

    private void updateScoreLabel() {
        score.setText(String.format("Punkte – X: %d   O: %d   Remis: %d", scoreX, scoreO, scoreDraw));
    }

    private void updateStatus() {
        if (!gameOver) {
            status.setText("Am Zug: " + current + "  (Regel: Max 3 Steine. Der älteste eigene Stein wird bei Bedarf entfernt.)");
        }
    }

    private void handleMove(int r, int c) {
        if (gameOver) return;
        if (board[r][c] != '\0') return; // Belegt

        // Wenn aktueller Spieler bereits 3 Steine hat: ältesten entfernen
        Deque<Point> deque = (current == 'X') ? posX : posO;
        if (deque.size() == 3) {
            Point old = deque.removeFirst();
            board[old.x][old.y] = '\0';
            JButton ob = cells[old.x][old.y];
            ob.setText("");
            ob.setBackground(Color.WHITE);
            ob.setEnabled(true);
        }

        // Neuen Stein setzen
        board[r][c] = current;
        cells[r][c].setText(String.valueOf(current));
        cells[r][c].setForeground(new Color(30,30,30));
        deque.addLast(new Point(r, c));

        // Sieg prüfen
        char winner = checkWinner();
        if (winner != '\0') {
            gameOver = true;
            highlightWin();
            if (winner == 'X') scoreX++; else scoreO++;
            status.setText("Gewonnen: " + winner + " – N für neue Runde");
            updateScoreLabel();
            disableAll();
            return;
        }

        // Klassisches Remis (hier selten): Board voll und kein Gewinner
        if (isBoardFull()) {
            gameOver = true;
            scoreDraw++;
            status.setText("Unentschieden – N für neue Runde");
            updateScoreLabel();
            disableAll();
            return;
        }

        // Spieler wechseln
        current = (current == 'X') ? 'O' : 'X';
        updateStatus();
    }

    private void disableAll() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                cells[r][c].setEnabled(false);
            }
        }
    }

    private boolean isBoardFull() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == '\0') return false;
            }
        }
        return true;
    }

    /**
     * Liefert 'X' oder 'O' bei Gewinn, sonst '\0'.
     */
    private char checkWinner() {
        int[][][] lines = {
                {{0,0},{0,1},{0,2}},
                {{1,0},{1,1},{1,2}},
                {{2,0},{2,1},{2,2}},
                {{0,0},{1,0},{2,0}},
                {{0,1},{1,1},{2,1}},
                {{0,2},{1,2},{2,2}},
                {{0,0},{1,1},{2,2}},
                {{0,2},{1,1},{2,0}}
        };
        for (int[][] line : lines) {
            char a = board[line[0][0]][line[0][1]];
            char b = board[line[1][0]][line[1][1]];
            char c = board[line[2][0]][line[2][1]];
            if (a != '\0' && a == b && b == c) return a;
        }
        return '\0';
    }

    private void highlightWin() {
        int[][][] lines = {
                {{0,0},{0,1},{0,2}},
                {{1,0},{1,1},{1,2}},
                {{2,0},{2,1},{2,2}},
                {{0,0},{1,0},{2,0}},
                {{0,1},{1,1},{2,1}},
                {{0,2},{1,2},{2,2}},
                {{0,0},{1,1},{2,2}},
                {{0,2},{1,1},{2,0}}
        };
        outer: for (int[][] line : lines) {
            char a = board[line[0][0]][line[0][1]];
            char b = board[line[1][0]][line[1][1]];
            char c = board[line[2][0]][line[2][1]];
            if (a != '\0' && a == b && b == c) {
                for (int[] p : line) {
                    cells[p[0]][p[1]].setBackground(new Color(180, 230, 180));
                }
                break outer;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TicTacToeV2::new);
    }
}
