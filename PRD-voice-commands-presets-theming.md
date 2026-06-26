# PRD — Presets, Voice Commands, Theming & Defaults

Status: Approved design (grilling session 2026-06-26)
Scope: four loosely-coupled features agreed via design interview. Each can ship independently except where noted.

---

## Feature 1 — AI Preset improvements

### 1a. Empty-input guard (bug fix)
**Problem:** On AI presets (Auto-Format, etc.) — but never on Raw — saying nothing produces invented text. The near-empty transcript is shipped to the Groq chat model, which fills the void.

**Fix:**
- In `FormatTranscriptUseCase`, return `Result.Success(rawText)` (no API call) when `rawText.isBlank()`. Guard on `isBlank()` only — **no word-count floor** (would eat legitimate short dictation like "yes"/"stop").
- Belt-and-suspenders: add to **every** preset system prompt: *"If the input contains no real content, return it unchanged — never invent text."*

### 1b. Preset prompt quality pass
Rewrite all `OutputPreset` system prompts to a consistent structure:
- Role line → 3–5 crisp rules → **one few-shot input→output example** → "return only the result" guardrail + the never-invent clause.
- **Output format: safe-everywhere plain text.** Title line + bullets / `—` separators + currency symbol when a price is heard. **No Markdown tables or `**bold**`** (renders as literal garbage in WhatsApp/SMS/plain fields, which is where this tool is mostly used).
- Groceries: auto-extract a title and parse **unstructured speech** into clean items with quantity/price.
- Few-shot examples are the main lever for "works out of the box" on the 70B model.

### 1c. Translation output language
- New **inline selector under the Translate card** when Translate is selected (mirrors the existing `CustomInstructionsEditor` spanning-grid-item pattern).
- Opens a bottom sheet styled like `LanguagePickerBottomSheet` with a **curated ~30-language list** — separate from and broader than the Whisper *input* language list (the LLM can write far more languages than Whisper can hear).
- New DataStore key `TRANSLATE_TARGET_LANGUAGE` (default `English`), read in `FormatTranscriptUseCase`, injected into the Translate prompt (`"Translate to {language}."`).

---

## Feature 2 — Voice Commands + `expand` / `insert` (first-word router)

### Core model
A single **first-word switch** drives all text-driven behavior, evaluated **before** formatting/insertion, inside a new centralized **`ProcessTranscriptUseCase`**:

1. First word == reserved **`expand`** → look up **second word** in Text Expander store → exact match inserts expansion; no match → fall through.
2. First word == reserved **`insert`** → look up **second word** in **My Info** store → exact match pastes value into focused field; no match → fall through.
3. First word matches a **user-defined command** → run its action with the **rest of the sentence** as the query.
4. Otherwise → normal path (preset format → insert).

**Architecture:** the use case *decides* and returns a sealed result (e.g. `Pasted` / `LaunchedApp` / `CopiedForManualPaste` / `ShowMessage`); the **service executes** it (launch activity via `Context`, paste via `TextInserter`). This removes the duplicated expand/format/insert logic currently inline in both `BubbleService` (~L511–527) and `TranscribeAudioUseCase`, so both voice entry points get the new features.

