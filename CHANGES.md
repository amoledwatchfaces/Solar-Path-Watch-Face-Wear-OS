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

2. **Interactive Action Buttons via Non-Customizable Slots:**
   WFF allows a maximum of 8 complication slots. Slots with `isCustomizable="FALSE"` remain hidden from user customization menus while still receiving input and rendering data:
   - **"Open App" Button (Slot 0):**
     - Sized with a `320×80` `<BoundingRoundBox>` positioned directly over the "Open App" pill graphic.
     - Automatically active when `showPrompt == true` via `SolarPathComplicationService`'s `tapAction`.
   - **"Dismiss" Button (Slot 6):**
     - Sized with a `210×62` `<BoundingRoundBox>` placed over the "Dismiss" pill graphic.
     - Powered by `DismissPromptComplicationService`. Tapping fires a `PendingIntent` to `DismissPromptActivity`.
     - `DismissPromptActivity` commits `isLocationPromptDismissed = true` to `UserPreferences` and calls `ContextUtils.updateComplications()`. This toggles `hasLocation` back to `1`, immediately transitioning the watch face into normal mode.

3. **Touch Bleed Prevention & Document-Order Shielding (Slot 7):**
   - **The Problem:** In Wear OS, setting `alpha="0"` on hidden complication slots (Slots 1–5) hides them visually, but their bounding boxes still capture touch events, causing accidental app launches underneath the warning prompt.
   - **Document-Order Z-Stacking:** In WFF, elements declared later in the XML sit on top of earlier elements for both rendering and hit-testing:
     ```
     ┌─────────────────────────────────────────────────────────┐
     │  Top Layer:    Slot 0 ("Open App") & Slot 6 ("Dismiss")  │
     ├─────────────────────────────────────────────────────────┤
     │  Shield Layer: Slot 7 (450×450 Full-Screen Blocker)     │
     ├─────────────────────────────────────────────────────────┤
     │  Bottom Layer: Slots 1–4 (Arcs) & Slot 5 (Center)       │
     └─────────────────────────────────────────────────────────┘
     ```
   - **Dynamic Shielding Mechanism:**
     - **`BlockerBroadcastReceiver`:** A lightweight, no-op `BroadcastReceiver` that safely absorbs touches.
     - **`BlockerComplicationService` (Slot 7):**
       - When `showPrompt == true`: Provides a `tapAction` targeting `BlockerBroadcastReceiver`. Taps on "Open App" or "Dismiss" hit the top layer (Slots 0 and 6). Taps anywhere else hit the full-screen Slot 7 shield and are absorbed by the receiver—never reaching Slots 1–5 beneath it.
       - When `showPrompt == false`: Sets `tapAction = null`. In Wear OS, complication slots with null tap actions do not intercept touches, allowing taps to fall straight through to Slots 1–5 unimpeded.

4. **Lock Screen / Keyguard Guard:**
   - Evaluates `KeyguardManager.isDeviceLocked` and `KeyguardManager.isKeyguardLocked`.
   - If the watch is locked (e.g., off-wrist lock or PIN screen), `showPrompt` is forced to `false`. The face bypasses the prompt and renders fallback/dummy data, avoiding intrusive UI popups on the lock screen.

5. **WFF 8-Slot Allocation Budget:**
   The implementation takes full, optimal advantage of the WFF 8-slot ceiling without exceeding limits:
   - `Slot 1`: Top-Left Arc (Customizable)
   - `Slot 2`: Top-Right Arc (Customizable)
   - `Slot 3`: Bottom-Right Arc (Customizable)
   - `Slot 4`: Bottom-Left Arc (Customizable)
   - `Slot 5`: Center Shortcut (Customizable)
   - `Slot 0`: Open App Button & Solar/Reference Provider (Non-customizable)
   - `Slot 6`: Dismiss Button (Non-customizable)
   - `Slot 7`: Full-Screen Touch Blocker (Non-customizable)

### 6. Architectural & System Updates
- Registered `DismissPromptActivity`, `DismissPromptComplicationService`, `BlockerComplicationService`, and `BlockerBroadcastReceiver` in `AndroidManifest.xml`.
- Extended `ContextUtils.updateComplications()` to ensure all custom complication services update in synchronization.
- Bumped project version to `v1.1.3` (version code `10000013`) in `build.gradle.kts`.
