
# Bandwidth Bandit 
A lightweight android network monitor focused on a persistent live-speed notification, daily Wi-Fi/mobile usage history, and per-app traffic breakdowns.

## Included features

- Foreground monitor service with combined speed in the compact notification
- Expanded notification with separate download and upload rates
- Daily usage totals split into Wi-Fi/mobile and download/upload
- Calendar-based history screen backed by `NetworkStatsManager`
- Per-app daily usage list for Wi-Fi and mobile traffic
- Daily usage threshold alerts
- Notification text color customization with optional dynamic speed-based colors
- Theme selection, speed-unit selection, and start-on-boot setting
- Signal strength reporting in dBm
- Lightweight quick speed test using Cloudflare endpoints

## Permissions and setup

- `PACKAGE_USAGE_STATS` is required for accurate daily and per-app usage tracking
- `READ_PHONE_STATE` improves signal-strength reporting
- `POST_NOTIFICATIONS` keeps the foreground monitor visible on Android 13+

When you first open the app, grant usage access from system settings so the history/calendar screen can populate correctly.

## Build

The project includes a Gradle wrapper and points at the local Android SDK in `local.properties`.

If command-line builds fail on this machine, increase the Windows paging file or build through Android Studio, because the local environment is currently failing to start the Gradle JVM reliably.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
