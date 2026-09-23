---
name: confluence-mermaid-diagram
description: Use this skill whenever creating, editing, or translating a Mermaid diagram (flowchart, sequence diagram, etc.) inside a Confluence page via the Atlassian MCP connector — including "add a diagram to this Confluence page", "translate the diagram on this page", "fix the flowchart on this wiki page", or "update the mermaid diagram". The confluence-mermaid-macro extension stores diagram source as doubly-JSON-encoded text inside an HTML attribute, so hand-editing the raw HTML (simple find/replace) reliably produces broken or garbled diagrams even when the save API call reports success. Always use this skill's script instead of manually constructing or editing the macro HTML.
---

# Confluence Mermaid Diagram (via Atlassian MCP)

## Why this skill exists

Confluence pages render Mermaid diagrams using the third-party
`confluence-mermaid-macro` extension. Its diagram source is stored like this:

```
<div data-type="extension" data-extension-key="confluence-mermaid-macro" ...
     data-parameters="{&quot;macroParams&quot;:{...,&quot;__bodyContent&quot;:
       {&quot;value&quot;:&quot;[{\&quot;body\&quot;:\&quot;flowchart TD...&quot;}"></div>
```

There are **three nested encoding layers** between the plain Mermaid text you want
to write and the HTML you actually send to Confluence:

1. The Mermaid source is JSON-encoded once as the `"body"` field of `{"body": ..., "date": ...}`.
2. That whole JSON array is JSON-encoded *again* as a string, to become `__bodyContent.value`.
3. The resulting JSON object (`macroParams` + `macroMetadata`) is serialized and
   HTML-attribute-escaped (`"` → `&quot;`, `<` → `&lt;`, `>` → `&gt;`, etc.) to
   become the `data-parameters` attribute.

If you hand-edit this HTML (e.g. simple text substitution to translate labels),
it is very easy to get the escaping wrong — usually under-escaping by a
backslash level. **The Atlassian MCP save call will still return `ok:true`** —
Confluence accepts the write — but the macro then fails to render, or renders
garbled/backslash-laden text. This is a silent failure mode: you must not trust
`ok:true` alone as proof the diagram renders correctly.

## Always use the script, never hand-edit the HTML

Use `scripts/build_mermaid_macro.py` for both directions:

- **Building/updating a diagram**: give it plain Mermaid source, get back the
  exact `<div ...></div>` HTML to pass as `body.value` (format `"html"`) to
  `createConfluenceContent` / `updateConfluenceContent`. It round-trip-verifies
  the output itself before writing the file, so a bad encode fails loudly
  instead of silently.
- **Verifying/reading an existing diagram**: give it the macro HTML (e.g. what
  a `getConfluenceContent` call returned), get back the plain Mermaid source —
  useful to confirm what a page *actually* contains, or as the starting point
  for a translation/edit.

```bash
# Build new/updated macro HTML from mermaid source
python3 scripts/build_mermaid_macro.py build \
  --mermaid-file diagram.mmd \
  --title "Mermaid diagram" \
  --out macro.html

# Or inline:
python3 scripts/build_mermaid_macro.py build \
  --mermaid 'flowchart TD; A["Start"] --> B["End"]' \
  --out macro.html

# Decode an existing macro's HTML back to plain mermaid (sanity check / editing base)
python3 scripts/build_mermaid_macro.py decode --html-file existing_macro.html
```

Prefer `;` as the statement separator in the Mermaid source (`flowchart TD; A --> B; B --> C`)
over literal newlines. Both work through this script, but semicolons avoid any
`\r\n` vs `\n` ambiguity and match what Confluence's own editor normalizes to.

## End-to-end workflow

1. **Get the page's current content and snapshot token** via
   `mcp__Atlassian_MCP__getConfluenceContent` (`detail: "full"`, `content_format: "html"`).
   Note the `snapshotToken` in the response — you need the latest one to update.
2. **If editing/translating an existing diagram**: run the script's `decode`
   command on the returned `body.value` HTML to get the plain Mermaid source.
   Edit that plain text (translate labels, change flow, etc.) — this is just
   editing a normal Mermaid flowchart, nothing Confluence-specific.
3. **Build the new macro HTML** with the script's `build` command from the
   edited Mermaid source. Keep the same `title`; a new `--local-id`/`--macro-id`
   is fine (the script generates one automatically) unless you want to preserve
   the exact original IDs, in which case pass `local_id=`/`macro_id=` explicitly
   in a small custom call to `build_macro_html()`.
4. **Update the page** with `mcp__Atlassian_MCP__updateConfluenceContent`,
   passing the built HTML as `body: {"format": "html", "value": "<the div...>"}`,
   the `cloudId` (the site's base URL, e.g. `https://yoursite.atlassian.net`
   works fine as the cloudId value), `contentUrl` (or the page id), the
   `snapshotToken` from step 1, and a descriptive `versionMessage`.
5. **Verify**: re-fetch the page with `getConfluenceContent` and run the
   script's `decode` command on the returned HTML. Confirm the decoded Mermaid
   text matches what you intended to write. Do not skip this — it is the only
   way to catch a broken escape chain, since the update call itself will
   report success even when the diagram won't render.

## Common pitfalls

- **Do not** manually add/remove backslashes when editing macro HTML directly
  — always regenerate through the script from plain Mermaid source instead.
- **Do not** trust `ok:true` from the update call as proof the diagram
  renders. Always decode-and-diff after writing.
- If a page's diagram appears to already have inconsistent/odd escaping when
  you decode it (e.g. `decode` raises a `JSONDecodeError`), do not try to
  patch around it with more manual escaping — extract whatever partial
  Mermaid text you can identify by eye, treat it as the source of truth, and
  rebuild the whole macro fresh with the `build` command.
- `cloudId` for `getConfluenceContent`/`updateConfluenceContent` can be the
  site's base URL (e.g. `https://yoursite.atlassian.net`) rather than a raw
  cloud UUID — the tool resolves it.
