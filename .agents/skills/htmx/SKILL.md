---
name: htmx
description: >
  Build interactive web UIs with htmx — declarative AJAX, CSS transitions, WebSockets, and SSE
  using HTML attributes. Use when the user wants to create pages with htmx, add AJAX behavior
  without JavaScript, implement real-time features, build forms with validation, or integrate
  htmx with any backend framework (Python, Ruby, Node, Java, Go, PHP, etc.). Trigger on
  mentions of htmx, HTMX_, hypermedia, server-side HTML responses, hx-get, hx-post, hx-trigger,
  hx-swap, hx-target, hx-boost, hx-on, out-of-band swaps, WebSockets, SSE, or building
  reactive UIs with server-rendered HTML. Also trigger when the user wants to replace React/Vue
  with a simpler approach, build SPAs with htmx, or add progressive enhancement to existing pages.
version: 1.0.0
category: frontend
tags: [htmx, hypermedia, ajax, server-side, no-javascript, declarative]
---

# htmx — Hypermedia-Driven Web UIs

Build interactive, reactive web interfaces using only HTML attributes. No build step, no JavaScript framework — just server-rendered HTML and htmx attributes.

## When to Use

- AJAX behavior without custom JavaScript
- Replace React/Vue/Svelte with server-rendered HTML
- Real-time updates (polling, WebSockets, SSE)
- Inline editing, infinite scroll, active search, progress bars
- Progressive enhancement — links/forms that work without JS
- CSS transitions/animations on dynamic content
- Any backend framework (Flask, Django, Rails, Express, Spring, etc.)

## When NOT to Use

- Complex client-side state management (use React/Vue)
- Heavy client-side computation or data visualization
- Native mobile apps
- User explicitly wants a JavaScript SPA framework

## Quick Start

```html
<script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.min.js"
        integrity="sha384-H5SrcfygHmAuTDZphMHqBJLc3FtshKjG7w/CeCpFReSfwBWDTKpkzPP8c+cLsK+V"
        crossorigin="anonymous"></script>
```

## Core Workflow

### 1. Define the Request

Add an HTTP verb attribute to any element:

```html
<button hx-get="/contacts">Load</button>
<button hx-post="/contacts/new">Create</button>
<button hx-put="/contacts/1">Update</button>
<button hx-delete="/contacts/1">Delete</button>
```

### 2. Define the Target

Where to put the response:

```html
<button hx-get="/contacts" hx-target="#contacts-list">Load</button>
<div hx-target="this" hx-swap="outerHTML">...</div>
<button hx-get="/next" hx-target="next .content">Next</button>
```

### 3. Define the Swap Style

| Swap Style | Use Case |
|------------|----------|
| `innerHTML` (default) | Replace inner content |
| `outerHTML` | Replace the element itself |
| `beforeend` | Append to target |
| `afterbegin` | Prepend to target |
| `delete` | Remove the target |
| `none` | No swap (OOB only) |

### 4. Define the Trigger

```html
<!-- Custom trigger with modifiers -->
<input hx-post="/search" hx-trigger="input changed delay:500ms, keyup[key=='Enter'], load">

<!-- Polling -->
<div hx-get="/status" hx-trigger="every 2s">Checking...</div>

<!-- Keyboard shortcut (global) -->
<div hx-trigger="keydown[key=='s' && ctrlKey] from:document" hx-get="/save" hx-swap="none"></div>

<!-- Scroll-based -->
<div hx-get="/more" hx-trigger="revealed">Load more on scroll</div>
```

### 5. Handle the Response

The server returns HTML that htmx swaps into the DOM.

**Server response for `hx-target="this" hx-swap="outerHTML"`:**
```html
<div><p>New content</p><button hx-post="/action">Do Something</button></div>
```

## Common Patterns

### Click To Edit

```html
<!-- Display mode -->
<div hx-target="this" hx-swap="outerHTML">
  <span>Joe Blow</span>
  <button hx-get="/contact/1/edit">Edit</button>
</div>

<!-- Server returns edit form on GET /contact/1/edit -->
<form hx-put="/contact/1" hx-target="this" hx-swap="outerHTML">
  <input name="name" value="Joe Blow">
  <button type="submit">Save</button>
  <button hx-get="/contact/1">Cancel</button>
</form>
```

### Inline Validation

```html
<form hx-post="/contact">
  <div hx-target="this" hx-swap="outerHTML">
    <label>Email</label>
    <input name="email" hx-post="/contact/email" hx-indicator="#ind">
    <img id="ind" src="/loading.svg" class="htmx-indicator">
  </div>
  <button type="submit">Submit</button>
</form>
```

### Active Search (Debounced)

```html
<input hx-post="/search" hx-trigger="input changed delay:500ms, keyup[key=='Enter'], load"
       hx-target="#results" placeholder="Search...">
<div id="results"></div>
```

### Delete with Confirmation

```html
<!-- Put attributes on parent — children inherit -->
<tbody hx-confirm="Are you sure?" hx-target="closest tr" hx-swap="outerHTML">
  <tr><td>Item</td><td><button hx-delete="/item/1">Delete</button></td></tr>
</tbody>
```

