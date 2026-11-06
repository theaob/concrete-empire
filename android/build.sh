#!/usr/bin/env bash
# Concrete Empire — signed APK (sideload/itch.io) + signed AAB (Google Play upload)
# Builds with Gradle + the Android Gradle Plugin (needed for the Google Play Games Services library):
# Java 17+, Gradle 8.13+ ($GRADLE or `gradle` on PATH) and an Android SDK at $ANDROID_HOME (platform 36, build-tools 36.0.0).
# Gradle downloads the plugin and libraries from Google Maven / Maven Central on first run.
# ---- config ----
APP_NAME="Concrete Empire"        # shown under the icon and inside the game
APP_ID="com.onur.concreteempire"   # changing it makes Android treat it as a new app (separate save)
VERSION_CODE=14
VERSION_NAME="2.3.2"
MIN_SDK=24
TARGET_SDK=36                       # Google Play: new apps/updates must target API 36 (Android 16) from 31 Aug 2026
BUILD_TOOLS="${BUILD_TOOLS:-36.0.0}"
GRADLE="${GRADLE:-gradle}"         # Gradle 8.13+ (CI installs 8.14.3)
B="$(cd "$(dirname "$0")" && pwd)"   # everything is relative to this folder
# signing: env overrides (CI) or the files next to this script (local). Never commit the key or password.
KEYSTORE="${KEYSTORE:-$B/katkarsiligi-release.jks}"
KEY_ALIAS="${KEY_ALIAS:-katkarsiligi}"
KEY_PASS_FILE="$B/keystore-password.txt"
OUT="$B/out/${APP_NAME// /}-${VERSION_NAME}.apk"
AAB="${OUT%.apk}.aab"
# ----------------
set -euo pipefail
unset JAVA_TOOL_OPTIONS
S=$B/src; W=$B/work
rm -rf "$W"; mkdir -p "$W" "$(dirname "$OUT")"

# the game itself lives in ../game/index.html; the app always packs the current copy
cp "$B/../game/index.html" "$S/assets/index.html"
# game's Settings version line and name follow VERSION_NAME / APP_NAME
sed -i "s/^const APP_VERSION='[^']*';/const APP_VERSION='${VERSION_NAME}';/" "$S/assets/index.html"
grep -q "^const APP_VERSION='${VERSION_NAME}';" "$S/assets/index.html"
sed -i "s/^const GAME_NAME='[^']*';/const GAME_NAME='${APP_NAME}';/; s#<title>[^<]*</title>#<title>${APP_NAME}</title>#" "$S/assets/index.html"
grep -q "^const GAME_NAME='${APP_NAME}';" "$S/assets/index.html"

# signing key: env (CI) or the files next to this script (local)
if [ -n "${KEYSTORE_PASSWORD:-}" ]; then
  export KSPASS="$KEYSTORE_PASSWORD"
elif [ -f "$KEY_PASS_FILE" ]; then
  export KSPASS="$(cat "$KEY_PASS_FILE")"
fi
if [ ! -f "$KEYSTORE" ] || [ -z "${KSPASS:-}" ]; then
  # No release key: refuse, unless a throwaway test key is explicitly allowed (CI without secrets).
  # A build signed this way cannot update an installed release build.
  if [ "${ALLOW_TEST_KEY:-}" != "1" ]; then
    echo "Release key missing: put katkarsiligi-release.jks + keystore-password.txt in android/ (or set KEYSTORE / KEYSTORE_PASSWORD)." >&2
    exit 1
  fi
  KEYSTORE="$W/test.jks"; KEY_ALIAS="test"; export KSPASS="testtest"
  keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$KSPASS" -dname "CN=Test" >/dev/null
  OUT="${OUT%.apk}-TESTKEY.apk"; AAB="${AAB%.aab}-TESTKEY.aab"
fi
export KEYSTORE KEY_ALIAS

# APK (v1+v2+v3) and Play bundle, both signed with this key. On Play it is the *upload key*;
# Google re-signs for users via Play App Signing.
"$GRADLE" -p "$B" --no-daemon --console=plain \
  -PappId="$APP_ID" -PappName="$APP_NAME" -PversionCode="$VERSION_CODE" -PversionName="$VERSION_NAME" \
  -PminSdk="$MIN_SDK" -PtargetSdk="$TARGET_SDK" -PbuildTools="$BUILD_TOOLS" \
  assembleRelease bundleRelease
# Play Console flags androidx.fragment 1.1.0 (pulled in by Play Games) as outdated: fail if it ever comes back.
FRAG="$("$GRADLE" -p "$B" --no-daemon --console=plain -q \
  -PappId="$APP_ID" -PminSdk="$MIN_SDK" -PtargetSdk="$TARGET_SDK" -PbuildTools="$BUILD_TOOLS" \
  dependencies --configuration releaseRuntimeClasspath \
  | sed -nE 's/.*androidx\.fragment:fragment:([0-9][^ ]*)( -> ([0-9][^ ]*))?.*/\1 \3/p' \
  | awk '{print ($2 != "" ? $2 : $1)}' | sort -u)"
echo "androidx.fragment resolved to: ${FRAG:-?}"
if [ -z "$FRAG" ] || echo "$FRAG" | grep -q '^1\.[0-7]\.'; then echo "androidx.fragment is outdated or missing" >&2; exit 1; fi
cp "$B/build/outputs/apk/release/"*-release.apk "$OUT"
cp "$B/build/outputs/bundle/release/"*-release.aab "$AAB"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
APKSIGNER="$SDK/build-tools/$BUILD_TOOLS/apksigner"
if [ -x "$APKSIGNER" ]; then "$APKSIGNER" verify --verbose "$OUT" | head -6; fi
jarsigner -verify "$AAB" | grep -m1 "jar verified"
ls -la "$OUT" "$AAB"
if [ -n "${GITHUB_OUTPUT:-}" ]; then echo "apk=$OUT" >> "$GITHUB_OUTPUT"; echo "aab=$AAB" >> "$GITHUB_OUTPUT"; fi
