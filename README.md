# 🏚️ Mystery Mansion – Escape & Survival

A desktop escape-room game built with **Java Swing** and **MySQL (JDBC)**. Explore a mansion, find the
hidden items, crack the lab safe and escape before you run out of HP or moves. Items and the safe
code are randomized every run.

<!-- Add a screenshot or GIF here, e.g.  ![Gameplay](docs/gameplay.gif) -->

## Features
- Three difficulty levels (HP, move budget and score multiplier change)
- Randomized item placement and safe passcode on every run
- Turn-based monster fights (attack / defend / flee)
- **Save & resume** – progress is stored in MySQL after every move
- **Leaderboard** – top scores saved to the database
- Three outcomes: Good ending, Normal ending, Game over
- Custom-painted dark UI (rounded buttons, animated stat bars, per-room colour themes)

## Tech stack
Java 17 · Swing · MySQL 8 · JDBC · Maven · JUnit 5

## Architecture
```
com.mysterymansion
├── model    Difficulty, Item, Room, Outcome      (enums – no magic strings)
├── engine   GameEngine, Combat, ActionResult     (all game rules, no Swing/JDBC)
├── dao      DatabaseManager                      (leaderboard + save/resume)
├── util     DbConfig                             (reads DB settings from config/env)
└── ui       MansionFrame + custom components     (display only)
```
The UI never decides game rules: it calls the engine, which returns an `ActionResult`
(messages to log, whether a fight starts, the ending reached). That keeps the rules unit-testable.

## Run it
1. **Database**
   ```bash
   mysql -u root -p < sql/schema.sql
   ```
2. **Configuration** – copy the template and put in your MySQL password (this file is git-ignored):
   ```bash
   cp config.example.properties config.properties
   ```
   Alternatively set `MANSION_DB_URL`, `MANSION_DB_USER`, `MANSION_DB_PASSWORD` as environment variables.
3. **Build & play** (needs JDK 17+ and Maven)
   ```bash
   mvn package
   java -jar target/mystery-mansion-1.0.0.jar
   ```
   Run it from the folder that contains `config.properties`.

## Tests
```bash
mvn test
```
Covers scoring, difficulty setup, item placement, traps, safe puzzle, endings, combat and save/restore.

## Game rules
| Action | Cost / effect |
|---|---|
| Move between rooms | 1 move |
| Search a room (once per room) | 1 move, +25 score per item |
| Enter Basement without Torch | −25 HP |
| Wrong safe code | −10 HP |
| Health Potion | +35 HP |
| Defeat the Mutant / flee | +50 / −15 score |

Final score on escape = (score + ending bonus + remaining HP + 2 × moves left) × difficulty multiplier
(Easy 1.0, Medium 1.25, Hard 1.5). Good ending (Key + Code Paper) bonus 200, Normal ending (Key only) 80.

## Roadmap
- Run database calls off the UI thread
- Player accounts with hashed passwords, normalized save tables
- More rooms and puzzles, achievements, per-difficulty leaderboards
