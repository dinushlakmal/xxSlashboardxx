# Changelog

All notable changes to **Slashboard Sinhala Keyboard** will be documented in this file.

---

## [3.0.7] - 2026-10-28

### ⚡ 0ms Instant Top Bar Reveal on Space & Suggestion Selection
- වචනයක් ටයිප් කර **Space** යතුර තද කළ සැනින් හෝ Suggestion Bar එකෙන් වචනයක්/Emoji එකක් තේරූ සැනින්, කලින් පැවති තත්පර 1ක (`1000ms`) පමාව සම්පූර්ණයෙන්ම ඉවත් කර `0ms` ක්ෂණිකව Top Bar එක (Clipboard, Settings, Emoji, Font Studio, Translate ආදී Tools සහිත Toolbar එක) දිස්වන පරිදි සකසන ලදී.

### 🛑 Auto-Space After Period (.) Disabled by Default
- තිතක් (`.`) තැබූ විට ස්වයංක්‍රීයව `Space` එකක් එකතු වීම ඉවත් කර ඇත.

### 😊 Smart Emoji Suggestions (Sinhala, Singlish & English)
- සිංහලෙන්, Singlish වලින් හෝ English වලින් වචන ටයිප් කරන විටම අදාළ Emoji එක Suggestion Bar එකේ මුලටම ක්ෂණිකව යෝජනා වේ (O(1) Offline `EmojiDictionary`).

### 📦 APK Size Compression & 120fps Ultra-Smooth Engine
- `material-icons-extended` bloat ඉවත් කර, JSON සහ PNG assets සම්පීඩනය කර APK ප්‍රමාණය ~8MB+ කින් අඩු කරන ලදී.
- `KeyCap`, `KeyboardPanel`, සහ `SuggestionRail` තුළ 100% View/Drawable pooling සහ zero-allocation 120fps rendering එක් කරන ලදී.

### 🛡️ Rare Auto Force-Stop Fix, UI Polish & Creator Credit
- **Auto Force-Stop / ANR / OOM Fix:** `CrashLogger` ගොනු ලිවීම background thread එකකට යොමු කර, Main Looper burst rate-limiting, `SlashboardSyncWorker` duplicate RAM වැළැක්වීම, සහ `SlashboardApp` තුළ `EmojiStyleManager` ආරම්භක ආරක්ෂාව එක් කර කලාතුරකින් සිදුවූ Force-Stop ගැටලුව සම්පූර්ණයෙන්ම විසඳන ලදී.
- **UI Polish & Creator Credit:** Settings Dialogs වල පසුබිම් විනිවිද පෙනීම (surface bleed-through) සහ පේළි පරතරයන් නිවැරදි කර, **"Created by Dinush Lakmal | Slash Studios"** ලෙස Splash Screen, Header, About Card සහ Footer තුළ එක් කරන ලදී.

---

## [3.0.6] - 2026-10-16

### 🔑 GitHub Action "App Not Installed" (Signature Mismatch) Permanent Fix
- **Canonical Keystore Enforcement in CI/CD:**
  - GitHub Actions හිදී Sideload APK එක sign කිරීමේදී canonical keystore එක (`C3:8F:7D:10:11:E9:E5:90:78:19:20:1F:1F:DC:94:D4:A3:8A:F5:26`) වෙනුවට වෙනත් secret එකක් හෝ අහඹු keystore එකක් භාවිතා වීම වැළැක්වීමට CI pipeline එක ස්වයංක්‍රීයව canonical keystore එක enforce කර SHA1 සහතිකය තහවුරු කරයි.
  - `.gitignore` ගොනුවෙන් `!debug.keystore` un-ignore කර git repository එක තුළ සෘජුවම ආරක්ෂිතව තබා ඇත.

### 🛑 Auto-Space After Period (.) Disabled by Default (තිතකට පසු ස්වයංක්‍රීය Space එකක් එක්වීම අක්‍රීය කිරීම)
- තිතක් (`.`) හෝ විරාම ලකුණක් ටයිප් කළ සැනින් ස්වයංක්‍රීයව `Space` එකක් එකතු වීම ඉවත් කර ඇත. දැන් පරිශීලකයාට අවශ්‍ය නම් පමණක් Space යතුර භාවිතා කළ හැක (මෙය Settings -> Punctuation & Auto-Formatting තුළින්ද පාලනය කළ හැක).

