---
name: htmx
description: >
  Build interactive web UIs with htmx — attribute-driven AJAX, CSS transitions,
  WebSockets, and SSE using HTML attributes, with no JavaScript framework or build
  step. Use when the user wants to create pages with htmx, add AJAX behavior without
  JavaScript, implement real-time features, build forms with validation, or integrate
  htmx with any backend framework (Python, Ruby, Node, Java, Go, PHP, etc.). Also use
  when migrating htmx 2.x to htmx 4.x. Trigger on mentions of htmx, hypermedia,
  server-side HTML responses, hx-get, hx-post, hx-trigger, hx-swap, hx-target,
  hx-boost, hx-on, hx-vals, hx-config, hx-status, :inherited, out-of-band swaps,
  WebSockets, SSE, or building reactive UIs with server-rendered HTML. Also trigger
  when the user wants to replace React/Vue with a simpler approach, build SPAs with
  htmx, add progressive enhancement to existing pages, or upgrade htmx 2 to htmx 4.
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 1.1.0
  category: frontend
  tags: [htmx, hypermedia, ajax, server-side, no-javascript, declarative]
---

# htmx — Hypermedia-Driven Web UIs

Server-rendered HTML fragments plus htmx attributes. No build step, no client framework.

> **Documents htmx 4.0.0.** Two version traps matter before you write anything:
>
> 1. The npm package is `htmx.org` (there is an old, broken package called `htmx`).
>    Its dist-tags are `latest = 2.0.10` and `next = 4.0.0`, so `@latest` and a bare
>    `npm install htmx.org` both hand back the **2.x** line. Pin an explicit version.
> 2. htmx 4 is a ground-up rewrite and is **not** source-compatible with 2.x markup.
>    Attributes, event names, config keys, headers and the JS API all moved.
>
> The published htmx.org website docs still describe 2.0.10, so anything quoted from
> them is 2.x content. The `master` branch is also still 2.x; v4 lives on the
> `v4.0.0` tag.

## Instructions

1. Establish which htmx version the project targets — "Which version am I dealing with?" disambiguates (attribute syntax differs across 1.x/2.x/4.x); for upgrades, use "Migrating from 2.x" before answering.
2. For a new page or form, follow "Core Workflow" (hx-target/hx-swap/hx-trigger attributes), then consult "Out-of-Band and Partials" and "Status-Based Handling" as soon as responses get composite.
3. Look up attributes and events by exact name in "Events", "Server-Side Headers" and "JavaScript API" when event-driven behavior is needed — don't invent `hx-*` names.
4. Check "Pitfalls" before delivering, and "Configuration"/"Extensions" before assuming a feature is core.

## Which version am I dealing with?

Any one of these means htmx 4; their absence with `XMLHttpRequest`/camelCase events means 2.x:

- `fetch()` rather than `XMLHttpRequest`
- the `:inherited` modifier on attributes
- colon-separated event names such as `htmx:after:swap`
- presence of `hx-config`, `hx-status`, `hx-partial`, `hx-action`, `hx-method`

If the project is on 2.x and the user wants 4, work through `references/upgrade-from-2.md`
rather than translating markup ad hoc — the rename order and inheritance steps have traps
that silently corrupt behaviour.

## Quick Start

```html
<script src="https://cdn.jsdelivr.net/npm/htmx.org@4.0.0/dist/htmx.min.js"
        integrity="sha384-BvJpBiO8Kh31EqtJe5DRIeWrHWnCGkwytKs9NKFi86Hhw96dEqdEMzZDeK9iEGTc"
        crossorigin="anonymous"></script>

<button hx-post="/clicked" hx-swap="outerHTML">Click Me</button>
```

The model: the server returns an **HTML fragment**, and htmx swaps it into the DOM.

## When to Use

