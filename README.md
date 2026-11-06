# GlassOS+

GlassOS+ is a community fork of [GlassOS by AbhishekKaisar](https://github.com/AbhishekKaisar/glassos-tv), based on upstream v1.0.2-beta. It adds a compact Continue Watching carousel, configurable app layout and faster Home return for Android TV / Google TV.

This fork is maintained by [Snowflake-27](https://github.com/Snowflake-27). It is independent of the upstream project and is distributed under the same [GNU GPL v3 license](LICENSE). The original project documentation and credits are preserved in [README.upstream.md](README.upstream.md).

## Features

- Compact **Continue Watching** immediately below Apps, using the app dock's card width and spacing.
- Public Android TV **Watch Next** and **Preview Channels** combined locally, including compatible Nuvio channels. Duplicate titles are merged; episode metadata, app source and genuine playback progress are shown when supplied.
- Original app resume links and episode/content extras are retained. An app must publish accessible entries; support is not guaranteed for every streaming service.
- Persistent **Auto-scroll Continue Watching**, **Off by default**. Manual left/right always works. When enabled, the row advances every five seconds and pauses during focus/interaction, resuming after nine seconds of inactivity.
- Original Hero presentation: Up from Apps expands the Hero area with only Apps visible; Down restores the compact rows and the previously focused app.
- Four to six app columns, configurable margins, spacing and icon size; hide/reorder apps without disabling their packages.
- Cinemeta movie/series backgrounds with larger artwork and selectable catalog content; optional TMDB using your own key.
- Configurable movie destination: Nuvio, Stremio or WuPlay. Catalog links use IMDb identifiers; Continue Watching uses each publishing app's original link.
- Optional **Fast Home** accessibility helper observes stock Home window changes without reading window contents. Optional Direct Home key filtering affects only HOME and requires separate consent. The usage-based Home watcher remains a fallback.

## Tested and limitations

Tested on a Xiaomi TV Box S (3rd generation), Android 14, with a 1080p UI. D-pad tests were driven through ADB. Fire TV and every streaming app have not been tested for these fork-specific features.

Some firmware keeps the stock launcher as the system Home even after assigning the Home role. Fast Home redirects it; a brief stock-launcher flash is possible. Do not disable setup/network/system packages to work around this.

Watch Next/Preview channels require Android 8 or newer and normal `READ_TV_LISTINGS` permission. Only rows exposed by the provider are available. Missing progress or timestamps are not invented; an unknown duration produces no progress bar. Artwork quality depends on the source app.

The installed application is labeled **GlassOS+**, with application ID `io.github.abhishekkaisar.glassos.personal`. This preserves compatibility with existing Personal installations. It coexists with the official GlassOS application.

## Build from source

Requirements: JDK 17, Android SDK platform 34 and build-tools 34.0.0, plus Python 3 for the lint-result check. Set `JAVA_HOME` and `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) for your machine. Gradle uses the included wrapper.

```sh
git clone https://github.com/Snowflake-27/glassos-plus.git
cd glassos-plus
bash build-personal.sh
```

The script runs the dependency-free Java rule checks, builds the debug APK and runs lint. Upstream targets Android SDK 28 for TV compatibility; the script identifies its inherited Google Play target-SDK publication issue and fails on any additional lint error.

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

A standard build is also available with `./gradlew --no-daemon assembleDebug lint`. No API key is required for the default Cinemeta catalog. TMDB is optional and uses a key explicitly supplied by the user; never commit credentials or local properties.

## Install and use

Enable wireless debugging on the TV and pair/connect using Android's normal instructions. Use the connection address shown by your TV, rather than copying another device's address.

```sh
adb -s <tv-address:port> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <tv-address:port> shell am start -n io.github.abhishekkaisar.glassos.personal/.MainActivity
```

Long-press OK on the clock/profile pill for Settings. Continue Watching requests TV-listings permission when needed. Fast Home can be enabled under Android Accessibility and is controlled by Replace home in GlassOS Settings. Keep the stock launcher installed and enabled while testing.

Use Up/Down for Hero ↔ Apps ↔ Continue Watching and the app drawer; Left/Right moves through the selected row; OK opens the selected app/content. Auto-scroll is optional in Settings.

After testing, turn off USB/wireless/network debugging and revoke debugging authorizations on the TV.

## Privacy and permissions

Watch history is read from Android's public TV provider, not from private app files, notifications or screen scraping. Metadata and artwork are cached locally. No new analytics or watch-history upload was added by this fork. Network artwork/catalog requests go to their respective providers. The upstream optional Apple/TMDB integrations remain available; see the upstream documentation for its broader permission and setup features.

Fast Home receives window-change events and cannot retrieve window content. The separate, opt-in Direct Home key feature requests key filtering; it consumes only HOME and can affect long-press Home behavior. Physical-remote filtering depends on firmware and is not guaranteed. Usage access and overlay permission support the fallback Home watcher. Continue Watching adds only the normal `android.permission.READ_TV_LISTINGS`; it does not request privileged EPG access.

Do not publish your TV backups, viewing history, account screenshots, device identifiers, ADB keys or signing keys.

## Updating and recovery

Android updates require the same application ID and a trusted signing key. Public releases use one dedicated release key, kept out of source control. Existing debug installations need a compatible migration; never uninstall or clear data merely to resolve a signature mismatch. See [release and recovery instructions](docs/RELEASES.md).

Keep a known-good, same-key build. A recovery build must have an equal/higher version code. Turn off Replace home / Fast Home and choose the stock Home in Android Settings if returning to it. Do not disable system packages.

## Credits

- [AbhishekKaisar / GlassOS](https://github.com/AbhishekKaisar/glassos-tv): original launcher, design, code and assets.
- [CleanYourTV](https://github.com/Gathin23/cleanyourtv): public Watch Next documentation and Home-helper reference.
- [NuvioTV PR #1656](https://github.com/NuvioMedia/NuvioTV/pull/1656): reference for its public Continue Watching preview-channel contract.
- Android TV provider APIs and the metadata/artwork providers used by the original project and this fork.

Original copyright and license notices are retained. Fork changes include the Continue Watching reader/carousel, compact Hero layout integration, layout controls, Cinemeta/Nuvio routing and Fast Home helper. See [CHANGELOG.md](CHANGELOG.md).

## Public releases

See [Hero selection and player protocols](docs/HERO.md) for the exact shuffle behavior, and [release/install/recovery instructions](docs/RELEASES.md) for signed APKs and Downloader setup. Download [the latest APK](https://github.com/Snowflake-27/glassos-plus/releases/latest/download/GlassOS-Plus.apk).
