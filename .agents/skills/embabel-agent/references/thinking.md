# Thinking (Reasoning) Reference

Extract LLM reasoning/thinking blocks alongside structured results. See SKILL.md for the core workflow.

## Motivation

Sometimes you need to validate an LLM's reasoning process in addition to obtaining a structured result. Thinking blocks help you:

- Verify the LLM adhered to constraints (e.g., dates within requested months, destinations in specified countries)
- Understand why the LLM made certain choices
- Diagnose what went wrong when the LLM cannot produce a valid object

## Core Types

| Type | Description |
|------|-------------|
| `ThinkingBlock` | Carries reasoning details: tag type, tag value, reasoning text |
| `ThinkingTagType` | Enum: `TAG` (XML-style `<think>`), `PREFIX` (line prefix `//THINKING:`), `NO_PREFIX` (untagged text before JSON) |
| `ThinkingResponse<T>` | Wrapper holding the result object and a list of `ThinkingBlock` instances |
| `ThinkingException` | Preserves thinking blocks when object instantiation fails |

## Using Thinking Mode

Call `.thinking()` on the `PromptRunner` before creating an object. This enables thinking extraction for any LLM that supports reasoning modes (e.g., Claude).

```java
PromptRunner runner = ai.withDefaultLlm()
    .withToolObject(Tooling.class);

String prompt = """
    What is the hottest month in Florida and its average high temperature?
    Please provide a detailed analysis of your reasoning.
    """;

ThinkingResponse<MonthItem> response = runner
    .thinking()
    .createObject(prompt, MonthItem.class);
```

### Graceful Failure Handling

Use `createObjectIfPossible` when the LLM might not be able to produce a valid result — the thinking blocks explain what went wrong:

```java
ThinkingResponse<MonthItem> response = runner
    .thinking()
    .createObjectIfPossible(prompt, MonthItem.class);

MonthItem result = response.getResult();
if (result != null) {
    // Process the result normally
} else {
    // Object creation failed — examine the reasoning to understand why
    for (ThinkingBlock block : response.getThinkingBlocks()) {
        logger.info("LLM reasoning: {}", block.getContent());
    }
}
```

## Extracting Thinking Output

Access thinking blocks from the `ThinkingResponse` wrapper. Each block carries its tag type, tag value, and content:

```java
List<ThinkingBlock> thinkingBlocks = response.getThinkingBlocks();

for (ThinkingBlock block : thinkingBlocks) {
    System.out.println("Type: " + block.getTagType());   // TAG, PREFIX, or NO_PREFIX
    System.out.println("Tag: " + block.getTagValue());   // e.g., "think", "analysis"
    System.out.println("Content: " + block.getContent());
}
```

## Thinking Tag Control

Without changing your prompt, control which XML-tagged reasoning blocks the model produces and returns:

- `includedTags` — keep only blocks wrapped in the named tags; the model is automatically instructed (via a system prompt hint) to use those tags for its reasoning
- `excludedTags` — drop blocks with specific tag names while keeping all others

Only named TAG blocks are affected. Untagged reasoning (PREFIX and NO_PREFIX style blocks) always passes through.

```java
// Only return the "analysis" reasoning block; model is guided to use that tag
ThinkingResponse<MonthItem> response = runner
        .thinking(Thinking.withIncludedTags("analysis"))
        .createObject(prompt, MonthItem.class);

// Drop "scratchpad" blocks, keep all others
ThinkingResponse<MonthItem> response2 = runner
        .thinking(Thinking.withExcludedTags("scratchpad"))
        .createObject(prompt, MonthItem.class);
```

```kotlin
val response = runner
        .thinking(Thinking.withIncludedTags("analysis"))
        .createObject(prompt, MonthItem::class.java)
```

## Provider Notes

Embabel exposes thinking through a provider-neutral API:

- `PromptRunner.thinking()` — enable thinking on a runner
- `PromptRunner.thinking(Thinking)` — enable with tag control (`withIncludedTags`, `withExcludedTags`)
- `LlmOptions.thinking` — configure thinking at the options level

Under the hood, provider integrations translate thinking options to provider-specific capabilities (e.g., Google GenAI maps to `includeThoughts` and `thinkingBudget`). No new application-level thinking API is required — existing applications should continue using Embabel's generic thinking API rather than provider-specific configuration.

Some providers expose reasoning on the assistant message itself; others expose it through generation metadata. As a result, the presence and shape of extracted thinking blocks may vary by provider and Spring AI integration version.
---

*Source: Embabel Agent v1.5.3 documentation — `references/thinking`*
