#!/usr/bin/env python3
"""
Build (or verify) the HTML snippet for a confluence-mermaid-macro extension node,
suitable for use as the `body.value` (format="html") argument to the Atlassian MCP
`createConfluenceContent` / `updateConfluenceContent` tools.

Why this exists
----------------
The confluence-mermaid-macro extension stores the diagram source inside a
`data-parameters` HTML attribute containing JSON, and that JSON has a field
(`__bodyContent.value`) which is ITSELF a JSON-encoded string containing the
mermaid source. Hand-editing this (e.g. simple find/replace on the raw HTML)
is extremely easy to get subtly wrong -- under- or over-escaping the nested
quotes -- which produces a macro that saves "successfully" (no API error) but
fails to render, or renders garbled text.

This script builds the attribute correctly using Python's own json/html
encoders (so escaping is never done by hand), and can also decode an existing
macro's HTML back into plain mermaid source for verification.

Usage
-----
Build new macro HTML from a mermaid source file:
    python3 build_mermaid_macro.py build --mermaid-file diagram.mmd --out macro.html

Build from a literal string:
    python3 build_mermaid_macro.py build --mermaid "flowchart TD; A-->B" --out macro.html

Decode existing macro HTML (paste the <div ...></div> block) back to mermaid,
to verify a page renders what you think it renders:
    python3 build_mermaid_macro.py decode --html-file macro.html
"""
import argparse
import html
import json
import time
import uuid
import re
import sys


def build_macro_html(mermaid_source: str, title: str = "Mermaid diagram",
                      search_text: str = None, local_id: str = None,
                      macro_id: str = None) -> str:
    """Return the full <div ...></div> string to use as body.value (format=html)."""
    if local_id is None:
        local_id = str(uuid.uuid4())
    if macro_id is None:
        macro_id = str(uuid.uuid4())
    if search_text is None:
        # cheap plain-text index: strip mermaid syntax noise
        search_text = re.sub(r'[\[\]{}|;"\\]|-->|<br\s*/?>', ' ', mermaid_source)
        search_text = re.sub(r'\s+', ' ', search_text).strip()

    timestamp_ms = int(time.time() * 1000)

    # Layer B: the array-of-{body,date} object, as a JSON string.
    # json.dumps handles ALL escaping of quotes/backslashes/newlines in
    # mermaid_source correctly -- do not hand-escape this yourself.
    layer_b_text = json.dumps([{"body": mermaid_source, "date": timestamp_ms}])

    macro_params = {
        "panZoom": {"value": ""},
        "zoom": {"value": ""},
        "look": {"value": "classic"},
        "download": {"value": ""},
        "searchText": {"value": search_text},
        "fullscreen": {"value": ""},
        "theme": {"value": "default"},
        "disableUseMaxWidth": {"value": ""},
        "copy": {"value": ""},
        "alignment": {"value": "left"},
        "exportWidth": {"value": ""},
        # Layer C happens automatically: layer_b_text is a plain python str
        # containing literal quotes; json.dumps of the OUTER object below
        # will escape it correctly for nesting. Never manually add backslashes.
        "__bodyContent": {"value": layer_b_text},
        "height": {"value": ""},
    }
    outer = {
        "macroParams": macro_params,
        "macroMetadata": {
            "macroId": {"value": macro_id},
            "schemaVersion": {"value": "1"},
            "indexedMacroParams": {"text": search_text, "type": "text"},
            "placeholder": [{
                "type": "icon",
                "data": {"url": "https://confluence-mermaid.weweave.net/images/icon.svg"}
            }],
            "title": title,
        },
    }

    full_json_text = json.dumps(outer, ensure_ascii=False)
    # This is the ONE html-escape pass: it turns the embedded mermaid arrows
    # (-->) into --&gt; and any <br> into &lt;br&gt;, and " into &quot;,
    # matching what Confluence itself produces.
    attr_value = html.escape(full_json_text, quote=True)

    return (
        f'<div data-local-id="{local_id}" data-type="extension" '
        f'data-extension-key="confluence-mermaid-macro" '
        f'data-extension-type="com.atlassian.confluence.macro.core" '
        f'data-layout="default" data-parameters="{attr_value}"></div>'
    )


def decode_macro_html(div_html: str) -> str:
    """Inverse of build_macro_html: extract the plain mermaid source, verifying
    every escaping layer parses cleanly. Raises on any malformed layer instead
    of silently returning garbage."""
    m = re.search(r'data-parameters="(.*?)"\s*>\s*</div>', div_html, re.S)
    if not m:
        raise ValueError("Could not find data-parameters attribute in the given HTML")
    attr_value = m.group(1)

    l1_text = html.unescape(attr_value)
    outer = json.loads(l1_text)  # raises JSONDecodeError if the macro is broken

    value_str = outer["macroParams"]["__bodyContent"]["value"]
    inner = json.loads(value_str)  # raises if under/over-escaped

    if not inner or "body" not in inner[0]:
        raise ValueError("Decoded structure missing body field")

    return inner[0]["body"]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    b = sub.add_parser("build", help="Build macro HTML from mermaid source")
    src = b.add_mutually_exclusive_group(required=True)
    src.add_argument("--mermaid", help="Mermaid source as a literal string")
    src.add_argument("--mermaid-file", help="Path to a file containing mermaid source")
    b.add_argument("--title", default="Mermaid diagram")
    b.add_argument("--out", required=True, help="Where to write the resulting HTML")

    d = sub.add_parser("decode", help="Decode existing macro HTML back to mermaid source")
    dsrc = d.add_mutually_exclusive_group(required=True)
    dsrc.add_argument("--html", help="Macro HTML as a literal string")
    dsrc.add_argument("--html-file", help="Path to a file containing the macro HTML")

    args = ap.parse_args()

    if args.cmd == "build":
        mermaid_source = args.mermaid if args.mermaid else open(args.mermaid_file, encoding="utf-8").read()
        result = build_macro_html(mermaid_source, title=args.title)
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(result)
        # Round-trip verify before declaring success
        decoded = decode_macro_html(result)
        assert decoded == mermaid_source, "round-trip verification failed!"
        print(f"Wrote {args.out} ({len(result)} chars). Round-trip verified OK.")
    else:
        html_text = args.html if args.html else open(args.html_file, encoding="utf-8").read()
        print(decode_macro_html(html_text))


if __name__ == "__main__":
    main()
