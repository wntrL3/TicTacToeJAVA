import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * TicTacToe – Ein komplettes, einzeldatei-basiertes Swing-Programm.
 *
 * Features:
 * - 3x3 Spielfeld mit Buttons
 * - Spieler X/O, abwechselnde Züge
 * - Gewinn-/Unentschieden-Erkennung
 * - Punktestände (X, O, Unentschieden)
 * - "Neu" (neue Runde) & "Punkte zurücksetzen" & "Beenden"
 * - Tastenkürzel: N (neu), R (reset Punkte), ESC (beenden)
 * - Sauber auf dem EDT gestartet
 */
public class TicTacToe extends JFrame {
    private final JButton[][] cells = new JButton[3][3];
    private final JLabel status = new JLabel("Bereit.");
    private final JLabel score = new JLabel();

    private char current = 'X';
    private int scoreX = 0;
    private int scoreO = 0;
    private int scoreDraw = 0;
    private boolean gameOver = false;

    public TicTacToe() {
        super("TicTacToe – Java Swing");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setMinimumSize(new Dimension(380, 460));

        // Menüleiste
        setJMenuBar(createMenuBar());

        // Info-Bereich (oben)
        JPanel info = new JPanel(new GridLayout(2, 1, 4, 4));
        status.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        score.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        info.add(status);
        info.add(score);
        add(info, BorderLayout.NORTH);

        // Spielfeld (Mitte)
        JPanel board = new JPanel(new GridLayout(3, 3, 6, 6));
        board.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 48);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton b = new JButton("");
                b.setFont(f);
                b.setFocusPainted(false);
                final int rr = r, cc = c;
                b.addActionListener(e -> handleMove(rr, cc));
                b.setBackground(Color.WHITE);
                cells[r][c] = b;
                board.add(b);
            }
        }
        add(board, BorderLayout.CENTER);

        // Toolbar (unten)
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

        // Key Bindings
        bindKeyStroke("N", this::newRound);
        bindKeyStroke("R", this::resetScores);
        bindKeyStroke("ESCAPE", () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        // Startwerte
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
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                cells[r][c].setText("");
                cells[r][c].setEnabled(true);
                cells[r][c].setBackground(Color.WHITE);
            }
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
            status.setText("Am Zug: " + current);
        }
    }

    private void handleMove(int r, int c) {
        if (gameOver) return;
        JButton b = cells[r][c];
        if (!b.getText().isEmpty()) return; // Feld bereits belegt
        b.setText(String.valueOf(current));
        b.setForeground(current == 'X' ? new Color(30, 30, 30) : new Color(30, 30, 30));

        char winner = checkWinner();
        if (winner != '\0') {
            gameOver = true;
            highlightWin();
            if (winner == 'X') scoreX++; else if (winner == 'O') scoreO++;
            status.setText("Gewonnen: " + winner + " – N für neue Runde");
            updateScoreLabel();
            disableAll();
            return;
        }

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
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                cells[r][c].setEnabled(false);
    }

    private boolean isBoardFull() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (cells[r][c].getText().isEmpty()) return false;
        return true;
    }

    /**
     * Liefert 'X' oder 'O' wenn gewonnen, sonst '\0'.
     */
    private char checkWinner() {
        String[][] g = new String[3][3];
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                g[r][c] = cells[r][c].getText();

        int[][][] lines = {
                // Reihen
                {{0,0},{0,1},{0,2}},
                {{1,0},{1,1},{1,2}},
                {{2,0},{2,1},{2,2}},
                // Spalten
                {{0,0},{1,0},{2,0}},
                {{0,1},{1,1},{2,1}},
                {{0,2},{1,2},{2,2}},
                // Diagonalen
                {{0,0},{1,1},{2,2}},
                {{0,2},{1,1},{2,0}}
        };

        for (int[][] line : lines) {
            String a = g[line[0][0]][line[0][1]];
            String b = g[line[1][0]][line[1][1]];
            String c = g[line[2][0]][line[2][1]];
            if (!a.isEmpty() && a.equals(b) && b.equals(c)) {
                return a.charAt(0);
            }
        }
        return '\0';
    }

    /**
     * Markiert die Gewinnlinie optisch (grünliche Hintergründe).
     */
    private void highlightWin() {
        String[][] g = new String[3][3];
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                g[r][c] = cells[r][c].getText();

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
            String a = g[line[0][0]][line[0][1]];
            String b = g[line[1][0]][line[1][1]];
            String c = g[line[2][0]][line[2][1]];
            if (!a.isEmpty() && a.equals(b) && b.equals(c)) {
                for (int[] pos : line) {
                    cells[pos[0]][pos[1]].setBackground(new Color(180, 230, 180));
                }
                return;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TicTacToe::new);
    }
}
