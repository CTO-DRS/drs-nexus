# DRS Nexus

**DRS Nexus** is a native Android toolkit that brings twenty real, fully-offline developer utilities into one polished, bilingual (Arabic/English) app.

Every tool runs 100% on-device. No accounts, no tracking, no network calls for tool execution — the only permissions requested are `INTERNET` and `ACCESS_NETWORK_STATE`.

## Tools (all real implementations)

| Tool | What it does |
|------|--------------|
| JSON Formatter | Format / minify / validate with line-column error positions and stats |
| Base64 | Encode/decode with URL-safe and padding options |
| Hash Calculator | SHA-256, SHA-1, SHA-512, MD5 via the platform security provider |
| Regex Tester | Live pattern testing with match positions, flags and replacement preview |
| URL Encode/Decode | Percent-encoding for URLs and query strings |
| Timestamp Converter | Unix epoch ↔ human date across timezones |
| Unit Converter | Length, weight, temperature, data size |
| UUID Generator | Bulk RFC 4122 v4 identifiers with formatting options |
| Password Generator | SecureRandom passwords with entropy analysis |
| Lorem Ipsum | Paragraphs / sentences / words |
| Text Utilities | Case conversion, statistics, reverse, trim, normalization |
| JWT Decoder | Decode tokens, inspect claims, expiry status |
| Base Converter | Binary / octal / decimal / hex and any base 2–36 |
| Color Converter | HEX ↔ RGB ↔ HSL with live preview and text-contrast advice |
| QR Generator | ZXing-powered QR codes, PNG export via SAF |
| Percentage Calculator | Percent of value, share of total, percent change |
| Loan Calculator | Monthly payment (EMI), total interest, payment count |
| BMI Calculator | Body mass index with WHO categories and healthy range |
| Date Difference | Days/weekdays between dates, date arithmetic |
| Currency Converter | Offline conversions for 22 currencies (built-in static rate table) |

## Architecture

- **Kotlin 2.0.21** + **Jetpack Compose** (Material 3), Navigation Compose, single activity
- **Multi-module Gradle**: `:core-common` (pure-Kotlin tool engines + unit tests), `:core-data` (Room + DataStore), `:app` (UI)
- **Room database**: real usage history + favorites, persisted locally
- **Home-screen AppWidget**: live runs/favorites stats straight from Room
- **History export**: full JSON export of usage data via SAF (no storage permission)
- **DataStore**: theme, language, onboarding and haptics settings
- **Manual DI** via an `AppContainer` — no framework magic
- Full **RTL support** with a complete Arabic and English string catalog
- iOS-inspired design system built on Material 3

## Build

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests (core-common engines)
./gradlew assembleRelease        # signed release APK (needs keystore)
```

Requirements: JDK 17, Android SDK 35.

## Project layout

```
core-common/   # pure Kotlin tool engines + 98 unit tests
core-data/     # Room entities/DAOs, DataStore settings repository
app/           # Compose UI: 20 tool screens + home/tools/history/favorites/settings + AppWidget
```

## License

MIT
