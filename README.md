# Offline AI Gallery

Offline AI Gallery is an Android workspace for running compatible language models entirely on
your own device. It is an independent, privacy-focused fork of Google AI Edge Gallery with a
smaller interface and an explicit offline boundary.

## Offline contract

The Android app is designed so privacy does not depend on a setting or a promise:

- The app does not request `INTERNET` or `ACCESS_NETWORK_STATE`. Manifest removal rules also stop
  transitive dependencies from adding them back.
- Firebase Analytics, Firebase Cloud Messaging, remote model catalogs, OAuth, and model downloads
  are removed.
- Android cloud backup is disabled.
- Models are imported only through Android's local document picker.
- Prompts, media, chat state, skill data, and model files stay in the app's on-device storage.
- Inference runs locally through LiteRT-LM.

Camera, microphone, calendar, and notification permissions remain optional because individual
on-device tools can use those Android features. They are requested only when the relevant feature
needs them.

## Using the app

1. Copy a compatible `.litertlm` or `.task` model file onto an Android 12+ device.
2. Open **Local models** and choose **Import model**.
3. Select the file and describe its supported capabilities.
4. Open an available on-device tool from the home screen.

The app does not discover or download models. This is intentional: model acquisition happens
outside the app, and the installed app remains useful without a network connection.

## What changed in the offline foundation

- Replaced the promotional, tabbed home page with a compact task list and clear first-run setup.
- Reworked model management around one local-file import path.
- Added visible privacy status and removed Google account/terms prompts.
- Removed Google services, Play Services model delegates, AICore integration, and Hugging Face
  authentication.
- Limited built-in Agent Skills to operations that work locally and hid remote MCP configuration.
- Rebranded the app, package ID, icon, and color system for this fork.

## Development

Android build instructions are in [DEVELOPMENT.md](DEVELOPMENT.md). The application ID is
`com.freneticphonetic.offlinegallery`; the internal Kotlin namespace is intentionally unchanged in
this first pass to keep the functional diff reviewable.

Near-term cleanup work includes removing the now-dormant MCP/Ktor implementation, renaming the
internal namespace, documenting compatible model formats in more detail, and adding a complete
in-app third-party license viewer.

## Attribution and license

This project is derived from
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) and uses the on-device
[LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) runtime. It is licensed under Apache
License 2.0; see [LICENSE](LICENSE).
