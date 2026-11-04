package com.mysterymansion.model;

/** Game difficulty: starting HP, move budget and final-score multiplier. */
public enum Difficulty {
    EASY("Easy", 120, 35, 1.0, "relaxed"),
    MEDIUM("Medium", 100, 25, 1.25, "balanced"),
    HARD("Hard", 70, 20, 1.5, "brutal");

    private final String label;
    private final int maxHealth;
    private final int maxMoves;
    private final double scoreMultiplier;
    private final String flavour;

    Difficulty(String label, int maxHealth, int maxMoves, double scoreMultiplier, String flavour) {
        this.label = label;
        this.maxHealth = maxHealth;
        this.maxMoves = maxMoves;
        this.scoreMultiplier = scoreMultiplier;
        this.flavour = flavour;
    }

    /** Name shown in the UI and stored in the database ("Easy", "Medium", "Hard"). */
    public String label() { return label; }
    public int maxHealth() { return maxHealth; }
    public int maxMoves() { return maxMoves; }
    public double scoreMultiplier() { return scoreMultiplier; }

    /** e.g. "120 HP  •  35 moves  (relaxed)" */
    public String summary() {
        return maxHealth + " HP  •  " + maxMoves + " moves  (" + flavour + ")";
    }

    /** Looks up by label; unknown values fall back to MEDIUM (same as the original switch default). */
    public static Difficulty fromLabel(String label) {
        for (Difficulty d : values()) {
            if (d.label.equalsIgnoreCase(label)) return d;
        }
        return MEDIUM;
    }

    @Override
    public String toString() { return label; }
}
