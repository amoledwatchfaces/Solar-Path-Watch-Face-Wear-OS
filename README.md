# <img src="preview.png" width="48" style="border-radius: 50%;" align="center" alt="Icon"> Solar Path Watch Face for Wear OS

[![Build & Release](https://github.com/amoledwatchfaces/Solar-Path-Watch-Face-Wear-OS/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/amoledwatchfaces/Solar-Path-Watch-Face-Wear-OS/actions/workflows/build-and-release.yml)
[![Platform](https://img.shields.io/badge/Platform-Wear_OS_5%2B_(API_34%2B)-brightgreen?logo=android&logoColor=white)](https://developer.android.com/wear)
[![Format](https://img.shields.io/badge/Format-Watch_Face_Format_2-orange)](https://developer.android.com/training/wearables/wff)
[![Target SDK](https://img.shields.io/badge/Target_SDK-37-blue)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Compose-Material_3-blue?logo=jetpackcompose)](https://developer.android.com/training/wearables/compose)
[![Latest Release](https://img.shields.io/github/v/release/amoledwatchfaces/Solar-Path-Watch-Face-Wear-OS?logo=github&color=blue)](https://github.com/amoledwatchfaces/Solar-Path-Watch-Face-Wear-OS/releases)
[![Privacy Policy](https://img.shields.io/badge/Privacy--Policy-Read-blue?logo=googleplay&logoColor=white)](https://amoledwatchfaces.github.io/apps/privacy/solarpath.html)

**Solar Path** is an astronomically accurate 24-hour solar dial watch face and companion application for Wear OS, inspired by the iconic solar dial design. It dynamically tracks the sun's passage across the celestial vault and relative to your local horizon—illustrating daylight, twilight phases, and nighttime with mathematical precision.

Developed with ❤️ by **[amoledwatchfaces™](https://amoledwatchfaces.com/)**.

---

<p align="center">
  <img src="preview.png" width="340" alt="Solar Path Watch Face Preview" />
</p>

---

## ✨ Features

- **24-Hour Circular Solar Dial**:
  - Solar noon at zenith (top / 12 o'clock / 0°).
  - Solar midnight at nadir (bottom / 6 o'clock / 180°).
  - Real-time celestial sun marker orbiting along the 24-hour circumference with a radiating solar beam.
- **Accurate Twilight & Night Arcs**:
  - **Daylight**: Sun elevation > 0°
  - **Civil Twilight**: 0° to -6°
  - **Nautical Twilight**: -6° to -12°
  - **Astronomical Twilight**: -12° to -18°
  - **Night**: < -18°
- **Dynamic Lighting & Atmospheres**:
  - Day / night base disk with smooth alpha transitions based on solar elevation.
  - Default sky hue and sunrise/sunset orange hue gradients.
  - Subtle starlight field emerging across the upper quadrant after civil dusk.
- **Dual Clock Modes**:
  - **Analog Clock**: Sleek hour, minute, and second hands with hour indices and date window.
  - **Digital Clock**: Switchable neutral and colorful styles with device font scaling.
- **Watch Face Format (WFF v2)**:
  - Pure declarative XML (`hasCode = false`) ensuring maximum battery efficiency, hardware rendering, and compliance with Google Play Wear OS requirements.
  - Standalone WFF APK bundled inside the host companion app and pushed via **AndroidX Watch Face Push** (`androidx.wear.watchfacepush`).
- **Complication Slots**:
  - 2 customizable complication slots (Top and Bottom) supporting all standard Wear OS complication types.

---

## 🧩 Included Complication Service

Solar Path includes a dedicated complication provider that can be used on **Solar Path** itself or added to **any other Wear OS watch face**:

| Complication Service | Supported Types | Description |
|:---|:---|:---|
| **Solar Path Complication** | `SHORT_TEXT` | Displays the next upcoming solar event (e.g., `SUNRISE`, `SUNSET`, `SOLAR NOON`, `CIVIL DUSK`) with exact time and sun icon. Tapping opens the Solar Path app. Encodes astronomical angles for WFF dials. |

---

## 📱 Wear OS Companion App

The integrated Wear OS app provides rich configuration and astronomical insight right from your wrist:

- **Solar Times Overview**: Real-time cards displaying sunrise, sunset, dawn, dusk, solar noon, and nadir times.
- **Location Management**:
  - Automatic GPS positioning via `FusedLocationProviderClient`.
  - Manual city search via `Geocoder` with recent locations history (swipe-to-remove).
  - Periodic background location syncing via `WorkManager` and passive location listener.
- **One-Tap Watch Face Activation**:
  - Easily push and activate the bundled standalone WFF watch face via Watch Face Push.
  - Automatic in-place watch face binary updates whenever the app is updated via Google Play (`ACTION_MY_PACKAGE_REPLACED`).
- **Interactive FAQ & Education**: Built-in guide explaining the celestial mechanics of solar noon, twilight sectors, and horizon calculations.
- **About Section**: Quick access to app version details and privacy policy.

---

## 🛠️ Tech Stack & Architecture

- **Watch Face**: [Watch Face Format (WFF v2)](https://developer.android.com/training/wearables/wff) XML rendered natively by the Wear OS system.
- **Deployment**: [AndroidX Watch Face Push](https://developer.android.com/reference/androidx/wear/watchfacepush/package-summary) (`androidx.wear.watchfacepush:1.0.0`).
- **UI & Presentation**: [Jetpack Compose for Wear OS](https://developer.android.com/training/wearables/compose) using **Wear Material 3** design guidelines.
- **Astronomy & Ephemeris Engine**: [Kastro](https://github.com/Yox/kastro) — 100% on-device local astronomical computations (no external weather/solar APIs required).
- **Location**: Google Play Services Location (`com.google.android.gms:play-services-location:21.4.0`) with `Geocoder`.
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/) (`hilt-android:2.60.1` + `hilt-compiler:1.4.0`).
- **Background Tasks**: [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (`work-runtime-ktx:2.12.0`).
- **Local Persistence**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) with Protocol Buffers / Kotlinx Serialization.

---

## 🚀 Installation & Releases

### Google Play Store
Download the app directly to your Wear OS smartwatch from Google Play:

<p align="left">
  <a href="https://play.google.com/store/apps/details?id=com.amoledwatchfaces.solarpath">
    <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="220" />
  </a>
</p>

### Sideloading (GitHub Releases)
You can also download precompiled `.apk` and `.aab` packages directly from our [GitHub Releases](https://github.com/amoledwatchfaces/Solar-Path-Watch-Face-Wear-OS/releases) page.

---

## 💻 Development Setup

### Prerequisites
- Android Studio Ladybug / Meerkat (2024.2+ or newer recommended)
- JDK 21
- Wear OS 5.0+ emulator or physical device (API 34+ / Target SDK 37)

### Building Locally

1. **Clone the repository**:
   ```bash
   git clone https://github.com/amoledwatchfaces/Solar-Path-Watch-Face-Wear-OS.git
   cd Solar-Path-Watch-Face-Wear-OS
   ```

2. **Build Debug APKs**:
   ```bash
   ./gradlew :wear:assembleDebug
   ```

3. **Build Release Binaries**:
   ```bash
   ./gradlew :wear:assembleRelease :wear:bundleRelease
   ```

---

## 📄 License & Credits

- Developed by **[amoledwatchfaces™](https://amoledwatchfaces.com/)**.
- Ephemeris calculations powered by [Kastro](https://github.com/Yox/kastro).
- All trademarks and brand names belong to their respective owners.
