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

- User wants AJAX behavior without writing custom JavaScript
- User wants to replace React/Vue/Svelte with server-rendered HTML
- User wants real-time updates (polling, WebSockets, SSE)
- User wants inline editing, infinite scroll, active search, progress bars
- User wants progressive enhancement — links/forms that work without JS
- User wants to add CSS transitions/animations to dynamic content
- User is building with any backend framework (Flask, Django, Rails, Express, Spring, etc.)

## When NOT to Use

- Complex client-side state management (use React/Vue instead)
- Heavy client-side computation or data visualization
- Native mobile apps
- When the user explicitly wants a JavaScript SPA framework

## Quick Start

```html
<!-- Include htmx (CDN) -->
<script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.min.js"
        integrity="sha384-H5SrcfygHmAuTDZphMHqBJLc3FtshKjG7w/CeCpFReSfwBWDTKpkzPP8c+cLsK+V"
        crossorigin="anonymous"></script>
```

## Core Workflow

### Step 1: Define the Request

Add an HTTP verb attribute to any element:

```html
<!-- GET request -->
<button hx-get="/contacts">Load Contacts</button>

<!-- POST request -->
<button hx-post="/contacts/new">Create</button>

<!-- PUT / PATCH / DELETE -->
<button hx-put="/contacts/1">Update</button>
<button hx-delete="/contacts/1">Delete</button>
```

### Step 2: Define the Target

Tell htmx where to put the response:

```html
<!-- Target a specific element by ID -->
<button hx-get="/contacts" hx-target="#contacts-list">Load</button>

<!-- Target the element itself (replace it) -->
<div hx-target="this" hx-swap="outerHTML">
  <button hx-get="/edit">Edit</button>
</div>

<!-- Extended selectors -->
<button hx-get="/next" hx-target="next .content">Next</button>
<button hx-get="/prev" hx-target="previous .content">Prev</button>
```

### Step 3: Define the Swap Style

How the response HTML gets inserted:

| Swap Style | Use Case |
|------------|----------|
| `innerHTML` (default) | Replace inner content of target |
| `outerHTML` | Replace the target element itself |
| `beforeend` | Append content to target |
| `afterbegin` | Prepend content to target |
| `beforebegin` | Insert before target |
| `afterend` | Insert after target |
| `delete` | Remove the target element |
| `none` | No swap (OOB only) |

```html
<!-- Replace entire div -->
<div hx-target="this" hx-swap="outerHTML">
  <p>Old content</p>
</div>

<!-- Append to list -->
<div hx-target="#list" hx-swap="beforeend">
  <button hx-get="/more">Load More</button>
</div>
```

### Step 4: Define the Trigger

What event fires the request:

```html
<!-- Default: click for buttons, change for inputs -->
<button hx-post="/save">Save</button>

<!-- Custom trigger with modifiers -->
<input hx-post="/search"
       hx-trigger="input changed delay:500ms, keyup[key=='Enter'], load">

<!-- Polling -->
<div hx-get="/status" hx-trigger="every 2s">Checking...</div>

<!-- Keyboard shortcut (global) -->
<div hx-trigger="keydown[key=='s' && ctrlKey] from:document"
     hx-get="/save" hx-swap="none"></div>

<!-- Scroll-based -->
<div hx-get="/more" hx-trigger="revealed">Load more on scroll</div>
```

### Step 5: Handle the Response

The server returns HTML that htmx swaps into the DOM.

**Server response for `hx-target="this" hx-swap="outerHTML"`:**
```html
<div>
  <p>New content</p>
  <button hx-post="/action">Do Something</button>
</div>
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

### Inline Field Validation

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
<input hx-post="/search"
       hx-trigger="input changed delay:500ms, keyup[key=='Enter'], load"
       hx-target="#results"
       placeholder="Search...">
<div id="results"></div>
```

### Delete with Confirmation

```html
<!-- Put attributes on parent — children inherit -->
<tbody hx-confirm="Are you sure?" hx-target="closest tr" hx-swap="outerHTML">
  <tr>
    <td>Item</td>
    <td><button hx-delete="/item/1">Delete</button></td>
  </tr>
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

### Progress Bar (Polling)

```html
<!-- State container -->
<div hx-trigger="done" hx-get="/job" hx-swap="outerHTML" hx-target="this">
  <!-- Inner polling -->
  <div hx-get="/job/progress" hx-trigger="every 600ms" hx-swap="innerHTML">
    <div class="progress-bar" style="width: 0%"></div>
  </div>
</div>
```

### Progressive Enhancement with Boosting

```html
<!-- All links/forms in nav become AJAX -->
<nav hx-boost="true">
  <a href="/page1">Page 1</a>
  <a href="/page2">Page 2</a>
  <form action="/search">
    <input name="q">
    <button>Search</button>
  </form>
