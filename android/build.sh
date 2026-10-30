#!/usr/bin/env bash
# Concrete Empire — signed APK build
# Tools: android/tools/ if present (aapt2, android.jar, d8, ecj, apksigner — no SDK needed),
# otherwise the Android SDK at $ANDROID_HOME (build-tools + platform, as on GitHub Actions runners).
# ---- config ----
APP_NAME="Concrete Empire"        # shown under the icon and inside the game
APP_ID="com.onur.concreteempire"   # changing it makes Android treat it as a new app (separate save)
VERSION_CODE=12
VERSION_NAME="2.3"
MIN_SDK=24
TARGET_SDK=34
BUILD_TOOLS="${BUILD_TOOLS:-34.0.0}"
B="$(cd "$(dirname "$0")" && pwd)"   # everything is relative to this folder
# signing: env overrides (CI) or the files next to this script (local). Never commit the key or password.
KEYSTORE="${KEYSTORE:-$B/katkarsiligi-release.jks}"
KEY_ALIAS="${KEY_ALIAS:-katkarsiligi}"
KEY_PASS_FILE="$B/keystore-password.txt"
OUT="$B/out/${APP_NAME// /}-${VERSION_NAME}.apk"
# ----------------
set -euo pipefail
unset JAVA_TOOL_OPTIONS
T=$B/tools; S=$B/src; W=$B/work
rm -rf "$W"; mkdir -p "$W/classes" "$W/dex" "$(dirname "$OUT")"

if [ -x "$T/aapt2" ]; then
  AAPT2="$T/aapt2"; ANDROID_JAR="$T/android.jar"; D8_JAR="$T/d8.jar"; APKSIGNER_JAR="$T/apksigner.jar"
  ECJ_JAR="$(ls "$T"/ecj-*.jar 2>/dev/null | head -1 || true)"
else
  SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  [ -n "$SDK" ] || { echo "No android/tools/ and no ANDROID_HOME" >&2; exit 1; }
  BT="$SDK/build-tools/$BUILD_TOOLS"
  AAPT2="$BT/aapt2"; ANDROID_JAR="$SDK/platforms/android-$TARGET_SDK/android.jar"
  D8_JAR="$BT/lib/d8.jar"; APKSIGNER_JAR="$BT/lib/apksigner.jar"; ECJ_JAR=""
fi

# the game itself lives in ../game/index.html; the APK always packs the current copy
cp "$B/../game/index.html" "$S/assets/index.html"

# 1) resources + manifest (game's Settings version line follows VERSION_NAME)
sed -i "s/^const APP_VERSION='[^']*';/const APP_VERSION='${VERSION_NAME}';/" "$S/assets/index.html"
grep -q "^const APP_VERSION='${VERSION_NAME}';" "$S/assets/index.html"
sed -i "s/^const GAME_NAME='[^']*';/const GAME_NAME='${APP_NAME}';/; s#<title>[^<]*</title>#<title>${APP_NAME}</title>#" "$S/assets/index.html"
grep -q "^const GAME_NAME='${APP_NAME}';" "$S/assets/index.html"
sed "s/android:label=\"[^\"]*\"/android:label=\"${APP_NAME}\"/" "$S/AndroidManifest.xml" > "$W/AndroidManifest.xml"
"$AAPT2" compile --dir "$S/res" -o "$W/res.zip"
"$AAPT2" link -o "$W/base.apk" -I "$ANDROID_JAR" --manifest "$W/AndroidManifest.xml" \
  --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK \
  --version-code $VERSION_CODE --version-name "$VERSION_NAME" --rename-manifest-package "$APP_ID" \
  -A "$S/assets" "$W/res.zip"

# 2) code
if [ -n "$ECJ_JAR" ]; then
  java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -nowarn \
    -bootclasspath "$ANDROID_JAR" -classpath "$ANDROID_JAR" -d "$W/classes" $(find "$S/java" -name '*.java')
else
  javac -source 8 -target 8 -encoding UTF-8 -nowarn -Xlint:-options \
    -bootclasspath "$ANDROID_JAR" -classpath "$ANDROID_JAR" -d "$W/classes" $(find "$S/java" -name '*.java')
fi
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api $MIN_SDK --lib "$ANDROID_JAR" \
  --output "$W/dex" $(find "$W/classes" -name '*.class')
(cd "$W/dex" && zip -q "$W/base.apk" classes.dex)

# 3) align (4-byte for stored entries, resources.arsc stored) then sign v1+v2+v3
python3 -I "$B/zipalign.py" "$W/base.apk" "$W/aligned.apk"
if [ -n "${KEYSTORE_PASSWORD:-}" ]; then
  export KSPASS="$KEYSTORE_PASSWORD"
elif [ -f "$KEY_PASS_FILE" ]; then
  export KSPASS="$(cat "$KEY_PASS_FILE")"
fi
if [ ! -f "$KEYSTORE" ] || [ -z "${KSPASS:-}" ]; then
  # No release key: refuse, unless a throwaway test key is explicitly allowed (CI without secrets).
  # An APK signed this way cannot update an installed release build.
  if [ "${ALLOW_TEST_KEY:-}" != "1" ]; then
    echo "Release key missing: put katkarsiligi-release.jks + keystore-password.txt in android/ (or set KEYSTORE / KEYSTORE_PASSWORD)." >&2
    exit 1
  fi
  KEYSTORE="$W/test.jks"; KEY_ALIAS="test"; export KSPASS="testtest"
  keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$KSPASS" -dname "CN=Test" >/dev/null
  OUT="${OUT%.apk}-TESTKEY.apk"
fi
java -jar "$APKSIGNER_JAR" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
  --ks-pass env:KSPASS --key-pass env:KSPASS \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT" "$W/aligned.apk"
java -jar "$APKSIGNER_JAR" verify --verbose "$OUT" | head -6
ls -la "$OUT"
[ -z "${GITHUB_OUTPUT:-}" ] || echo "apk=$OUT" >> "$GITHUB_OUTPUT"
