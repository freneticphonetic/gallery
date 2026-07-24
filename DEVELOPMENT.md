# Development notes

## Start with the project rules

Before changing the application, read:

1. [`AGENTS.md`](AGENTS.md)
2. [`docs/PROJECT_CHARTER.md`](docs/PROJECT_CHARTER.md)
3. Any relevant Architecture Decision Records in [`docs/adr/`](docs/adr/)

Offline AI Gallery is an independent local-first fork. Dormant upstream code and documentation do not authorize restoring removed cloud behavior.

## No OAuth or remote model setup

This fork does not require a Hugging Face developer application, OAuth client ID, redirect URI, Google account, or remote model catalog.

Do not configure upstream authentication placeholders to build or run the application.

Models are acquired outside the app, copied onto the Android device, and imported through the local document picker.

## Requirements

The current project configuration uses:

- Android Studio with Android SDK Platform 37 installed.
- JDK 21.
- The included Gradle wrapper from `Android/src`.
- An Android device or emulator running Android 12 or newer.

The first build may require internet access on the development machine so Gradle can download build dependencies. This does not add network permission to the installed application.

## Open the project in Android Studio

Open the following directory as the Android Studio project:

```text
Android/src
```

Allow Gradle synchronization to complete, select the `app` configuration, and run it on a compatible device or emulator.

## Build from the command line

From the repository root:

```bash
cd Android/src
chmod +x gradlew
./gradlew --no-daemon :app:assembleDebug
```

The debug APK is normally written to:

```text
Android/src/app/build/outputs/apk/debug/app-debug.apk
```

To produce the current release build:

```bash
cd Android/src
./gradlew --no-daemon :app:assembleRelease
```

The release APK is normally written to:

```text
Android/src/app/build/outputs/apk/release/app-release.apk
```

Current pre-release builds use development signing and are not production-signed releases.

## Run checks

From `Android/src`:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

With a device or emulator available:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Run the checks relevant to the files changed. Navigation changes should also be smoke-tested manually so every retained tool remains reachable.

## Verify application permissions

Changes to manifests, dependencies, plugins, or build configuration can alter the merged Android manifest.

After building an APK, inspect its declared permissions with Android SDK tooling, for example:

```bash
apkanalyzer manifest permissions app/build/outputs/apk/debug/app-debug.apk
```

The offline application must not declare `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE` unless an explicitly approved architectural change introduces a separate controlled network boundary.

Also confirm that dependencies have not restored removed Google-service, account, analytics, messaging, or cloud permissions through manifest merging.

## Test local model import

1. Copy a compatible `.litertlm` or `.task` model file onto the device.
2. Launch Offline AI Gallery.
3. Open **Local Models**.
4. Choose **Import model**.
5. Select the model through Android's document picker.
6. Review its settings and declare the capabilities it supports.
7. Open a compatible tool and verify local inference.

Do not add model downloading, authentication, or a remote catalog merely to simplify development testing.

## GitHub Actions

The repository's Android build workflow uses `Android/src` as its working directory and builds the release APK with:

```bash
./gradlew --no-daemon --console=plain :app:assembleRelease
```

Pull requests that change files under `Android/` should trigger the Android build workflow. Review its build log and artifacts before merging.

## Reporting completed work

When submitting a change, report:

- Files changed.
- Validation performed.
- User-facing capabilities affected.
- Permission or security-boundary changes.
- Known limitations.
- Assumptions made.
