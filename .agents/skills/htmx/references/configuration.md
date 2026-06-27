---
source: https://htmx.org/docs/
fetched: 2026-06-26
---

# htmx Configuration Reference

## Configuration Options

All options are set on `htmx.config`:

| Option | Default | Description |
|--------|---------|-------------|
| `historyEnabled` | `true` | Enable browser history API |
| `historyCacheSize` | `10` | Number of pages to cache |
| `refreshOnHistoryMiss` | `false` | Full page refresh on history miss vs AJAX |
| `defaultSwapStyle` | `innerHTML` | Default swap style |
| `defaultSwapDelay` | `0` | Default swap delay (ms) |
| `defaultSettleDelay` | `20` | Default settle delay (ms) |
| `includeIndicatorStyles` | `true` | Load indicator CSS |
| `indicatorClass` | `htmx-indicator` | Class for loading indicators |
| `requestClass` | `htmx-request` | Class added during requests |
| `addedClass` | `htmx-added` | Class for newly added content |
| `settlingClass` | `htmx-settling` | Class during settling phase |
| `swappingClass` | `htmx-swapping` | Class during swap phase |
| `allowEval` | `true` | Allow eval-based features (trigger filters, hx-on) |
| `allowScriptTags` | `true` | Allow script tags in responses |
| `inlineScriptNonce` | `''` | Nonce for inline scripts in responses |
| `inlineStyleNonce` | `''` | Nonce for inline styles in responses |
| `attributesToSettle` | `["class","style","width","height"]` | Attributes to settle |
| `wsReconnectDelay` | `full-jitter` | WebSocket reconnection strategy |
| `wsBinaryType` | `blob` | Binary type for WebSocket data |
| `disableSelector` | `[hx-disable],[data-hx-disable]` | Elements to skip processing |
| `disableInheritance` | `false` | Disable attribute inheritance globally |
| `withCredentials` | `false` | Allow cross-site credentials |
| `timeout` | `0` | Request timeout (ms), 0 = no timeout |
| `scrollBehavior` | `instant` | Scroll behavior for `show` modifier (`instant`, `smooth`, `auto`) |
| `defaultFocusScroll` | `false` | Scroll focused element into view |
| `getCacheBusterParam` | `false` | Append cache-buster to GET requests |
| `globalViewTransitions` | `false` | Use View Transition API on all swaps |
| `methodsThatUseUrlParams` | `["get","delete"]` | Methods that encode params in URL |
| `selfRequestsOnly` | `true` | Only allow AJAX requests to same domain |
| `ignoreTitle` | `false` | Ignore `<title>` tags in responses |
| `scrollIntoViewOnBoost` | `true` | Scroll boosted target into view |
| `triggerSpecsCache` | `null` | Cache for trigger spec parsing |
| `responseHandling` | `[]` | Default response handling for status codes |
| `allowNestedOobSwaps` | `true` | Process OOB swaps on nested elements |
| `historyRestoreAsHxRequest` | `true` | Treat history cache miss as HX-Request |
| `reportValidityOfForms` | `false` | Report validation errors and update focus |

## Declarative Configuration

```html
<!-- Put in <head> -->
<meta name="htmx-config" content='{"defaultSwapStyle":"outerHTML"}'>
```

## Security Configuration

```javascript
// Disable eval (required for strict CSP)
htmx.config.allowEval = false;

// Disable script tag processing in responses
htmx.config.allowScriptTags = false;

// Only allow same-domain AJAX requests
htmx.config.selfRequestsOnly = true;

// Set CSRF timeout
htmx.config.timeout = 5000;
```

## Response Handling Configuration

Define how different HTTP status codes are handled:

```javascript
htmx.config.responseHandling = [
  {
    code: "2xx",
    swap: true,
    showValue: false
  },
  {
    code: "4xx",
    swap: false,
    error: true,
    title: "Client Error",
    detail: "A client error occurred",
    showIndicator: "#indicator",
    swapThreshold: 0
  },
  {
    code: "5xx",
    swap: false,
    error: true,
    title: "Server Error",
    detail: "A server error occurred",
    showIndicator: "#indicator",
    swapThreshold: 0
  }
];
```

## Key Defaults to Know

- **Default swap style**: `innerHTML`
- **Default settle delay**: 20ms
- **Default swap delay**: 0ms
- **Self requests only**: true (security)
- **History enabled**: true
- **Allow eval**: true (disable for CSP)
- **Allow script tags**: true (disable for CSP)