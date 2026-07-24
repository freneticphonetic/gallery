# ADR 0003: Consolidated Application Navigation

- Status: Proposed
- Date: 2026-07-24

## Context

The current home screen presents the application's tools as a vertical collection of large cards.

This makes every available feature visible, but it also causes the home screen to function as a complete feature catalog. As more tools and settings are added, the screen becomes longer, more repetitive, and less useful as an everyday workspace.

Several tool entries repeat the same model-readiness instructions.

The project should preserve all existing tools while reducing clutter and making the primary screen more contextual.

## Decision

Adopt a shared navigation structure for the application's major destinations.

On phones, this may be implemented as a modal navigation drawer or another suitable compact menu.

On larger screens, the same destinations may be presented through a permanent drawer, navigation rail, or other adaptive layout.

The navigation structure should include the existing tools:

- AI Chat.
- Ask Image.
- Audio Scribe.
- Prompt Lab.
- Agent Skills.
- Mobile Actions.
- Tiny Garden.

It should also provide access to application-level destinations such as:

- Local Models.
- Conversations or recent activity, when available.
- Permissions.
- Settings.
- Offline and privacy information.
- About and licenses.

## Home-screen role

The home screen should become a contextual workspace.

It may emphasize:

- Whether a compatible model has been imported.
- The currently selected or recently used model.
- A clear first-run import action.
- A primary continuation action.
- Recent activity or conversations.
- Current offline or privacy status.

The initial model-import experience must remain prominent when no models are available.

Once a model is ready, first-run guidance may collapse or be replaced by more useful ongoing information.

## Capability preservation

Navigation consolidation must not remove tools or make them unreachable.

Moving a feature into shared navigation is not equivalent to removing it.

Before completing the navigation refactor:

1. Record every existing destination.
2. Map each destination to its new route.
3. Verify that each route remains reachable.
4. Verify that back navigation behaves correctly.
5. Confirm that model selection and tool capability checks still work.

## Disabled tools

A tool without a compatible imported model may remain visible but unavailable.

The interface should explain the missing requirement without repeating the same full instruction under every navigation item.

The exact disabled-state interaction should be determined during implementation.

## Consequences

### Positive

- The main screen becomes less cluttered.
- Existing capabilities remain available.
- Navigation can scale as new settings and tools are added.
- Repetitive setup messaging can be reduced.
- Phone and tablet layouts can share the same information architecture.

### Negative

- Some tools will require opening navigation rather than appearing directly on the home screen.
- Navigation state and back-stack behavior will require careful testing.
- The refactor may expose assumptions currently embedded in the home-screen implementation.

## Open questions

- Which tool should be the default working destination after a model is selected?
- Should recent conversations appear on the home screen, in navigation, or both?
- Should model selection appear in the top app bar, navigation header, or workspace content?
- Which navigation pattern best adapts to tablets without duplicating implementation?
