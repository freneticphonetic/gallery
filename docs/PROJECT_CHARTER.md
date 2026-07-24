# Offline AI Gallery Project Charter

## Purpose

Offline AI Gallery is a local-first Android workspace for running compatible AI models and using them through practical on-device tools.

It began as a fork of Google AI Edge Gallery, but its design is no longer governed by the upstream product's priorities. The project may reuse useful foundations while making independent decisions about privacy, navigation, permissions, model management, and future connectivity.

## Product philosophy

### Preserve capability. Simplify access.

The application should retain useful existing tools and functionality while making them easier to understand and reach.

Interface cleanup should primarily consolidate, reorganize, and clarify. It should not achieve visual simplicity by quietly deleting capabilities.

### Local-first is the baseline

The application should remain useful when the device has no network connection.

Local model import, storage, selection, inference, and ordinary tool use should not depend on:

- A cloud account.
- A remote model catalog.
- Analytics or telemetry.
- Google Play Services.
- OAuth.
- Automatic model downloads.
- A permanently available server.

Local-first does not mean that the project can never support networking. It means local operation is the foundation rather than a degraded fallback.

### Permissions are architectural decisions

Application permissions should not be added casually or inherited merely because an upstream dependency requests them.

Adding or broadening a permission requires an explicit task and a clear feature-level reason.

The project should not invent new permission flows, onboarding screens, or fallback behavior unless the relevant feature design requires them.

### Models are not trusted application principals

A model may suggest or request an action, but the host application remains responsible for deciding whether the action is permitted and how it is performed.

Models should not receive unrestricted access to:

- Network sockets.
- Local files.
- Authentication credentials.
- Cookies or session data.
- Android services.
- Arbitrary application components.
- Other models' private state.

Any powerful capability must be exposed through a constrained, inspectable interface controlled by the application.

### Networking must be deliberate

Future networking may support features such as web search, bounded document retrieval, or explicitly connected agent tools.

Those features must not turn every loaded model into an unrestricted network client.

Network access should be mediated by a host-controlled boundary with defined operations, limits, and user authority. Until that architecture is approved and implemented, the default application should remain without network permission.

### The interface is a workspace

The home screen should help the user understand what is ready and what they can do next.

It does not need to display every tool as a full-size card at all times.

The application may use shared navigation, such as a drawer or adaptive navigation panel, to provide access to:

- AI Chat.
- Ask Image.
- Audio Scribe.
- Prompt Lab.
- Agent Skills.
- Mobile Actions.
- Tiny Garden.
- Local Models.
- Conversations or recent activity.
- Permissions.
- Settings.
- Privacy information.
- Licenses and application information.

The exact presentation may change with screen size, but the information architecture should remain coherent.

## Project priorities

The current priorities are:

1. Preserve and stabilize existing on-device capabilities.
2. Correct stale upstream documentation and configuration.
3. Establish project governance for contributors and agents.
4. Review and document application permissions.
5. Consolidate navigation and reduce home-screen clutter.
6. Continue removing dormant cloud and Google-specific implementation code.
7. Improve model compatibility, testing, and documentation.
8. Design future network functionality before granting network permission.

## Non-goals

The project is not currently intended to become:

- A cloud-hosted AI service.
- An account-driven model marketplace.
- A telemetry or advertising platform.
- A thin client that depends on remote inference.
- An unrestricted autonomous agent runtime.
- A visual reskin that preserves all upstream product assumptions.
- A smaller application produced by deleting useful functionality indiscriminately.

## Decision process

Important architectural decisions should be documented in `docs/adr/`.

An Architecture Decision Record should explain:

- The context.
- The decision.
- The consequences.
- The alternatives considered.
- Whether the decision is accepted, proposed, superseded, or rejected.

Changes that affect permissions, networking, model access, persistence, package identity, or major navigation structure should generally receive an ADR.