</nav>
```

### Out-of-Band (OOB) Swaps

Update multiple elements from one response:

```html
<!-- Request -->
<button hx-post="/action" hx-target="#main" hx-swap="innerHTML">
  Action
</button>
<span id="badge">0</span>

<!-- Server response -->
<div id="main">Updated main content</div>
<span id="badge" hx-swap-oob="true">5</span>
```

## Animations

Use CSS with htmx lifecycle classes:

```css
/* Fade out on removal */
.fade-out.htmx-swapping {
  opacity: 0;
  transition: opacity 0.5s ease-out;
}

/* Fade in on addition */
.fade-in.htmx-added {
  opacity: 0;
}
.fade-in {
  opacity: 1;
  transition: opacity 0.5s ease-out;
}
```

```html
<button class="fade-out" hx-delete="/item" hx-swap="outerHTML swap:0.5s">
  Delete
</button>

<button class="fade-in" hx-post="/item" hx-swap="outerHTML settle:0.5s">
  Add
</button>
```

### View Transitions

```html
<button hx-get="/new-content" hx-swap="innerHTML transition:true">
  Swap
</button>
```

## Security

```javascript
// Disable eval (required for strict CSP)
htmx.config.allowEval = false;

// Disable script tags in responses
htmx.config.allowScriptTags = false;

// Only allow same-domain requests
htmx.config.selfRequestsOnly = true;
```

Add CSRF token:
```html
<!-- Via meta tag -->
<meta name="csrf" content="{{ csrf_token }}">

<!-- Via hx-headers -->
<button hx-post="/action"
        hx-headers='{"X-CSRF-Token": document.querySelector("meta[name=csrf]").content}'>
  Submit
</button>
```

## htmx Request Headers (Server-Side)

| Header | Meaning |
|--------|---------|
| `HX-Request` | Always `"true"` for htmx requests |
| `HX-Target` | ID of the target element |
| `HX-Trigger` | ID of the element that triggered the request |
| `HX-Current-URL` | Current browser URL |
| `HX-Boosted` | `"true"` if via `hx-boost` |
| `HX-Prompt` | User response to `hx-prompt` |

## htmx Response Headers (Server-Side)

| Header | Purpose |
|--------|---------|
| `HX-Location` | Client-side redirect (replace URL + swap content) |
| `HX-Redirect` | Full page redirect to URL |
| `HX-Refresh` | Full page refresh (`"true"`) |
| `HX-Push-Url` | Push URL into history |
| `HX-Reswap` | Override swap style for response |
| `HX-Retarget` | CSS selector for different target |
| `HX-Trigger` | Fire client-side event |
| `HX-Trigger-After-Swap` | Fire event after swap |
| `HX-Trigger-After-Settle` | Fire event after settle |

## JavaScript API (When You Need It)

```javascript
// AJAX request
htmx.ajax('POST', '/api/data', { target: '#result', swap: 'innerHTML' });

// Find element
htmx.find('#myElement');
htmx.findAll('.items');

// Event handling
htmx.on('#btn', 'htmx:afterSwap', function(e) { /* ... */ });
htmx.off('#btn', 'htmx:afterSwap', handler);

// Process dynamically added content
htmx.process(document.body);

// Initialize 3rd party libs on new content
htmx.onLoad(function(elt) {
  if (elt.classList.contains('select2')) $(elt).select2();
});

// Trigger event
htmx.trigger(element, 'htmx:abort');
```

## Extensions

Load via `hx-ext="name"`:

| Extension | Load | Use For |
|-----------|------|---------|
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
// Programmatic
htmx.config.defaultSwapStyle = 'innerHTML';
htmx.config.defaultSwapDelay = 0;
htmx.config.defaultSettleDelay = 20;
htmx.config.historyEnabled = true;
htmx.config.historyCacheSize = 10;
htmx.config.timeout = 0;
htmx.config.scrollBehavior = 'instant';
htmx.config.globalViewTransitions = false;

// Declarative (put in <head>)
<meta name="htmx-config" content='{"defaultSwapStyle":"outerHTML"}'>
```

## Key Principles

1. **Server responds with HTML** — not JSON. Keep logic on the server.
2. **Declarative over imperative** — prefer attributes over `hx-on` scripts.
3. **Inheritance** — attributes cascade down the DOM tree. Put them on parents.
4. **HATEOAS** — links in responses drive navigation. The server controls the flow.
5. **Progressive enhancement** — use `hx-boost` for links that work without JS.
6. **CSS transitions** — use lifecycle classes (`htmx-swapping`, `htmx-added`, `htmx-settling`) as hooks.
7. **Events for JS integration** — use `htmx:afterSwap`, `htmx:configRequest`, etc.