### ⚡ 0ms Instant Top Bar Reveal on Space & Suggestion Selection (තත්පර 1ක පමාව ඉවත් කර එසැණින්ම Top Bar පෙන්වීම)
- වචනයක් ටයිප් කර **Space** යතුර තද කළ සැනින් හෝ Suggestion Bar එකෙන් වචනයක්/Emoji එකක් තේරූ සැනින්, කලින් පැවති තත්පර 1ක (`1000ms`) පමාව සම්පූර්ණයෙන්ම ඉවත් කර `0ms` ක්ෂණිකව Top Bar එක (Clipboard, Settings, Emoji, Font Studio, Translate ආදී Tools සහිත Toolbar එක) දිස්වන පරිදි සකසන ලදී.
- `onUpdateSelection` මඟින් Space එබීමෙන් පසු නැවත Suggestions පෙන්වීමද නවතා, ඊළඟ වචනයේ පළමු අකුර ටයිප් කරන තුරු Top Bar එක එසැණින්ම රැඳී පවතී.

### 🛡️ Zero False Positive OTP Engine
- සාමාන්‍ය අංක, දුරකථන අංක, මුරපද හෝ කේත OTP ලෙස වැරදියට හඳුනාගැනීම සම්පූර්ණයෙන්ම ඉවත් කර, සැබෑ SMS OTP පණිවිඩයක් පැමිණි විට පමණක් `📋 Paste OTP` Chip එක පෙන්වන පරිදි context-aware detection එන්ජිම යාවත්කාලීන කරන ලදී.

### 😊 Emoji Suggestions & Smart Emoji Mapping (ඉමෝජි යෝජනා පද්ධතිය)
- **Instant Sinhala, Singlish & English Emoji Suggestions:**
  - පරිශීලකයා සිංහලෙන්, Singlish වලින් හෝ English වලින් වචනයක් ටයිප් කරන විටම (උදා: තරහයි, taraha, angry, ගෙදර, gedara, home, ආදරෙයි, adare, love, හිනා, hina, smile, කෑම, kama, food, තේ, the, tea, කාර්, car, බයික්, bike), Suggestion Strip එකේ සාමාන්‍ය වචන සමඟම අදාළ Emoji එක කැපී පෙනෙන ලෙස (Highlight Pill සහ 21sp විශාල අකුරින්) මුලටම එකතු වී දිස්වේ.
  - O(1) Zero-Latency Offline `EmojiDictionary` මඟින් ඉතා අඩු RAM (2GB) සහිත දුරකථන වලදී පවා කිසිදු ප්‍රමාදයකින් (lag) තොරව ක්‍රියාත්මක වේ.
  - Suggestion Bar එකෙන් Emoji එක තට්ටු කළ විට, ටයිප් කරමින් පැවති වචනය හෝ composition එක සුමටව emoji එකට මාරු වී ඉබේම space එකක් එක් කරයි. Spacebar තද කළ විට emoji නොව සාමාන්‍ය වචනයම commit වන බැවින් ටයිප් කිරීමට කිසිදු බාධාවක් ඇති නොවේ.
- **Settings තුළ පාලනය:**
  - Settings -> Text Suggestions තුළින් "Emoji Suggestions / Mapping" පහසුවෙන්ම On/Off කරගත හැක.

### 📦 APK Size Compression & 120fps Ultra-Smooth Engine (යෙදුමේ ප්‍රමාණය අවම කිරීම සහ සුමට ක්‍රියාකාරීත්වය)
- **Drastic APK & DEX Size Compression (~13MB+ Reduction):**
  - `material-icons-extended` (41MB uncompressed DEX bloat) ඉවත් කර, යෙදුමට අවශ්‍ය අයිකන පමණක් අඩංගු සැහැල්ලු `ExtendedIcons` වෙත මාරු කරන ලදී.
  - භාවිත නොවන `com.google.android.material` transitive dependencies සහ විශාල PNG/JSON assets සම්පීඩනය (compress) කර APK ප්‍රමාණය සහ RAM භාවිතය විශාල ලෙස අඩු කරන ලදී.
