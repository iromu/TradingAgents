# htmx Events & JavaScript API Reference

Colon-case event names, the 2.x -> 4.x rename maps, the changed event-detail shape, and the
v4 JavaScript API.

Documents: **htmx 4.0.0**. Notes marked "2.x" describe the older line (npm `latest` is
`2.0.10`; `4.0.0` is on `next`; the htmx.org website still documents 2.x).

## Table of contents

- [Naming convention](#naming-convention)
- [Core events](#core-events)
- [Extension-dispatched events](#extension-dispatched-events)
- [Rename maps (2.x -> 4.x)](#rename-maps-2-x---4-x)
- [Removed events](#removed-events)
- [Event names in `hx-on:` and `hx-trigger:`](#event-names-in-hx-on-and-hx-trigger)
- [Changed event-detail shape](#changed-event-detail-shape)
- [JavaScript API](#javascript-api)
- [Removed JavaScript API](#removed-javascript-api)

## Naming convention

4.x events are colon-namespaced: `htmx:phase:action` (e.g. `htmx:after:swap`).
2.x used camelCase (`htmx:afterSwap`). To tell which version a project targets, look for
`fetch()` usage, the `:inherited` modifier, or colon-separated event names -- all 4.x markers.

## Core events

Dispatched to the DOM. Cancel a request in `htmx:config:request` or `htmx:before:request`
with `evt.preventDefault()`; an extension hook cancels by returning `false`.

| Group | Events |
|-------|--------|
| Lifecycle | `htmx:before:process`, `htmx:after:process`, `htmx:before:init`, `htmx:after:init`, `htmx:before:cleanup`, `htmx:after:cleanup`, `htmx:before:on:init` |
| Request | `htmx:confirm`, `htmx:config:request`, `htmx:before:request`, `htmx:before:response`, `htmx:after:request`, `htmx:finally:request`, `htmx:error`, `htmx:response:error` |
| Swap | `htmx:before:swap`, `htmx:after:swap`, `htmx:finally:swap`, `htmx:before:settle`, `htmx:after:settle` |
| History | `htmx:before:history:update`, `htmx:after:history:update`, `htmx:after:history:push`, `htmx:after:history:replace`, `htmx:before:history:restore` |
| View Transitions | `htmx:before:viewTransition`, `htmx:after:viewTransition` |

`htmx:abort` is an event you **dispatch** to cancel an element's in-flight requests, not one
htmx fires:

```js
htmx.trigger("#slow-thing", "htmx:abort");
```

`htmx:confirm` detail carries `issueRequest` and `dropRequest` for async confirmation.

**Extension-only hooks** (never reach the DOM): `htmx:before:morph:node`,
`htmx:before:morph:attr`, `htmx:after:implicitInheritance`, `htmx:process:<type>`.

## Extension-dispatched events

Declared in `htmx.web-types.json`; fired by the corresponding extension. The web-types file
stores every event name **without** the `htmx:` prefix (core events too -- it lists `after:swap`,
not `htmx:after:swap`); the `htmx:` prefix is applied here for consistency with the core events
above. The exact prefix on these extension-only events is not independently confirmed by the
narrative guides, so verify against your build before relying on a specific one.

| Extension | Events |
|-----------|--------|
| SSE (`hx-sse.js`) | `htmx:sse:before:connection`, `htmx:sse:after:connection`, `htmx:sse:close`, `htmx:sse:error`, `htmx:sse:before:message`, `htmx:sse:after:message` |
| WS (`hx-ws.js`) | `htmx:ws:before:connection`, `htmx:ws:after:connection`, `htmx:ws:close`, `htmx:ws:error`, `htmx:ws:before:message:outgoing`, `htmx:ws:after:message:outgoing`, `htmx:ws:before:message:incoming`, `htmx:ws:after:message:incoming` |
| Download | `htmx:download:start`, `htmx:download:progress`, `htmx:download:complete` |
| Head | `htmx:head:before:merge`, `htmx:head:before:add`, `htmx:head:before:remove`, `htmx:head:after:merge` |
| History cache | `htmx:history:cache:before:save`, `htmx:history:cache:after:save`, `htmx:history:cache:miss`, `htmx:history:cache:hit`, `htmx:history:cache:before:restore`, `htmx:history:cache:after:restore` |
| CSP (`hx-csp.js`) | `htmx:security:strip`, `htmx:security:violation` |
| Prompt | `htmx:prompt` |

## Rename maps (2.x -> 4.x)

Primary source: `upgrade-check.py`. Where the migration guide's Step 5 table differs, the
divergence is called out below -- do not silently trust the guide's value.

### Core

| 2.x (camelCase) | 4.x (colon) |
|-----------------|-------------|
| `htmx:afterOnLoad` | `htmx:after:init` |
| `htmx:afterProcessNode` | `htmx:after:init` (guide maps to `htmx:after:process` -- CONFLICT) |
| `htmx:afterRequest` | `htmx:after:request` |
| `htmx:afterSettle` | `htmx:after:swap` (guide maps to `htmx:after:settle` -- CONFLICT) |
| `htmx:afterSwap` | `htmx:after:swap` |
| `htmx:beforeCleanupElement` | `htmx:before:cleanup` |
| `htmx:beforeHistorySave` | `htmx:before:history:update` |
| `htmx:beforeOnLoad` | `htmx:before:init` |
| `htmx:beforeProcessNode` | `htmx:before:process` |
| `htmx:beforeRequest` | `htmx:before:request` |
| `htmx:beforeSwap` | `htmx:before:swap` |
| `htmx:configRequest` | `htmx:config:request` |
| `htmx:historyCacheMiss` | `htmx:before:history:restore` |
| `htmx:historyRestore` | `htmx:before:history:restore` |
| `htmx:load` | `htmx:after:init` |
| `htmx:oobAfterSwap` | `htmx:after:swap` |
| `htmx:oobBeforeSwap` | `htmx:before:swap` |
| `htmx:pushedIntoHistory` | `htmx:after:history:push` |
| `htmx:replacedInHistory` | `htmx:after:history:replace` |
| `htmx:responseError` | `htmx:response:error` |
| `htmx:sendError` | `htmx:error` |
| `htmx:swapError` | `htmx:error` |
| `htmx:targetError` | `htmx:error` |
| `htmx:timeout` | `htmx:error` |

Guide-only mappings (not in the tool's map, from the guide's Step 5 table):
`htmx:beforeSend` -> `htmx:before:request`, `htmx:beforeHistoryUpdate` ->
`htmx:before:history:update`, `htmx:beforeTransition` -> `htmx:before:viewTransition`,
`htmx:sendAbort` -> `htmx:error`.

### SSE

| 2.x | 4.x |
|-----|-----|
| `htmx:sseOpen` | `htmx:sse:after:connection` |
| `htmx:sseError` | `htmx:sse:error` |
| `htmx:sseBeforeMessage` | `htmx:sse:before:message` |
| `htmx:sseMessage` | `htmx:sse:after:message` |
| `htmx:sseClose` | `htmx:sse:close` |

### WS

| 2.x | 4.x |
|-----|-----|
| `htmx:wsOpen` | `htmx:ws:after:connection` |
| `htmx:wsClose` | `htmx:ws:close` |
| `htmx:wsConfigSend` | `htmx:ws:before:message:outgoing` |
| `htmx:wsBeforeSend` | `htmx:ws:before:message:outgoing` |
| `htmx:wsAfterSend` | `htmx:ws:after:message:outgoing` |
| `htmx:wsBeforeMessage` | `htmx:ws:before:message:incoming` |
| `htmx:wsAfterMessage` | `htmx:ws:after:message:incoming` |

## Removed events

No 4.x equivalent (from `upgrade-check.py` `REMOVED_EVENTS`):

`htmx:validation:validate`, `htmx:validation:failed`, `htmx:validation:halted` -- use native
form validation; `htmx:xhr:loadstart`, `htmx:xhr:loadend`, `htmx:xhr:progress`,
`htmx:xhr:abort` -- XMLHttpRequest is gone (4.x uses `fetch()`).

## Event names in `hx-on:` and `hx-trigger:`

Inline handlers use `hx-on:<event>`. The `hx-on::<event>` shorthand prepends `htmx:`, so
`hx-on::after:swap` is equivalent to `hx-on:htmx:after:swap`.

```html
<!-- htmx 2 -->
<div hx-on:htmx:afterSwap="console.log('done')">
<!-- htmx 4 -->
<div hx-on:htmx:after:swap="console.log('done')">
<div hx-on::after:swap="console.log('done')">   <!-- same, htmx: implied -->
```

When `hx-trigger` references an htmx event, use the colon form too:

```html
<!-- htmx 2 -->
<div hx-get="/data" hx-trigger="htmx:afterSwap from:body">
<!-- htmx 4 -->
<div hx-get="/data" hx-trigger="htmx:after:swap from:body">
```

## Changed event-detail shape

In 2.x `event.detail` carried the XHR object directly. In 4.x it carries `event.detail.ctx`,
the request context; the body is a `FormData`.

```js
// htmx 2
document.addEventListener('htmx:configRequest', (evt) => {
  evt.detail.headers['X-Custom'] = 'value';
  evt.detail.parameters['key'] = 'value';
  evt.detail.path = '/modified-url';
});

// htmx 4
document.addEventListener('htmx:config:request', (evt) => {
  evt.detail.ctx.request.headers['X-Custom'] = 'value';
  evt.detail.ctx.request.body.set('key', 'value');   // FormData
  evt.detail.ctx.request.action = '/modified-url';
});
```

Other useful `ctx` fields: `ctx.sourceElement`, `ctx.target`, `ctx.swap`, `ctx.request.method`,
`ctx.request.headers`, and after the request `ctx.text` (response text) and `ctx.response`
(`status`, `headers`). Modify `ctx.text` in `htmx:after:request` to rewrite the response before
the swap.

## JavaScript API

The v4 public API (from the guidance):

| Call | Description |
|------|-------------|
| `htmx.version` | Version string (read-only) |
| `htmx.ajax(method, url, options)` | Programmatic request, returns a Promise |
| `htmx.on(event, handler)` | Event listener |
| `htmx.onLoad(cb)` | Callback for new content |
| `htmx.process(element)` | Initialize htmx on dynamic content |
| `htmx.initialize()` | Set up history and process `document.body` |
| `htmx.find(selector)` | Extended CSS selector query (single) |
| `htmx.findAll(selector)` | Extended CSS selector query (all) |
| `htmx.trigger(elt, name, detail)` | Fire an event |
| `htmx.swap(ctx)` | Manual swap (signature changed from 2.x) |
| `htmx.timeout(ms)` | Promise resolving after a delay (new in 4.x) |
| `htmx.parseInterval(str)` | Parse a time interval to ms |
| `htmx.registerExtension(name, hooks)` | Register an extension |

The `hx-live` extension adds an `htmx.live` namespace: `take()`, `toggle()`, `attr()`,
`q()` (alias `$`), `debounce()`, `refresh()`, `forEvent()`, `nextFrame()`.

## Removed JavaScript API

From `upgrade-check.py` `REMOVED_JS_API`. 4.x drops the DOM-manipulation helpers in favour of
native methods.

| 2.x call | Replacement |
|----------|-------------|
| `htmx.addClass(elt, cls)` | `elt.classList.add(cls)` |
| `htmx.removeClass(elt, cls)` | `elt.classList.remove(cls)` |
| `htmx.toggleClass(elt, cls)` | `elt.classList.toggle(cls)` |
| `htmx.closest(elt, sel)` | `elt.closest(sel)` |
| `htmx.remove(elt)` | `elt.remove()` |
| `htmx.off(elt, evt, fn)` | `removeEventListener()` (`htmx.on()` returns the callback) |
| `htmx.location(...)` | `htmx.ajax()` |
| `htmx.takeClass(elt, cls)` | Removed from core. Restore 2.x verbatim with the snippet below, or load `hx-live` and use `htmx.live.take()` |
| `htmx.logAll()` | `htmx.config.logAll = true` |
| `htmx.logNone()` | `htmx.config.logAll = false` (errors/warnings go to `console.*`) |
| `htmx.logger` | Removed; logs go to `console.error`/`warn`/`log` directly |
| `htmx.defineExtension(name, obj)` | `htmx.registerExtension()` |
| `htmx.values(elt)` | `new FormData(elt)` |

`htmx.takeClass` restore snippet (from the tool):

```js
htmx.takeClass = (elt, cls) => {
  elt = typeof elt === 'string' ? document.querySelector(elt) : elt;
  for (let c of elt.parentElement.children) c.classList.remove(cls);
  elt.classList.add(cls);
};
```
