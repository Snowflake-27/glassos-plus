# Release 1.1.0 validation

Checked 2026-11-05 on Xiaomi TV Box S (3rd generation), model MiTV-AFMU0, Android 14 / SDK 34, firmware UKG3.250826.001.V816.0.7.0.UZFAABX, with its existing 1080p UI configuration. No device identifiers, private logs, preferences or viewing history are included in this repository.

- JVM rules passed 864 layout combinations, exact player-link validation, shuffle-bag exclusion/deduplication/empty/singleton cases and existing Continue Watching/Home-key rules.
- Debug and optimized release builds passed. Release Gradle unit-test task has no Android/JVM test sources; the dependency-free checks above are the actual logic tests.
- Debug and release lint: zero new errors. The inherited targetSdk 28 Google Play deadline error remains documented and is checked explicitly. This is a sideloaded release, not a Google Play submission.
- Release signature verified with Android apksigner v1/v2/v3, SDK 22 minimum, and zip alignment checked. Dedicated certificate digest is in RELEASES.md.
- One-time owned debug→release v3 rotation and a subsequent ordinary universal release update both installed with `adb install -r`. No uninstall or app-data clear was used. The previous layout settings remained intact (five apps per row and existing margins).
- Installed release: versionName 1.1.0, versionCode 10, stable personal application ID, not debuggable.
- D-pad: Hero Open ↔ refresh icon, Hero ↔ Apps ↔ Continue Watching; expanded Hero hides Continue Watching and retains the original app dock. Shuffle keeps focus and changes titles using crossfade. Repeated debug tests did not select the same title consecutively. Release screenshots confirmed title/artwork change.
- Nuvio and Stremio: selected catalog movie and series IDs produced exact package-scoped intents; corresponding content screens were checked in private screenshots. No playback or seek-offset claim is made.
- WuPlay is absent from the test device. Its documented IMDb movie/series protocol has logic checks; activation while absent stayed in GlassOS+. Actual WuPlay content opening, TMDB-native links and episode routing are not certified.
- Injected Home returned to GlassOS+ after updating. A real remote's zero-flash/direct HOME filtering and held-volume behavior cannot be certified by injected keys. The optional experiment remains separately opt-in and firmware dependent.
- Disabled-package list stayed identical. No system packages were enabled/disabled, no data of other apps was changed and existing CleanYourTV optimizations were preserved.
- Fire TV and other manufacturers remain untested for these fork-specific changes.

Private recovery APK, signing backup, before/after package lists, screenshots and app data archive are retained locally outside the public repository. See RELEASES.md for recovery and safe debugging closure.

## Catalog expansion 1.1.1

Public endpoint probes returned 211 unique movie and 203 unique series candidates across Popular and Featured pages on the test date (counts vary over time). JVM checks verified fresh-title priority, deduplication, sparse fallback and empty input. The optimized release and release lint passed with the same inherited target-SDK issue and no new errors. VersionCode 11 was signed with the existing dedicated release key and installed as an update without clearing data. The TV logged 120 successfully cached Cinemeta titles.
