# ADR 0004: Controlled Future Network Access

- Status: Proposed
- Date: 2026-07-24

## Context

The current application is intentionally usable without network permission.

Future features may benefit from controlled connectivity, including:

- Web search.
- Retrieval of a user-approved document or URL.
- Explicitly connected agent tools.
- Optional access to documentation or metadata.
- Other narrowly defined operations.

Granting unrestricted network access to every loaded model would weaken the project's privacy and security model.

Android network permission applies to the application package rather than to an individual model. A model therefore cannot be meaningfully granted package-level network permission as though it were an isolated Android user.

## Decision

Do not add network permission until a concrete connected feature and its security boundary have been designed and approved.

Future network operations should be mediated by an application-controlled broker.

A model may request a defined operation, but it must not receive unrestricted access to arbitrary sockets, credentials, browser state, or application networking APIs.

Possible broker operations may include:

- `search(query)`
- `fetchDocument(url)`
- `retrieveApprovedResource(identifier)`

The final operation set must be defined by the implementation task rather than assumed in advance.

## Required properties

A future network broker should provide:

- Explicitly defined operations.
- Application-controlled request validation.
- Timeouts.
- Response-size limits.
- Redirect limits.
- MIME-type validation where relevant.
- Blocking of private or local network targets unless explicitly supported.
- Separation from stored credentials and account sessions.
- Sanitized results returned to the model.
- Source metadata visible to the user.
- Logging or an inspectable activity record where appropriate.
- User approval for sensitive operations.

The model should receive only the information needed to complete the approved task.

## Packaging options

Possible implementations include:

1. A separate connected build variant with a distinct application ID.
2. A separate companion application that owns network permission.
3. Another architecture that establishes an equivalent enforceable boundary.

A separate process inside the same permission-bearing package is not, by itself, considered a complete permission boundary.

The final packaging decision remains open.

## Current consequences

Until this ADR is accepted and implemented:

- The default application remains without network permission.
- Existing local tools should not be redesigned around assumed future connectivity.
- Dormant HTTP, MCP, OAuth, or remote-download code must not be reactivated.
- Agents must not add network permission as an incidental part of another task.

## Future review

Before accepting an implementation, the project should document:

- The exact user-facing feature.
- The exact data transmitted.
- The destinations contacted.
- The model's allowed request vocabulary.
- User approval behavior.
- Credential handling.
- Logging and deletion behavior.
- Offline behavior when the connected component is unavailable.
