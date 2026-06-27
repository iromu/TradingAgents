---
source: https://htmx.org/reference/
fetched: 2026-06-26
---

# htmx Events & JavaScript API Reference

## Events

| Event | When |
|-------|------|
| `htmx:abort` | Abort a request on an element |
| `htmx:afterOnLoad` | After AJAX request completed processing a successful response |
| `htmx:afterProcessNode` | After htmx has initialized a node |
| `htmx:afterRequest` | After AJAX request has completed |
| `htmx:afterSettle` | After the DOM has settled |
| `htmx:afterSwap` | After new content has been swapped in |
| `htmx:beforeCleanupElement` | Before htmx disables or removes an element |
| `htmx:beforeOnLoad` | Before any response processing occurs |
| `htmx:beforeProcessNode` | Before htmx initializes a node |
| `htmx:beforeRequest` | Before an AJAX request is made |
| `htmx:beforeSwap` | Before a swap is done — configure the swap |
| `htmx:beforeSend` | Just before an ajax request is sent |
| `htmx:beforeTransition` | Before the View Transition wrapped swap |
| `htmx:configRequest` | Before the request — customize parameters, headers |
| `htmx:confirm` | After a trigger — cancel or delay issuing the AJAX request |
| `htmx:historyCacheError` | On an error during cache writing |
| `htmx:historyCacheHit` | On a cache hit in the history subsystem |
| `htmx:historyCacheMiss` | On a cache miss in the history subsystem |
| `htmx:historyCacheMissLoadError` | On an unsuccessful remote retrieval |
| `htmx:historyCacheMissLoad` | On a successful remote retrieval |
| `htmx:historyRestore` | When htmx handles a history restoration action |
| `htmx:beforeHistorySave` | Before content is saved to the history cache |
| `htmx:load` | When new content is added to the DOM |
| `htmx:oobAfterSwap` | After an out of band element has been swapped in |
| `htmx:oobBeforeSwap` | Before an out of band element swap — configure it |
| `htmx:oobErrorNoTarget` | When an OOB element has no matching ID in DOM |
| `htmx:prompt` | After a prompt is shown |
| `htmx:pushedIntoHistory` | After a URL is pushed into history |
| `htmx:replacedInHistory` | After a URL is replaced in history |
| `htmx:responseError` | When an HTTP response error occurs (non-200/300) |
| `htmx:sendAbort` | When a request is aborted |
| `htmx:sendError` | When a network error prevents a request |
| `htmx:sseError` | When an SSE source error occurs |
| `htmx:sseOpen` | When an SSE source is opened |
| `htmx:swapError` | When an error occurs during the swap phase |
| `htmx:targetError` | When an invalid target is specified |
| `htmx:timeout` | When a request timeout occurs |
| `htmx:validation:validate` | Before an element is validated |
| `htmx:validation:failed` | When an element fails validation |
| `htmx:validation:halted` | When a request is halted due to validation errors |
| `htmx:xhr:abort` | When an ajax request aborts |
| `htmx:xhr:loadend` | When an ajax request ends |
| `htmx:xhr:loadstart` | When an ajax request starts |
| `htmx:xhr:progress` | Periodically during an ajax request with progress events |

## JavaScript API

| Method | Description |
|--------|-------------|
| `htmx.addClass(elt, className)` | Adds a class to the given element |
| `htmx.ajax(type, url, target)` | Issues an htmx-style ajax request |
| `htmx.closest(elt, selector)` | Finds the closest parent matching the selector |
| `htmx.config` | Current htmx config object |
| `htmx.createEventSource(url)` | Create SSE EventSource objects |
| `htmx.createWebSocket(url)` | Create WebSocket objects |
| `htmx.defineExtension(name, obj)` | Defines an htmx extension |
| `htmx.find(selector)` | Finds a single element matching the selector |
| `htmx.findAll(selector)` | Finds all elements matching a selector |
| `htmx.logAll()` | Installs a logger that logs all htmx events |
| `htmx.logger` | Current logger (default null) |
| `htmx.off(elt, event, handler)` | Removes an event listener |
| `htmx.on(elt, event, handler)` | Creates an event listener |
| `htmx.onLoad(callback)` | Adds a callback for the htmx:load event |
| `htmx.parseInterval(str)` | Parses an interval declaration to milliseconds |
| `htmx.process(elt)` | Processes the element and children, hooking up htmx behavior |
| `htmx.remove(elt)` | Removes the given element |
| `htmx.removeClass(elt, className)` | Removes a class from the element |
| `htmx.removeExtension(name)` | Removes an htmx extension |
| `htmx.swap(elt, content, swapSpec)` | Performs swapping (and settling) of HTML content |
| `htmx.takeClass(elt, from)` | Takes a class from other elements for the given element |
| `htmx.toggleClass(elt, className)` | Toggles a class from the given element |
| `htmx.trigger(elt, name, detail)` | Triggers an event on an element |
| `htmx.values(elt)` | Returns the input values associated with the element |