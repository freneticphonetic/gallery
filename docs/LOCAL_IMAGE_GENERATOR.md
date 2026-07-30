# Local Image Generator

The Local Image Generator is a native, offline custom tool. It uses `stable-diffusion.cpp` through
the pinned Llamatik Android wrapper; it does not use Gallery's WebView skill runner or a server.

## Use

1. Open **+ → More tools → Image Generator**.
2. Choose a single-file Stable Diffusion checkpoint from device storage.
3. Wait for the model to load.
4. Enter a prompt and, optionally, a negative prompt.
5. Adjust canvas, steps, guidance, or seed under **Generation controls**.
6. Tap **Generate locally**.
7. Use **Save PNG** to export a copy to a user-selected location.

The app requests persisted, read-only access through Android's system document picker. It reads the
checkpoint in place rather than duplicating a multi-gigabyte file.

## Supported first-release model shape

- Single-file `.safetensors`, `.ckpt`, or `.gguf` checkpoints supported by the bundled
  `stable-diffusion.cpp` version.
- Text-to-image generation.
- 512 × 512, 384 × 512, and 512 × 384 canvases.
- CPU inference using up to eight available cores.

Models that require separately configured text encoders, VAE files, ControlNet, or LoRA files are
not yet supported by this UI.

## Operational limits

- Models commonly require multiple gigabytes of storage and RAM.
- Loading and generation may each take several minutes on a phone.
- The native wrapper does not yet provide image-generation progress or safe cancellation. Keep the
  app open until the current operation completes.
- An empty result usually means an incompatible checkpoint or insufficient memory. Try a smaller
  SD 1.x checkpoint before a larger SDXL, SD3, or FLUX model.

## Privacy boundary

- The app has no Android internet permission.
- No prompt, checkpoint, seed, or generated image leaves the device.
- The model is opened read-only through the Storage Access Framework.
- Generated working files stay in app-specific storage until the user explicitly exports a PNG.
