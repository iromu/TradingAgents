# htmx Extensions Reference

The shipped extension inventory, the `htmx.registerExtension()` authoring shape, the
whitelist-name gotcha, and the SSE/WS attribute migrations.

Documents: **htmx 4.0.0**. Notes marked "2.x" describe the older line (npm `latest` is
`2.0.10`; `4.0.0` is on `next`).

## Table of contents

- [Loading an extension](#loading-an-extension)
- [Shipped inventory](#shipped-inventory)
- [Registration name vs. file name](#registration-name-vs-file-name)
- [`htmx.registerExtension()` shape](#htmxregisterextension-shape)
- [Hook naming and cancellation](#hook-naming-and-cancellation)
- [Migrating a 2.x extension](#migrating-a-2-x-extension)
- [SSE and WS attribute migrations](#sse-and-ws-attribute-migrations)

## Loading an extension

Include the extension script after `htmx.js`; it applies page-wide and activates on its custom
attributes. Optionally restrict which extensions may register via the `extensions` config
whitelist (see configuration.md).

```html
<script src="/path/to/htmx.js"></script>
<script src="/path/to/hx-my-ext.js"></script>
<meta name="htmx-config" content='{"extensions": "my-ext"}'>
```

Without the `extensions` whitelist, all registered extensions are active.

## Shipped inventory

Files under `src/ext/` (built to `dist/ext/`). The "Registers as" column is the name passed to
`htmx.registerExtension()` -- what the `extensions` whitelist matches against.

| File (`src/ext/`) | Registers as | Purpose |
|-------------------|--------------|---------|
| `hx-multipart.js` | `hx-multipart` | Stream HTML with `multipart/mixed` |
| `hx-sse.js` | `sse` | Stream HTML with `text/event-stream` (SSE) |
| `hx-ws.js` | `ws` | Stream HTML and send data over WebSockets |
| `hx-browser-indicator.js` | `browser-indicator` | Show the browser tab's own spinner |
| `hx-live.js` | `hx-live` | DOM-based reactive scripting (`htmx.live` namespace) |
| `hx-pending.js` | `hx-pending` | Show custom content during requests |
| `hx-prompt.js` | `hx-prompt` | Restores 2.x's `hx-prompt` attribute |
| `hx-preload.js` | `preload` | Preload on hover or other triggers |
| `hx-history-cache.js` | `history-cache` | Restore back/forward pages from `sessionStorage` |
| `hx-ptag.js` | `ptag` | Skip unchanged polls with `HX-PTag` |
| `hx-download.js` | `download` | Download files with `hx-swap="download"` |
| `hx-head.js` | `hx-head` | Merge `<head>` tags with `hx-head="merge"` |
| `hx-targets.js` | `hx-targets` | Target many elements with `hx-targets` |
| `hx-upsert.js` | `upsert` | Update or insert elements with `hx-swap="upsert"` |
| `htmx-2-compat.js` | `compat` | Restore 2.x defaults and event names |
| `hx-alpine-compat.js` | `alpine-compat` | Run htmx alongside Alpine.js without conflicts |
| `hx-csp.js` | `hx-csp` | Make htmx work under a strict Content Security Policy |

`htmax.js` bundles htmx with the most popular extensions in one file.

## Registration name vs. file name

The `extensions` whitelist matches the name passed to `htmx.registerExtension()`, **not** the
file name. The shipped set is inconsistent here, so use the "Registers as" value above:

- `hx-sse.js` registers as `sse` (not `hx-sse`)
- `hx-preload.js` registers as `preload` (not `hx-preload`)
- `htmx-2-compat.js` registers as `compat` (not `htmx-2-compat`)
- `hx-live.js` registers as `hx-live` (matches the file stem)

So the whitelist entry for the SSE extension is `{"extensions": "sse"}`, not `"hx-sse"`.

## `htmx.registerExtension()` shape

Wrap in an IIFE, store the internal API from `init` in a closure, and define event hooks as
`htmx_<event_name>` methods. Every hook receives `(elt, detail)`.

```js
(() => {
  let api;

  htmx.registerExtension('my-ext', {
    init: (internalAPI) => {
      api = internalAPI;                       // store for later hooks
    },

    htmx_after_init: (elt, detail) => {
      let value = api.attributeValue(elt, "hx-my-attr");
      if (!value) return;                      // only act when the attribute is present
      // per-element setup
    },

    htmx_before_request: (elt, detail) => {
      // detail.ctx has the request context
      // return false to cancel the request
    },

    htmx_after_request: (elt, detail) => {
      // detail.ctx.text is the response text (edit before the swap)
      // detail.ctx.response has status and headers
    },

    htmx_before_cleanup: (elt, detail) => {
      // remove listeners, clear timers
    },
  });
})();
```

Conventions: store per-element state on `elt._htmx`; store per-request state on `detail.ctx`;
check `api.attributeValue()` before acting; clean up in `htmx_before_cleanup`.

## Hook naming and cancellation

A hook name is the event name with every `:` replaced by `_`: `htmx:before:morph:node` ->
`htmx_before_morph_node`. Any dispatched event can be hooked this way, including events other
extensions dispatch. Cancel by returning `false` or setting `detail.cancelled = true`.

```js
htmx_before_request: (elt, detail) => {
  if (!isValid(detail.ctx)) return false;   // cancel
}
```

A custom swap strategy is the one hook not derived from an event: `handle_swap(swapStyle,
target, fragment, swapSpec)` -- return truthy if it handled the swap. `htmx_process_<type>`
handles a `<template hx type="<type>">` element in a response (as `hx-upsert.js` uses
`htmx_process_upsert`).

## Migrating a 2.x extension

2.x extensions were callback-based (`onEvent`, `transformResponse`); 4.x is event-hook based.

| 2.x | 4.x | Notes |
|-----|-----|-------|
| `htmx.defineExtension()` | `htmx.registerExtension()` | Different function name |
| `onEvent(name, evt)` | Specific hooks (`htmx_before_request`, ...) | Use underscored hook names |
| `transformResponse(text, xhr, elt)` | `htmx_after_request` | Modify `detail.ctx.text` |
| `handleSwap(style, target, fragment)` | `handle_swap(style, target, fragment, swapSpec)` | Extra `swapSpec` param; return truthy |
| `encodeParameters(xhr, params, elt)` | `htmx_before_request` | Modify final `detail.ctx.request.body` and `.headers` |
| `getSelectors()` | `htmx_after_init` | Check `api.attributeValue(elt, "attr")` instead |
| `isInlineSwap(swapStyle)` | Not needed | Move logic into `handle_swap` |

## SSE and WS attribute migrations

The streaming extensions renamed their 2.x attributes (from `upgrade-check.py`
`EXT_ATTR_RENAMES`). Use the colon form in 4.x.

| 2.x attribute | 4.x | Notes |
|---------------|-----|-------|
| `sse-connect` | `hx-sse:connect` | Open an SSE stream |
| `sse-close` | `hx-sse:close` | Close an SSE stream |
| `sse-swap` | removed | SSE now integrates with the standard htmx request pipeline |
| `ws-connect` | `hx-ws:connect` | Open a WebSocket |
| `ws-send` | `hx-ws:send` | Send over a WebSocket |

The SSE/WS scripts are `dist/ext/hx-sse.js` and `dist/ext/hx-ws.js` in 4.x -- the bare
`sse.js` / `ws.js` names belong to the 1.x legacy layout and are not the 4.x paths. Their
whitelist names are `sse` and `ws` (see above). Related events are listed in events-api.md
(the `htmx:sse:` and `htmx:ws:` event families).
