# Spec: Synchronous LLM Calls

## Purpose

Enforce the use of Embabel's synchronous `createObject()` API for all LLM calls in agent code, eliminating manual Flux streaming and ensuring consistency with Embabel best practices.

## Requirements

### Requirement: All LLM calls use synchronous createObject()

Every LLM call in the refactored agents SHALL use Embabel's synchronous `createObject()` API:

```java
return context.ai()
    .withLlmByRole(role)
    .withId("actionId")
    .withTemplate("templateName")
    .createObject(String.class, model);
```

The synchronous `createObject()` SHALL handle streaming internally when the model supports it, and fall back to non-streaming otherwise.

Manual Flux handling, `StreamingPromptRunner` casts, and `.subscribeOn(Schedulers.boundedElastic())` SHALL NOT be used anywhere in the agent code.

The `streamWithTemplate()` private helper method SHALL be removed entirely.

#### Scenario: LLM call uses createObject for template rendering
- **WHEN** an action needs to call an LLM with a Jinja template
- **THEN** it uses `.withTemplate("name").createObject(String.class, model)` — a single fluent chain

#### Scenario: LLM call uses createObject for structured output
- **WHEN** an action needs to produce a typed record (e.g., `Ticker`, `FundamentalsReport`)
- **THEN** it uses `.creating(Ticker.class).fromPrompt(prompt)` — the structured creation API

#### Scenario: No manual Flux in agent code
- **WHEN** scanning all agent source files for reactive imports
- **THEN** no `Flux`, `Publisher`, or `StreamingPromptRunner` imports are present

### Requirement: PromptRunner fluent API usage

All LLM calls SHALL use the PromptRunner fluent API:
- `.withLlmByRole(role)` for role-based model selection
- `.withId("actionId")` for traceability and testing
- `.withTemplate("templateName")` for template-based prompts
- `.creating(T.class).fromPrompt(prompt)` for structured output
- `.withTemplate("name").createObject(T.class, model)` for template + structured output

#### Scenario: All LLM calls have .withId() for traceability
- **WHEN** every action makes an LLM call
- **THEN** the call includes `.withId("actionName")` for process tracing

#### Scenario: All LLM calls use role-based model selection
- **WHEN** an action needs an LLM call
- **THEN** it uses `.withLlmByRole(CHEAPEST_ROLE)` or `.withLlmByRole(BEST_ROLE)` instead of hardcoded model names