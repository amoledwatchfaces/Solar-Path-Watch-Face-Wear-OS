# Solar Path Watch Face

A Wear OS watch face and companion application recreating the iconic **Apple Solar Dial** watch face.

## Overview
Solar Path tracks the passage of the sun relative to the horizon on a 24-hour circular path, depicting daylight and twilight phases calibrated dynamically to the user's location.

### Features
- **24-Hour Solar Dial**: Solar noon at zenith (top / 12 o'clock), solar midnight at nadir (bottom / 6 o'clock).
- **Accurate Twilight Arcs**:
  - Daylight (Sun elevation > 0°)
  - Civil Twilight (0° to -6°)
  - Nautical Twilight (-6° to -12°)
  - Astronomical Twilight (-12° to -18°)
  - Night (< -18°)
- **Orbiting Sun Marker**: Real-time celestial disc tracing current sun position across the 24-hour dial.
- **Horizon Line**: Dividing chord between day and twilight/night sectors.
- **Inner 12-Hour Analog Clock**: Sleek hour, minute, second hands, date, and complication slots.
- **Two-Module Architecture with Watch Face Push**:
  - `:wear:watchface`: Pure XML Watch Face Format (WFF v4) with `hasCode = false`.
  - `:wear`: Wear OS app with Compose Material 3 UI, location services, Kastro astronomy engine, Complication Services, and Watch Face Push installer.
- **Location Engine**:
  - Manual location search via `TextInputDialog` and `Geocoder`.
  - Current GPS location using `FusedLocationProviderClient`.
  - Periodic background updates via `LocationWorker` and passive location listener.
  - Reverse geocoding with city and regional area identification.
  - Recent locations history with swipe-to-delete.
- **Astronomy & Complication Engine**:
  - Powered by [Kastro](https://github.com/Yox/kastro).
  - Exposes `SolarPathComplicationService` supporting `SHORT_TEXT`, `RANGED_VALUE`, and `LONG_TEXT` for both the WFF default watch face and external complications.

## License
Copyright (c) amoledwatchfaces™
