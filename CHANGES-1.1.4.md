# Changes in v1.1.4

## Version Information
- **Version Name:** 1.1.4
- **Version Code:** 10000014

---

## Summary of Changes

### 1. Configuration App Layout & Navigation Refinement

- **Removed "Set Watch Face" Button:**
  - Removed the push watch face button and associated active watch face state monitoring from the top of `MainScreen`.
- **Relocated FAQ to Top of About Section:**
  - Moved the FAQ button to the very top of the About section (immediately beneath the About section header) for better discoverability.
- **Swapped Privacy Policy & App Version:**
  - In the About section, reordered the entries so Privacy Policy precedes App Version (FAQ → Privacy Policy → Version).
- **Relocated Recalculate Button:**
  - Moved the Recalculate button directly beneath the Solar Times breakdown card, positioning it alongside the solar calculations it refreshes.
- **Prominent "Set Location" Warning Button:**
  - Added an outlined action button directly above the Solar Times section, rendered with a 2dp red outline (`#FFFF5252`), red text, and red icon.
  - Labeled **Set Location** and subtitled **No location set**.
  - Dynamically appears only when no location is configured (`preferences.locationName == "- -" || (preferences.latitude == 0.0 && preferences.longitude == 0.0)`).
  - Tapping opens the location selection menu (`location_choose`) with Search and Current Location options.
- **Updated Copyright Footer:**
  - Updated the bottom list footer to display the full copyright notice:
    ```
    Copyright (c) 2026
    amoledwatchfaces.com
    All rights reserved
    ```

### 2. Background Location Updates & Interval Adjustments

- **Renamed Button Label:**
  - Renamed the background location toggle to **Background Location Updates** (`R.string.background_location`).
- **30-Minute Interval Progression:**
  - Updated the update interval slider from 15-minute increments to 30-minute increments across the same 30–240 minute range (`30..240 step 30`).

### 3. Unified Location Access Architecture & Sequential Permission Requesting

- **Replaced Multi-Stage Permission Dialogs:**
  - Removed the obsolete multi-step `BackgroundPermissionDialog` and its separate rationale and instruction screens.
  - Replaced all secondary location permission entry points with the unified **Location Access** initialization disclosure (`InitialLocationDialog` in-app, `LocationDisabledScreen` on first run).
  - Updated `location_disclosure_message` to comprehensively detail both foreground calculation access and optional background location access, reminding users of the manual location fallback option.
- **Unified Trigger Flows:**
  - Tapping **Current Location** in `LocationChooseScreen` when location permissions are missing now presents the Location Access dialog.
  - Toggling **Background Location Updates** in `MainScreen` when background permission is missing now presents the Location Access dialog.
- **Sequential Dual-Level Permission Flow (`requestPermissionsSequentially`):**
  - Implemented sequential runtime permission dispatching in `MainActivity`:
    1. Prompts for approximate/foreground location permission (`ACCESS_COARSE_LOCATION`).
    2. Once foreground access is granted, immediately triggers the NOAA location query and sequentially requests background location permission (`ACCESS_BACKGROUND_LOCATION`).
    3. If background permission is granted, automatically activates background updates (`viewModel.setBackgroundLocation(true)`).
    4. Automatically pops `LocationChooseScreen` back to `MainScreen` upon successful location authorization.
  - Applied consistently whenever permissions are requested from the Location Access prompt, including first run (`LocationDisabledScreen`) and in-app requests (`InitialLocationDialog`).

### 4. Watch Face "Heads up!" Screen Layout Adjustment
- **Vertical Rebalance on Circular Displays:**
  - Shifted the Info Badge icon, "Heads up!" title, and the three subtitle lines down by 30px closer to the "TAP TO OPEN" text indicator (`watchface.xml`).
  - Eliminates excessive dead space above the tap-to-open action and perfectly centers the message card vertically within circular watch displays.

### 5. Build & Versioning
- Bumped project version to `v1.1.4` (version code `10000014`) in root `build.gradle.kts`.
- Archived `CHANGES.md` to `CHANGES-1.1.3.md`.