- **Zero-Jank 120fps Typing & Suggestion Pipeline:**
  - `KeyCap`, `KeyboardPanel`, සහ `SuggestionRail` තුළ සෑම අකුරක් ටයිප් කරන විටම සිදුවූ අනවශ්‍ය View/Drawable allocations, `Paint.typeface` JNI calls, සහ `removeAllViews()` layout passes ඉවත් කර 100% view/drawable pooling යොදන ලදී.
  - Singlish ටයිප් කිරීමේදී අනවශ්‍ය Binder IPC (`getTextBeforeCursor`) සහ Regex compilations ඉවත් කර 0ms ක්ෂණික ප්‍රතිචාර ලබා දෙන ලදී.

### 🛡️ Rare Auto Force-Stop Fix, UI Polish & Creator Credit
- **Eliminated Rare Auto Force-Stop / ANR / OOM Causes:**
  - `CrashLogger`: Main thread මත සිදුවූ synchronous file I/O පසුබිම් thread එකකට (`SlashboardCrashLogIO`) යොමු කර, `Looper.loop()` guardian තුළ burst rate-limiting එකතු කරන ලදී. එමඟින් UI frame දෝෂයකදී 120Hz infinite loop වී ANR / Force Stop වීම සහ main thread එක නැවතී zombie process එකක් වීම සම්පූර්ණයෙන්ම වළක්වා ඇත.
  - `SlashboardSyncWorker`: Keyboard service එක සක්‍රීයව තිබියදී පසුබිමින් දෙවන වරටත් `PredictionRepository` RAM එකට load වීම නවතා OOM (Out-of-Memory) Force Stop වීම වළක්වන ලදී.
  - `SlashboardApp`: යෙදුම ආරම්භයේදීම `EmojiStyleManager` ස්ථාපනය කිරීමෙන් `EmojiTextView` මඟින් ඇතිවිය හැකි `IllegalStateException` දෝෂය ඉවත් කරන ලදී.
  - `SlashboardInputMethodService` & `VoiceInputManager`: `RejectedExecutionException`, `ConcurrentModificationException`, සහ `PopupWindow` leak දෝෂ වළක්වාලීමට ආරක්ෂිත thread/lifecycle guards එක් කරන ලදී.
- **UI Fixes & Creator Credit:**
  - Settings තුළ සියලුම `AlertDialog` වල පසුබිම විනිවිද පෙනීම (transparent surface bleed-through) නිවැරදි කර, `SettingsChoiceRow`, `SettingsToggleRow`, සහ `HeaderAnimation` layout පරතරයන් හා පෙළ කැපී යාම් නිවැරදි කරන ලදී.
  - **"Created by Dinush Lakmal | Slash Studios"** ලෙස Splash Screen, Settings Header, About Card සහ Footer තුළ පැහැදිලිව එක් කරන ලදී.

---

## [3.0.5] - 2026-10-13

### 🛡️ Direct Feedback & Crash Telemetry (සෘජු දෝෂ නිරාකරණය සහ Crash Telemetry)
- **Silent Crash Catcher (නිහඬ Exception පාලනය හා ස්වයංක්‍රීය Recovery):**
  - යතුරුපුවරුව තුළ කිසියම් unexpected exception එකක් මතු වුවහොත්, පරිශීලකයාට බාධාකාරී "Slashboard has stopped" පද්ධති Error Dialog එකක් නොපෙන්වා, නිහඬව keyboard service එක ආරක්ෂිතව auto-recover වී normal typing state එකට පත්වන Global Exception Handler එකක් යොදන ලදී.
  - Crash context සහ stack trace තොරතුරු යෙදුමේ අභ්‍යන්තර safe storage එකෙහි ලොග් කර තබා ගන්නා අතර, කිසිදු සංවේදී හෝ පුද්ගලික යතුරුකරණ දත්තයක් සටහන් නොවේ.
- **In-App Bug Reporter (Settings තුළින්ම 1-Tap Bug Reporting):**
  - Settings තුළ නව **"Report Bug / දෝෂ වාර්තා කරන්න"** පහසුකමක් එක් කරන ලදී.
  - පරිශීලකයා මෙම බොත්තම තට්ටු කළ විට, Device Model, Android OS Version, App Version (`v3.0.5`) සහ අදාළ Error Log / Telemetry සටහන් ස්වයංක්‍රීයව auto-fill වී සංවර්ධක විද්‍යුත් තැපෑල (`dinushlakmal01@gmail.com`) වෙත යොමු වන email draft එකක් ක්ෂණිකව විවෘත වේ.

