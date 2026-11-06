# Building, signing, installing and recovering

Public release certificate SHA-256: `5465e56a9f329f5463d1f46eab26a01b525d2c0af0e237997808b1acea971e58`. Verify with Android SDK `apksigner verify --print-certs GlassOS-Plus.apk` and compare the APK checksum with the release SHA256SUMS.

The application ID is permanently `io.github.abhishekkaisar.glassos.personal`; the label is GlassOS+. `version.properties` defines the public version. Increment versionCode for every update; do not change the signing key.

Run `bash build-personal.sh` for JVM checks, debug build and lint, then `./gradlew --no-daemon assembleRelease lintRelease` for the optimized release. The inherited targetSdk 28 Google Play deadline warning is documented by the build helper. This release is for direct Android TV installation, not Google Play submission. minSdk remains 22; Watch Next needs Android 8+. Fire TV paths are inherited and have not been verified on hardware.

Provide `GLASSOS_KEYSTORE`, `GLASSOS_STORE_PASSWORD`, `GLASSOS_KEY_PASSWORD`, optionally `GLASSOS_KEY_ALIAS` (default `glassos-plus`) and run `bash scripts/sign-release.sh`. It aligns, signs and verifies the APK and creates `dist/GlassOS-Plus.apk` and `dist/SHA256SUMS`. Keep the dedicated keystore and password in separate secure backups. Never commit them. Published binaries are release assets, not source-tree files.

GitHub Actions Release runs checks on `v*` tags, requires the tag to match versionName, builds the optimized APK, signs with repository secrets and publishes assets. Configure `GLASSOS_KEYSTORE_BASE64`, `GLASSOS_STORE_PASSWORD`, `GLASSOS_KEY_PASSWORD`, and `GLASSOS_CERT_SHA256` once. CI verifies the public certificate digest against the expected signer before publication. A missing secret fails the release instead of producing an unsigned APK. PR builds cannot use release secrets.

Download the official fork APK from:

https://github.com/Snowflake-27/glassos-plus/releases/latest/download/GlassOS-Plus.apk

Enable installation permission for Downloader/file manager if Android asks, install the downloaded APK, then select GlassOS+ using Android's Home/default-app mechanism. Optional accessibility/overlay/usage features are explained in Settings; grant only what you use. Do not disable stock/system packages. After ADB testing, turn off USB, wireless/network debugging and revoke debugging authorizations.

## Downloader

At [AFTVnews URL Shortener](https://go.aftvnews.com/), submit the permanent latest-download URL above, complete the CAPTCHA and save the numeric code returned. A code is not assigned in advance. Reuse the same URL and asset filename for future releases so the code follows the latest APK. Never substitute another project's code.

## Existing debug installations

A debug-signed installation cannot normally accept the dedicated release key. Do not uninstall or clear data to fix a signature error. The maintainer can prepare a one-time Android v3 signing-key rotation migration using both owned keys on Android 9+, then verify an ordinary release update. This migration APK is private to the existing installation and is not the public universal download. Other developers' debug keys cannot be migrated without their keys. The old developer debug private key is never uploaded to CI.

## Recovery without uninstalling

Keep a known-good APK and its matching source tag. Android requires an equal/higher versionCode and trusted signer for `adb install -r <apk>`. An older APK may be rejected; rebuild the known-good source with a greater version using `-PreleaseVersionCode=<higher-number> -PreleaseVersionName=<recovery-version>` and the same release key. Do not use data-clearing or uninstall commands. Signing-key rotation can prevent returning directly to an old debug-signed APK; prepare a release-signed recovery build first.

Disable Replace Home and optional Direct Home key in GlassOS+ Settings, then select the stock Home through normal Android settings to stop using the launcher. Existing CleanYourTV/package changes are independent and must not be toggled by the release procedure.

The maintainer verified the one-time migration on the existing Android 14 test installation, followed by a normal release-signed update. A local known-good recovery APK has versionCode 10 and the same release certificate, so it can replace this first release without clearing data. This private migration is not required for new public installations. See [validation](VALIDATION.md).

For the 1.1.1 update, the locally retained recovery build was rebuilt with versionCode 11 and the same release key. Future recovery builds must keep pace with the installed versionCode.