### Matching rules
- Lowercase + strip trailing punctuation (reuse the expander's existing sanitization).
- `expand`/`insert`: **key = second word**, **exact** match required.
- commands: **command = first word**, **query = remaining words** (passed through, not looked up).
- Reserved words **`expand`** and **`insert`** cannot be used as command triggers or shortcut/key names (rejected at save).

### Action types (commands)
- **Web Search** → Chrome specifically if installed, else default browser (`https://google.com/search?q=…`). ← the "chrome best player" case.
- **YouTube Search**, **Maps Search**, **Play Store Search** → respective search intents/URIs.
- **Open App** → launch any installed app + leave the query on the clipboard for manual paste. **No auto-paste** (target field isn't focused while the app opens).

### Stores (new/changed)
- **Text Expander** becomes **prefix-only** (`expand <shortcut>`); bare whole-transcript matching is removed. Update `ExpandTextUseCase`.
- **My Info** — new Room table + list screen (separate from Text Expander; foundation for future multi-field autofill). Single-field paste only in this PRD.
- **Voice Commands** — new Room table + list screen. Entry = `{ triggerWord, actionType, targetAppPackage? }`.

### Safety model (critical)
1. **Global on/off toggle** in Productivity ("Voice Commands & Shortcuts"), **default on**.
2. **No exact match → paste the original transcript untouched.** The router can only ever *add* behavior on an exact match, never corrupt normal dictation. (e.g. "insert the cable into port two" → "the" matches no key → full sentence pasted.)
3. Reserved/command words kept deliberately uncommon as sentence openers; documented in UI.
4. App not installed / uninstalled → show "{App} is not installed" + paste full sentence (nothing lost).
5. Command word with no query → search actions open homepage/empty search; Open App just launches.

### Package visibility
Open-App picker enumerates launchable apps via a `<queries>` element for `CATEGORY_LAUNCHER`. **Do NOT use `QUERY_ALL_PACKAGES`** (sensitive, Play-review risk).

### Bubble feedback
Result states map to bubble messages: `Pasted ✓`, `Copied — paste into {App} ✓`, `Opening {App}…`, `{App} is not installed`.

---

## Feature 3 — Unified "one app" theming

**Root cause:** chrome (bottom tab bar, landscape rail) uses real live liquid glass and refracts the accent-glow backdrop; content panels (`SettingsSectionOptimized`, `GlassBox`, `GlassCard`) are painted with **opaque `#1C1C1E`**, so they read as dead black rectangles → "many separate apps" feeling.

**Fix:**
- Replace the opaque fill with a **translucent shared surface token** (e.g. `WhispryTokens.SurfaceGlass` / low-alpha dark) over the shared glowing background, keeping `GlassBorder`. The accent glow bleeds through → cohesive glass system.
- **No live `drawBackdrop` blur** on scrolling content panels (that perf cost is why they were made solid). Live glass stays on the chrome only.
- Apply the one token **consistently across all tabs** (Settings sections, History cards, Presets cards, Home cards).

---

## Feature 4 — Useful seeded defaults

All seeded rows are **fully editable (trigger AND content) and deletable** — defaults are normal rows, nothing locked.

- **Text Expander:** 3 working snippets — `ty` → "Thank you so much, I really appreciate it!", `omw` → "On my way, see you soon!", `meet` → "Could we schedule a quick meeting? What time works for you?"
- **My Info:** labeled **empty** placeholder rows — Address, Email, Phone, Full Name — with "Tap to add your…" hints. No fake sample values (avoids inserting placeholder junk into real forms).
- **Voice Commands:** ~4 pre-seeded visible rows — search→Web, youtube→YouTube, maps→Maps, chrome→Web.
- Seed strategy: pre-seed visible DB rows on first run (users learn by example).

---

## Recommended build order

1. **Feature 1a** (empty-input guard) — tiny, fixes a live bug, no dependencies.
2. **Feature 3** (theming token) — isolated, high visible impact, low risk.
3. **`ProcessTranscriptUseCase` extraction** (Feature 2 core) — refactor existing expand/format/insert into one use case with no behavior change yet; lands the seam everything else hangs off.
4. **Feature 1b/1c** (prompt rewrite + translate language) — prompt content + one DataStore key + inline selector.
5. **Feature 2 stores & screens** — My Info table/screen, Voice Commands table/screen, expander → prefix-only, the first-word router + action executors, global toggle, package-visibility `<queries>`.
6. **Feature 4** (seeded defaults) — last, once all three stores exist.

## Explicitly out of scope (future)
- Multi-field form autofill for `insert` (walking accessibility nodes, label heuristics) — its own design session.
- Live-glass on scrolling content panels.
- Markdown/adaptive per-app output formatting.
