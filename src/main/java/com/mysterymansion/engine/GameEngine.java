package com.mysterymansion.engine;

import com.mysterymansion.model.Difficulty;
import com.mysterymansion.model.Item;
import com.mysterymansion.model.Outcome;
import com.mysterymansion.model.Room;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * All game rules live here. This class has no Swing or JDBC code, so it can be unit-tested
 * and reused with any front end.
 */
public class GameEngine {
    private static final int POTION_HEAL = 35;
    private static final int ITEM_SCORE = 25;
    private static final int DARKNESS_DAMAGE = 25;
    private static final int WRONG_CODE_DAMAGE = 10;
    private static final double ENCOUNTER_CHANCE = 0.55; // original: Math.random() > 0.45

    private final String playerName;
    private final Difficulty difficulty;
    private final Random rng;
    private final int maxHealth;
    private final int maxMoves;
    private final String safePasscode;

    private int health;
    private int score;
    private int movesRemaining;
    private Room currentRoom;
    private boolean escaped;
    private String scoreBreakdown = "";

    private final List<Item> inventory;
    private final Map<Room, Item> roomItems;
    private final Set<Room> searchedRooms;

    /** New game. */
    public GameEngine(String playerName, Difficulty difficulty) {
        this(playerName, difficulty, new Random());
    }

    /** New game with an injectable Random (lets tests make runs deterministic). */
    public GameEngine(String playerName, Difficulty difficulty, Random rng) {
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.rng = rng;
        this.maxHealth = difficulty.maxHealth();
        this.health = maxHealth;
        this.maxMoves = difficulty.maxMoves();
        this.movesRemaining = maxMoves;
        this.currentRoom = Room.MAIN_HALL;
        this.score = 0;
        this.safePasscode = String.format("%04d", rng.nextInt(10000));
        this.inventory = new ArrayList<>();
        this.roomItems = new EnumMap<>(Room.class);
        this.searchedRooms = EnumSet.noneOf(Room.class);
        placeItems();
    }

    /** Rebuilds a game exactly as it was saved. */
    public GameEngine(String playerName, Difficulty difficulty, int health, int maxHealth, int score,
                      int movesRemaining, int maxMoves, Room currentRoom, String safePasscode,
                      List<Item> inventory, Map<Room, Item> roomItems, Set<Room> searchedRooms) {
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.rng = new Random();
        this.health = health;
        this.maxHealth = maxHealth;
        this.score = score;
        this.movesRemaining = movesRemaining;
        this.maxMoves = maxMoves;
        this.currentRoom = currentRoom;
        this.safePasscode = safePasscode;
        this.inventory = new ArrayList<>(inventory);
        this.roomItems = new EnumMap<>(Room.class);
        this.roomItems.putAll(roomItems);
        this.searchedRooms = EnumSet.noneOf(Room.class);
        this.searchedRooms.addAll(searchedRooms);
    }

