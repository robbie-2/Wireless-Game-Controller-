# eFootball Remote (Android-to-Android prototype)

An Android Studio Kotlin project with **controller** and **receiver** modes. Both phones install the same app.

## Build
1. Open this folder in Android Studio (JDK 17, Android SDK 35).
2. Let Gradle sync and choose **Build > Build APK(s)**.
3. Install the resulting APK on both Android phones (Android 8+).

## Connect
1. Put both devices on the same trusted Wi-Fi network (avoid guest Wi-Fi/client isolation).
2. On Phone 2 open Receiver, note its local IPv4 address, enable the app's Accessibility Service, and press Start Receiver.
3. On Phone 1 open Controller, enter Phone 2's IP address, and press Connect.
4. On Phone 2 calibrate button coordinates to match eFootball's on-screen layout.

## IMPORTANT limitations
- **Prototype, not a tested eFootball-compatible gamepad.** eFootball may ignore or restrict synthetic touches.
- Android Accessibility gesture dispatch does **not** reliably support sustained multi-touch or continuous joystick holds. This prototype sends short swipes and taps; smooth movement, simultaneous sprint + move, and other real controller behaviors are NOT implemented.
- Calibration currently uses the app's content region, not an eFootball screenshot, so coordinates may require adjustment. The service uses physical screen dimensions while the calibration surface is smaller: this is a known mismatch to fix before real gameplay.
- Receiver must stay alive while the game is open; the Activity-based TCP server may be suspended when backgrounded. A foreground service and notification are required for reliable use.
- The TCP listener is unauthenticated and unencrypted. Use only a private trusted LAN; do not expose port 47321 to the internet.
- The service can inject touches anywhere on the screen while enabled. Disable it when not in use.
- No root is needed for this experiment. Android may require additional user confirmation to enable accessibility for a sideloaded app.
- No Gradle wrapper or prebuilt APK included. Build requires Android Studio/SDK.

## Build APK with GitHub Actions (no PC needed)
1. Create an empty GitHub repository and upload the **contents** of this project folder to the repository root, including the hidden `.github/workflows/build-apk.yml` file.
2. Open **Actions** > **Build Android APK** > **Run workflow** (or wait for the push build).
3. Open the completed successful run and download the `efootball-remote-debug-apk` artifact.
4. Unzip the artifact to get `app-debug.apk`; install on both phones.

Note: This workflow is provided but has not been executed here. If the build fails, inspect the Actions log. This is an experimental prototype, not a verified working eFootball controller.
