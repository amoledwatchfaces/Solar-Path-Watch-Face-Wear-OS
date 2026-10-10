# Changes in v1.1.3

## Version Information
- **Version Name:** 1.1.3
- **Version Code:** 10000013

---

## Summary of Changes

### 1. Always-On Display (AOD): "Time Only" Option
- Added a new **Time Only** AOD style option (`aodStyle = 2`).
- In ambient mode under this setting, only the digital time remains visible—hiding complications, solar path rings, and secondary elements for maximum battery conservation and a minimalist ambient look.
- Added visual preview drawables for settings customization (`prev_aod_2.png`) and updated existing AOD previews (`prev_aod_0.png`, `prev_aod_1.png`).

### 2. Clock Sub-Text Customization
- Added a new user configuration option for customizable text beneath the central digital clock:
  1. **Day of Week** (e.g., `SATURDAY`)
  2. **AM/PM** indicator (e.g., `PM`)
  3. **Seconds** (e.g., `45`)
  4. **Weather** (Current condition & temperature, e.g., `72° SUNNY`)
- Integrated 14 weather condition vector drawables for weather state visualization (`ic_clear`, `ic_cloudy`, `ic_fog`, `ic_heavy_rain`, `ic_heavy_snow`, `ic_partly_cloudy_day`, `ic_partly_cloudy_night`, `ic_rain`, `ic_sleet`, `ic_snow`, `ic_sunny`, `ic_thunderstorm`, `ic_unknown`, `ic_windy`).
- Added preview graphics for each sub-text option in the watchface customization menu (`prev_clock_subtext_0.png` through `prev_clock_subtext_3.png`).

### 3. Battery-Efficient Smooth Seconds
- Replaced continuous millisecond-based expressions (`[SECOND_MILLISECOND]`) with an efficient, smooth per-second animation sweep.
- Eliminates unnecessary high-frequency display processor wakeups, reducing battery draw while maintaining fluid second hand motion.

### 4. Center Tap Action & Complication Slot Redesign
- **Configurable Center Shortcut (Slot 5):**
  - Converted the center clock / inner solar ring area into a dedicated, user-customizable complication slot (`slotId="5"`, labeled "Center Shortcut").
  - Configured with a `230×230` circular touch target (`<BoundingOval>`) contoured tight against the inner solar path ring.
  - Defaults to Wear OS App Shortcut (`APP_SHORTCUT`), allowing users to launch any app of their choosing directly from the center of the watch face.
- **Improved Arc Complication Bounding Boxes (Slots 1–4):**
  - Redefined and tightened `<BoundingRoundBox>` coordinates for the four corner arc complications (Top-Left, Top-Right, Bottom-Right, Bottom-Left).
  - Prevents target overlap and makes selecting and customizing individual slots on the watch's on-device complication editor significantly easier and more accurate.

### 5. "Heads Up!" Location Setup Screen & Touch Management Architecture

#### Overview & Goal
When location permissions have not yet been granted and no manual coordinates are set, the watch face displays a full-screen guidance screen directing users to the configuration app or allowing them to dismiss the prompt to use the face in fallback mode.

Because Google's **Watch Face Format (WFF)** is a declarative XML specification executed by the Wear OS system renderer (`dwf.receiver`), developers cannot run imperative Kotlin inside the watch face to dynamically create, destroy, or unbind touch targets. Accomplishing this required an innovative combination of WFF state references, non-customizable complication slots, and document-order touch shielding.

#### Detailed Implementation Architecture

1. **State Evaluation & Reactive Signal Pipeline:**
   - **Service Layer (`SolarPathComplicationService`):**
     Evaluates state during complication update requests:
     ```kotlin
     val isLocationSetup = (hasPermission && (latitude != 0.0 || longitude != 0.0 || locationName != "- -")) || hasConfiguredLocation
     val isLocked = keyguardManager?.isDeviceLocked == true || keyguardManager?.isKeyguardLocked == true
     val showPrompt = !isLocationSetup && !prefs.isLocationPromptDismissed && !isLocked
     ```
     When `showPrompt == true`, it outputs `Title = "NO_LOCATION"` and `Text = "SETUP"`, attaching a `tapAction` targeting `MainActivity`. When `showPrompt == false`, it outputs regular solar calculations and sets `tapAction = null`.
   - **WFF Signal Extraction (`watchface.xml`):**
     Inside Slot 0, an invisible 1×1 `<Group>` evaluates the complication data into a scene-wide reference named `hasLocation`:
     ```xml
     <Group name="ref_has_location" x="0" y="0" width="1" height="1" alpha="0">
         <Transform target="x" value="([COMPLICATION.TITLE] == &quot;NO_LOCATION&quot; || [COMPLICATION.TEXT] == &quot;SETUP&quot;) ? 0 : 1"/>
         <Reference name="hasLocation" source="x" defaultValue="1"/>
     </Group>
     ```
   - **Scene-Wide Visibility Switching:**
     - Normal Watch Face Elements (Clock, Arcs, Complications 1–5):
       `<Transform target="alpha" value="[REFERENCE.hasLocation] == 0 ? 0 : 255" />`
     - Heads Up! Layout (Warning icon, Title, Description, Button graphics):
       `<Transform target="alpha" value="[REFERENCE.hasLocation] == 0 ? 255 : 0" />`