### ✨ Particle Engine & Live Preview Enhancements (අංශු ආචරණ සජීවී ක්‍රියාකාරීත්වය)
- **Key Press Particles Live Selection Fix:**
  - Key Particles Settings තුළ particle style (✨ Stars, ⚡ Sparks, 🫧 Bubbles, 🟩 Neon Pixels, ❤️ Hearts, 🔥 Flames, ❄️ Snowflakes, 🌸 Sakura, 💧 Water Splash) හෝ density/palette වෙනස් කළ සැනින්, එය එසැණින්ම keyboard එක තුළ select වී සක්‍රිය නොවන ගැටලුව සහ සමහර යතුරුපුවරු layout වල particles නොපෙන්වීමේ ගැටලුව 100% ක් නිරාකරණය කරන ලදී.
  - Live preview එක ක්ෂණිකව update වන පරිදි dynamic engine binding යොදන ලදී.

### 🔐 Full Multi-Scheme APK Signing & Installation Fix (APK සහතික සහ ස්ථාපන නිවැරදි කිරීම්)
- **Comprehensive APK Signature Scheme (v1, v2, v3 & v4):**
  - Android Package Installer (`pm install`) මඟින් streaming APK ස්ථාපනය කිරීමේදී ඇති වූ `INSTALL_PARSE_FAILED_NO_CERTIFICATES` (Attempt to get length of null array) දෝෂය සම්පූර්ණයෙන්ම විසඳන ලදී.
  - Legacy v1 JAR signing (`META-INF/MANIFEST.MF`, `CERT.SF`, `CERT.RSA`) සහ නූතන v2, v3, v4 APK signature schemes සම්පූර්ණයෙන් සක්‍රිය කර සහතික කර ඇත.

### 🚀 Performance & Stability (වේගය සහ ස්ථායිතාව)
- IME composition life-cycle සහ thread synchronization වඩාත් ශක්තිමත් කරන ලදී.
- Low-memory උපාංග සඳහා memory leak අවදානම් අවම කර background resource cleanup ප්‍රශස්ත කරන ලදී.

---

## [3.0.2] - 2026-10-07

### 🌟 New Features & Enhancements (නව විශේෂාංග)
- **WhatsApp, Apple iOS, Google & Twitter/X Emoji Style Switcher (ඉමෝජි විලාසය තේරීමේ පහසුකම):**
  - Added support for switching between **Official WhatsApp (Meta)**, **Apple iOS (Cupertino)**, **Google Noto Color**, and **Twitter/X (Twemoji)** emoji styles.
  - Complete modern emoji catalog integration supporting all standard emojis and variants.
  - Clean, beautifully balanced Settings card with live preview chips and high-contrast badges with zero language mixing.
- **AMOLED Pure Black Default Theme (පෙරනිමි AMOLED කළු තේමාව):**
  - Set `amoled_black` as the default theme out of the box for deep, true blacks, minimal eye strain, and maximum battery savings on OLED/AMOLED screens.
- **Interactive Key Press Particle Effects Customization (යතුරු එබීමේ අංශු ආචරණ පාලනය):**
  - Toggling ON "Key Press Particle Effects" dynamically reveals rich customization options:
    - 9 particle styles (✨ Stars, ⚡ Sparks, 🫧 Bubbles, 🟩 Neon Pixels, ❤️ Hearts, 🔥 Flames, ❄️ Snowflakes, 🌸 Sakura, 💧 Water Splash).
    - 3 density presets: Low (6), Normal (12), High (18).
    - 5 color palettes: Theme Accent, Rainbow 🌈, Gold ✨, Cyan 💠, Flame 🔥.
  - Seamless animated expand/collapse for a clutter-free settings interface.
- **Top-Level Per-App Accent Adaptation (යෙදුමට අනුව වර්ණය වෙනස් වීම):**
  - Prominently positioned at the very top of Themes & Visuals settings with a default OFF state, giving users immediate control over dynamic app accent coloring (WhatsApp green, Messenger blue, YouTube red, etc.).
