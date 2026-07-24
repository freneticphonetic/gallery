# Agent Instructions

## Mission

Offline AI Gallery is an independent Android application for running compatible AI models locally.

The project preserves the useful capabilities inherited from Google AI Edge Gallery while removing unnecessary cloud dependencies, simplifying the interface, and establishing clear privacy and security boundaries.

Before modifying the repository, read:

1. This file.
2. `docs/PROJECT_CHARTER.md`.
3. Any relevant accepted Architecture Decision Records in `docs/adr/`.
4. The files directly related to the assigned task.

## Core project rules

1. Preserve existing user-facing capabilities unless the assigned task explicitly authorizes removing one.

2. Interface simplification means reorganizing and consolidating functionality, not deleting it merely to reduce the number of visible controls.

3. The default application remains local-first and usable without a network connection.

4. Do not add or broaden application permissions unless the assigned task explicitly requires it.

5. Do not add analytics, telemetry, advertising, cloud backup, mandatory accounts, Firebase services, OAuth, remote model catalogs, or automatic model downloads unless explicitly authorized by an accepted project decision.

6. Models are imported from local files and inference remains on-device by default.

7. Models, prompts, skills, and agent-controlled code must not receive unrestricted access to the network, filesystem, credentials, or device services.

8. Future network features must use an explicitly designed and approved boundary. Do not add network permission as part of unrelated work.

9. Dormant upstream code, dependencies, comments, and configuration are historical material. Their presence does not authorize restoration of removed upstream behavior.

10. Prefer small, reviewable changes over broad rewrites.

## Scope discipline

- Make only the changes needed for the assigned task.
- Do not perform unrelated cleanup.
- Do not replace architecture merely because another structure appears cleaner.
- Do not remove an apparently unused feature without first confirming that it is obsolete.
- Do not alter permissions, manifests, application IDs, persistence formats, signing configuration, or security boundaries unless they are explicitly within scope.
- Do not restore removed upstream services merely because dormant code or dependencies still reference them.
- When project documents conflict, stop and report the conflict instead of silently choosing one.
- Record significant architectural changes in `docs/adr/`.

## Interface rules

- Keep all existing tools reachable unless their removal is explicitly authorized.
- Treat the main screen as a working area rather than a complete catalog of every feature.
- Shared navigation may contain tools, model management, permissions, settings, privacy information, and licenses.
- Disabled tools should explain what model capability or device feature is required.
- Avoid repeating identical setup instructions beneath every tool.
- Preserve clear first-run model import and model-readiness information.
- Do not interpret visual simplification as permission to reduce functionality.

## Security boundaries

- The offline application must not silently acquire network capability through manifest merging or dependency changes.
- Models must not directly control arbitrary network requests.
- Models must not receive stored credentials, authentication tokens, cookies, or unrestricted device access.
- New integrations require an explicit host-controlled interface and documented limits.
- Security-sensitive behavior must fail closed rather than silently widening access.

## Required validation

Before declaring work complete:

- Build the affected application or module.
- Run relevant tests and static checks.
- Review the merged Android manifest when permissions or dependencies may be affected.
- Confirm that retained tools remain reachable after navigation changes.
- Confirm that no unrelated user-facing capability was removed.
- Report:
  - Files changed.
  - Validation performed.
  - Permissions or security boundaries affected.
  - Known limitations.
  - Assumptions made.

## Authority

Explicit task instructions take priority for that task.

Otherwise, use the following order:

1. `AGENTS.md`
2. Accepted Architecture Decision Records
3. `docs/PROJECT_CHARTER.md`
4. Current repository documentation
5. Upstream documentation, comments, and dormant implementation code

If a lower-authority source conflicts with a higher-authority source, do not restore the lower-authority behavior.
