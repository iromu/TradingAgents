# htmx Configuration Reference

The `htmx.config` surface, how config is supplied, and the 2.x -> 4.x config changes.

Documents: **htmx 4.0.0**. Notes marked "2.x" describe the older line (npm `latest` is
`2.0.10`; `4.0.0` is on `next`; the htmx.org website still documents 2.x).

## Table of contents

- [How config is supplied](#how-config-is-supplied)
- [Configuration keys](#configuration-keys)
- [The `prefix` behaviour](#the-prefix-behaviour)
- [The `extensions` whitelist](#the-extensions-whitelist)
- [Compatibility flags](#compatibility-flags)
- [Renamed config keys](#renamed-config-keys)
- [Removed config keys](#removed-config-keys)

## How config is supplied

Config is set declaratively with a `<meta name="htmx-config">` tag (values use HCON, htmx's
configuration object notation, which also accepts JSON), or imperatively in JavaScript.

```html
<!-- JSON form -->
<meta name="htmx-config" content='{"defaultSwap":"outerHTML"}'>

<!-- HCON short form (same meaning) -->
<meta name="htmx-config" content="defaultSwap:outerHTML, logAll:true">
```

```js
htmx.config.defaultSwap = "outerHTML";
htmx.config.defaultTimeout = 60000;
```

## Configuration keys

All keys below appear in the v4 guidance config table; defaults are as stated there.

| Key | Default | Description |
|-----|---------|-------------|
| `defaultSwap` | `innerHTML` | Default swap strategy |
| `defaultTimeout` | `60000` | Request timeout in ms |
| `defaultSettleDelay` | `1` | Delay in ms between swap and settle |
| `defaultFocusScroll` | `false` | Scroll focused elements into view after a swap |
| `noSwap` | `[204, 304]` | Status codes that skip swapping |
| `allowEmptySwapAfterOOB` | `false` | Run the main swap when the response holds only OOB or partial content |
| `implicitInheritance` | `false` | Auto-inherit attributes from parents (2.x behaviour) |
| `transitions` | `false` | Enable View Transitions globally |
| `logAll` | `false` | Log every event to the console (debugging) |
| `mode` | `same-origin` | Fetch mode (`cors`, `no-cors`, `same-origin`) |
| `history` | `true` | History support (`true`, `false`, `"reload"`) |
| `extensions` | `""` | Whitelist of allowed extensions; empty allows all |
| `prefix` | `"data-hx-"` | Second attribute prefix, checked in addition to `hx-` |
| `metaCharacter` | unset (acts as `:`) | Character that introduces an attribute modifier |
| `indicatorClass` | `htmx-indicator` | Class on elements shown during a request |
| `requestClass` | `htmx-request` | Class added while a request is in flight |
| `includeIndicatorCSS` | `true` | Inject the default indicator stylesheet |
| `inlineScriptNonce` | unset | Nonce added to scripts htmx inserts |
| `morphIgnore` | `["data-htmx-powered"]` | Attribute-name prefixes left unchanged when morphing |
| `morphScanLimit` | `10` | Sibling scan limit during morphing |
| `morphSkip` | `'[hx-morph-skip]'` | CSS selector for elements to skip morphing entirely |
| `morphSkipChildren` | `'[hx-morph-skip-children]'` | CSS selector for elements whose children skip morphing |

New or changed relative to 2.x, worth calling out: `prefix`, `extensions`, `noSwap`,
`implicitInheritance`, `allowEmptySwapAfterOOB`, and `defaultTimeout` (renamed from `timeout`,
and its default changed from `0` to `60000`).

## The `prefix` behaviour

`prefix` is **additive, not a replacement**, at its default value. Out of the box `prefix`
is `"data-hx-"`, so both `hx-get` and `data-hx-get` work -- you do not need to convert
`data-hx-*` markup when moving to 4.x.

Setting `prefix` replaces the `"data-hx-"` prefix, so any existing `data-hx-*` markup stops
being recognised. The `hx-*` prefix always works regardless of `prefix`.

```html
<!-- adds a third spelling "x-hx-*"; data-hx-* markup STOPS working -->
<meta name="htmx-config" content='{"prefix": "x-hx-"}'>
```

There is no `wg-` prefix in the v4 sources -- that string appears nowhere in the release.

## The `extensions` whitelist

`extensions` restricts which extensions may load. It takes the **registration name** passed to
`htmx.registerExtension()`, which is not always the file name (see extensions.md).

```html
<meta name="htmx-config" content='{"extensions": "preload"}'>
```

An empty (default) value allows every registered extension to load.

## Compatibility flags

Two config flags restore 2.x defaults for an incremental migration (see upgrade-from-2.md):

```html
<meta name="htmx-config" content='{
  "implicitInheritance": true,
  "noSwap": [204, 304, "4xx", "5xx"]
}'>
```

- `implicitInheritance: true` -- restores automatic attribute inheritance (skips the
  `:inherited` migration step).
- `noSwap: [204, 304, "4xx", "5xx"]` -- restores 2.x's "4xx/5xx do not swap" behavior.
  In 4.x the default `noSwap` is `[204, 304]`, so all other status codes swap.

## Renamed config keys

From `upgrade-check.py` `CONFIG_RENAMES` (authoritative):

| 2.x key | 4.x key |
|---------|---------|
| `defaultSwapStyle` | `defaultSwap` |
| `globalViewTransitions` | `transitions` |
| `historyEnabled` | `history` |
| `includeIndicatorStyles` | `includeIndicatorCSS` |

Additional renames the migration guide documents (not in the tool's map):

| 2.x key | 4.x key | Notes |
|---------|---------|-------|
| `timeout` | `defaultTimeout` | Default also changed `0` -> `60000` ms |
| `selfRequestsOnly` | `mode = 'same-origin'` | Different mechanism, not a 1:1 rename |
| `responseHandling` (array) | `noSwap` + `hx-status` | Simpler model; `responseHandling` itself is removed |

## Removed config keys

From `upgrade-check.py` `REMOVED_CONFIG` (authoritative -- no 4.x equivalent):

`addedClass`, `allowEval`, `allowNestedOobSwaps`, `allowScriptTags`, `attributesToSettle`,
`defaultSwapDelay`, `disableSelector`, `getCacheBusterParam`, `historyCacheSize`,
`methodsThatUseUrlParams`, `refreshOnHistoryMiss`, `responseHandling`, `scrollBehavior`,
`scrollIntoViewOnBoost`, `selfRequestsOnly`, `settlingClass`, `swappingClass`,
`triggerSpecsCache`, `useTemplateFragments`, `withCredentials`, `wsBinaryType`,
`wsReconnectDelay`.

The migration guide additionally lists `ignoreTitle` as removed; it is NOT in the tool's
`REMOVED_CONFIG` set, so treat it as guide-only and verify against your build.

Note: 2.x used `allowEval` / `allowScriptTags` / `selfRequestsOnly` as CSP/security switches.
Those keys are gone in 4.x; strict-CSP support is handled through the `hx-csp` extension and
`inlineScriptNonce`, not through these removed flags.
