package com.mysterymansion.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One turn-based fight against the Mutant. Pure logic: the UI just asks for a move and prints the messages. */
public class Combat {
    public static final int MONSTER_MAX_HEALTH = 40;

    private final GameEngine engine;
    private final Random rng;
    private int monsterHealth = MONSTER_MAX_HEALTH;
    private boolean fled;

    Combat(GameEngine engine, Random rng) {
        this.engine = engine;
        this.rng = rng;
    }

    public String introMessage() { return "👾 ALERT! A dangerous Mutant Monster attacks!"; }

    public int monsterHealth() { return Math.max(0, monsterHealth); }

    public boolean isOver() { return monsterHealth <= 0 || engine.getHealth() <= 0 || fled; }

    public boolean playerWon() { return monsterHealth <= 0; }

    public List<String> attack() {
        List<String> log = new ArrayList<>();
        if (isOver()) return log;
        int playerDamage = rng.nextInt(15) + 12;
        int enemyDamage = rng.nextInt(12) + 5;
        monsterHealth -= playerDamage;
        engine.modifyHealth(-enemyDamage);
        log.add("⚔️ You struck the mutant for " + playerDamage + " DMG! The mutant clawed back for " + enemyDamage + " DMG.");
        if (monsterHealth <= 0) {
            engine.addScore(50);
            log.add("🏆 VICTORY! You defeated the Mutant Monster and earned +50 Score!");
        }
        return log;
    }

    public List<String> defend() {
        List<String> log = new ArrayList<>();
        if (isOver()) return log;
        int enemyDamage = rng.nextInt(5);
        engine.modifyHealth(-enemyDamage);
        log.add("🛡️ You shielded the attack! Took only " + enemyDamage + " DMG.");
        return log;
    }

    public List<String> flee() {
        List<String> log = new ArrayList<>();
        if (isOver()) return log;
        fled = true;
        engine.addScore(-15);
        log.add("🏃 You fled back to safety, losing 15 Score Points!");
        return log;
    }
}
