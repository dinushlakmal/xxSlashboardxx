# Changelog

All notable changes to **Slashboard Sinhala Keyboard** will be documented in this file.

---

## [2.6.0] - 2026-09-23

### 🚀 Automated GitHub Releases & CI/CD Pipeline (ස්වයංක්‍රීය GitHub Release පද්ධතිය)
- **GitHub Actions Release Automation**: Added dedicated `.github/workflows/release.yml` to automatically run unit tests, build signed debug/release APKs, and publish GitHub Releases with assets (`Slashboard-v2.6.0.apk`) upon pushing version tags (`v*`) or via manual dispatch.
- **Continuous Integration (CI) Cleanup**: Purged invalid legacy build files (`ant.yml`, `maven-publish.yml`) that caused workflow failures on push, consolidating on Gradle with JDK 17.

### ⚡ Performance, Battery & Display Optimization (කාර්යක්ෂමතාව සහ බැටරි ඉතිරිය)
- **Idle Particle Sleep**: Optimized `KeyboardParticleSystem` to immediately sleep frame updates when no particle animations are active, avoiding unnecessary redraw loops.
- **Dynamic App Accent Refinement**: Streamlined palette color resolution and luminosity calculation in `KeyboardPalette` for cleaner dynamic theming.

### 📐 Ergonomics & Layout Calibration (යතුරුපුවරු පිරිසැලසුම් වැඩිදියුණු කිරීම)
- **Gesture Bar Clearance**: Fine-tuned bottom space clearance defaults and persistence across device restarts, ensuring zero obstruction on full-screen gesture navigation phones.
- **Consistent Key Geometry**: Improved touch-target bounds for comma, period, and symbol keys to prevent accidental taps.

### 🌐 Social & Community Ecosystem Integration (සමාජ මාධ්‍ය සහ සබැඳි ඒකාබද්ධ කිරීම)
- **Direct Release & Community Links**: Added 1-click links to the official Facebook page (`https://www.facebook.com/profile.php?id=61593856756750`), GitHub developer profile (`https://github.com/dinushlakmal`), and GitHub Releases download across Settings, About screen, and Web documentation.
- **Synchronized Documentation**: Updated landing page (`index.html`), feature guide (`home.html`), and terms/privacy documentation to reflect v2.6.0 release standards.

---

## [2.5.0] - 2026-09-20

### 📏 IME & Gesture Bar Spacing Control (කීබෝඩ් සහ Gesture Bar පරතරය ලං / ඈත් කිරීමේ පහසුකම)
- **Dynamic IME Space Adjuster**: Added an interactive quick adjuster toolbar button (`ime_space`) and in-keyboard slider bar (`ImeSpaceAdjusterBar`) to smoothly adjust the gap between the navigation gesture bar and the keyboard.
- **Quick Preset Controls**: Easily toggle between 0 dp (flush / ගෑවෙන්නම ලං කරන්න), 8 dp, 16 dp (default / සාමාන්‍ය), 24 dp, 36 dp, up to 48 dp (උපරිම ඈත් කරන්න) with immediate live updates and persistent preferences.
- **Settings Integration**: Added bottom space configuration directly into the "Top Bar & Toolbar" settings panel.

### 🎛️ Clean Modular Separation of Themes & Tools (තීම්ස් බාර් සහ ටූල්බාර් වෙන් කිරීම)
- **Theme Bar Purging**: Strictly separated theme settings and tool items. "Themes & Appearance" now exclusively contains visual styling, key spacing, height, contrast, translucent keyboards, key-press particle effects, and dynamic RGB lighting.
- **Tools Reorganization**: Moved non-theme utilities (Font Studio, Top Bar Customization, Spatial Decoder) to "Top Bar & Toolbar" and "Typing" settings.
- **Toolbar Quick-Theme Shortcut**: Added an instant Themes shortcut button (`ic_palette`) directly on the top suggestion rail and within Top Bar Customization.

### ⌨️ Bottom Row Layout & Square Key Symmetry (කමාව, තිත සහ භාෂා අයිකන සමචතුරස්‍ර හැඩය)
- **Ergonomic Key Reordering**: Reordered bottom-row keys so the Language switch icon (`🌐 / LANG`) is conveniently positioned next to the Comma key, and the Comma key is placed right beside the Global icon.
- **Uniform Square Keys**: Standardized both Comma (`,`) and Period (`.`) keycaps into equal square bounds (`KeyboardGeometry.PERIOD`), providing balanced visual rhythm and effortless touch precision.