- AJAX behaviour without custom JavaScript
- Replace React/Vue/Svelte with server-rendered HTML
- Real-time updates (polling, WebSockets, SSE)
- Inline editing, infinite scroll, active search, lazy loading
- Progressive enhancement — links/forms that work without JS
- CSS transitions / View Transitions on dynamic content
- Any backend framework (Flask, Django, Rails, Express, Spring, etc.)

## When NOT to Use

- Complex client-side state management (use React/Vue)
- Heavy client-side computation or data visualisation
- Native mobile apps
- The user explicitly wants a JavaScript SPA framework

## Core Workflow

### 1. Issue the request

| Attribute | Method |
|---|---|
| `hx-get` | GET |
| `hx-post` | POST |
| `hx-put` | PUT |
| `hx-patch` | PATCH |
| `hx-delete` | DELETE |
| `hx-query` | QUERY |

Default triggers: `input`/`textarea`/`select` on `change`, `form` on `submit`,
everything else on `click`. Override with `hx-trigger`.

### 2. Choose the target

`hx-target` takes a CSS selector and defaults to the element itself. Extended selectors:
`this`, `closest <sel>`, `find <sel>`, `next [sel]`, `previous [sel]` — these avoid
sprinkling `id`s through loop-generated markup such as table rows.

```html
<button hx-get="/data" hx-target="closest .container">Load</button>
```

### 3. Choose the swap

| Value | Effect |
|---|---|
| `innerHTML` (default) | Replace inner HTML |
| `outerHTML` | Replace the element |
| `outerSync` | Morph attributes, then replace children; node stays in DOM |
| `innerMorph` / `outerMorph` | Morph children / the element, preserving DOM state |
| `textContent` | Replace text, no HTML parsing |
| `before`/`beforebegin`, `prepend`/`afterbegin` | Insert before target / before first child |
| `append`/`beforeend`, `after`/`afterend` | Insert after last child / after target |
| `delete` | Delete the target regardless of response |
| `none` | No swap (OOB swaps and headers still processed) |

Modifiers follow the style, space-separated:
`hx-swap="innerHTML swap:100ms settle:200ms transition:true scroll:top showTarget:#hdr strip:true"`.
Others: `ignoreTitle:true`, `scrollTarget:<sel>`, `focusScroll:true`, `swapEmpty:true`,
`target:<sel>`. Note the v4 form of the old combined `show:#sel:top` syntax is
`show:top showTarget:#sel`.

### 4. Choose the trigger

```html
<input hx-get="/search" hx-target="#results"
       hx-trigger="input changed delay:500ms, keyup[key=='Enter']">

<div hx-get="/updates" hx-trigger="every 2s">Poll</div>

<tr hx-get="/page/3" hx-trigger="revealed" hx-swap="afterend">last row</tr>
```

Modifiers: `once`, `changed`, `delay:<t>`, `throttle:<t>`, `from:<sel>`, `target:<sel>`,
`prevent`, `stop` (alias `consume`), `halt`, `capture`, `passive`. `from:` accepts
`document`, `window`, `closest`, `find`, `next`, `previous`. A selector containing
whitespace needs parentheses: `from:(form input)`. Special events: `load`, `revealed`,
`intersect` (with `root:<sel>`, `threshold:<float>`). To receive an `HX-Trigger`
dispatched from the server, listen `from:body`.

### 5. Inheritance — the change that bites

**In htmx 4 inheritance is explicit.** A parent attribute does not reach descendants
unless you mark it `:inherited`:

```html
<!-- WRONG in v4: children do not inherit hx-target -->
<div hx-target="#output">
  <button hx-get="/a">A</button>
  <button hx-get="/b">B</button>
</div>

<!-- CORRECT -->
<div hx-target:inherited="#output">
  <button hx-get="/a">A</button>
  <button hx-get="/b">B</button>
</div>
```

`hx-include:inherited:append="[name='extra']"` appends to inherited values instead of
replacing. Rather than marking everything, mark the attributes that are actually being
inherited — typically `hx-target`, `hx-include`, `hx-swap`, `hx-boost`, `hx-confirm`,
`hx-headers`, `hx-indicator`, `hx-sync`, `hx-config`, `hx-encoding`, `hx-validate`.
To restore 2.x behaviour wholesale, set `"implicitInheritance": true` in config.