2. **Full-Screen Tap Target & "TAP TO OPEN" UI (Slot 0):**
   - The Heads Up screen graphics have been streamlined to remove the individual pill buttons and display a prominent, elegant `"TAP TO OPEN"` call-to-action in the theme accent color.
   - Slot 0 (`SolarPathComplicationService`) is expanded to full-screen bounds (`450×450`) with `<BoundingBox x="0" y="0" width="450" height="450"/>`.
   - **When `showPrompt == true`:** Slot 0 attaches `openAppIntent()`. Because it spans the entire 450×450 display and sits above Slots 1–5 in document order, tapping anywhere on the screen cleanly launches `MainActivity`, with zero risk of accidental touches bleeding through to complications underneath.
   - **When `showPrompt == false`:** Slot 0 sets `tapAction = null`. In Wear OS, complication slots with null tap actions do not intercept touch events, allowing touches to pass through directly to Slots 1–5 underneath.
   - This eliminates the need for separate internal "Blocker" and "Dismiss" complication slots.

3. **Consolidated "Solar Path Shortcut" Complication:**
   - Instead of exposing internal helper services to other watch faces or system pickers, the app now exports a single, clean, user-friendly complication: **"Solar Path Shortcut"** (`SolarPathComplicationService`).
   - Supports all common Wear OS complication types (`SHORT_TEXT`, `MONOCHROMATIC_IMAGE`, `SMALL_IMAGE`, `LONG_TEXT`).
   - In all slot types, provides a simple monochromatic twilight icon (`wb_twilight_24px`) and opens the `MainActivity` app.
   - In `SHORT_TEXT` on the Solar Path watch face (Slot 0), also encodes NOAA solar calculation angles into the complication title to drive the 8 dynamic twilight arcs and provide next event times.

4. **In-App Location Warning Banner (`MainActivity` / `MainScreen`):**
   - When location permissions are not granted and the prompt has not been dismissed (`!areLocationPermissionsGranted() && !preferences.isLocationPromptDismissed`), a color-highlighted card appears at the very top of `MainScreen`.
   - Explains that location is disabled and the app works best with location permissions enabled to calculate accurate solar times.
   - Provides two dedicated action buttons:
     - **"Enable location":** Triggers the system runtime permission prompt (`ACCESS_COARSE_LOCATION`). When granted, automatically queries current coordinates and updates complications.
     - **"Use without location":** Commits `isLocationPromptDismissed = true` to DataStore and notifies `updateComplications()`. This immediately dismisses both the watch face Heads Up overlay and the in-app warning banner.

5. **Lock Screen / Keyguard Guard:**
   - Evaluates `KeyguardManager.isDeviceLocked` and `KeyguardManager.isKeyguardLocked`.
   - If the watch is locked (e.g., off-wrist lock or PIN screen), `showPrompt` is forced to `false`. The face bypasses the prompt and renders fallback/dummy data, avoiding intrusive UI popups on the lock screen.

6. **WFF Slot Allocation Budget:**
   With internal helper complications removed, the watch face operates well within WFF slot limits:
   - `Slot 1`: Top-Left Arc (Customizable)
   - `Slot 2`: Top-Right Arc (Customizable)
   - `Slot 3`: Bottom-Right Arc (Customizable)
   - `Slot 4`: Bottom-Left Arc (Customizable)
   - `Slot 5`: Center Shortcut (Customizable)
   - `Slot 0`: Full-Screen Tap Target & Solar/Reference Provider (Non-customizable)

### 7. Architectural & Cleanup Summary
- Deleted helper components `DismissPromptActivity` and `BlockerBroadcastReceiver` and removed their declarations from `AndroidManifest.xml`.
- Removed Complication Slots 6 and 7 from `watchface.xml`.
- Renamed complication provider to `"Solar Path Shortcut"` in `strings.xml`.
- Extended `SolarPathComplicationService` with preview and runtime data for `SHORT_TEXT`, `MONOCHROMATIC_IMAGE`, `SMALL_IMAGE`, and `LONG_TEXT`.
- Added location warning card with "Enable location" and "Use without location" actions to `MainScreen.kt` and `MainViewModel.kt`.
- Bumped project version to `v1.1.3` (version code `10000013`) in `build.gradle.kts`.
