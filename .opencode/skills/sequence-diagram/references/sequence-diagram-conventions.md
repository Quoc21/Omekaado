# Sequence Diagram Conventions (System Flow from a User Action Flow)

Mapping rules for turning a user action flow into a Mermaid system sequence
diagram, plus message conventions, patterns, a worked template, and a review
checklist.

## Mapping: User Action Flow → Sequence Diagram

| User action flow element | Sequence diagram representation |
| --- | --- |
| User action (`User clicks Create button`) | `U->>C: Click Create button` |
| User input (`User enters number of pieces`) | `U->>C: Enter number of pieces` |
| Inline field error | `C-->>U: Error above <field> input` (inside the validation `alt`) |
| Error toast | `C-->>U: Toast error, please try again` (inside the failure `alt`) |
| Redirect to a page | `C->>C: Redirect to <target page>` (self-interaction) |
| Rendered final state | `C-->>U: Render <final state>` |
| Decision diamond `{"Input valid?"}` | `alt` fragment — one `else` branch per outcome |
| Decision `{"Create result?"}` | `alt create failed` / `else create succeeded` around server responses |
| Under-the-hood work (not in user flow) | Added here: `C->>C`, `C->>S`, `S->>D`, `D-->>S`, `S-->>C` messages |

The user action flow never contains system internals; the sequence diagram must
contain them. The two diagrams overlap exactly on user-visible actions and
feedback — everything else in the sequence diagram is the system machinery that
produces that experience.

## Participant and Message Conventions

- Participants, in typical order: `U as User`, `C as Client`, `S as Server`,
  `D as DB`; add external services (`P as Payment gateway`) as required.
- Solid arrow with two chevrons (`->>`) for a request/action; dashed arrow with
  two chevrons (`-->>`) for a response/return.
- Requests between system participants carry the payload summary:
  `C->>S: POST create puzzle (image, width, height, rows, cols)`.
- Responses carry a status code plus the data the caller needs:
  `S-->>C: 201 Created with puzzle id`, `S-->>C: 500 Internal Server Error`.
- Internal work is a self-interaction, never a Note:
  `C->>C: Compute rows, cols and piece paths from image width and height`.
- Message text: avoid `<`, `>`, and `;` (they break parsing); write "below 4",
  "above max". Parentheses, commas, and colons after the participant prefix are safe.
- Use `;` as the statement separator and declare `autonumber;` first so steps read like a specification.

## Fragments

- `alt <condition>` / `else <condition>` / `end`: one branch per outcome of a
  user action flow decision. Validation decisions become one `alt` with a branch
  per invalid field plus a final `else input valid` branch.
- Nest fragments when a system-level outcome follows a user-level branch (e.g.
  create success/failure `alt` nested inside the `else input valid` branch).
- `loop <condition>`: only for genuinely repeating interactions (polling,
  pagination); form-validation retries are implicit — do not add loop fragments
  or "user can try again" notes for them.

## Validation Placement

Validation in the sequence diagram must occur where the user action flow shows
it. If the user action flow validates after the user clicks the submit button:

```
U->>C: Click Create button;
alt piece count invalid (below 4 or above max);
C-->>U: Error above pieces input;
else no image file;
C-->>U: Error above image input;
else input valid;
C->>C: <internal computation>;
C->>S: <request>;
...
end;
```

## Full Worked Template

```
sequenceDiagram;
autonumber;
participant U as User;
participant C as Client;
participant S as Server;
participant D as DB;
U->>C: <user input 1>;
U->>C: <user input 2>;
U->>C: Click <submit button>;
alt <field 1> invalid (<rule>);
C-->>U: Error above <field 1> input;
else <field 2> missing;
C-->>U: Error above <field 2> input;
else input valid;
C->>C: <client-side computation>;
C->>S: <HTTP verb> <resource> (<payload>);
S->>D: <write/read operation>;
alt <operation> failed;
S-->>C: 5xx <error>;
C-->>U: Toast error, please try again;
else <operation> succeeded;
S-->>C: 2xx <status> with <id/data>;
C->>C: Redirect to <target page>;
C-->>U: Render <final state>;
end;
end;
```

## Review Checklist

Before delivering a sequence diagram, verify every item:

1. Every user action and every piece of user-visible feedback in the source user action flow appears as a message.
2. Every edge case in the user action flow (each invalid-field branch, each failure branch) has a corresponding `alt`/`else` branch.
3. Validation messages sit after the click that triggers them, matching the user action flow.
4. All under-the-hood work is explicit: computation, requests, DB operations — nothing reduced to a Note.
5. Every request has an explicit response with a status code; success responses carry the data the caller needs next.
6. Redirects are `C->>C` self-interactions; internal computations are `C->>C` or `S->>S` self-interactions.
7. No retry notes and no gratuitous `loop` fragments for implicit retries.
8. No `<`, `>`, or `;` inside message text; statement separator is `;`; `autonumber;` declared.
9. The success path ends with the same final user-visible state as the user action flow.
