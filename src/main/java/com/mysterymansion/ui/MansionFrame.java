package com.mysterymansion.ui;

import static com.mysterymansion.ui.Theme.*;

import com.mysterymansion.dao.DatabaseManager;
import com.mysterymansion.engine.ActionResult;
import com.mysterymansion.engine.Combat;
import com.mysterymansion.engine.GameEngine;
import com.mysterymansion.model.Difficulty;
import com.mysterymansion.model.Outcome;
import com.mysterymansion.model.Room;
import java.awt.*;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MansionFrame extends JFrame {

    // ------------------------------------------------------------------
    // State & components
    // ------------------------------------------------------------------
    private GameEngine engine;
    private boolean gameEnded;

    private JPanel cardPanel, gamePanel, endPanel;
    private CardLayout cardLayout;

    private JTextField nameField;
    private JLabel lastResultLabel;
    private JComboBox<String> diffBox;

    private JTextPane logPane;
    private JLabel scoreLabel, roomLabel, inventoryLabel, roomTitleBanner;
    private StatBar healthBar, movesBar;
    private GradientPanel roomVisualPanel;
    private final Map<Room, RoundedButton> roomButtons = new LinkedHashMap<>();

    public MansionFrame() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Mystery Mansion");

        setTitle("Mystery Mansion – Escape & Survival");
        setSize(1200, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1100, 700));
        getContentPane().setBackground(BG);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(BG);

        cardPanel.add(createWelcomePanel(), "WELCOME");
        add(cardPanel);
        setVisible(true);
    }

    // ==================================================================
    // WELCOME SCREEN
    // ==================================================================
    private JPanel createWelcomePanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);

        JLabel title = centeredLabel("MYSTERY MANSION", 48, RED, Font.BOLD);
        JLabel subtitle = centeredLabel("Escape & Survival  •  Hardcore Edition", 16, MUTED, Font.ITALIC);
        JLabel tagline = centeredLabel("Find the key. Beat the clock. Don't die.", 13, new Color(110, 110, 115), Font.PLAIN);

        // Form card
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(25, 30, 25, 30)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(18);
        styleTextField(nameField);
        diffBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        diffBox.setSelectedItem("Medium");
        styleComboBox(diffBox);

        JLabel diffInfo = new JLabel(Difficulty.MEDIUM.summary(), SwingConstants.CENTER);
        diffInfo.setForeground(MUTED);
        diffInfo.setFont(new Font(FONT, Font.ITALIC, 12));
        diffBox.addActionListener(e ->
                diffInfo.setText(Difficulty.fromLabel((String) diffBox.getSelectedItem()).summary()));

        gbc.gridx = 0; gbc.gridy = 0;
        card.add(whiteLabel("Player Name"), gbc);
        gbc.gridx = 1;
        card.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        card.add(whiteLabel("Difficulty"), gbc);
        gbc.gridx = 1;
        card.add(diffBox, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        card.add(diffInfo, gbc);

        // Buttons
        RoundedButton startBtn = new RoundedButton("▶  New Game", ACCENT);
        RoundedButton resumeBtn = new RoundedButton("💾  Resume Game", new Color(30, 110, 70));
        RoundedButton howBtn = new RoundedButton("❓  How to Play", BTN);
        RoundedButton lbBtn = new RoundedButton("🏆  Leaderboard", BTN);

        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        btnPanel.setOpaque(false);
        btnPanel.add(startBtn);
        btnPanel.add(resumeBtn);
        btnPanel.add(howBtn);
        btnPanel.add(lbBtn);
        btnPanel.setMaximumSize(new Dimension(520, 98));
        btnPanel.setPreferredSize(new Dimension(520, 98));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(520, 220));

        box.add(title);
        box.add(Box.createVerticalStrut(4));
        box.add(subtitle);
        box.add(Box.createVerticalStrut(4));
        box.add(tagline);
        box.add(Box.createVerticalStrut(30));
        box.add(card);
        box.add(Box.createVerticalStrut(20));
        box.add(btnPanel);
        box.add(Box.createVerticalStrut(18));
        lastResultLabel = centeredLabel(" ", 13, GOLD, Font.BOLD);
        box.add(lastResultLabel);

        root.add(box);

        startBtn.addActionListener(e -> startGame());
        resumeBtn.addActionListener(e -> resumeGame());
        nameField.addActionListener(e -> startGame());
        howBtn.addActionListener(e -> showInstructionsDialog());
        lbBtn.addActionListener(e -> showLeaderboardDialog());

        return root;
    }

    private String readPlayerName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your player name!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (name.length() > 30) name = name.substring(0, 30);
        return name;
    }

    /** Flowchart: Enter Player Details -> New Game -> Select Difficulty -> Explore Mansion. */
    private void startGame() {
        String name = readPlayerName();
        if (name == null) return;

        if (DatabaseManager.hasSavedGame(name)) {
            int c = JOptionPane.showConfirmDialog(this,
                    "A saved game already exists for \"" + name + "\".\nStarting a new game will overwrite it. Continue?",
                    "Overwrite Saved Game?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
        }

        engine = new GameEngine(name, Difficulty.fromLabel((String) diffBox.getSelectedItem()));
        launchGame(false);
    }

    /** Flowchart: Enter Player Details -> Resume -> Load Saved Progress -> Explore Mansion. */
    private void resumeGame() {
        String name = readPlayerName();
        if (name == null) return;

        try {
            GameEngine saved = DatabaseManager.loadGame(name);
            if (saved == null) {
                JOptionPane.showMessageDialog(this, "No saved game found for \"" + name + "\".\nStart a New Game instead.",
                        "Nothing to Resume", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            engine = saved;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load the saved game. Is MySQL running?",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        launchGame(true);
    }

    private void launchGame(boolean resumed) {
        gameEnded = false;

        if (gamePanel != null) cardPanel.remove(gamePanel);
        gamePanel = createGamePanel();
        cardPanel.add(gamePanel, "GAME");
        cardLayout.show(cardPanel, "GAME");

        showRoomView(engine.getCurrentRoom());
        updateStatusLabels();

        if (resumed) {
            logMessage("💾 Welcome back, " + engine.getPlayerName() + ". Your progress has been restored.");
            logMessage("📍 You are in the " + engine.getCurrentRoom() + " with " + engine.getHealth()
                    + " HP and " + engine.getMovesRemaining() + " moves left.");
        } else {
            logMessage("Welcome, " + engine.getPlayerName() + ". The doors have locked behind you.");
            logMessage("⚠️ MANSION ALARM ACTIVATED: You have limited moves before the collapse!");
            logMessage("Explore carefully. Items and codes are randomized every run.");
            logMessage("💾 Progress is saved automatically after every move.");
        }
    }

    // ==================================================================
    // GAME SCREEN
    // ==================================================================
    private JPanel createGamePanel() {
        roomButtons.clear();

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);

        // ---------- Top status dashboard ----------
        healthBar = new StatBar("HEALTH", GREEN);
        movesBar = new StatBar("MOVES LEFT", ORANGE);
        scoreLabel = new JLabel();
        roomLabel = new JLabel();
        scoreLabel.setFont(new Font(FONT, Font.BOLD, 14));
        roomLabel.setFont(new Font(FONT, Font.BOLD, 14));

        JPanel statusPanel = new JPanel(new GridLayout(1, 4, 25, 0));
        statusPanel.setBackground(PANEL);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(12, 18, 12, 18)));
        statusPanel.add(healthBar);
        statusPanel.add(movesBar);
        statusPanel.add(scoreLabel);
        statusPanel.add(roomLabel);
        panel.add(statusPanel, BorderLayout.NORTH);

        // ---------- Left: instruction panel + inventory ----------
        panel.add(createLeftPanel(), BorderLayout.WEST);

        // ---------- Center: room banner + story log ----------
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(12, 12, 12, 12));

        roomVisualPanel = new GradientPanel(new Color(45, 25, 25));
        roomVisualPanel.setPreferredSize(new Dimension(600, 90));
        roomVisualPanel.setBorder(BorderFactory.createLineBorder(LINE));
        roomTitleBanner = new JLabel("", SwingConstants.CENTER);
        roomTitleBanner.setForeground(Color.WHITE);
        roomVisualPanel.add(roomTitleBanner, BorderLayout.CENTER);
        setBanner("🚪 MAIN HALL", "The reinforced oak door is secured with a digital keypad and lock.");

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(14, 14, 17));
        logPane.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(LINE));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        center.add(roomVisualPanel, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        // ---------- Right: action menu ----------
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(PANEL);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, LINE), new EmptyBorder(15, 15, 15, 15)));
        controlPanel.setPreferredSize(new Dimension(235, 0));

        RoundedButton btnLiving = createRoomButton(Room.LIVING_ROOM, "🛋️  Living Room");
        RoundedButton btnLibrary = createRoomButton(Room.LIBRARY, "📚  Library");
        RoundedButton btnLab = createRoomButton(Room.LABORATORY, "🔬  Laboratory");
        RoundedButton btnBasement = createRoomButton(Room.BASEMENT, "🔐  Basement");
        RoundedButton btnHall = createRoomButton(Room.MAIN_HALL, "🚪  Main Hall");

        RoundedButton btnSearch = createActionButton("🔍  Search Area", BTN);
        RoundedButton btnPuzzle = createActionButton("🧩  Solve Safe Code", BTN);
        RoundedButton btnPotion = createActionButton("🧪  Use Health Potion", BTN);
        RoundedButton btnEscape = createActionButton("🏃  Attempt Escape", ACCENT);
        RoundedButton btnQuit = createActionButton("💾  Save & Quit", new Color(90, 40, 40));

        btnSearch.setToolTipText("Search this room for items (costs 1 move)");
        btnPuzzle.setToolTipText("Only works inside the Laboratory");
        btnPotion.setToolTipText("Restores 35 HP");
        btnEscape.setToolTipText("Needs the Mansion Key (and the Code Paper for the best ending)");

        controlPanel.add(createSectionHeader("MANSION MAP  (1 move each)"));
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnLiving);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnLibrary);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnLab);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnBasement);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnHall);
        controlPanel.add(Box.createVerticalStrut(18));
        controlPanel.add(createSectionHeader("INTERACTIONS"));
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnSearch);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnPuzzle);
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(btnPotion);
        controlPanel.add(Box.createVerticalStrut(14));
        controlPanel.add(btnEscape);
        controlPanel.add(Box.createVerticalGlue());
        controlPanel.add(btnQuit);

        panel.add(controlPanel, BorderLayout.EAST);

        // ---------- Action handlers ----------
        btnLiving.addActionListener(e -> changeRoom(Room.LIVING_ROOM));
        btnLibrary.addActionListener(e -> changeRoom(Room.LIBRARY));
        btnLab.addActionListener(e -> changeRoom(Room.LABORATORY));
        btnBasement.addActionListener(e -> changeRoom(Room.BASEMENT));
        btnHall.addActionListener(e -> changeRoom(Room.MAIN_HALL));

        btnSearch.addActionListener(e -> handleRoomSearch());
        btnPuzzle.addActionListener(e -> handleSafePuzzle());
        btnPotion.addActionListener(e -> {
            if (engine.useHealthPotion()) {
                logMessage("✨ Used Health Potion. Restored 35 HP!");
                updateStatusLabels();
                saveProgress();
            } else {
                logMessage("⚠️ You don't have a Health Potion in your inventory!");
            }
        });
        btnEscape.addActionListener(e -> handleEscapeAttempt());
        btnQuit.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Save and return to the menu?\nYou can resume this run later using the same player name.",
                    "Save & Quit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c == JOptionPane.YES_OPTION) {
                saveProgress();
                gameEnded = true;
                cardLayout.show(cardPanel, "WELCOME");
            }
        });

        return panel;
    }

    /** Left sidebar: scrollable "How to Play" panel on top, inventory card at the bottom. */
    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(PANEL);
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, LINE), new EmptyBorder(15, 15, 15, 15)));
        left.setPreferredSize(new Dimension(280, 0));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(createSectionHeader("HOW TO PLAY"), BorderLayout.NORTH);
        top.add(createInstructionPane(), BorderLayout.CENTER);

        inventoryLabel = new JLabel();
        inventoryLabel.setVerticalAlignment(SwingConstants.TOP);
        JPanel invCard = new JPanel(new BorderLayout(0, 6));
        invCard.setBackground(CARD);
        invCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(10, 12, 10, 12)));
        invCard.setPreferredSize(new Dimension(0, 105));
        JLabel invHeader = createSectionHeader("🎒 INVENTORY");
        invCard.add(invHeader, BorderLayout.NORTH);
        invCard.add(inventoryLabel, BorderLayout.CENTER);

        left.add(top, BorderLayout.CENTER);
        left.add(invCard, BorderLayout.SOUTH);
        return left;
    }

    private JScrollPane createInstructionPane() {
        JEditorPane ep = new JEditorPane("text/html", buildInstructionsHtml());
        ep.setEditable(false);
        ep.setBackground(CARD);
        ep.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ep);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getVerticalScrollBar().setUnitIncrement(12);
        return sp;
    }

    private String buildInstructionsHtml() {
        return "<html><body style='font-family:SansSerif; font-size:11px; color:#E5E5EA; margin:8px;'>"
                + "<p><b><font color='#FF453A'>🎯 OBJECTIVE</font></b><br>"
                + "Escape the mansion before you run out of HP or moves.</p>"

                + "<p><b><font color='#0A84FF'>🕹️ HOW TO PLAY</font></b><br>"
                + "1. Use the <b>Mansion Map</b> to walk between rooms (1 move each).<br>"
                + "2. Press <b>Search Area</b> to look for items (1 move). Each room can be searched once.<br>"
                + "3. Find the <b>Code Paper</b> – it reveals the 4-digit safe code.<br>"
                + "4. Go to the <b>Laboratory</b> and press <b>Solve Safe Code</b> to get the <b>Mansion Key</b>.<br>"
                + "5. Press <b>Attempt Escape</b> to leave!<br>"
                + "6. Progress auto-saves after every move. Enter your name and press <b>Resume Game</b> to continue later.</p>"

                + "<p><b><font color='#FFD60A'>🏁 ENDINGS</font></b><br>"
                + "⭐⭐⭐ <b>Good:</b> Key + Code Paper (+200 pts)<br>"
                + "⭐⭐ <b>Normal:</b> Key only (+80 pts)<br>"
                + "💀 <b>Game Over:</b> 0 HP or 0 moves left</p>"

                + "<p><b><font color='#FF9F0A'>⚠️ DANGERS</font></b><br>"
                + "• Entering the <b>Basement</b> without a Torch: -25 HP<br>"
                + "• <b>Laboratory</b> and <b>Basement</b> can trigger a Mutant fight (Attack / Defend / Flee)<br>"
                + "• Wrong safe code: -10 HP</p>"

                + "<p><b><font color='#30D158'>🎒 ITEMS &amp; SCORE</font></b><br>"
                + "• Torch, Health Potion (+35 HP) and Code Paper are hidden in the Living Room, Library and Basement – different every run!<br>"
                + "• +25 pts per item, +50 pts for beating the Mutant, -15 pts for fleeing.<br>"
                + "• On escape: + remaining HP + 2 per move left, then x difficulty (Easy 1, Medium 1.25, Hard 1.5).</p>"

                + "<p><b><font color='#98989D'>💡 TIP</font></b><br>"
                + "Don't waste moves. Every room change and every search counts.</p>"
                + "</body></html>";
    }

    private void showInstructionsDialog() {
        JScrollPane sp = createInstructionPane();
        sp.setPreferredSize(new Dimension(480, 440));
        JOptionPane.showMessageDialog(this, sp, "How to Play", JOptionPane.PLAIN_MESSAGE);
    }

    // ==================================================================
    // GAME LOGIC (UI side)
    // ==================================================================
    /** Applies a room's banner and colour (also used when a saved game is resumed). */
    private void showRoomView(Room room) {
        roomVisualPanel.setColor(new Color(room.rgb()));
        setBanner(room.title(), room.description());
    }

    private void logAll(ActionResult result) {
        for (String m : result.messages()) logMessage(m);
    }

    private void changeRoom(Room room) {
        if (gameEnded) return;
        ActionResult result = engine.moveTo(room);
        showRoomView(room);
        logAll(result);
        if (result.combatTriggered()) runCombat();
        updateStatusLabels();
        checkGameOver();
    }

    /** Shows the fight dialogs; every rule (damage, score, victory) is decided by {@link Combat}. */
    private void runCombat() {
        Combat combat = engine.startCombat();
        logMessage(combat.introMessage());

        while (!combat.isOver()) {
            String[] choices = {"⚔️ Attack (Sword)", "🛡️ Defend & Counter", "🏃 Flee Room"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "MUTANT HP: " + combat.monsterHealth() + "  |  YOUR HP: " + engine.getHealth()
                            + "\nChoose your tactical move:",
                    "Combat Encounter!",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, choices, choices[0]);

            List<String> messages;
            if (choice == 0) messages = combat.attack();
            else if (choice == 1) messages = combat.defend();
            else messages = combat.flee(); // flee, or the dialog was closed
            for (String m : messages) logMessage(m);
        }
        updateStatusLabels();
    }

    private void handleRoomSearch() {
        if (gameEnded) return;
        logAll(engine.search());
        updateStatusLabels();
        checkGameOver();
    }

    private void handleSafePuzzle() {
        if (gameEnded) return;
        if (engine.getCurrentRoom() != Room.LABORATORY) {
            logAll(engine.solveSafe(null)); // explains there is no safe here
            return;
        }

        String inputCode = JOptionPane.showInputDialog(this,
                "🔬 Keypad Lock Screen:\nEnter 4-Digit Passcode to open the safe:", "Lab Safe", JOptionPane.PLAIN_MESSAGE);
        if (inputCode != null) logAll(engine.solveSafe(inputCode));

        updateStatusLabels();
        checkGameOver();
    }

    private void handleEscapeAttempt() {
        if (gameEnded) return;
        ActionResult result = engine.attemptEscape();
        logAll(result);
        updateStatusLabels();

        Outcome ending = result.outcome();
        if (ending != null) {
            gameEnded = true; // lock immediately so points can only be awarded once
            showEndScreen(true, ending.label(), ending.headline(), ending.message(), ending.tip());
        }
    }

    /** Flowchart: Update and Save Progress (health > 0 and escape conditions not met). */
    private void saveProgress() {
        DatabaseManager.saveGame(engine);
    }

    private void checkGameOver() {
        if (gameEnded) return;
        if (!engine.isGameOver()) {
            saveProgress();
            return;
        }
        Outcome defeat = engine.defeatOutcome();
        logMessage(defeat.logLine());
        showEndScreen(false, defeat.label(), defeat.headline(), defeat.message(), defeat.tip());
    }

    // ==================================================================
    // VICTORY / DEFEAT SCREEN
    // ==================================================================
    private void showEndScreen(boolean won, String ending, String headline, String message, String tip) {
        gameEnded = true;
        // Win: Save Score to Database -> Update Leaderboard.  Game over: Save Relevant Game Data.
        DatabaseManager.saveScore(engine.getPlayerName(), engine.getScore(), ending);
        DatabaseManager.deleteSavedGame(engine.getPlayerName()); // a finished run can't be resumed

        Color accent = won ? GREEN : RED;
        String rating = ending.equals("Good Ending") ? "⭐⭐⭐"
                : ending.equals("Normal Ending") ? "⭐⭐" : "💀";

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(won ? new Color(10, 26, 16) : new Color(30, 10, 12));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2), new EmptyBorder(30, 50, 30, 50)));

        JLabel icon = centeredLabel(won ? "🏆" : "☠️", 64, Color.WHITE, Font.PLAIN);
        JLabel head = centeredLabel(headline, 38, accent, Font.BOLD);
        JLabel tag = centeredLabel(ending.toUpperCase() + "  " + rating, 14, GOLD, Font.BOLD);
        JLabel msg = centeredLabel("<html><div style='text-align:center; width:460px'>" + message + "</div></html>",
                14, new Color(229, 229, 234), Font.PLAIN);

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        stats.setMaximumSize(new Dimension(560, 80));
        stats.add(statBox("FINAL SCORE", String.valueOf(engine.getScore()), GOLD));
        stats.add(statBox("HEALTH LEFT", String.valueOf(engine.getHealth()), RED));
        stats.add(statBox("MOVES LEFT", String.valueOf(Math.max(0, engine.getMovesRemaining())), ORANGE));
        stats.add(statBox("ITEMS FOUND", String.valueOf(engine.getInventory().size()), GREEN));

        JLabel playerInfo = centeredLabel(engine.getPlayerName() + "  •  " + engine.getDifficulty() + " difficulty",
                12, MUTED, Font.ITALIC);
        JLabel tipLabel = centeredLabel("<html><div style='text-align:center; width:460px'>💡 " + tip + "</div></html>",
                12, MUTED, Font.PLAIN);

        RoundedButton again = new RoundedButton("🏠  Play Again", ACCENT);
        RoundedButton exit = new RoundedButton("✖  Quit Game", new Color(150, 40, 40));
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(420, 46));
        row.add(again);
        row.add(exit);

        // Play Again -> back to the home page (result is already saved to the leaderboard)
        again.addActionListener(e -> cardLayout.show(cardPanel, "WELCOME"));
        exit.addActionListener(e -> System.exit(0));

        // Remember this result on the home page too, so it is never "lost"
        lastResultLabel.setText("Last game: " + engine.getPlayerName() + "  •  "
                + engine.getScore() + " pts  •  " + ending + "  (saved to leaderboard)");

        card.add(icon);
        card.add(Box.createVerticalStrut(6));
        card.add(head);
        card.add(Box.createVerticalStrut(6));
        card.add(tag);
        card.add(Box.createVerticalStrut(16));
        card.add(msg);
        card.add(Box.createVerticalStrut(22));
        card.add(stats);
        card.add(Box.createVerticalStrut(14));
        card.add(playerInfo);
        if (!won) {
            card.add(Box.createVerticalStrut(6));
            card.add(centeredLabel("The safe passcode was " + engine.getSafePasscode(), 12, ORANGE, Font.BOLD));
        }
        card.add(Box.createVerticalStrut(10));
        card.add(tipLabel);
        card.add(Box.createVerticalStrut(24));
        card.add(row);

        panel.add(card);

        if (endPanel != null) cardPanel.remove(endPanel);
        endPanel = panel;
        cardPanel.add(endPanel, "END");
        cardLayout.show(cardPanel, "END");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    private JPanel statBox(String title, String value, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setBackground(PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(10, 14, 10, 14)));
        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font(FONT, Font.BOLD, 24));
        v.setForeground(color);
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font(FONT, Font.PLAIN, 10));
        t.setForeground(MUTED);
        p.add(v);
        p.add(t);
        return p;
    }

    // ==================================================================
    // STATUS / LOG
    // ==================================================================
    private void updateStatusLabels() {
        if (engine == null || healthBar == null) return;

        double ratio = engine.getHealth() / (double) engine.getMaxHealth();
        healthBar.setColor(ratio > 0.5 ? GREEN : ratio > 0.25 ? ORANGE : RED);
        healthBar.update(engine.getHealth(), engine.getMaxHealth());
        movesBar.update(engine.getMovesRemaining(), engine.getMaxMoves());

        scoreLabel.setText("<html><font color='#FFD60A'><b>⭐ Score:</b> " + engine.getScore() + "</font></html>");
        roomLabel.setText("<html><font color='#40C4FF'><b>📍 Room:</b> " + engine.getCurrentRoom() + "</font></html>");

        String itemsText = engine.getInventory().isEmpty()
                ? "<i>Empty</i>" : String.join(", ", engine.getInventoryNames());
        inventoryLabel.setText("<html><body style='width:200px'><font color='#69F0AE'>" + itemsText + "</font></body></html>");

        // Highlight the room the player is currently standing in
        for (Map.Entry<Room, RoundedButton> entry : roomButtons.entrySet()) {
            boolean here = entry.getKey() == engine.getCurrentRoom();
            entry.getValue().setBase(here ? new Color(30, 90, 160) : BTN);
        }
    }

    private void setBanner(String title, String desc) {
        roomTitleBanner.setText("<html><div style='text-align:center'>"
                + "<span style='font-size:22px'><b>" + title + "</b></span><br>"
                + "<span style='font-size:11px; color:#D0D0D5'>" + desc + "</span></div></html>");
    }

    private void logMessage(String msg) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, pickLogColor(msg));
        StyleConstants.setFontFamily(attrs, "Monospaced");
        StyleConstants.setFontSize(attrs, 13);
        try {
            StyledDocument doc = logPane.getStyledDocument();
            doc.insertString(doc.getLength(), msg + "\n", attrs);
            logPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private Color pickLogColor(String msg) {
        if (msg.contains("❌") || msg.contains("☠") || msg.contains("DMG")) return new Color(255, 105, 97);
        if (msg.contains("🏆") || msg.contains("🔓") || msg.contains("✨")) return new Color(105, 240, 174);
        if (msg.contains("⚠") || msg.contains("👾")) return new Color(255, 179, 64);
        if (msg.contains("📍")) return new Color(100, 210, 255);
        if (msg.contains("🔍")) return new Color(255, 224, 130);
        return new Color(229, 229, 234);
    }

    // ==================================================================
    // LEADERBOARD
    // ==================================================================
    private void showLeaderboardDialog() {
        List<String[]> rows = DatabaseManager.getTopScores();

        String[] cols = {"#", "Player", "Score", "Result", "Played At"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        int rank = 1;
        for (String[] r : rows) {
            String when = (r[3] != null && r[3].length() > 19) ? r[3].substring(0, 19) : r[3];
            String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);
            model.addRow(new Object[]{medal, r[0], r[1], r[2], when});
            rank++;
        }

        JTable table = new JTable(model);
        table.setBackground(CARD);
        table.setForeground(Color.WHITE);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setRowSelectionAllowed(false);
        table.setFont(new Font(FONT, Font.PLAIN, 13));
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setBackground(CARD);
        center.setForeground(Color.WHITE);
        table.setDefaultRenderer(Object.class, center);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BTN);
        header.setForeground(Color.WHITE);
        header.setFont(new Font(FONT, Font.BOLD, 12));
        header.setReorderingAllowed(false);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getViewport().setBackground(CARD);
        sp.setPreferredSize(new Dimension(580, 200));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(PANEL);
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel heading = new JLabel("🏆  HALL OF FAME  –  TOP 5", SwingConstants.CENTER);
        heading.setForeground(GOLD);
        heading.setFont(new Font(FONT, Font.BOLD, 18));
        content.add(heading, BorderLayout.NORTH);
        content.add(sp, BorderLayout.CENTER);
        if (rows.isEmpty()) {
            JLabel empty = new JLabel("No records logged yet. Be the first to escape!", SwingConstants.CENTER);
            empty.setForeground(MUTED);
            content.add(empty, BorderLayout.SOUTH);
        }

        JOptionPane.showMessageDialog(this, content, "Top Scores", JOptionPane.PLAIN_MESSAGE);
    }

    // ==================================================================
    // STYLING HELPERS
    // ==================================================================
    private JLabel createSectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(MUTED);
        lbl.setFont(new Font(FONT, Font.BOLD, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel whiteLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font(FONT, Font.PLAIN, 13));
        return l;
    }

    private JLabel centeredLabel(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font(FONT, style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private RoundedButton createActionButton(String text, Color base) {
        RoundedButton btn = new RoundedButton(text, base);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setFont(new Font(FONT, Font.PLAIN, 13));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setPreferredSize(new Dimension(205, 38));
        btn.setMaximumSize(new Dimension(205, 38));
        return btn;
    }

    private RoundedButton createRoomButton(Room room, String label) {
        RoundedButton btn = createActionButton(label, BTN);
        roomButtons.put(room, btn);
        return btn;
    }

    private void styleTextField(JTextField tf) {
        tf.setBackground(new Color(44, 44, 46));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font(FONT, Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(72, 72, 74)), new EmptyBorder(5, 8, 5, 8)));
    }

    private void styleComboBox(JComboBox<String> cb) {
        cb.setBackground(new Color(44, 44, 46));
        cb.setForeground(Color.WHITE);
        cb.setFont(new Font(FONT, Font.PLAIN, 13));
    }
}
