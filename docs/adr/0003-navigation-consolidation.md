# ADR 0003: Unified Home Composer and Consolidated Navigation

- Status: Accepted
- Date: 2026-07-30

## Context

The current home screen presents the application's tools as a vertical collection of large cards.

This makes every available feature visible, but it also causes the home screen to function as a complete feature catalog. As more tools and settings are added, the screen becomes longer, more repetitive, and less useful as an everyday workspace.

Several tool entries repeat the same model-readiness instructions.

The project should preserve all existing tools while reducing clutter and making the primary screen more contextual.

## Decision

Make the home screen a composer-first workspace instead of a tool catalog.

The home workspace provides:

- One persistent text field.
- A `+` control that opens the complete tool and input picker.
- A microphone control that opens Audio Scribe's local recording flow.
- A send control.
- A visible selected-tool and compatible-model context.

The user explicitly selects the intended tool. The application does not ask a local model to infer
which task runtime should receive a prompt.

The tool picker includes every existing tool:

- AI Chat.
- Ask Image.
- Audio Scribe.
- Prompt Lab.
- Agent Skills.
- Mobile Actions.
- Tiny Garden.

It also provides access to Local Models. Other application-level destinations remain reachable
through the app bar or their existing navigation:

- Local Models.
- Conversations or recent activity, when available.
- Permissions.
- Settings.
- Offline and privacy information.
- About and licenses.

### Prompt delivery

Each tool retains its existing runtime, model initialization, capability checks, and specialized
screen:

| Tool | Home-composer behavior |
| --- | --- |
| AI Chat | Submit text after opening the compatible local model. |
| Agent Skills | Submit text after opening the compatible local model. Images, audio, and skill selection remain available in its composer. |
| Ask Image | Carry text forward as an editable draft so an image can be attached before sending. |
| Audio Scribe | Carry text forward as an editable draft so audio can be recorded or attached before sending. |
| Prompt Lab | Carry text forward as editable Prompt Lab input. |
| Mobile Actions | Submit text to the existing constrained device-action runtime. |
| Tiny Garden | Submit text to the existing garden runtime. |

When more than one compatible model is ready, the home workspace prefers the already selected
compatible model and otherwise uses the first ready model. The destination screen retains its
existing model selector.

The microphone control is a shortcut to Audio Scribe's recorder and local audio-capable model. It
does not use unrestricted networking or add a new permission. Microphone permission continues to
be requested only when recording is started.

## Home-screen role

The home screen is a contextual workspace. It emphasizes:

- Whether a compatible model has been imported.
- The currently selected or recently used model.
- A clear first-run import action.
- The selected tool and compatible model.
- A primary text, attachment, or voice action.
- Current offline or privacy status.

The initial model-import experience must remain prominent when no models are available.

Once a model is ready, the tool cards are replaced by the selected-tool context and persistent
composer.

## Capability preservation

Navigation consolidation must not remove tools or make them unreachable.

Moving a feature into shared navigation is not equivalent to removing it.

The navigation refactor must:

1. Record every existing destination.
2. Map each destination to its new route.
3. Verify that each route remains reachable.
4. Verify that back navigation behaves correctly.
5. Confirm that model selection and tool capability checks still work.

## Disabled tools

A tool without a compatible imported model may remain visible but unavailable.

The interface should explain the missing requirement without repeating the same full instruction under every navigation item.

The home composer is disabled when the selected tool has no compatible ready model and provides a
direct Local Models action. The tool remains visible in the picker with its missing-model
requirement.

## Consequences

### Positive

- The main screen becomes less cluttered.
- Existing capabilities remain available.
- Navigation can scale as new settings and tools are added.
- Repetitive setup messaging can be reduced.
- Phone and tablet layouts can share the same information architecture.

### Negative

- Specialized tools still open their own working surface after the home composer routes the input.
- Media prompts require a second send after the user attaches the required image or audio.
- Navigation state and back-stack behavior will require careful testing.
- The refactor may expose assumptions currently embedded in the home-screen implementation.

## Deferred work

- Inline home-screen attachment previews may replace the media-task handoff in a later change.
- A dedicated imported-model speech-to-text service may eventually place dictated text directly
  into the home field. The current microphone path keeps transcription inside Audio Scribe.
- Recent conversations and adaptive tablet navigation remain separate design decisions.
