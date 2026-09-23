# Activity-Diagram Conventions for Mermaid Flowcharts

Mapping of UML activity diagram elements to Mermaid `flowchart` syntax, plus naming
rules, patterns, and a review checklist for authoring user action flows.

## Element Mapping

| UML activity diagram element | Mermaid flowchart syntax | Example |
| --- | --- | --- |
| Start node (filled circle) | Circle node | `S((Start))` |
| End node (bullseye) | Double-circle node | `EN(((End)))` |
| Action / activity (user-visible) | Rectangle node, quoted label | `A["User clicks Create button"]` |
| Decision (branch) | Diamond node, question label | `D{"Input valid?"}` |
| Guard condition / outcome | Labeled edge | `D -->|Failed| E` |
| Error / exception path | Rectangle action + loop-back edge | `E["Error toast: please try again"]; E --> C;` |
| Sequential flow | Arrow edge | `A --> B` |
| Note | Round-edged node (use sparingly) | `N1(["Assumption: ..."])` |

## User-Visibility Rule (the defining constraint)

A user action flow contains ONLY actions the user can see and interact with. It
describes the user experience, never the implementation:

- Allowed: clicks, typing, file selection, dialogs opening, inline field errors,
  toasts, page redirects, rendered content, anything the user perceives.
- Forbidden: API calls ("send request to server"), client-side computation
  ("compute rows, cols, paths"), database writes, auth checks, serialization,
  or any other under-the-hood operation. Those belong in the system sequence
  diagram (see the `sequence-diagram` skill), which implements this flow.

Decisions are allowed only when their outcome is user-perceivable: "Input valid?"
(user sees an inline error or not), "Create result?" (user sees a toast or a
redirect). A decision whose branches differ only internally (e.g. "cache hit?")
must not appear.

Place validation decisions where the user experiences the validation. If input
checking happens when the user clicks the submit button, the decision sits right
after that click:

```
C --> D["User clicks <submit button> in dialog"];
D --> E{"Input valid?"};
E -->|Invalid <field 1>| F["Inline error above <field 1> input"]; F --> C;
E -->|Missing <field 2>| G["Inline error above <field 2> input"]; G --> C;
E -->|Valid| H{"<Operation> result?"};
```

## Node Naming Rules

- Node ids: single uppercase letters in order of appearance — `A`, `B`, `C`, ... —
  plus `S` reserved for the start terminal and `EN` reserved for the end terminal.
- All wording lives in node labels, never in ids.
- Always quote labels: `["..."]` for actions, `{"..."}` for decisions. Unquoted
  labels break on `&`, `?`, `:`, `,`, and parentheses.
- Action labels are user-perspective verb phrases describing what the user does
  or sees: "User clicks Create button", "User enters number of pieces",
  "Redirect to puzzle page" (a redirect is visible navigation).
- Decision labels are questions ending in `?` about user-perceivable outcomes:
  "Input valid?", "Create result?", "Payment authorized?".
- Edge labels are outcomes, capitalized: `|Success|`, `|Failed|`, `|Yes|`, `|No|`,
  `|Valid|`, `|Invalid piece count|`, `|No image file|`.

## Error-Retry Loop Pattern

Every decision that can fail produces user-visible error feedback that loops back
to the step that should be retried — an error path must never dead-end and never
reach `EN`:

```
D --> E{"<Operation> result?"};
E -->|Failed| F["Error toast: please try again"];
F --> D;
E -->|Success| G["Next user-visible action"];
```

If failure is unrecoverable, terminate that branch with its own end terminal
(e.g. `ENX(((End — exit flow)))`) rather than leaving it dangling — but prefer
retry loops for user action flows.

## Full Worked Template

```
flowchart TD;
S((Start)) --> A["User clicks <entry point> button"];
A --> B["Open dialog: <form fields>"];
B --> C["User fills in <inputs>"];
C --> D["User clicks <submit button> in dialog"];
D --> E{"Input valid?"};
E -->|Invalid <field 1>| F["Inline error above <field 1> input"]; F --> C;
E -->|Missing <field 2>| G["Inline error above <field 2> input"]; G --> C;
E -->|Valid| H{"<Operation> result?"};
H -->|Failed| I["Error toast: please try again"]; I --> D;
H -->|Success| J["Redirect to <target page>"];
J --> K["Render <final state>"];
K --> EN(((End)))
```

## Review Checklist

Before delivering a user action flow, verify every item:

1. Exactly one `S((Start))` terminal, placed before the first action.
2. Exactly one `EN(((End)))` terminal, reached only by the success path.
3. Every node is a user-visible action or feedback — no API calls, no computation, no storage, no system internals.
4. Input-validation decisions sit after the click that triggers them, not on field entry (unless the user genuinely sees live validation).
5. Every decision is a diamond with a question label ending in `?` about a user-perceivable outcome.
6. Every edge leaving a decision carries an outcome label.
7. Every error action loops back to the step being retried.
8. All action and decision labels are quoted.
9. Statement separator is `;` and the header is `flowchart TD;`.
10. Reading top-to-bottom narrates the user story from entry to final success state.
