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
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell svc power stayon true || true
adb shell wm dismiss-keyguard || true
adb shell input keyevent 82 || true
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

wait_for_package_manager() {
  local attempt
  for ((attempt = 1; attempt <= 30; attempt++)); do
    if adb shell cmd package path android >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  adb shell service list | grep package || true
  return 1
}

install_apk() {
  local attempt
  for ((attempt = 1; attempt <= 3; attempt++)); do
    if adb install --no-streaming -r "$apk_path"; then
      return 0
    fi

    if ((attempt < 3)); then
      echo "APK install attempt $attempt failed; reconnecting to the emulator."
      adb reconnect || true
      adb wait-for-device
      sleep 5
      wait_for_package_manager
    fi
  done

  return 1
}

wait_for_package_manager
install_apk

wait_for_app() {
  local attempt
  for ((attempt = 1; attempt <= 45; attempt++)); do
    if adb shell dumpsys activity activities 2>/dev/null |
      grep -Eq "(mResumedActivity|topResumedActivity).*$application_id"; then
      sleep 8
      return 0
    fi
    sleep 2
  done

  adb shell dumpsys activity activities |
    grep -E "mResumedActivity|topResumedActivity|mFocusedApp" || true
  adb logcat -d -t 300 |
    grep -E "AndroidRuntime|FATAL EXCEPTION|Process: $application_id" || true
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
