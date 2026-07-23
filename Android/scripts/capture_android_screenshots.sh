#!/usr/bin/env bash
# Installs the debug APK on the CI emulator and captures the requested app screens.
set -euo pipefail

repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
apk_path="$repository_root/Android/src/app/build/outputs/apk/debug/app-debug.apk"
output_dir="$repository_root/artifacts/screenshots"
application_id="com.freneticphonetic.offlinegallery"
activity="com.google.ai.edge.gallery.MainActivity"

test -f "$apk_path"
mkdir -p "$output_dir"

adb wait-for-device
adb shell wm dismiss-keyguard || true
adb shell input keyevent 82 || true
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r "$apk_path"

wait_for_app() {
  local attempt
  for ((attempt = 1; attempt <= 30; attempt++)); do
    if adb shell dumpsys window windows | grep -q "mCurrentFocus.*$application_id"; then
      sleep 4
      return 0
    fi
    sleep 1
  done

  adb shell dumpsys window windows | grep -E "mCurrentFocus|mFocusedApp" || true
  return 1
}

capture_screen() {
  local file_name="$1"
  adb exec-out screencap -p > "$output_dir/$file_name"
  test -s "$output_dir/$file_name"
}

adb shell am force-stop "$application_id"
adb shell am start -W -n "$application_id/$activity"
wait_for_app
capture_screen "01-home.png"

adb shell am force-stop "$application_id"
adb shell am start -W \
  -n "$application_id/$activity" \
  -a android.intent.action.VIEW \
  -d "com.google.ai.edge.gallery://global_model_manager"
wait_for_app
capture_screen "02-local-models.png"

ls -lh "$output_dir"
