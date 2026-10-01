#!/usr/bin/env bash
# Runs the instrumented SmokeTest on a connected emulator/device and pulls its screenshots
# into ./screenshots. Used by .github/workflows/ui-smoke.yml; also works locally with adb.
set -uo pipefail

PKG=io.github.poodicraft.serverscope
OUT=screenshots
mkdir -p "$OUT"
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c

adb shell am instrument -w -e class "$PKG.SmokeTest" "$PKG.test/androidx.test.runner.AndroidJUnitRunner" \
  | tee "$OUT/api$API-instrumentation.txt"

for file in $(adb shell run-as "$PKG" ls files/screenshots 2>/dev/null | tr -d '\r'); do
  adb exec-out run-as "$PKG" cat "files/screenshots/$file" > "$OUT/$file"
done
adb logcat -d -v brief '*:E' > "$OUT/api$API-logcat-errors.txt" || true
ls -l "$OUT"

# Only crashes of ServerScope itself count (shell commands like `svc` can log their own).
if grep -q "Process: $PKG," "$OUT/api$API-logcat-errors.txt" \
  || grep -q "Process crashed" "$OUT/api$API-instrumentation.txt"; then
  echo "::error::ServerScope crashed during the smoke test"
  grep -B 2 -A 30 "Process: $PKG," "$OUT/api$API-logcat-errors.txt"
  exit 1
fi
grep -q "^OK (" "$OUT/api$API-instrumentation.txt" || { echo "::error::Smoke test failed"; exit 1; }
