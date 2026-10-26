# Upgrading htmx 2.x to 4.x

Condensed 13-step migration path from the upstream upgrade guide, with the ordering warnings
that change correctness if skipped.

Documents: **htmx 4.0.0** (target). Source line is `2.0.10` (npm `latest`; `4.0.0` is on
`next`; the htmx.org website still documents 2.x).

## Table of contents

- [Step 0: run the upgrade checker](#step-0-run-the-upgrade-checker)
- [Step 1: attribute renames (order matters)](#step-1-attribute-renames-order-matters)
- [Step 2: removed attributes](#step-2-removed-attributes)
- [Step 3: attribute inheritance](#step-3-attribute-inheritance)
- [Step 4: `data-hx-*` attributes](#step-4-data-hx--attributes)
- [Step 5-6: events and handler code](#step-5-6-events-and-handler-code)
- [Step 7: configuration](#step-7-configuration)
- [Step 8: server-side headers](#step-8-server-side-headers)
- [Step 9-10: JS API and extensions](#step-9-10-js-api-and-extensions)
- [Step 11: GET/DELETE form data](#step-11-getdelete-form-data)
- [Step 12: OOB ordering](#step-12-oob-ordering)
- [Step 13: non-2xx swapping](#step-13-non-2xx-swapping)
- [Compatibility routes](#compatibility-routes)

## Step 0: run the upgrade checker

Run the shipped checker first to build the worklist. It prints clickable `file:line` hits with
a suggested fix and rewrites nothing.

```bash
npx htmx.org@4.0.0 upgrade-check -- ./path/to/project

# add file extensions the scanner does not know
npx htmx.org@4.0.0 upgrade-check --ext .vue ./path/to/project
```

Pin the version to the release you are moving to. Default extensions scanned: `.html`, `.php`,
`.js`, `.ts`, `.jinja`, `.jinja2`, `.j2`, `.erb`, `.hbs`. Add more with `--ext` (repeatable, or
comma-separated). Work the remaining steps using its output.

## Step 1: attribute renames (order matters)

Rename `hx-disable -> hx-ignore` FIRST, then `hx-disabled-elt -> hx-disable`. In 2.x
`hx-disable` stopped htmx processing (= 4.x `hx-ignore`); 4.x `hx-disable` means "disable during
request" (= 2.x `hx-disabled-elt`). Running them in the wrong order clobbers the freshly created
`hx-disable` attributes and silently corrupts behaviour.

```
hx-disable       ->  hx-ignore      # 2.x "stop htmx processing"
hx-disabled-elt  ->  hx-disable     # 2.x "disable elements during request"
```

## Step 2: removed attributes

| Find | Replace with |
|------|--------------|
| `hx-vars='...'` | `hx-vals='js:...'` (wrap the value in a `js:` prefix) |
| `hx-params="..."` | Remove; filter params in the `htmx:config:request` event |
| `hx-prompt="..."` | Load the `hx-prompt` extension (same syntax as 2.x) |
| `hx-ext="..."` | Remove (just include the extension script) |
| `hx-disinherit="..."` | Remove (inheritance is explicit by default) |
| `hx-inherit="..."` | Remove (use the `:inherited` modifier per attribute) |
| `hx-request='...'` | `hx-config='...'` (HCON accepts the old JSON) |
| `hx-history="false"` | Remove (history no longer uses localStorage) |

## Step 3: attribute inheritance

2.x inherited every attribute from ancestors implicitly. 4.x requires an explicit `:inherited`
modifier. **Do not blanket-add `:inherited`.** Analyze the codebase and add it only where a
descendant that makes a request actually relies on an ancestor's value.

Look for a parent carrying an attribute while child elements make requests without declaring it
themselves. Frequently-inherited attributes: `hx-target`, `hx-include`, `hx-swap`, `hx-boost`,
`hx-confirm`, `hx-headers`, `hx-indicator`, `hx-sync`, `hx-config`, `hx-encoding`,
`hx-validate`. For `hx-boost`, a value on a non-link/non-form element is almost always meant for
its descendants.

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

A `hx-headers` CSRF token on a `<body>` base template is the classic miss: without
`:inherited` the header no longer reaches child-template requests and the server rejects them.

## Step 4: `data-hx-*` attributes

No change needed. `htmx.config.prefix` defaults to `"data-hx-"` and is additive, so both
`hx-get` and `data-hx-get` work. Leave `data-hx-*` markup as is. Setting `prefix` adds a third
spelling but replaces `"data-hx-"` (so `data-hx-*` markup stops working); `hx-*` always works.

## Step 5-6: events and handler code

Rename camelCase event names to colon form (`htmx:afterSwap` -> `htmx:after:swap`, etc.) in JS
listeners, in `hx-on:` attributes, and in `hx-trigger` values. Full maps are in events-api.md;
run the checker to catch them in place.

The handler detail shape changed: `event.detail` now carries `event.detail.ctx`.

```js
// 2.x
evt.detail.headers['X-Custom'] = 'v';
evt.detail.parameters['k'] = 'v';
evt.detail.path = '/modified-url';
// 4.x
evt.detail.ctx.request.headers['X-Custom'] = 'v';
evt.detail.ctx.request.body.set('k', 'v');   // FormData
evt.detail.ctx.request.action = '/modified-url';
```

## Step 7: configuration

Rename `defaultSwapStyle->defaultSwap`, `globalViewTransitions->transitions`,
`historyEnabled->history`, `includeIndicatorStyles->includeIndicatorCSS`, `timeout->defaultTimeout`
(new default `60000`). Full rename/removed tables are in configuration.md.

## Step 8: server-side headers

Update code that reads request headers or sends response headers (often middleware / base controllers).

| 2.x request header | 4.x | Change |
|--------------------|-----|--------|
| `HX-Trigger` | `HX-Source` | Renamed; value format changed from an element ID to `tag#id` (e.g. `button#submit`) |
| `HX-Trigger-Name` | Removed | Use `HX-Source` |
| `HX-Target` | `HX-Target` | Value format now `tag#id` |
| `HX-Prompt` | via extension | Load the `hx-prompt` extension to restore the header |

New request header: **`HX-Request-Type`** (`"full"` or `"partial"`).

Removed response headers: **`HX-Trigger-After-Swap`** and **`HX-Trigger-After-Settle`** -- use
`HX-Trigger` (or JavaScript) instead. Still supported: `HX-Trigger`, `HX-Push-Url`,
`HX-Replace-Url`, `HX-Redirect`, `HX-Location`, `HX-Refresh`, `HX-Retarget`, `HX-Reswap`,
`HX-Reselect`.

## Step 9-10: JS API and extensions

Replace removed helpers with native DOM calls (`htmx.addClass` -> `classList.add`, etc.) and
`htmx.defineExtension` with `htmx.registerExtension` -- see events-api.md for the full table.
Custom extensions need a full rewrite to the event-hook model (see extensions.md).

## Step 11: GET/DELETE form data

In 4.x `hx-get` and `hx-delete` no longer include the enclosing form's inputs. If a delete (or
get) button relied on form data, add `hx-include="closest form"`.

```html
<!-- 2.x: form data included automatically; 4.x: must include the form explicitly -->
<form>
  <input type="hidden" name="token" value="abc">
  <button hx-delete="/item/1" hx-include="closest form">Delete</button>
</form>
```

## Step 12: OOB ordering

In 2.x out-of-band swaps happened before the main swap. In 4.x the main content swaps first,
then OOB/partial elements swap after. Code that assumed OOB elements were present during the main
swap may need restructuring. An OOB/partial-only response also no longer performs an empty main
swap by default; set `htmx.config.allowEmptySwapAfterOOB = true` or add `swapEmpty:true` to
`hx-swap` if you relied on OOB-only responses clearing the main target.

## Step 13: non-2xx swapping

In 2.x 4xx/5xx responses did not swap by default. In 4.x all responses swap except 204 and 304.
If your server returns error HTML in 4xx/5xx and you do not want it swapped in:

- `htmx.config.noSwap = [204, 304, "4xx", "5xx"]` to restore 2.x behavior, or
- `hx-status:4xx="swap:none"` / `hx-status:5xx="swap:none"` on specific elements, or
- return `204 No Content` when you want no swap.

## Compatibility routes

For large codebases, two bridges ease an incremental migration (not long-term solutions):

Config flags -- restore 2.x defaults via the config meta tag (skips Steps 3 and 13):

```html
<meta name="htmx-config" content='{
  "implicitInheritance": true,
  "noSwap": [204, 304, "4xx", "5xx"]
}'>
```

Compatibility extension -- `htmx-2-compat.js` (registers as `compat`) fires old event names
alongside new ones and handles old attribute names:

```html
<script src="/path/to/htmx.js"></script>
<script src="/path/to/ext/htmx-2-compat.js"></script>
```
