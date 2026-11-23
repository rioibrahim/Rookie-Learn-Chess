<div align="center">

# Rookie

**Rookie: Learn Chess** · Offline-First Tactical Chess, Live Coaching & Tabletop Play

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web-7fa650?style=flat-square)](#architecture--build)
[![Engine](https://img.shields.io/badge/Runtime-Capacitor%206-1f1d1a?style=flat-square)](#architecture--build)
[![Audio](https://img.shields.io/badge/Audio-Offline%20Base64%20WAV-4b9bff?style=flat-square)](#standout-highlights)
[![Languages](https://img.shields.io/badge/Languages-EN%20%2F%20ID-e9e3d7?style=flat-square)](#standout-highlights)

*Zero accounts. Zero internet required. Crisp modern vector pieces, tactile feedback, smart post-game analysis, and tabletop 2-player ergonomics packaged into a fast, single-file chess experience.*

</div>

---

## Overview

**Rookie** (displayed in-game as **Rookie: Learn Chess**) is built for players who want a distraction-free board that feels immediate on mobile—whether you are learning how pieces move for the first time, testing unorthodox Sandbox rules, sparring against five AI bot tiers, or placing one phone flat on a table for an over-the-board match.

Everything from legal move generation and bot search to voice/move sound synthesis runs **100% locally on your device**, with automatic progress saving after every move.

---

## Game Modes

| Mode | What It Does | Best For |
| :--- | :--- | :--- |
| **2-Player Tabletop** | Prompts you on entry to pick **Face-to-Face** or **Same Direction** play. | Playing with a friend on one device |
| **Bot Arena** | Choose your side (`White`, `Random`, or `Black`) against 5 bot personalities: `Easy`, `Medium`, `Hard`, `Nightmare`, and `Mittens`. | Solo practice & escalating challenge |
| **Coach Gemini** | Real-time mentor offering live commentary, threat arrows, positional tips, and blunder callouts as you play. | Improving decision-making mid-game |
| **Sandbox** | Custom board editor with rule-breakers, hybrid piece abilities, and instant chaos presets. | Experimenting & creative chess |
| **Learn & Puzzles** | **10 interactive lessons** (23 guided steps) plus **3 daily sequential tactical puzzles** synchronized to local midnight. | Fundamentals & pattern recognition |

---

## Deep Dive: 2-Player Ergonomics

When you start a 2-Player match, Rookie lets you choose how both players share the screen:

- **Face-to-Face (Physical Board Style)**
  - The board stays locked in place while opposing pieces naturally face each other (`180°`), mimicking a real wooden board across a table.
  - **Dual Mirrored Coordinates** (`A–H` and `1–8`) run along all four outer borders so both sides read squares effortlessly.
  - **Dual Facing Graveyards** show captures on each player's own side.
  - **Dynamic Moving Turn Banner** physically relocates and flips upright toward whoever's turn it is—upright below White's tray on White's turn, rotated `180°` above Black's tray on Black's turn.
- **Same Direction (Shared Screen Style)**
  - Smoothly rotates the board `180°` after every valid turn so the active player always looks forward.
  - Uses a compact single **active-turn graveyard** strip and a glowing white turn banner.

---

## Standout Highlights

### 1. Coach Gemini & the "Pure Idiot" Tier
Instead of generic one-liners, **Coach Gemini** reacts to board context in both **English** and **Indonesian (`EN` / `ID`)** with 25+ dynamic lines—pointing out specific piece threats and directional attacks with visual arrows.
- Make a sharp developing move? Expect tactical encouragement.
- Walk straight into a catastrophic, indefensible blunder? Trigger the special **`Pure Idiot`** commentary tier styled in glowing fiery red.

### 2. Post-Game Replay & Smart Match Analyzer
Finished a match? Open **Replay Match** to step through every move or use **Auto-Play** and **Smart Skip**:
- Filters past quiet routine moves to jump straight to critical turning points.
- Automatically flags **Genius / Alien Gambits**, **Brilliant Quiet Moves**, **Fair Trades**, **Bad Trades (`-1`)**, **Blunders**, and **Pure Idiot** moments.

### 3. Custom Vector Chess Set & Dual Coordinates
- Hand-tuned SVG vector silhouettes—highlighted by the sharp **Fierce Warhorse Knight** (featured both on the board and centered on the **Rookie** app launcher icon).
- Crisp high-contrast light/dark piece styling with dark, light, and system theme support (`Dark`, `Light`, `Auto`).

### 4. Zero-Latency Local Audio Engine
- Instead of relying on inconsistent Android WebView `speechSynthesis` TTS engines, Rookie uses an **embedded HTML5 `<audio>` + Base64 WAV sound engine**.
- Triggers instant move sounds, capture/check cues, and replay callouts offline with independent mute, cue toggle, and volume slider controls.

### 5. Flexible Exports & Persistent Auto-Save
- **Interactive PGN Export**: Preview and copy games in either:
  - **Classic Algebraic** (`1. Nf3 d5 2. Rg1`)
  - **Modern Beginner-Friendly** (`1. f3(Knight) d5(Pawn) 2. g1(Rook)`)
- **Hybrid 720×720 PNG Board Snapshot**: Generates a crisp shareable image complete with move highlights, arrows, and a `"Rookie: Learn Chess"` watermark via Clipboard API or Android's native Share Sheet.
- **Auto-Save**: Board state, Sandbox setup, move history, and preferences persist automatically in `localStorage` (`rookie` & `rookie_match_v1`).

---

## Sandbox Chaos & Rule-Breakers

Sandbox mode locks cleanly into **Same Direction** view and unlocks full board customization:
- **Presets**: *All Queens*, *All Knights*, *All Rooks*, *Super Hybrid*
- **Hybrid Movement Modifiers**: Combine extra abilities onto pieces (such as `+ Knight Jump` or `+ Leap`)
- **Rule-Breakers**: Enable *Capture Own Pieces* or *Allow Illegal / Capture King*
- **Board Tools**: Tap-to-draw tactical arrows (`Draw Mode`), step back/forward (`Undo` / `Redo`), flip board, or copy FEN/PGN at any moment.

---

## Architecture & Build

```text
├── www/index.html               # Self-contained chess engine, UI, audio & analyzer
├── capacitor.config.json        # Capacitor 6 config (appId: com.rookie.learnchess, appName: Rookie)
├── android/app/src/main/res/    # Launcher strings & adaptive fierce-knight icon assets
├── app/src/main/res/            # Shared Android launcher resources & adaptive icon mipmaps
└── .github/workflows/build.yml  # Automated Capacitor 6 Android Debug APK CI pipeline
```

### Automated Android APK Pipeline
Every push or manual dispatch to `.github/workflows/build.yml` automatically:
1. Installs `@capacitor/core@6`, `@capacitor/cli@6`, and `@capacitor/android@6`.
2. Syncs `www/` into a clean Capacitor Android project using `capacitor.config.json`.
3. Enforces the `"Rookie"` launcher display name and injects the centered fierce-knight adaptive & raster mipmap icons across all densities (`mdpi`–`xxxhdpi`).
4. Increments `versionCode` / `versionName` and compiles `Rookie-Learn-Chess-v<run>.apk`.