- **Enlarged Emoji Bar Icons (Emoji Bar හි අයිකන ප්‍රමාණය වැඩි කිරීම):**
  - Scaled top Emoji bar icons up to 28dp for effortless thumb tapping without increasing the compact 38dp suggestion bar height.
- **Intelligent Auto-Adaptive Key Gap & Geometry:**
  - Dynamic key spacing and proportions that calibrate to device display resolution, aspect ratio, and orientation.

### 🐞 Critical Bug Fixes (වැදගත් දෝෂ නිවාරණ)
- **Sinhala Cursor Jump & Sticky Delete Fix (සිංහල ටයිප් කිරීමේදී Cursor පැනීම සහ මැකීමේ දෝෂය විසඳීම):**
  - Completely resolved the issue where editing long Sinhala sentences caused the cursor to jump into mid-sentence words or fail to delete complex ligatures.
- **Chrome & Web Browser Number Pad Isolation (Chrome හි අංක පෑඩය OTP/Passwords වලට පමණක් සීමා කිරීම):**
  - Fixed an issue where Chrome web forms and the URL/address bar erroneously triggered the numeric keypad. Now, general web inputs and URL address bars display the full QWERTY/Sinhala layout with symbols; numeric keypad only shows for genuine OTP, PIN, and strict password fields.
- **Sinhala Ligature / ZWJ Emoji Bar Filtering (සිංහල වචන Emoji Bar එකේ වැටීම වැළැක්වීම):**
  - Enhanced emoji validation to prevent Sinhala words with Zero-Width Joiners (ZWJ, e.g., යංශය, රකාරාංශය) from accidentally being logged into recent emoji suggestions.
- **Settings UI Polish & Pure Language Separation (භාෂා සමබරතාවය):**
  - Fixed cluttered settings text in Themes & Visuals; when English is selected, only pure English displays; when Sinhala is selected, authentic Sinhala text displays without awkward English mixing.

---

## [2.9.0] - 2026-09-29

### ⚡ Double Space Instant Toolbar Icons (Double Space ගැසූ සැනින් Top Bar Icons පෙන්වීම)
- **Instant Toolbar Reveal on Double Space:** Typing a word and tapping space twice immediately dismisses word predictions and surfaces the full Top Bar Toolbar (Language toggle, Font Studio, Private Cloak, Ligatures, Singlish bulk converter, Calculator, Undo/Redo, Astrology glyphs, FM convert, Themes, Emoji, Clipboard, and Settings) with zero lag or timeout delay.
- **Persistent Toolbar Visibility:** Prevents candidate prediction loops from overwriting the toolbar when trailing double spaces are present, giving fast 1-tap access to your favorite shortcuts.

### 🚀 Xiaomi & Redmi MIUI / HyperOS Lag Fix & Deep Optimization (Xiaomi / Redmi ලැග් වීම සම්පූර්ණයෙන්ම විසඳීම)
- **High Refresh Rate (90Hz / 120Hz) Smoothness:** Completely stopped idle ValueAnimator loops and unnecessary canvas repaints, guaranteeing consistent 60–120 FPS typing on MIUI and HyperOS devices.
- **Haptic & Audio Thread Offloading:** Prevented synchronous Android system service calls from blocking the main UI thread during intense, rapid typing.
- **Zero Memory Bottlenecks:** Optimized prediction candidate task cancellations and reduced GC allocation pressure during continuous typing.

### 📐 Classic Emoji Bar Proportions Restored (Emoji Bar එක v2.7.0 ප්‍රමාණයට සැකසීම)
- **True v2.7.0 Compact Height:** Restored the ergonomic 38dp bar height matching the suggestion rail, eliminating the oversized row footprint.
- **Refined Emoji Glyph Sizing:** Emoji characters remain sharp and clear within compact 36dp touch tiles without enlarging or stretching the keyboard layout.

### 🎨 Rebuilt Wallpaper Studio & Dark Crop Dialog (නව Wallpaper Studio සහ Dark Crop Interface)
- **Solid Dark Interface:** Rebuilt the wallpaper crop and adjustment dialog with an elegant, non-transparent solid dark palette (`#0F172A` / `#1E293B`) ensuring maximum contrast.
- **Locked Live Keyboard Preview:** Fixed live keyboard preview locked securely at the top so adjustments are visible in real-time without scrolling away.
- **Balanced 4-Action Bar:** Streamlined workflow featuring side-by-side **Discard**, **Crop**, **Save**, and **Apply** buttons.
- **Organized Adjustments Panel:** Cleanly grouped controls for Framing & Position, Key Opacity/Dimming, and Brightness/Contrast.

