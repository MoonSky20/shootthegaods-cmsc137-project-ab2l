# 🔫 Shoot The Gaods!
> A 1-4 player co-op top-down survival shooter — inspired by Box Head

Survive endless waves of Gaods (enemies) that get faster and more numerous every level. Work together, revive fallen teammates, and last as long as possible for the highest score.

---

## 📋 Requirements

| Tool | Version | Download |
|------|---------|----------|
| BellSoft Liberica **Full** JDK | 21 (Full JDK) | https://bell-sw.com/pages/downloads/ |
| IntelliJ IDEA Community | Latest | https://www.jetbrains.com/idea/download/ |

> ⚠️ You **must** download the **Full JDK** from BellSoft — NOT the Standard version.
> The Full JDK includes JavaFX. The Standard one does not.
> When installing, the folder should be named `LibericaJDK-21-Full`.

---

## 📁 Project Structure

```
ShootTheGaods/src/
├── main/
│   └── Main.java               → Entry point, launches the JavaFX window
├── core/
│   ├── GameLoop.java           → Custom game loop (update + render, ~60fps)
│   ├── InputHandler.java       → 4-player keyboard + mouse input
│   └── WaveManager.java        → Wave timer, difficulty scaling, Gaod spawning
├── model/
│   ├── Player.java             → Player movement, health, respawn logic
│   ├── Gaod.java               → Enemy — chases nearest living player
│   └── Bullet.java             → Projectile movement and collision
└── scenes/
    ├── MenuScene.java          → Main menu — select number of players
    └── GameScene.java          → Main gameplay — rendering, collisions, game over
```

---

## ⚙️ Setting Up in IntelliJ

### Step 1 — Install Liberica Full JDK 21
1. Go to https://bell-sw.com/pages/downloads/
2. Select: Version **21**, Package **Full JDK**, your OS and architecture
3. Download and run the **MSI** installer (Windows)
4. Confirm the folder `C:\Program Files\BellSoft\LibericaJDK-21-Full\` exists after install

### Step 2 — Open the Project
1. Open IntelliJ IDEA
2. Click **File → Open** and select the `ShootTheGaods` folder

### Step 3 — Point IntelliJ to Liberica Full JDK
1. Press **Ctrl + Alt + Shift + S** to open Project Structure
2. Go to **SDKs → + → Add JDK**
3. Navigate to:
   ```
   C:\Program Files\BellSoft\LibericaJDK-21-Full
   ```
4. Select it and click **OK**
5. Go to **Project** and set the SDK dropdown to **LibericaJDK-21-Full**
6. Click **Apply → OK**

### Step 4 — Mark Sources Root
1. Right-click the `src` folder in the project panel
2. Select **Mark Directory as → Sources Root**

### Step 5 — Run the Game
1. Open `Main.java`
2. Click the green ▶️ **Run** button
3. The menu will appear — select your player count and play!

---

## 🎮 Controls

### Player 1
| Input | Action |
|-------|--------|
| `W A S D` | Move |
| `Left Mouse Click` | Shoot (aimed at cursor) |

### Player 2
| Input | Action |
|-------|--------|
| `Arrow Keys` | Move |
| `L` | Shoot (in movement direction) |

### Player 3
| Input | Action |
|-------|--------|
| `I J K L` | Move |
| `U` | Shoot (in movement direction) |

### Player 4
| Input | Action |
|-------|--------|
| `Numpad 8 4 5 6` | Move |
| `Numpad 0` | Shoot (in movement direction) |

---

## 🕹️ Game Rules

### Objective
Survive as long as possible. The game ends when **all players are eliminated simultaneously**.

### Waves
- Waves last **30–60 seconds**
- Every level: enemies get **faster** and **more spawn**
- Wave duration increases **+15 seconds every 4 levels** (capped at 60s)

### Elimination & Respawn
- Players have **3 health points**
- Eliminated players **respawn after 5 seconds** — as long as at least one teammate is alive
- Respawn delay increases **+5 seconds every 3 levels** (capped at 15s)

### Scoring
- **+10 points** per Gaod eliminated
- Score is also based on **time survived** and **level reached**

---

## 📊 Difficulty Scaling Reference

| Level | Wave Duration | Gaods Spawned | Enemy Speed | Respawn Delay |
|-------|--------------|---------------|-------------|---------------|
| 1 | 30s | 3 | 1.5 | 5s |
| 2 | 30s | 5 | 1.8 | 5s |
| 3 | 30s | 7 | 2.1 | 5s |
| 4 | 30s | 9 | 2.4 | 10s |
| 5 | 45s | 11 | 2.7 | 10s |
| 8 | 45s | 17 | 3.6 | 15s |
| 12+ | 60s | 25+ | 4.8+ | 15s |

---

## ✅ Spec Compliance Notes

| Requirement | How We Meet It |
|-------------|---------------|
| No full game engine | Pure JavaFX + Java — no Unity, libGDX, etc. |
| Own game loop | `GameLoop.java` manually calls `update()` then `render()` every frame |
| 2D graphics library | JavaFX Canvas (approved 2D sprite/graphics library) |
| 1–3 players co-op | Supports 1–4 players on one keyboard |
| Progressive difficulty | `WaveManager.java` scales speed, count, and respawn delay per level |
| Respawn system | `Player.java` + `WaveManager.getRespawnDelayFrames()` |

---

## ❓ Troubleshooting

**"Cannot find JavaFX" / import errors**
→ Make sure the SDK is set to **LibericaJDK-21-Full** (not Standard or Oracle JDK)

**Game window doesn't open**
→ Right-click `src/` → **Mark Directory as → Sources Root**, then re-run `Main.java`

**Player not responding to keys**
→ Click on the game window first to make sure it has focus

**Two players' keys conflicting**
→ This is a hardware limitation (key rollover). Try a different keyboard or reassign keys in `InputHandler.java`

**Want to change key bindings?**
→ Edit `InputHandler.java` — each player's keys are clearly labeled in separate blocks