### 6. Respond with HTML

```html
<div><p>New content</p><button hx-post="/action">Do Something</button></div>
```

## Out-of-Band and Partials

Mark extra elements in the response with `hx-swap-oob`; the **main content swaps first**,
then OOB/partial elements — the reverse of 2.x. A response containing only OOB content
does not clear the main target; set `allowEmptySwapAfterOOB` or use `swapEmpty:true`.

```html
<div id="main">Updated main content</div>
<span id="badge" hx-swap-oob="true">5</span>
```

## Status-Based Handling

```html
<button hx-post="/save" hx-status:4xx="swap:none" hx-status:5xx="swap:none">Save</button>
```

Unlike 2.x, **every** response swaps by default except 204 and 304. To restore the old
behaviour set `"noSwap": [204, 304, "4xx", "5xx"]`.

## Configuration

Via meta tag (HCON accepts JSON, or the shorter form) or from JavaScript:

```html
<meta name="htmx-config" content='{"defaultSwap":"outerHTML","logAll":true}'>
<meta name="htmx-config" content="defaultSwap:outerHTML, logAll:true">
```

| Config | Default | Notes |
|---|---|---|
| `defaultSwap` | `innerHTML` | was `defaultSwapStyle` in 2.x |
| `defaultTimeout` | `60000` | was `timeout`; default changed to 60s |
| `defaultSettleDelay` | `1` | |
| `noSwap` | `[204, 304]` | status codes that skip swapping |
| `allowEmptySwapAfterOOB` | `false` | |
| `implicitInheritance` | `false` | `true` restores 2.x auto-inheritance |
| `transitions` | `false` | was `globalViewTransitions` |
| `history` | `true` | `true` / `false` / `"reload"` |
| `logAll` | `false` | log every event; debugging |
| `mode` | `same-origin` | `cors` / `no-cors` / `same-origin` |
| `extensions` | `""` | whitelist of registration names; empty allows all |
| `prefix` | `"data-hx-"` | **additive** — `hx-get` and `data-hx-get` both work |
| `metaCharacter` | unset (acts as `:`) | character introducing an attribute modifier |
| `indicatorClass` / `requestClass` | `htmx-indicator` / `htmx-request` | |
| `includeIndicatorCSS` | `true` | was `includeIndicatorStyles` |
| `inlineScriptNonce` | unset | nonce on inserted scripts |
| `morphIgnore` | `["data-htmx-powered"]` | |
| `morphScanLimit` | `10` | |
| `morphSkip` / `morphSkipChildren` | `'[hx-morph-skip]'` / `'[hx-morph-skip-children]'` | |

Setting `prefix` replaces `data-hx-`, which stops any `data-hx-*` markup working; `hx-*`
always works. There is no `wg-` prefix.

## Events

Naming convention `htmx:phase:action`. Lifecycle: `htmx:before:process` /
`htmx:after:process`, `htmx:before:init` / `htmx:after:init`, `htmx:before:cleanup` /
`htmx:after:cleanup`. Request: `htmx:confirm`, `htmx:config:request`,
`htmx:before:request`, `htmx:before:response`, `htmx:after:request`,
`htmx:finally:request`, `htmx:error`, `htmx:response:error`. Swap:
`htmx:before:swap` / `htmx:after:swap`, `htmx:finally:swap`, `htmx:before:settle` /
`htmx:after:settle`. History: `htmx:before:history:update`, `htmx:after:history:update`,
`htmx:after:history:push`, `htmx:after:history:replace`, `htmx:before:history:restore`.
View Transitions: `htmx:before:viewTransition` / `htmx:after:viewTransition`.

Handlers read the request context off `detail.ctx`:

```js
document.body.addEventListener('htmx:config:request', (evt) => {
  const ctx = evt.detail.ctx;
  ctx.request.headers['X-CSRF'] = token;   // headers object
  ctx.request.body.set('key', 'value');     // FormData
  ctx.request.action = '/modified-url';
  // also: ctx.sourceElement, ctx.target, ctx.swap,
  //       ctx.request.method, ctx.text (after request)
});
```

Cancel a request from `htmx:config:request` or `htmx:before:request` with
`evt.preventDefault()`. Abort in-flight work by dispatching `htmx:abort` at the element —
`htmx.trigger("#slow-thing", "htmx:abort")` — it is an event you send, not one htmx fires.
Inline: `hx-on:htmx:after:swap="alert('done')"`.

## Server-Side Headers

Sent by htmx: `HX-Request` (`"true"`), `HX-Source` and `HX-Target` (as `tag#id`, e.g.
`button#submit`), `HX-Current-URL`, `HX-Request-Type` (`"partial"` vs `"full"`),
`HX-Boosted`, `HX-History-Restore-Request`.

Honoured from the server: `HX-Trigger`, `HX-Push-Url`, `HX-Replace-Url`, `HX-Redirect`,
`HX-Location`, `HX-Refresh`, `HX-Retarget`, `HX-Reswap`, `HX-Reselect`.
`HX-Trigger-After-Swap` and `HX-Trigger-After-Settle` were **removed** — use `HX-Trigger`.

## JavaScript API

```js
htmx.version                                   // read-only version string
htmx.ajax("GET", "/data", {target: "#result"}) // returns a Promise
htmx.on("htmx:after:swap", (evt) => {})
htmx.onLoad((elt) => {})
htmx.process(element)                          // initialise dynamically added content
htmx.initialize()                              // history + process document.body
htmx.find("closest .container") / htmx.findAll(".items")
htmx.trigger(elt, "myEvent", {detail: ...})
htmx.swap(ctx); htmx.timeout(1000); htmx.parseInterval("2s")
htmx.registerExtension("name", hooks)
```

Class helpers were dropped in favour of the platform: use `element.classList` and
`removeEventListener` (`htmx.on` returns its callback, which is what you pass to it).
`htmx.defineExtension` became `htmx.registerExtension`. The `hx-live` extension adds an
`htmx.live` namespace: `take()`, `toggle()`, `attr()`, `q()` (alias `$`), `debounce()`,
`refresh()`, `forEvent()`, `nextFrame()`.

## Extensions

Include the script; extensions apply page-wide, and no `hx-ext` attribute is needed
(`hx-ext` was removed). The `extensions` config is an optional whitelist keyed by
**registration name**, which is not always the file name.

| File | Registers as | For |
|---|---|---|
| `hx-sse.js` | `sse` | Stream HTML via `text/event-stream` |
| `hx-ws.js` | `ws` | Stream + send over WebSockets |
| `hx-multipart.js` | `hx-multipart` | Stream HTML via `multipart/mixed` |
| `hx-live.js` | `hx-live` | DOM-based reactive scripting |
| `hx-pending.js` | `hx-pending` | Custom content during requests |
| `hx-preload.js` | `preload` | Preload on hover/other triggers |
| `hx-history-cache.js` | `history-cache` | Back/forward from `sessionStorage` |
| `hx-ptag.js` | `ptag` | Skip unchanged polls with `HX-PTag` |
| `hx-download.js` | `download` | `hx-swap="download"` |
| `hx-head.js` | `hx-head` | Merge `<head>` with `hx-head="merge"` |
| `hx-targets.js` | `hx-targets` | Target many elements |
| `hx-upsert.js` | `upsert` | `hx-swap="upsert"` |
| `hx-prompt.js` | `hx-prompt` | Restores 2.x `hx-prompt` syntax |
| `hx-browser-indicator.js` | `browser-indicator` | Use the tab's own spinner |
| `hx-csp.js` | `hx-csp` | Work under a strict CSP |
| `htmx-2-compat.js` | `compat` | 2.x defaults and event names |
| `hx-alpine-compat.js` | `alpine-compat` | Coexist with Alpine.js |