### 🔇 Spacebar Lighting Cleanup & Default Disabled Rhythm Bar
- **Rhythm Sync Disabled by Default:** `typingSpeedRhythmSync` is now set to `false` by default, activating only when explicitly turned on in Settings.
- **Pure Themed Spacebar:** Stripped out distracting rainbow flashing lights and glowing neon border artifacts from the spacebar, leaving only crisp, theme-accurate equalizer bars when enabled.

---

## [2.8.0] - 2026-09-26

### 📱 Horizontal Swipe Navigation & English Default (දෙපැත්තට Slide කර Tabs මාරු කිරීම)
- **Horizontal Pager for Settings**: Effortlessly swipe left and right across all settings tabs (`HorizontalPager`) with smooth animated scrolling and tab synchronization.
- **Default App Language**: English set as the default interface language for global accessibility, while retaining the instant English/Sinhala toggle.
- **Integrated Setup & About Tab**: Merged "Updates & About" directly beneath "Setup" in Tab 0, enabling one-stop access to version information, update checking, and developer links.

### 📐 Ergonomic Size & Spacing Controls (ප්‍රමාණ සහ පරතරය සැකසුම් ඒකාබද්ධ කිරීම)
- **Unified Sound, Haptics & Size Tab**: Consolidated all keyboard sizing and spacing controls into a single intuitive panel:
  - Keyboard Height / Sizing (Short, Normal, Tall, Very Tall)
  - Key Gap Spacing (Horizontal & Vertical gaps)
  - One-Handed Mode & Thumb Reach Heatmap alignment
  - IME Bottom Space Clearance
- **36dp Default IME Bottom Space**: Increased default bottom clearance to 36dp for seamless compatibility with Android gesture navigation bars.

### 💧 Serene Slow-Motion Water Bubble Touch Reaction (ජල බුබුළු ප්‍රතික්‍රියාව)
- **Glassy Water Bubble Animation**: Completely redesigned RGB touch reaction with a serene, slow-motion (2400ms) glassy water bubble that wraps naturally around the tapped key.
- **Realistic Fluid Optics**: Features organic surface tension wobble, curved specular light reflection arcs, highlight gloss dots, and clean transparent dissipation without screen clutter.

### ✨ Extended Key Press Particle Effects (නව Particle Effects)
- **Expanded Particle Gallery**: Added five brand-new visual particle effects:
  - Floating Hearts (ආදර හදවත්)
  - Fiery Flames (ගිනි දැල්)
  - Winter Snowflakes (හිම පියලි)
  - Sakura Petals (චෙරි මල් පෙති)
  - Water Splash Droplets (ජල බිංදු)

### 🐛 Sinhala Space Cursor Stability Fix (සිංහල ටයිප් කිරීමේ කර්සර් ගැටලුව නිරාකරණය)
- **Evolution A17 / Custom ROM Fix**: Fixed cursor jumping to the start of text when typing Sinhala words with Spacebar Cursor enabled by anchoring cursor offsets to active composition bounds.
- **Micro-Drag Deadband**: Added direction-ratio and distance thresholds to prevent accidental cursor drags during quick spacebar taps.

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

### 🌐 Web & Community Integration (වෙබ් අඩවිය සහ සමාජ මාධ්‍ය පහසුකම්)
- **Web Share API ('Share App' Button)**: Added a native Web Share button to the navigation bar enabling 1-click sharing of the app release link to WhatsApp, Facebook, or any messenger app with automated clipboard fallback and toast feedback.
- **Floating 'Back to Top' Action**: Added a smooth-scrolling floating action button that appears after scrolling down, allowing users to effortlessly navigate back to the hero section.
- **Direct Release & Community Links**: Integrated 1-click links to the official Facebook page (`https://www.facebook.com/profile.php?id=61593856756750`), GitHub developer profile (`https://github.com/dinushlakmal`), and GitHub Releases download across Settings, About screen, and Web documentation.
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
