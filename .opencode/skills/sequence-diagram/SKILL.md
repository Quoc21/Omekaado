---
name: sequence-diagram
description: This skill should be used when creating or editing a system sequence diagram for a feature — a Mermaid sequenceDiagram that reflects, as exactly as possible, the under-the-hood system flow that implements the feature (API requests, internal computations, database operations) and handles every edge case that appears in the feature's user action flow. Triggers on requests like "create a sequence diagram for feature X", "show the system flow behind this user flow", or "draw the API/database interactions for this feature". Takes the user action flow (from the user-action-flow skill) as its input contract; for publishing the diagram to a Confluence page, use the confluence-mermaid-diagram skill instead.
---

# Sequence Diagram

## Purpose

Produce system sequence diagrams as Mermaid `sequenceDiagram` source that show, as
exactly as possible, how the system operates under the hood to handle a feature:
participants (User, Client, Server, DB / external services), the messages between
them (requests, responses with status codes, data), internal work as
self-interactions, and every branch and edge case — with the feature's user action
flow as the input contract. The user action flow defines what the user sees; this
diagram defines how the system delivers it.

## When to Use This Skill

- Specifying the implementation flow (client / server / DB interactions) behind a feature
- Deriving the system flow from an existing user action flow
- Reviewing a sequence diagram for missing edge cases, missing responses, or wrong interaction direction

## Core Conventions

1. Declare `sequenceDiagram;` + `autonumber;`, then participants with short ids and `as` labels: `participant U as User;`, `participant C as Client;`, `participant S as Server;`, `participant D as DB;`. Add external services as needed. Use `participant` (not `actor`) for render compatibility.
2. Derive participants from the user action flow: user-visible actions become `U->>C` messages; feedback the user sees becomes `C-->>U` return messages; redirects become a `C->>C` self-interaction.
3. Show under-the-hood operations explicitly — this is the diagram's whole point: client-side computation, request construction, server-side processing, DB reads/writes (`C->>C`, `C->>S`, `S->>D`, `D-->>S`, `S-->>C`).
4. Internal work on a participant is a self-interaction (`C->>C: Compute rows, cols and piece paths`), never a `Note`.
5. Every request gets an explicit response with a status code: `S-->>C: 201 Created with puzzle id`, `S-->>C: 500 Internal Server Error`. A request without a response is an error.
6. Every decision in the user action flow becomes an `alt` / `else` / `end` fragment — one branch per outcome, including every field-validation edge case and the success case. Nest `alt` fragments when a system outcome branches after a user-level branch.
7. Validation timing must match the user action flow: if input checking happens after the user clicks the submit button, the validation `alt` sits after the `Click` message.
8. Do not use `Note` for retry semantics; retries are implicit (the user simply repeats earlier messages). Only use `Note` for assumptions/constraints that are not interactions.
9. Use `;` as the statement separator; avoid `<`, `>`, and `;` inside message text (write "below 4" instead of "< 4"); parentheses and commas are safe.

Full mapping table, patterns, worked template, and checklist: read `references/sequence-diagram-conventions.md`.

## Workflow

1. Take the feature's user action flow (from the `user-action-flow` skill) as the input contract. If none exists, author it first.
2. List every user-visible action and feedback from the flow, then decide for each what the system does behind it: which participant acts, what is computed, what is sent, what is stored.
3. Draft the sequence: user actions as `U->>C` messages, feedback as `C-->>U` returns, internals as self-interactions and cross-participant messages, every flow decision as an `alt` fragment with all its edge-case branches.
4. Verify against the checklist in `references/sequence-diagram-conventions.md` — especially: every user action flow decision has a corresponding fragment, every request has a response, no internal work is hidden in a Note.
5. Deliver the Mermaid source. If the target is a Confluence page, stop authoring here and follow the `confluence-mermaid-diagram` skill to encode and publish it — never hand-edit the macro HTML.

## References

- `references/sequence-diagram-conventions.md` — user action flow → sequence diagram mapping, participant/message/response conventions, validation placement, worked template, and review checklist.
