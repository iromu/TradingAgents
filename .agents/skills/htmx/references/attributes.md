# htmx Attributes Reference

Attribute surface for writing htmx-powered HTML, plus the 2.x -> 4.x attribute changes.

Documents: **htmx 4.0.0**. Where a note says "2.x" it describes the older line (npm
`latest` is still `2.0.10`; `4.0.0` is on `next`; the htmx.org website still documents 2.x).

## Table of contents

- [Request attributes](#request-attributes)
- [Target and swap](#target-and-swap)
- [Trigger and inline handlers](#trigger-and-inline-handlers)
- [Request configuration](#request-configuration)
- [Status-based handling](#status-based-handling)
- [Morphing](#morphing)
- [OOB and partial updates](#oob-and-partial-updates)
- [Validation, disable, ignore](#validation-disable-ignore)
- [Extension attributes](#extension-attributes)
- [Attribute inheritance and `:inherited`](#attribute-inheritance-and-inherited)
- [Removed attributes](#removed-attributes)
- [Renamed attributes (read this first)](#renamed-attributes-read-this-first)

## Request attributes

Issue an HTTP request; the value is the URL. Any element (not just `a`/`form`) can carry them.

| Attribute | Description |
|-----------|-------------|
| `hx-get` | Issues a GET to the URL |
| `hx-post` | Issues a POST to the URL |
| `hx-put` | Issues a PUT to the URL |
| `hx-patch` | Issues a PATCH to the URL |
| `hx-delete` | Issues a DELETE to the URL |
| `hx-query` | Issues a QUERY to the URL |
| `hx-action` | Request URL when the method comes from `hx-method` |
| `hx-method` | HTTP method, paired with `hx-action` |
| `hx-boost` | Progressive enhancement for links and forms |

Default triggers: `input`/`textarea`/`select` fire on `change`, `form` fires on `submit`,
everything else fires on `click`. Override with `hx-trigger`.

```html
<button hx-post="/clicked" hx-swap="outerHTML">Click Me</button>
```

## Target and swap

| Attribute | Description |
|-----------|-------------|
| `hx-target` | Where the response goes; defaults to the element itself |
| `hx-swap` | How content is placed relative to the target (default `innerHTML`) |
| `hx-select` | CSS selector picking part of the response to swap |
| `hx-select-oob` | Pick response elements by ID for an out-of-band swap |
| `hx-swap-oob` | Mark a response element to swap out-of-band into the page |
| `hx-preserve` | Keep this element unchanged across swaps |
| `hx-push-url` | Push a URL into browser history |
| `hx-replace-url` | Replace the current URL in history |
| `hx-history-elt` | Element to restore on history navigation instead of `body` |

`hx-swap` values (space-separated modifiers follow the style): `innerHTML`, `outerHTML`,
`outerSync`, `innerMorph`, `outerMorph`, `textContent`, `before`/`beforebegin`,
`prepend`/`afterbegin`, `append`/`beforeend`, `after`/`afterend`, `delete`, `none`.

```html
<div hx-get="/data" hx-target="closest .container"
     hx-swap="innerHTML swap:100ms settle:200ms transition:true scroll:top">Load</div>
```

## Trigger and inline handlers

| Attribute | Description |
|-----------|-------------|
| `hx-trigger` | Event that triggers the request (with modifiers/filters) |
| `hx-on:<event>` | Inline script run on the event (see events-api.md) |

Trigger modifiers: `once`, `changed`, `delay:<time>`, `throttle:<time>`, `from:<selector>`,
`target:<selector>`, `prevent`, `stop` (synonym `consume`), `halt` (= `prevent stop`),
`capture`, `passive`. A selector with whitespace needs parentheses: `from:(form input)`.

Special events: `load`, `revealed`, `intersect` (options `root:<sel>`, `threshold:<float>`).
Polling: `hx-trigger="every 2s"`. Multiple triggers are comma-separated.

```html
<input hx-get="/search" hx-trigger="input changed delay:500ms, keyup[key=='Enter']"
       hx-target="#results">
```

## Request configuration

| Attribute | Description |
|-----------|-------------|
| `hx-config` | Per-element Fetch config (`timeout`, `credentials`, `cache`, ...). Cannot override `mode` |
| `hx-headers` | Add custom headers to the request (HCON/JSON) |
| `hx-vals` | Add values to the request; `js:` prefix for dynamic values (HCON/JSON) |
| `hx-include` | Include additional elements' values in the request |
| `hx-encoding` | Change encoding, e.g. `multipart/form-data` for file uploads |
| `hx-confirm` | Confirmation dialog; `js:` prefix for async confirmation |
| `hx-sync` | Synchronize requests between elements |
| `hx-indicator` | Element to show during the request (gets `htmx-request` class) |

```html
<button hx-get="/slow" hx-config='{"timeout":5000}' hx-indicator="#spinner">Load</button>
```

Parameters: non-GET/DELETE requests include enclosing form values automatically.
**GET and DELETE do NOT include form data** -- add `hx-include="closest form"` when needed.

## Status-based handling

| Attribute | Description |
|-----------|-------------|
| `hx-status:<code>` | Change target/swap/history for one status code; supports wildcards `5xx`, `50x`, `404` |

Value keys: `swap:`, `target:`, `select:`, `push:`, `replace:`, `transition:`.

```html
<form hx-post="/register" hx-target="#result"
      hx-status:422="target:#errors select:#validation-errors"
      hx-status:5xx="swap:none">
  <input name="email" type="email">
  <div id="errors"></div><div id="result"></div>
  <button type="submit">Register</button>
</form>
```

## Morphing

| Attribute | Description |
|-----------|-------------|
| `hx-morph-skip` | Freeze this element (attrs + children) during a morph swap |
| `hx-morph-skip-children` | Update attributes but freeze children during a morph swap |

Use with the `innerMorph` / `outerMorph` swap styles, which preserve focus, scroll,
listeners, and form values. Morphing cannot reset form inputs -- use `innerHTML`/`outerHTML` for that.

## OOB and partial updates

For multi-region updates the response can carry extra content:

- **Out-of-band (OOB):** a response element carries `hx-swap-oob="beforeend:#contacts-table"`
  and is swapped into the matching target by ID. In **htmx 4 OOB swaps happen AFTER the main
  swap** (2.x swapped OOB first).
- **Partial tags (new in 4):** a more general OOB. Each `<hx-partial>` names its own target
  and swap, preferred over OOB for explicit targeting.

```html
<hx-partial hx-target="#messages" hx-swap="beforeend">
  <div>New message</div>
</hx-partial>
```

An OOB/partial-only response performs no empty main swap by default. Restore the old
behavior with `htmx.config.allowEmptySwapAfterOOB = true` or `swapEmpty:true` on `hx-swap`.

## Validation, disable, ignore

| Attribute | Description |
|-----------|-------------|
| `hx-validate` | Force form elements to validate before the request |
| `hx-ignore` | Disable htmx processing for the element and its children |
| `hx-disable` | Disable the listed elements during a request |

```html
<form hx-post="/save" hx-disable="find button, find input">
  <input name="data"><button type="submit">Save</button>
</form>
```

See the rename warning below -- the 2.x meanings of `hx-disable` and `hx-ignore` are swapped
relative to 4.x, so the migration order matters.

## Extension attributes

These ship as extensions (see extensions.md). They are declared valid by the release's
`htmx.web-types.json`.

| Attribute | Extension | Description |
|-----------|-----------|-------------|
| `hx-preload` | `hx-preload.js` | Preload content on trigger events (e.g. hover) |
| `hx-pending` | `hx-pending.js` | Show custom content during a request |
| `hx-head` | `hx-head.js` | Merge `<head>` tags (`hx-head="merge"`) |
| `hx-targets` | `hx-targets.js` | Target many elements at once |
| `hx-ptag` | `hx-ptag.js` | Skip unchanged polls via `HX-PTag` |
| `hx-browser-indicator` | `hx-browser-indicator.js` | Use the browser tab's own spinner |
| `hx-live` / `hx-live:*` | `hx-live.js` | DOM-based reactive scripting |
| `hx-nonce` | -- | Per-element CSP nonce; declared valid by the release (see the hx-csp note below) |
| `hx-prompt` | `hx-prompt.js` | Show a prompt before submitting (restores 2.x syntax) |
| `hx-sse:connect`, `hx-sse:close` | `hx-sse.js` | Connect / close a Server-Sent Events stream |
| `hx-ws:connect`, `hx-ws:send` | `hx-ws.js` | Connect / send over a WebSocket |

`hx-nonce` is a distinct attribute from the `hx-csp` extension (`src/ext/hx-csp.js`); there is
no `hx-nonce` -> `hx-csp` rename in the v4 sources.

## Attribute inheritance and `:inherited`

**htmx 4 makes inheritance explicit.** An attribute on a parent is NOT inherited by children
unless you add the `:inherited` modifier. This is the single largest behavioral change from 2.x.

```html
<!-- WRONG in htmx 4: children do not inherit hx-target -->
<div hx-target="#output">
  <button hx-get="/a">A</button>
  <button hx-get="/b">B</button>
</div>

<!-- CORRECT: :inherited on the parent -->
<div hx-target:inherited="#output">
  <button hx-get="/a">A</button>
  <button hx-get="/b">B</button>
</div>
```

Worked example from the migration guide (a parent supplying target + auth headers to children):

```html
<!-- htmx 2 -->
<div hx-target="#output" hx-headers='{"X-Token":"abc"}'>
  <button hx-get="/items">Load</button>
  <button hx-delete="/item/1">Delete</button>
</div>

<!-- htmx 4 -->
<div hx-target:inherited="#output" hx-headers:inherited='{"X-Token":"abc"}'>
  <button hx-get="/items">Load</button>
  <button hx-delete="/item/1">Delete</button>
</div>
```

`:append` adds to an inherited value instead of replacing it:

```html
<div hx-include:inherited="[name='token']">
  <button hx-post="/save" hx-include:inherited:append="[name='extra']">Save</button>
</div>
```

To revert to 2.x-style automatic inheritance globally: `htmx.config.implicitInheritance = true`.

Attributes that were **implicitly inherited in 2.x** (flagged by `upgrade-check` when a
request-making descendant relies on them). Add `:inherited` where children actually depend on them:

`hx-boost`, `hx-confirm`, `hx-encoding`, `hx-headers`, `hx-include`, `hx-indicator`,
`hx-push-url`, `hx-replace-url`, `hx-select`, `hx-select-oob`, `hx-swap`, `hx-sync`,
`hx-target`, `hx-vals` (and the renamed `hx-disabled-elt`).

Do NOT blanket-add `:inherited` everywhere -- only where a descendant request reads the parent
value (see upgrade-from-2.md, Step 3).

## Removed attributes

From `upgrade-check.py` `REMOVED_ATTRS` (authoritative). The 2.x column no longer works in v4.

| 2.x attribute | Prescribed fix |
|---------------|----------------|
| `hx-vars` | Use `hx-vals` with a `js:` prefix |
| `hx-params` | Use the `htmx:config:request` event |
| `hx-prompt` | Load the `hx-prompt` extension to keep the same syntax (survives via the extension, not gone) |
| `hx-ext` | Include extension scripts directly (no attribute needed in v4) |
| `hx-disinherit` | Not needed (inheritance is explicit in v4) |
| `hx-inherit` | Not needed (inheritance is explicit in v4) |
| `hx-request` | Use `hx-config` |
| `hx-history` | Removed (no localStorage cache in v4). NOTE: `htmx.web-types.json` still declares `hx-history`; treat it as inert |

## Renamed attributes (read this first)

From `upgrade-check.py` `RENAMED_ATTRS`. **Order matters for `hx-disable`.** In 2.x
`hx-disable` meant "stop htmx processing"; in 4.x that meaning moved to `hx-ignore`, and
`hx-disable` now means "disable elements during a request". Apply the renames in this order:

| Order | Find | Replace with | Why |
|-------|------|--------------|-----|
| 1st | `hx-disable` | `hx-ignore` | 2.x `hx-disable` = "stop htmx processing" = 4.x `hx-ignore` |
| 2nd | `hx-disabled-elt` | `hx-disable` | 2.x `hx-disabled-elt` = "disable during request" = 4.x `hx-disable` |

If you run the second rename first, the freshly created `hx-disable` attributes get clobbered
by the first rule and the behavior is silently corrupted. Rename `hx-disable -> hx-ignore`
before touching `hx-disabled-elt`.

Other renames supported by the sources:

| 2.x | 4.x | Notes |
|-----|-----|-------|
| `hx-request='{"timeout":5000}'` | `hx-config='{"timeout":5000}'` | Renamed; HCON accepts the old JSON |
| attributes inherited implicitly | `:inherited` modifier | e.g. `hx-target:inherited="#out"` |
| `hx-ext="my-ext"` | include the script file | No attribute needed; config whitelist optional |
