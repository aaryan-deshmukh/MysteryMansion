package com.mysterymansion.model;

/** How a run can end. Holds the end-screen text so the UI doesn't hard-code it. */
public enum Outcome {
    GOOD_ENDING("Good Ending", true, 200, "YOU ESCAPED!",
            "You unlocked the main gate with the Mansion Key and the passcode. A masterful escape!",
            "Flawless run. Try Hard difficulty for a real challenge.",
            "🏆 GOOD ENDING: You unlocked the main gate with the Key and Passcode! ESCAPED!"),
    NORMAL_ENDING("Normal Ending", true, 80, "YOU GOT OUT!",
            "You smashed through a window with the key handle and fled into the night, leaving the mansion's secrets behind.",
            "Find the Code Paper as well to unlock the Good Ending (+200 pts).",
            "😐 NORMAL ENDING: You broke through a window using the key handle, leaving without full clues."),
    DEATH("Game Over", false, 0, "YOU DIED",
            "The mansion claims another victim. Your body was never found...",
            "Grab the Torch before entering the Basement and keep a Health Potion for fights.",
            "❌ GAME OVER: You died from the damage you took."),
    TIMEOUT("Game Over", false, 0, "TIME'S UP",
            "The ceiling collapses as the alarm falls silent. You never made it out in time.",
            "Plan your route – every room change and every search costs a move.",
            "❌ GAME OVER: You ran out of moves!");

    private final String label;
    private final boolean won;
    private final int bonus;
    private final String headline;
    private final String message;
    private final String tip;
    private final String logLine;

    Outcome(String label, boolean won, int bonus, String headline, String message, String tip, String logLine) {
        this.label = label;
        this.won = won;
        this.bonus = bonus;
        this.headline = headline;
        this.message = message;
        this.tip = tip;
        this.logLine = logLine;
    }

    /** Stored in the leaderboard's game_result column. */
    public String label() { return label; }
    public boolean isWin() { return won; }
    /** Ending bonus added to the final score (0 for defeats). */
    public int bonus() { return bonus; }
    public String headline() { return headline; }
    public String message() { return message; }
    public String tip() { return tip; }
    public String logLine() { return logLine; }
}