    /** Torch, Health Potion and Code Paper go into three random rooms; the safe is always in the Laboratory. */
    private void placeItems() {
        List<Room> rooms = new ArrayList<>(List.of(Room.LIVING_ROOM, Room.LIBRARY, Room.BASEMENT));
        Collections.shuffle(rooms, rng);
        roomItems.put(rooms.get(0), Item.TORCH);
        roomItems.put(rooms.get(1), Item.HEALTH_POTION);
        roomItems.put(rooms.get(2), Item.CODE_PAPER);
        roomItems.put(Room.LABORATORY, Item.SAFE_KEY);
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------
    public String getPlayerName() { return playerName; }
    public Difficulty getDifficulty() { return difficulty; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getScore() { return score; }
    public int getMovesRemaining() { return movesRemaining; }
    public int getMaxMoves() { return maxMoves; }
    public Room getCurrentRoom() { return currentRoom; }
    public String getSafePasscode() { return safePasscode; }
    public String getScoreBreakdown() { return scoreBreakdown; }
    public boolean hasEscaped() { return escaped; }

    public List<Item> getInventory() { return Collections.unmodifiableList(inventory); }
    public boolean hasItem(Item item) { return inventory.contains(item); }

    public List<String> getInventoryNames() {
        List<String> names = new ArrayList<>();
        for (Item i : inventory) names.add(i.displayName());
        return names;
    }

    /** Copies, used when saving progress. */
    public Map<Room, Item> getRoomItems() {
        Map<Room, Item> copy = new EnumMap<>(Room.class);
        copy.putAll(roomItems);
        return copy;
    }

    public Set<Room> getSearchedRooms() {
        Set<Room> copy = EnumSet.noneOf(Room.class);
        copy.addAll(searchedRooms);
        return copy;
    }

    // ------------------------------------------------------------------
    // Low-level state changes
    // ------------------------------------------------------------------
    public void modifyHealth(int amount) {
        health = Math.max(0, Math.min(maxHealth, health + amount));
    }

    public void addScore(int points) { score += points; }

    /** Adds a new item to the inventory (+25 score). Duplicates are ignored. */
    public void addItem(Item item) {
        if (!inventory.contains(item)) {
            inventory.add(item);
            addScore(ITEM_SCORE);
        }
    }

    // ------------------------------------------------------------------
    // Player actions
    // ------------------------------------------------------------------

    /** Walks to another room (1 move). May spring the darkness trap or flag a monster encounter. */
    public ActionResult moveTo(Room room) {
        currentRoom = room;
        movesRemaining--;

        List<String> log = new ArrayList<>();
        log.add("\n📍 Moved to " + room.displayName() + ". " + room.description());

        if (room == Room.BASEMENT && !hasItem(Item.TORCH)) {
            log.add("⚠️ DARKNESS TRAP! Entering the pitch black basement without a Torch caused "
                    + DARKNESS_DAMAGE + " HP Damage!");
            modifyHealth(-DARKNESS_DAMAGE);
        }

        // Skipped if the trap already killed the player
        boolean fight = health > 0 && room.isDangerous() && rng.nextDouble() < ENCOUNTER_CHANCE;
        return new ActionResult(log, fight, null);
    }

    /** Starts a monster fight (call when {@link ActionResult#combatTriggered()} is true). */
    public Combat startCombat() { return new Combat(this, rng); }

    /** Searches the current room (1 move). Each room can be searched once. */
    public ActionResult search() {
        movesRemaining--;
        if (searchedRooms.contains(currentRoom)) {
            return ActionResult.of("🔍 You already thoroughly searched this area.");
        }
        searchedRooms.add(currentRoom);

        Item found = roomItems.get(currentRoom);
        if (found == null) {
            return ActionResult.of("🔍 Searched the area... Found nothing of value.");
        }
        if (found == Item.SAFE_KEY) {
            return ActionResult.of("🔍 Found the Laboratory Biometric Safe! Click 'Solve Safe Code' to crack it.");
        }
        addItem(found);
        if (found == Item.CODE_PAPER) {
            return ActionResult.of("🔍 DISCOVERY! You found a blood-stained paper with text: 'SAFE CODE: "
                    + safePasscode + "'!");
        }
        return ActionResult.of("🔍 DISCOVERY! You found a [" + found.displayName() + "]!");
    }

    /** Tries a passcode on the Laboratory safe. Wrong code: -10 HP. */
    public ActionResult solveSafe(String code) {
        if (currentRoom != Room.LABORATORY) {
            return ActionResult.of("🧩 No biometric safe found in this room. Search the Laboratory!");
        }
        if (code != null && code.trim().equals(safePasscode)) {
            addItem(Item.MANSION_KEY);
            return ActionResult.of("🔓 ACCESS GRANTED! The safe opened and you retrieved the [Mansion Key]!");
        }
        modifyHealth(-WRONG_CODE_DAMAGE);
        return ActionResult.of("❌ ACCESS DENIED! Wrong passcode. Electric shock caused -"
                + WRONG_CODE_DAMAGE + " HP!");
    }

    /** Drinks a Health Potion if the player has one. */
    public boolean useHealthPotion() {
        if (inventory.remove(Item.HEALTH_POTION)) {
            modifyHealth(POTION_HEAL);
            return true;
        }
        return false;
    }

    /** Tries to leave through the main gate. Needs the Mansion Key; the Code Paper unlocks the good ending. */
    public ActionResult attemptEscape() {
        if (escaped) return new ActionResult(List.of(), false, null);

        if (!hasItem(Item.MANSION_KEY)) {
            return ActionResult.of("❌ The main gate is deadlocked! You need the Mansion Key from the Laboratory safe.");
        }
        Outcome ending = hasItem(Item.CODE_PAPER) ? Outcome.GOOD_ENDING : Outcome.NORMAL_ENDING;
        escaped = true; // points can only be awarded once
        calculateFinalScore(ending.bonus());
        return new ActionResult(List.of("🏆 FINAL SCORE " + scoreBreakdown, ending.logLine()), false, ending);
    }

    /**
     * final = (score + ending bonus + remaining HP + 2 x moves left) x difficulty multiplier
     */
    public int calculateFinalScore(int endingBonus) {
        int movesBonus = Math.max(0, movesRemaining) * 2;
        double multiplier = difficulty.scoreMultiplier();
        int base = score + endingBonus + health + movesBonus;
        int collected = score;
        score = (int) Math.round(base * multiplier);
        scoreBreakdown = "(" + collected + " collected + " + endingBonus + " ending + " + health + " HP + "
                + movesBonus + " moves) x " + multiplier + " = " + score;
        return score;
    }

    public boolean isGameOver() { return health <= 0 || movesRemaining <= 0; }

    /** Which defeat applies; only meaningful when {@link #isGameOver()} is true. */
    public Outcome defeatOutcome() { return health <= 0 ? Outcome.DEATH : Outcome.TIMEOUT; }
}
