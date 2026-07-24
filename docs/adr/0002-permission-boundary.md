# ADR 0002: Application Permission Boundary

- Status: Accepted
- Date: 2026-07-24

## Context

The application contains tools that may interact with Android features such as the camera, microphone, calendar, notifications, or boot-completed events.

The upstream project also contained network and Google-service permissions associated with cloud functionality that this fork has removed.

Permissions affect the application's privacy boundary and should not be treated as incidental implementation details.

At the same time, the project should not prescribe elaborate permission flows before individual features have been reviewed and designed.

## Decision

Application permissions must not be added or broadened unless the assigned task explicitly requires the change.

Every permission change must identify:

- The feature requiring it.
- Why the existing permission set is insufficient.
- Whether the permission affects the whole package.
- Whether a narrower implementation is possible.
- How the change affects the project's local-first and security boundaries.

Dependencies must not be allowed to silently restore removed permissions through manifest merging.

Network permission is considered a separate architectural decision and is governed by ADR 0004.

## Consequences

### Positive

- Permission changes become intentional and reviewable.
- Dormant upstream dependencies cannot quietly widen application access.
- Agents and contributors have a clear boundary without being forced to invent premature permission UX.
- The application's privacy claims remain connected to verifiable technical behavior.

### Negative

- Some feature work may require additional review before implementation.
- Existing permissions may need to be examined individually over time.
- Contributors cannot assume that a dependency's requested permissions are acceptable.

## Current policy

- Existing permissions are not automatically removed by this decision.
- Existing permissions should be reviewed when work touches the associated feature.
- No permission should be added as unrelated cleanup.
- No permission should be added solely because dormant upstream code expects it.
- Permission explanations, request timing, and fallback behavior should be designed at the feature level rather than imposed universally in advance.

## Validation

When a change may affect permissions:

1. Inspect the source manifest.
2. Inspect the merged manifest or built APK.
3. Report any added or removed permissions.
4. Confirm that transitive dependencies did not widen the permission set.
