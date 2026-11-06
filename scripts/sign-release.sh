#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
: "${ANDROID_HOME:?Android SDK required}"
: "${GLASSOS_KEYSTORE:?Dedicated release keystore required}"
: "${GLASSOS_STORE_PASSWORD:?Keystore password required}"
: "${GLASSOS_KEY_PASSWORD:?Key password required}"
: "${GLASSOS_KEY_ALIAS:=glassos-plus}"
build_tools="$ANDROID_HOME/build-tools/34.0.0"
mkdir -p dist
"$build_tools/zipalign" -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk dist/aligned.apk
"$build_tools/apksigner" sign --ks "$GLASSOS_KEYSTORE" --ks-key-alias "$GLASSOS_KEY_ALIAS" \
  --ks-pass env:GLASSOS_STORE_PASSWORD --key-pass env:GLASSOS_KEY_PASSWORD \
  --v4-signing-enabled false --out dist/GlassOS-Plus.apk dist/aligned.apk
"$build_tools/apksigner" verify --verbose --print-certs dist/GlassOS-Plus.apk
"$build_tools/zipalign" -c -p 4 dist/GlassOS-Plus.apk
python3 - <<'CHECKSUM'
import hashlib
from pathlib import Path
p = Path('dist/GlassOS-Plus.apk')
Path('dist/SHA256SUMS').write_text(hashlib.sha256(p.read_bytes()).hexdigest() + '  GlassOS-Plus.apk\n')
CHECKSUM
