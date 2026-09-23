---
name: user-action-flow
description: This skill should be used when creating or editing a user action flow diagram — a Mermaid flowchart authored in UML activity-diagram style with explicit Start/End terminal nodes, decision diamonds, labeled outcome edges, and error-retry loops, containing ONLY actions the user can see and interact with (no under-the-hood operations such as API calls, client-side computation, or database writes). Triggers on requests like "create a user action flow", "draw the user flow for feature X as an activity diagram", or "add start/end nodes so the flowchart reads like an activity diagram". For authoring conventions only; for publishing the diagram to a Confluence page, use the confluence-mermaid-diagram skill instead.
---

# User Action Flow

## Purpose

Produce user action flow diagrams as Mermaid `flowchart` source that read like UML
activity diagrams and reflect exactly what the user expects to do: a single explicit
start terminal, a single explicit end terminal, actions as rectangles, decisions as
diamonds with labeled outgoing edges, and error paths that loop back for retry
instead of dead-ending. The flow is written purely from the user's point of view —
it is a specification of the user experience, not of the system implementation.

## When to Use This Skill

- Creating a new user action flow for a feature or user story
- Converting an existing plain flowchart into activity-diagram style
- Reviewing a flow for missing terminals, unlabeled decision edges, dead-end error paths, or leaked system internals

## Core Conventions

1. Include ONLY actions the user can see and interact with: clicks, typing, file selection, dialogs, visible feedback (inline errors, toasts, redirects, rendered content). NEVER include under-the-hood operations — no API calls, no client-side computation, no "send request to server", no database writes. If the user cannot perceive it, it does not belong in this flow; it belongs in the system sequence diagram instead.
2. Validate input where the user experiences validation. If checking happens when the user clicks the submit button, place the input-validity decision after that click — not on entry.
3. Declare `flowchart TD;` (top-down) and use `;` as the statement separator.
4. Wrap the flow with exactly one start and one end terminal:
   - Start: `S((Start))` — circle
   - End: `EN(((End)))` — double circle (approximates the UML bullseye)
5. Actions: `A["Verb phrase describing what the user does or sees"]` — user perspective.
6. Decisions: `D{"Question?"}` — phrased as a question about a user-perceivable outcome; multi-way decisions (one per field error plus the valid case) are fine.
7. Label every edge leaving a decision with its outcome: `D -->|Success| F` and `D -->|Failed| E`.
8. Error handling: error feedback (inline field error, toast) loops back to the step to retry: `E --> C;` — never leave an error path dangling or flowing to End.
9. Short single-letter node ids (A, B, C...; S for start, EN for end) with all wording in the node labels.
10. Quote all node labels with `["..."]` to avoid parsing issues with `&`, `?`, `:` and other special characters.

Full mapping table, naming rules, and templates: read `references/activity-diagram-conventions.md`.

## Workflow

1. Gather the user-visible journey: entry action, each interaction the user performs, the feedback the user sees at each point (including where validation feedback appears), and the final visible success state.
2. Apply the Core Conventions above to draft the Mermaid source — user-visible actions and feedback only.
3. Verify the draft against the checklist in `references/activity-diagram-conventions.md` (terminals present, decision edges labeled, error loops return to a retry point, exactly one End, no system internals).
4. Deliver the Mermaid source. If the target is a Confluence page, stop authoring here and follow the `confluence-mermaid-diagram` skill to encode and publish it — never hand-edit the macro HTML.
5. The resulting flow is the contract for the user experience. To specify how the system implements it (API calls, computations, storage, and every edge case behind each decision), follow the `sequence-diagram` skill.

## References

- `references/activity-diagram-conventions.md` — UML activity → Mermaid flowchart mapping, node naming rules, error-loop patterns, full worked template, and review checklist.
