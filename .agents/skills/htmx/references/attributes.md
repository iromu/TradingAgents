# htmx Attributes Quick Reference

## Core Attributes

| Attribute | Description |
|-----------|-------------|
| `hx-get` | Issues a GET to the specified URL |
| `hx-post` | Issues a POST to the specified URL |
| `hx-put` | Issues a PUT to the specified URL |
| `hx-patch` | Issues a PATCH to the specified URL |
| `hx-delete` | Issues a DELETE to the specified URL |
| `hx-on*` | Handle events with inline scripts on elements |
| `hx-target` | Specifies the target element to be swapped |
| `hx-swap` | Controls how content will swap in |
| `hx-trigger` | Specifies the event that triggers the request |
| `hx-push-url` | Push a URL into the browser location bar for history |
| `hx-select` | Select content to swap in from a response |
| `hx-select-oob` | Select content to swap in from a response, out of band |
| `hx-swap-oob` | Mark element to swap in from a response (out of band) |
| `hx-vals` | Add values to submit with the request (JSON format) |

## Additional Attributes

| Attribute | Description |
|-----------|-------------|
| `hx-boost` | Add progressive enhancement for links and forms |
| `hx-confirm` | Shows a confirm() dialog before issuing a request |
| `hx-disable` | Disables htmx processing for the node and children |
| `hx-disabled-elt` | Disables specified elements during a request |
| `hx-disinherit` | Control/disable automatic attribute inheritance for children |
| `hx-encoding` | Changes the request encoding type (e.g., multipart/form-data) |
| `hx-ext` | Extensions to use for this element |
| `hx-headers` | Adds headers to the request |
| `hx-history` | Prevent sensitive data from being saved to the history cache |
| `hx-history-elt` | The element to snapshot during history navigation |
| `hx-include` | Include additional data in requests |
| `hx-indicator` | Element to put the htmx-request class on during the request |
| `hx-inherit` | Control/enable automatic attribute inheritance for children |
| `hx-params` | Filters the parameters submitted with a request |
| `hx-preserve` | Specifies elements to keep unchanged between requests |
| `hx-prompt` | Shows a prompt() before submitting a request |
| `hx-replace-url` | Replace the URL in the browser location bar |
| `hx-request` | Configures various aspects of the request |
| `hx-sync` | Control how requests made by different elements are synchronized |
| `hx-validate` | Force elements to validate themselves before a request |

## CSS Classes

| Class | Description |
|-------|-------------|
| `htmx-added` | Applied to new content before swap, removed after settle |
| `htmx-indicator` | Toggled visible (opacity:1) when htmx-request is present |
| `htmx-request` | Applied to element during an ongoing request |
| `htmx-settling` | Applied after content is swapped, removed after settling |
| `htmx-swapping` | Applied before content is swapped, removed after swap |

## Request Headers (Sent to Server)

| Header | Description |
|--------|-------------|
| `HX-Boosted` | Indicates request is via an element using hx-boost |
| `HX-Current-URL` | The current URL of the browser |
| `HX-History-Restore-Request` | "true" if request is for history restoration after cache miss |
| `HX-Prompt` | The user response to an hx-prompt |
| `HX-Request` | Always "true" |
| `HX-Target` | The id of the target element if it exists |
| `HX-Trigger` | The id of the triggered element if it exists |
| `HX-Trigger-Name` | The name of the triggered element if it exists |

## Response Headers (From Server)

| Header | Description |
|--------|-------------|
| `HX-Location` | Client-side redirect without full page reload |
| `HX-Push-Url` | Pushes a new URL into the history stack |
| `HX-Redirect` | Client-side redirect to a new location |
| `HX-Refresh` | If "true", client does a full page refresh |
| `HX-Replace-Url` | Replaces the current URL in the location bar |
| `HX-Reswap` | Specifies how the response will be swapped |
| `HX-Retarget` | CSS selector that updates the target of the content update |
| `HX-Reselect` | CSS selector that chooses which part of the response is swapped in |
| `HX-Trigger` | Triggers client-side events |
| `HX-Trigger-After-Settle` | Triggers client-side events after the settle step |
| `HX-Trigger-After-Swap` | Triggers client-side events after the swap step |