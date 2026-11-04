CREATE DATABASE IF NOT EXISTS mystery_mansion;
USE mystery_mansion;

-- Leaderboard: written when the game ends (win or game over)
CREATE TABLE IF NOT EXISTS leaderboard (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    game_result VARCHAR(20) NOT NULL,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Save and Resume: one row per player, overwritten after every move
CREATE TABLE IF NOT EXISTS saved_games (
    player_name     VARCHAR(50) PRIMARY KEY,
    difficulty      VARCHAR(10) NOT NULL,
    health          INT NOT NULL,
    max_health      INT NOT NULL,
    score           INT NOT NULL,
    moves_remaining INT NOT NULL,
    max_moves       INT NOT NULL,
    current_room    VARCHAR(30) NOT NULL,
    safe_passcode   CHAR(4) NOT NULL,
    inventory       VARCHAR(255) NOT NULL DEFAULT '',
    room_items      VARCHAR(255) NOT NULL DEFAULT '',
    rooms_searched  VARCHAR(255) NOT NULL DEFAULT '',
    saved_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);