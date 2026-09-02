# Marbles

**English | [简体中文](./README.zh-CN.md)**

A Java hexagonal-grid marble shooter (Games Programming coursework Ass-2), featuring a Sans boss level and special marble mechanics.

## Contents

- [Background](#background)
- [Run](#run)
- [Gameplay](#gameplay)
- [Scoring](#scoring)
- [Special Marbles](#special-marbles)
- [Project Structure](#project-structure)
- [License](#license)

## Background

Coursework for Games Programming (Ass-2): hexagonal-grid marble shooter gameplay, extended at level 4 with a Sans boss mechanic. The design notes live in `备注.txt`.

## Run

- Open the project directory in IntelliJ IDEA (make sure `resources/` loads correctly).
- JDK: 26 (see the original `Readme.md`).
- Entry class: `src/Main.java`.

## Gameplay

- Hexagonal grid: starts with 8 rows, up to 17 rows; hexagon side length 24.22 (see `备注.txt`).
- Launch marbles; they stick to same-colored marbles or the ceiling; 3+ connected same colors pop.
- The whole board sinks every second (y += 2/5 hexagon side length per second).
- Level 4 triggers the Sans boss with periodic skills (color shuffle, bedrock blockade, board drop, creeper marbles) plus battle dialogue.

## Scoring

- Clearing 3 or fewer: 10 points each.
- Clearing 4-6: 15 points each.
- Clearing more than 6: 20 points each.
- Fallen marbles: 20 points each.

(Rules in `备注.txt`.)

## Special Marbles

| Marble | Behavior | Levels |
|--------|----------|--------|
| normal | pops with 3+ same color | all |
| creeper | on hit, pops normal marbles within +3 range | 2 / 3 / 4 |
| bedrock | indestructible obstacle, only falls | 3 / 4 |
| heart | indestructible, only falls | 4 |

## Project Structure

```
src/
├─ Main.java            # entry
├─ GameEngine.java      # engine
├─ ScreenGame.java      # game screen
├─ ScreenStart.java     # start screen
├─ Level.java           # levels
├─ Marbles.java         # hex grid & spawn logic
├─ Marble.java          # marble entity
├─ MarbleLaunch.java    # marble launching
├─ BossSans.java        # Sans boss (level 4)
└─ ResourceManager.java # resource management
resources/              # images / sprites
备注.txt                # design notes (Chinese)
```

## License

MIT License, see [LICENSE](LICENSE).
