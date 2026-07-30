# ADR 0005: Local Image Generation Runtime

- Status: Accepted
- Date: 2026-07-30

## Context

Gallery's Agent Skills execute JavaScript in a hidden WebView and currently have a 60-second host
timeout. They are suitable for lightweight transformations and rendered results, but not for a
multi-gigabyte diffusion checkpoint that can take minutes to initialize and run on a phone.

The offline fork also removes `INTERNET` and `ACCESS_NETWORK_STATE`. An image generator must not
introduce a cloud fallback, a model downloader, or broad storage permissions.

Google's MediaPipe Image Generator remains available but is deprecated and is no longer actively
maintained. LiteRT-LM does not currently expose an image-generation pipeline.

## Decision

Implement image generation as a native custom task, not as WebView skill code.

- Use the `com.llamatik:library-android:1.7.0` Maven artifact, which wraps
  `stable-diffusion.cpp`, and pin the version in the Gradle catalog.
- Run text-to-image inference locally with a CPU-first configuration.
- Accept only user-selected, single-file `.safetensors`, `.ckpt`, or `.gguf` checkpoints.
- Use Android's Storage Access Framework for read-only, persisted access. The model remains in its
  original location and is exposed to the native runtime through `/proc/self/fd/<fd>`.
- Keep prompt, negative prompt, seed, steps, guidance, canvas size, model path, and generated pixels
  inside the app process.
- Bound canvas dimensions and generation controls in host code.
- Save working output in app-specific storage and require an explicit Storage Access Framework
  destination when the user exports a PNG.
- Do not add Android permissions. Existing manifest removal markers continue to reject transitive
  network permissions.

The task is reachable from the home composer's **More tools** section. A prompt already entered in
the home composer is passed into the generator as a draft.

## Consequences

- The packaged application grows by approximately 52 MB compressed because the dependency includes
  native libraries for four Android ABIs.
- A compatible image checkpoint is still user-supplied and can require several gigabytes of local
  storage and RAM.
- CPU generation can take several minutes. The selected runtime does not currently expose safe
  cancellation or progress callbacks for image inference, so navigation is held while native model
  loading or generation is active.
- The first release supports text-to-image with a single checkpoint. Separate VAE, CLIP, LoRA,
  image-to-image, GPU, and NPU configuration are deferred.
- The dependency is isolated behind `LocalImageGeneratorEngine`, allowing the native backend to be
  replaced without changing the task UI or storage boundary.

## Alternatives considered

- **JavaScript Agent Skill:** rejected because of the host timeout, WebView memory constraints, and
  the absence of a maintained browser text-to-image pipeline that can load these checkpoints.
- **MediaPipe Image Generator:** rejected for a new feature because the API is deprecated and the
  sample was removed upstream.
- **Custom in-repository JNI fork:** viable, but significantly increases native build and
  maintenance surface for the initial release.
- **Remote generation:** rejected because it violates the accepted local-first and permission
  boundary decisions.
