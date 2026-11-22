# Rookie: Learn Chess

**Rookie: Learn Chess** is a modern, lightweight, mobile-first web chess experience powered by **Capacitor**, built for instant offline play on Android and desktop browsers. It combines tactile SVG visuals, an embedded local HTML5 audio engine, a post-game smart replay analyzer, dynamic contextual captured-piece graveyards, and beginner-friendly PGN/PNG export in a single self-contained app.

---

## Key Features

- **Dynamic Contextual Graveyard (Captured Pieces Boxes)**
  - **Bot & Coach Modes:** Displays **only 1 minimalist graveyard box** at the bottom for the player, maximizing vertical screen space for the chessboard.
  - **2-Player Face-to-Face:** Automatically rotates the board 180° each turn and renders **2 facing graveyard boxes** (top box rotated 180° for the top player, bottom box upright for the bottom player).
  - **2-Player Same Direction:** Keeps a fixed board view with **1 active graveyard box** at the bottom that dynamically switches between White and Black captures on every turn.
  - **Latest Capture Glow:** Highlights the most recently captured piece inside the graveyard box with an animated accent ring.

- **Local Audio Engine & Bilingual Support (`EN` / `ID`)**
  - Powered by an embedded offline **HTML5 `<audio>` + Base64 WAV sound engine** — instant, zero-latency board SFX, move audio cues, and replay commentary with zero internet dependency and without relying on flaky WebView TTS engines.
  - Full bilingual interface supporting **English** (*King, Queen, Rook, Bishop, Knight, Pawn*) and **Indonesian** (*Raja, Menteri, Benteng, Gajah, Kuda, Pion*), synced seamlessly with mute, cue toggle, and volume slider controls.

- **Post-Game Replay & Smart Match Analyzer**
  - Step forward, backward, or auto-play through any completed match move by move.
  - **Smart Filtering:** Automatically skips ordinary quiet development moves so commentary focuses exclusively on pivotal moments:
    - **Genius / Alien Gambit:** Multi-piece tactical sacrifices and deep exchanges.
    - **Brilliant Quiet Moves:** Unstoppable forks, pins, or forced tactical blows.
    - **Blunders & Trades:** Hanging pieces, mate-in-1 blunders, Fair Trades, and Bad Trades (`-1`).

- **Interactive PGN Export (Classic vs. Modern Format)**
  - Clicking **Copy PGN** opens an interactive format selector with a live preview before copying to the clipboard:
    - **Classic:** Standard algebraic notation (e.g., `1. Nf3 d5 2. Rg1`).
    - **Modern (Beginner-Friendly):** Destination square first followed by piece name in parentheses (e.g., `1. f3(Knight) d5(Pawn) 2. g1(Rook)`).

- **Game State Auto-Save & Hybrid Copy-PNG / Share Sheet**
  - Automatically persists active match progress, board setup, Sandbox rules, move history, and preferences to `localStorage` (`rookie` & `rookie_match_v1`) so games resume right where you left off.
  - Renders a crisp 720×720 board snapshot complete with highlights, arrows, and a `"Rookie: Learn Chess"` watermark using a hybrid approach: direct Clipboard API image write with automatic Android Web Share API fallback.

---

## Game Modes

1. **2-Player Mode (with Orientation Prompt)**
   - Prompts you on entry to choose between:
     - **Face-to-Face:** Board rotates smoothly 180° after each turn with dual top/bottom facing graveyards.
     - **Same Direction:** Fixed board view with a single shifting active-turn graveyard box.
2. **Bot Match (`Easy`, `Medium`, `Hard`, `Nightmare`, `Mittens`)**
   - Pick your preferred bot tier and piece color (`White`, `Random`, or `Black`) in a two-step selection flow before starting.
3. **Coach Gemini (Live Mentor)**
   - Friendly real-time blunder alerts, threat arrows, tactical hints, and move explanations in English or Indonesian.
4. **Sandbox Mode (Custom Rules & Piece Roster)**
   - Place custom pieces, apply chaos presets (*All Queens*, *All Knights*, *All Rooks*, *Super Hybrid*), mix hybrid movement rules (e.g., `+ Knight Jump`, `+ Leap`), allow rule-breakers (*Capture Own Pieces*, *Allow Illegal / Capture King*), and choose 2-Player orientation (*Face-to-Face* or *Same Direction*) — with Bot opponent restricted in Sandbox.
5. **Learning Hub & Daily Puzzles**
   - 10 structured interactive lessons across 23 steps plus 3 daily sequential tactical puzzles synchronized to your local midnight reset.

---

## The Developer's Corner (Funny FAQ from the Studio)

### Q: Kenapa logo/ikon aplikasi ini tidak sepenuhnya melambangkan catur standar?
> **Jawaban Studio:**
> Percayalah, awalnya kami ingin menggambar Kuda (Knight) yang super realistis lengkap dengan surai angin dramatis. Tapi setelah 3 jam bertarung dengan kurva Bezier, hasilnya malah terlihat lebih mirip kuda laut yang sedang kaget atau sendok bengkok. Akhirnya tim mengambil keputusan filosofis: **"Minimalis Modern"**. Di *Rookie: Learn Chess*, ikon mahkota geometris bersih melambangkan bahwa siapa pun—bahkan seorang *rookie* sekalipun—bisa berevolusi dari Pion sederhana menjadi Menteri paling berbahaya di papan, tanpa harus bingung melihat bayangan kuda laut di layar utama HP kamu.

### Q: Kenapa nama atau saat di-install kadang terasa tidak sesuai ekspektasi awal?
> **Jawaban Studio:**
> Selamat datang di dunia ajaib **Capacitor & Android Resource Mapping**! Kadang di balik layar terjadi "drama identitas" antara `appName` di `capacitor.config.json`, label `app_name` / `title_activity_main` di `android/app/src/main/res/values/strings.xml`, dan `package_name` (`com.rookie.learnchess`). Belum lagi OS Android punya hobi memotong judul yang panjang di launcher atau menyimpan cache ikon/nama APK versi lama kalau tidak di-uninstall dulu. Singkatnya: kalau saat install nama atau perilakunya sempat bikin kamu mengernyitkan dahi, itu bukan bug—itu cara Android menyapa developer sambil bilang *"Coba sync Gradle & bersihkan cache dulu, bro!"* Sekarang semuanya sudah dikunci rapi ke **"Rookie: Learn Chess"**.

---

## Screenshots / In-Game Gallery

| Main Board & Dynamic Graveyard | 2-Player Orientation Prompt |
| :---: | :---: |
| ![Main Board Preview](docs/screenshots/main-board.png) | ![2-Player Orientation Modal](docs/screenshots/p2-orientation-modal.png) |
| *Clean board layout with contextual graveyard boxes* | *Face-to-Face vs. Same Direction selection* |

| Interactive PGN Export (Classic vs. Modern) | Post-Game Smart Replay Analyzer |
| :---: | :---: |
| ![PGN Export Modal](docs/screenshots/pgn-export-modal.png) | ![Smart Replay Analyzer](docs/screenshots/replay-analyzer.png) |
| *Switch between `Rg1` and `g1(Rook)` notation* | *Automatic detection of Alien Gambits & Blunders* |