### File Upload with Progress

```html
<form hx-encoding="multipart/form-data" hx-post="/upload">
  <input type="file" name="file">
  <button>Upload</button>
  <progress id="pb" value="0" max="100"></progress>
</form>
<script>
  htmx.on('form', 'htmx:xhr:progress', function(e) {
    htmx.find('#pb').value = (e.detail.loaded / e.detail.total) * 100;
  });
</script>
```

### Progressive Enhancement with Boosting

```html
<nav hx-boost="true">
  <a href="/page1">Page 1</a>
  <a href="/page2">Page 2</a>
  <form action="/search"><input name="q"><button>Search</button></form>
</nav>
```

### Out-of-Band (OOB) Swaps

```html
<!-- Request -->
<button hx-post="/action" hx-target="#main" hx-swap="innerHTML">Action</button>
<span id="badge">0</span>

<!-- Server response -->
<div id="main">Updated main content</div>
<span id="badge" hx-swap-oob="true">5</span>
```

## Animations

Use CSS with htmx lifecycle classes:

```css
.fade-out.htmx-swapping { opacity: 0; transition: opacity 0.5s ease-out; }
.fade-in.htmx-added { opacity: 0; }
.fade-in { opacity: 1; transition: opacity 0.5s ease-out; }
```

```html
<button class="fade-out" hx-delete="/item" hx-swap="outerHTML swap:0.5s">Delete</button>
<button class="fade-in" hx-post="/item" hx-swap="outerHTML settle:0.5s">Add</button>
```

### View Transitions

```html
<button hx-get="/new-content" hx-swap="innerHTML transition:true">Swap</button>
```

## Security

```javascript
htmx.config.allowEval = false;          // Required for strict CSP
htmx.config.allowScriptTags = false;    // Prevent script tags in responses
htmx.config.selfRequestsOnly = true;    // Same-domain AJAX only
```

Add CSRF token:
```html
<meta name="csrf" content="{{ csrf_token }}">
<button hx-post="/action"
        hx-headers='{"X-CSRF-Token": document.querySelector("meta[name=csrf]").content}'>
  Submit
</button>
```

## Server-Side Headers

**Request headers sent to server:**
- `HX-Request` — always `"true"`
- `HX-Target` — ID of target element
- `HX-Trigger` — ID of triggering element
- `HX-Current-URL` — current browser URL
- `HX-Boosted` — `"true"` if via `hx-boost`

**Response headers from server:**
- `HX-Location` — client-side redirect (swap content + replace URL)
- `HX-Redirect` — full page redirect
- `HX-Refresh` — full page refresh (`"true"`)
- `HX-Push-Url` — push URL into history
- `HX-Reswap` — override swap style
- `HX-Retarget` — CSS selector for different target
- `HX-Trigger` — fire client-side event
- `HX-Trigger-After-Swap` / `HX-Trigger-After-Settle` — fire event after swap/settle

## JavaScript API (When Needed)

```javascript
htmx.ajax('POST', '/api/data', { target: '#result', swap: 'innerHTML' });
htmx.find('#myElement');
htmx.on('#btn', 'htmx:afterSwap', fn);
htmx.process(document.body);
htmx.onLoad(function(elt) { if (elt.classList.contains('select2')) $(elt).select2(); });
```

## Extensions

Load via `hx-ext="name"`:

| Extension | CDN Path | Use For |
|-----------|----------|---------|
| `head-support` | `ext/head-support.js` | Process `<head>` updates |
| `idiomorph` | `ext/idiomorph.js` | Morph-based swapping (preserves focus) |
| `preload` | `ext/preload.js` | Preload resources |
| `sse` | `ext/sse.js` | Server-Sent Events |
| `ws` | `ext/ws.js` | WebSockets |
| `response-targets` | `ext/response-targets.js` | Target responses from any element |

```html
<script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/ext/sse.js"></script>
<div hx-ext="sse" sse-connect="https://stream.example.com" sse-swap="message">
  <p>SSE content here</p>
</div>
```

## Configuration

```javascript
htmx.config.defaultSwapStyle = 'innerHTML';
htmx.config.defaultSettleDelay = 20;
htmx.config.timeout = 0;
htmx.config.globalViewTransitions = false;
```

Declarative (in `<head>`):
```html
<meta name="htmx-config" content='{"defaultSwapStyle":"outerHTML"}'>
```

## Key Principles

1. **Server responds with HTML** — not JSON. Keep logic on the server.
2. **Declarative over imperative** — prefer attributes over `hx-on` scripts.
3. **Inheritance** — attributes cascade down the DOM tree. Put them on parents.
4. **HATEOAS** — links in responses drive navigation.
5. **Progressive enhancement** — use `hx-boost` for links that work without JS.
6. **CSS transitions** — use lifecycle classes (`htmx-swapping`, `htmx-added`, `htmx-settling`) as hooks.
7. **Events for JS integration** — use `htmx:afterSwap`, `htmx:configRequest`, etc.

## References

For detailed attribute, event, and configuration tables, see:
- `references/attributes.md` — All attributes, CSS classes, and request/response headers
- `references/configuration.md` — All 30+ config options with defaults