---

## [2.4.0] - 2026-09-20

### 🚀 Performance & UI Responsiveness (වේගවත් බව සහ සුමට බව)
- **Zero-Lag Keyboard Launch**: Replaced synchronous icon bitmap decoding in `AppAccentResolver` with an instantaneous 0ms deterministic Material color derivation algorithm. The keyboard now opens immediately without freezing or dropping frames on any third-party app.
- **Single-Pass View Configuration**: Streamlined `onStartInputView` life-cycle flow, eliminating duplicate `applyTheme()` and `render()` passes on layout shifts.

### 🎨 Visual & Icon Design (නව App Icon නිර්මාණය)
- **Adaptive Icon Safe-Zone Compliance**: Redesigned `ic_launcher_background.xml` with a neon cyan and deep violet gradient ring positioned safely within the standard 72dp Android adaptive icon boundary.
- **Visual Depth & Polish**: Added ambient radial glow, micro-accent corner notches, and diagonal flare lines for a modern, sleek aesthetic.

### 🔤 Transliteration & Phonetic Engine (සිංග්ලිෂ් අක්ෂර පරිවර්තන වැඩිදියුණු කිරීම්)
- **Rakaransaya + Vowel Combinations**: Fully resolved complex Rakaransaya + vowel syllables (e.g., `dro` $\rightarrow$ **ඩ්‍රො**, `kra` $\rightarrow$ **ක්‍ර**, `kro` $\rightarrow$ **ක්‍රො**, `kya` $\rightarrow$ **ක්‍ය**).
- **Phonetic Conjuncts**: Verified accurate nasal consonant mappings (`ng` $\rightarrow$ `න්ග්`, `nga` $\rightarrow$ `න්ග`).

### ✍️ Typing Accuracy & Text Interleaving Fixes (Cursor Jump සහ අකුරු පැටලීම් විසඳීම)
- **Resolved "ගෙගියාදර" Issue**: Synchronized `localRecentText` buffer update and fixed selection-range evaluation in `onUpdateSelection`, ensuring sentences like *"මං ගෙදර ගියා"* type smoothly without the cursor jumping back or interleaving text.
- **Calibrated Space Gestures**: Increased horizontal space-drag threshold from 12dp to 36dp and reduced space bar touch-stealing (`SPACE_STEAL` from 0.28 to 0.10), completely eliminating unintended cursor movement during rapid space tapping.

### 🪄 Sinhala Pillam Auto-Corrector (පිල්ලම් ස්වයංක්‍රීයව නිවැරදි කිරීම)
- **Double-Vowel Sign Upgrade**: Fast double tapping of vowel signs automatically upgrades them to their elongated forms (`ි` + `ි` $\rightarrow$ `ී`, `ු` + `ු` $\rightarrow$ `ූ`, `ැ` + `ැ` $\rightarrow$ `ෑ`).
- **Conflicting Sign Replacement**: Typing a new vowel sign over an existing one automatically replaces it cleanly (e.g., `කා` + `ි` $\rightarrow$ `කි`) avoiding illegal stacked glyphs.

### 🛡️ System Compatibility & Crash Protection (ස්ථාවරත්වය)
- **OEM Window Decor Safeguards**: Protected translucent window and inputArea manipulations against manufacturer-specific window manager exceptions (MIUI, OneUI, ColorOS).

---

## [2.3.0] - 2026-09-18
- Added Live Unit Converter in suggestion bar (currency, length, temperature conversions).
- Added Custom Font Studio with local TTF typography support.
- Added Theme Creator with custom background opacity, colors, and gradients.
- Introduced App-Specific Layout Memory and Smart URL / Email keys.
- Enhanced WhatsApp/iOS style emoji selector with fast Sinhala search.

---

## [2.2.0] - 2026-09-10
- Instant In-Keyboard Sinhala ⇄ English translation bar.
- On-device smart word learning and next-word prediction engine.
- Smart clipboard manager with pin/unpin functionality.
- Offline & Private architecture with zero internet permissions.
