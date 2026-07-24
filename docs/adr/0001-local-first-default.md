# ADR 0001: Local-First Default

- Status: Accepted
- Date: 2026-07-24

## Context

Offline AI Gallery was created from a project that included cloud-oriented services, online model discovery, authentication, remote downloads, and Google-specific integrations.

The fork has removed those requirements so that compatible models can be imported from local storage and used without network access.

Future development may introduce optional connected capabilities, but those capabilities must not make ordinary local use dependent on a server or account.

## Decision

Offline AI Gallery will remain local-first.

The default application must support its core workflow without a network connection:

1. Launch the application.
2. Import a compatible model from local storage.
3. Store and manage the model on the device.
4. Select a compatible tool.
5. Run inference locally.
6. Store ordinary application data locally.

The default application will not require:

- A user account.
- OAuth.
- A remote model catalog.
- Automatic model downloads.
- Analytics.
- Telemetry.
- Cloud backup.
- Remote inference.
- Google Play Services.

Local-first describes the application's operating foundation. It does not permanently prohibit all future network features.

## Consequences

### Positive

- The application remains useful without connectivity.
- Privacy claims can be verified through application behavior and permissions.
- Users retain direct control over model acquisition and storage.
- Core functionality does not disappear when an external service changes or shuts down.
- The project remains independent of a specific model host or account provider.

### Negative

- Users must acquire compatible model files outside the application.
- Model compatibility may require more documentation.
- Some convenient cloud-oriented workflows are unavailable.
- Future connected functionality will require a separately designed boundary.

## Guardrails

- Do not restore remote model downloading as incidental cleanup.
- Do not restore OAuth because dormant upstream code expects it.
- Do not add a network dependency merely to simplify a local workflow.
- Do not describe network absence as a temporary defect.
- Any future connected feature must preserve the local workflow.