```html
<script src="https://cdn.jsdelivr.net/npm/htmx.org@4.0.0/dist/htmx.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/htmx.org@4.0.0/dist/ext/hx-sse.js"></script>
<div hx-sse:connect="/events" hx-sse:close="closeEvents">…</div>
```

Note the v4 paths are `dist/ext/hx-sse.js` / `hx-ws.js`; the bare `sse.js` / `ws.js` names
belong to the 1.x legacy directory. `htmax.js` bundles htmx with the popular extensions.

## Examples

**"Live search box, no React."** "Core Workflow" -> `hx-get` + `hx-target` + `hx-trigger="keyup changed delay:300ms"`; "Status-Based Handling" -> the 4xx/5xx swap handling, not just the 200 path.

**"Swap a whole page's sidebar links to partial-page navigation."** Out-of-band and partials + `hx-boost` coverage -> boost the container rather than stamping per-link attributes.

**"Real-time price ticker over WebSocket/SSE."** WebSockets/SSE material in "Core Workflow" and "Events"; confirm 2.x vs 4.x with "Which version am I dealing with?" first - the attribute syntax differs.

## Pitfalls

| Pitfall | Avoidance |
|---|---|
| Installed 2.x while writing v4 syntax | `npm install htmx.org` gives `latest` = 2.0.10; pin `@4.0.0` |
| Renaming `hx-disable` in the wrong order | Do `hx-disable`→`hx-ignore` **first**, then `hx-disabled-elt`→`hx-disable`; reversing it silently corrupts both |
| Attributes set on a parent do nothing | v4 needs `:inherited`; or set `implicitInheritance: true` |
| `hx-delete` lost its form data | v4 `hx-delete`, like `hx-get`, no longer includes enclosing form inputs; add `hx-include="closest form"` |
| Error pages now render into the page | All responses swap except 204/304; use `hx-status:4xx="swap:none"` or `noSwap` |
| OOB-dependent code breaks | Main swaps first, then OOB; use `allowEmptySwapAfterOOB` / `swapEmpty:true` |
| CSRF header recipe stops applying | 2.x implicit inheritance meant a parent `hx-headers` covered everything; it now needs `:inherited` |
| Upload progress bar does nothing | XHR is gone, so `htmx:xhr:*` events do not exist; show in-request state with `hx-pending` |
| Config key silently ignored | `defaultSwapStyle`→`defaultSwap`, `globalViewTransitions`→`transitions`, `historyEnabled`→`history`, `includeIndicatorStyles`→`includeIndicatorCSS`; ~22 keys were removed outright |
| Extension does not load | No `hx-ext` in v4 — include the file; and the `extensions` whitelist takes the registration name (`preload`, not `hx-preload.js`) |

## Migrating from 2.x

Run the shipped checker first to scope the work — it prints clickable `file:line` hits with
suggested fixes and rewrites nothing:

```bash
npx htmx.org@4.0.0 upgrade-check -- ./path/to/project
npx htmx.org@4.0.0 upgrade-check --ext .vue ./path/to/project
```

It scans `.html`, `.php`, `.js`, `.ts`, `.jinja`, `.jinja2`, `.j2`, `.erb`, `.hbs` by
default. For a large codebase, `"implicitInheritance": true` and
`"noSwap": [204,304,"4xx","5xx"]` skip the two largest steps, and `htmx-2-compat.js`
fires old event names alongside new ones. These are bridges, not destinations.

## References

- `references/attributes.md` — attribute surface by purpose, plus the removed/renamed tables
- `references/configuration.md` — full config keys, defaults, HCON form, renames and removals
- `references/events-api.md` — colon-case events, rename maps, `detail.ctx` shape
- `references/upgrade-from-2.md` — the 13-step migration with its ordering traps
- `references/extensions.md` — extensions and the `registerExtension()` hook API
