# Spec: htmx Usage

## ADDED Requirements

### Requirement: htmx scripts SHALL only be loaded on pages that use htmx attributes

The htmx library (`htmx.org@2.0.5`) SHALL only be included in a page's `<head>` if that page (or a fragment it includes) uses at least one htmx attribute (`hx-*`).

#### Scenario: form.html loads htmx via layout
- **WHEN** the user navigates to `/`
- **THEN** `form.html` includes htmx (via `hx-boost` on the form)
- **THEN** the script is loaded through the layout's `_htmx` mechanism

#### Scenario: platform.html does NOT load htmx
- **WHEN** the user navigates to `/platform`
- **THEN** `platform.html` does not include the htmx script tag
- **THEN** no htmx attributes are present on the page

#### Scenario: plan.html does NOT load htmx
- **WHEN** the user navigates to a completed plan result
- **THEN** `plan.html` does not include the htmx script tag
- **THEN** no htmx attributes are present on the page

### Requirement: Forms SHALL use progressive enhancement with hx-boost

The main ticker input form in `form.html` SHALL use `hx-boost="true"` on the `<form>` element to enable AJAX form submission without changing the controller.

#### Scenario: Form submits via AJAX
- **WHEN** the user submits the ticker form
- **THEN** the request is made via AJAX (htmx boost)
- **THEN** the `HX-Boosted` header is sent to the server
- **THEN** the controller response is swapped into the page

#### Scenario: Form degrades gracefully
- **WHEN** JavaScript is disabled
- **THEN** the form submits as a traditional POST
- **THEN** the page navigates normally (no htmx required)

### Requirement: Kill Process button SHALL target a valid element

The "Kill Process" button in `processing.html` SHALL target an element that can display the response from the delete endpoint, not `hx-target="this"` which would inject the response into the button itself.

#### Scenario: Kill Process targets the status container
- **WHEN** the user clicks "Kill Process" and confirms
- **THEN** the DELETE request is sent to `/api/v1/process/{id}`
- **THEN** the response is swapped into the `#agent-status` container (or appropriate parent)
- **THEN** the button is removed from the DOM as part of the swap

### Requirement: Navigation SHALL use hx-push-url for history management

Key navigation transitions SHALL use `hx-push-url="true"` so the browser history reflects the app state.

#### Scenario: Form submission updates URL
- **WHEN** the user submits the ticker form with `hx-push-url`
- **THEN** the URL updates to `/plan/status/{processId}`
- **THEN** the browser back button returns to the form page

## REMOVED Requirements

### Requirement: htmx scripts on non-HTMX pages

Pages that do not use any htmx attributes SHALL NOT include the htmx script tag.

**Reason:** Dead JS payload increases page weight and can confuse developers about which pages use htmx.

**Migration:** Remove `<script src="https://unpkg.com/htmx.org@2.0.5"></script>` from `platform.html` and `plan.html`. Convert `form.html` to use the layout's `_htmx` model attribute or add `hx-boost` and keep the script.