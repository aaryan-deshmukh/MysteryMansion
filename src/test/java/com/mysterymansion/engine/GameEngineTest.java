package com.mysterymansion.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mysterymansion.model.Difficulty;
import com.mysterymansion.model.Item;
import com.mysterymansion.model.Outcome;
import com.mysterymansion.model.Room;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameEngineTest {

    private GameEngine newGame(Difficulty d) {
        return new GameEngine("Tester", d, new Random(42));
    }

    // ---- setup ----------------------------------------------------------

    @Test
    void difficultyControlsStartingStats() {
        GameEngine easy = newGame(Difficulty.EASY);
        GameEngine hard = newGame(Difficulty.HARD);
        assertEquals(120, easy.getHealth());
        assertEquals(35, easy.getMovesRemaining());
        assertEquals(70, hard.getHealth());
        assertEquals(20, hard.getMovesRemaining());
        assertEquals(Room.MAIN_HALL, easy.getCurrentRoom());
    }

    @Test
    void passcodeIsFourDigits() {
        for (int seed = 0; seed < 50; seed++) {
            GameEngine g = new GameEngine("T", Difficulty.MEDIUM, new Random(seed));
            assertTrue(g.getSafePasscode().matches("\\d{4}"), g.getSafePasscode());
        }
    }

    @Test
    void itemsAreSpreadAcrossThreeDifferentRooms() {
        for (int seed = 0; seed < 50; seed++) {
            GameEngine g = new GameEngine("T", Difficulty.MEDIUM, new Random(seed));
            Set<Item> found = EnumSet.noneOf(Item.class);
            for (Room r : new Room[]{Room.LIVING_ROOM, Room.LIBRARY, Room.BASEMENT}) {
                Item item = g.getRoomItems().get(r);
                assertNotNull(item);
                found.add(item);
            }
            assertEquals(EnumSet.of(Item.TORCH, Item.HEALTH_POTION, Item.CODE_PAPER), found);
            assertEquals(Item.SAFE_KEY, g.getRoomItems().get(Room.LABORATORY));
        }
    }

    // ---- movement, search, health ---------------------------------------

    @Test
    void movingCostsOneMove() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.moveTo(Room.LIBRARY);
        assertEquals(24, g.getMovesRemaining());
        assertEquals(Room.LIBRARY, g.getCurrentRoom());
    }

    @Test
    void basementWithoutTorchHurts() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.moveTo(Room.BASEMENT);
        assertEquals(75, g.getHealth());
    }

    @Test
    void basementWithTorchIsSafe() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.addItem(Item.TORCH);
        g.moveTo(Room.BASEMENT);
        assertEquals(100, g.getHealth());
    }

    @Test
    void safeRoomsNeverTriggerCombat() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        for (int i = 0; i < 100; i++) {
            assertFalse(g.moveTo(Room.LIBRARY).combatTriggered());
        }
    }

    @Test
    void healthIsClampedBetweenZeroAndMax() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.modifyHealth(+500);
        assertEquals(100, g.getHealth());
        g.modifyHealth(-500);
        assertEquals(0, g.getHealth());
        assertTrue(g.isGameOver());
        assertEquals(Outcome.DEATH, g.defeatOutcome());
    }

    @Test
    void runningOutOfMovesEndsTheGame() {
        GameEngine g = newGame(Difficulty.HARD);
        for (int i = 0; i < 20; i++) g.search();
        assertTrue(g.isGameOver());
        assertEquals(Outcome.TIMEOUT, g.defeatOutcome());
    }

    @Test
    void roomCanOnlyBeSearchedOnce() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.moveTo(Room.LIVING_ROOM);
        g.search();
        int movesAfterFirst = g.getMovesRemaining();
        ActionResult second = g.search();
        assertTrue(second.messages().get(0).contains("already"));
        assertEquals(movesAfterFirst - 1, g.getMovesRemaining()); // still costs a move
    }

    @Test
    void foundItemsAreAddedOnceAndScore25() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        Room torchRoom = null;
        for (var e : g.getRoomItems().entrySet()) {
            if (e.getValue() == Item.TORCH) torchRoom = e.getKey();
        }
        g.moveTo(torchRoom);
        g.search();
        assertTrue(g.hasItem(Item.TORCH));
        assertEquals(25, g.getScore());
        g.addItem(Item.TORCH); // duplicate ignored
        assertEquals(25, g.getScore());
        assertEquals(1, g.getInventory().size());
    }

    @Test
    void healthPotionHealsAndIsConsumed() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        assertFalse(g.useHealthPotion());
        g.addItem(Item.HEALTH_POTION);
        g.modifyHealth(-50);
        assertTrue(g.useHealthPotion());
        assertEquals(85, g.getHealth());
        assertFalse(g.hasItem(Item.HEALTH_POTION));
    }

    // ---- safe puzzle ----------------------------------------------------

    @Test
    void safeOnlyWorksInLaboratory() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        ActionResult r = g.solveSafe(g.getSafePasscode());
        assertTrue(r.messages().get(0).contains("No biometric safe"));
        assertFalse(g.hasItem(Item.MANSION_KEY));
        assertEquals(100, g.getHealth());
    }

    @Test
    void wrongCodeShocksYou() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.moveTo(Room.LABORATORY);
        g.solveSafe("not-the-code");
        assertEquals(90, g.getHealth());
        assertFalse(g.hasItem(Item.MANSION_KEY));
    }

    @Test
    void correctCodeGivesMansionKey() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.moveTo(Room.LABORATORY);
        g.solveSafe("  " + g.getSafePasscode() + " "); // whitespace is trimmed
        assertTrue(g.hasItem(Item.MANSION_KEY));
    }

    // ---- escaping & scoring ---------------------------------------------

    @Test
    void cannotEscapeWithoutKey() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        ActionResult r = g.attemptEscape();
        assertNull(r.outcome());
        assertFalse(g.hasEscaped());
    }

    @Test
    void goodEndingScoreFollowsTheFormula() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.addItem(Item.MANSION_KEY); // +25
        g.addItem(Item.CODE_PAPER);  // +25
        ActionResult r = g.attemptEscape();
        assertEquals(Outcome.GOOD_ENDING, r.outcome());
        // (50 collected + 200 ending + 100 HP + 2*25 moves) x 1.25 = 500
        assertEquals(500, g.getScore());
    }

    @Test
    void normalEndingWithKeyOnly() {
        GameEngine g = newGame(Difficulty.EASY);
        g.addItem(Item.MANSION_KEY); // +25
        ActionResult r = g.attemptEscape();
        assertEquals(Outcome.NORMAL_ENDING, r.outcome());
        // (25 + 80 + 120 + 2*35) x 1.0 = 295
        assertEquals(295, g.getScore());
    }

    @Test
    void escapeCannotBeScoredTwice() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        g.addItem(Item.MANSION_KEY);
        g.attemptEscape();
        int scoreAfterFirst = g.getScore();
        ActionResult second = g.attemptEscape();
        assertNull(second.outcome());
        assertEquals(scoreAfterFirst, g.getScore());
    }

    // ---- combat ---------------------------------------------------------

    @Test
    void fleeingCostsScoreAndEndsTheFight() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        Combat c = g.startCombat();
        c.flee();
        assertTrue(c.isOver());
        assertFalse(c.playerWon());
        assertEquals(-15, g.getScore());
    }

    @Test
    void attackingEventuallyEndsTheFight() {
        GameEngine g = newGame(Difficulty.EASY);
        Combat c = g.startCombat();
        int rounds = 0;
        while (!c.isOver() && rounds++ < 20) c.attack();
        assertTrue(c.isOver());
        if (c.playerWon()) assertEquals(50, g.getScore());
    }

    @Test
    void finishedFightIgnoresFurtherMoves() {
        GameEngine g = newGame(Difficulty.MEDIUM);
        Combat c = g.startCombat();
        c.flee();
        assertTrue(c.attack().isEmpty());
        assertEquals(-15, g.getScore());
    }

    // ---- save / resume --------------------------------------------------

    @Test
    void restoredGameMatchesTheOriginal() {
        GameEngine g = newGame(Difficulty.HARD);
        g.addItem(Item.TORCH);
        g.moveTo(Room.LIBRARY);
        g.search();
        g.modifyHealth(-12);

        GameEngine r = new GameEngine(g.getPlayerName(), g.getDifficulty(), g.getHealth(), g.getMaxHealth(),
                g.getScore(), g.getMovesRemaining(), g.getMaxMoves(), g.getCurrentRoom(), g.getSafePasscode(),
                g.getInventory(), g.getRoomItems(), g.getSearchedRooms());

        assertEquals(g.getHealth(), r.getHealth());
        assertEquals(g.getScore(), r.getScore());
        assertEquals(g.getMovesRemaining(), r.getMovesRemaining());
        assertEquals(g.getCurrentRoom(), r.getCurrentRoom());
        assertEquals(g.getSafePasscode(), r.getSafePasscode());
        assertEquals(g.getInventory(), r.getInventory());
        assertEquals(g.getRoomItems(), r.getRoomItems());
        assertEquals(g.getSearchedRooms(), r.getSearchedRooms());
    }
}
