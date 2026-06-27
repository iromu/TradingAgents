# Interceptors & Callbacks Reference

Tool loop callbacks for observing and transforming LLM interactions. See SKILL.md for the core workflow.

## Tool Loop Architecture

LLM invocations run inside a `ToolLoop`. The loop separates **observation** (inspectors) from **transformation** (transformers).

### Inspectors (Read-Only)

Implement `ToolLoopInspector` to observe the loop without modifying it:

```kotlin
interface ToolLoopInspector : ToolLoopCallback {
    fun beforeLlmCall(context: BeforeLlmCallContext) = Unit
    fun afterLlmCall(context: AfterLlmCallContext) = Unit
    fun afterToolResult(context: AfterToolResultContext) = Unit
    fun afterIteration(context: AfterIterationContext) = Unit
}
```

Use for: logging, metrics, debugging.

### Transformers (Modifying)

Implement `ToolLoopTransformer` to modify data flowing through the loop:

```kotlin
interface ToolLoopTransformer : ToolLoopCallback {
    fun transformBeforeLlmCall(context: BeforeLlmCallContext): List<Message> = context.history
    fun transformAfterLlmCall(context: AfterLlmCallContext): Message = context.response
    fun transformAfterToolResult(context: AfterToolResultContext): String = context.resultAsString
    fun transformAfterIteration(context: AfterIterationContext): List<Message> = context.history
}
```

Use for: truncating large tool results, sliding window on conversation history, redacting sensitive content.

## Built-In Callbacks

| Callback | Type | Purpose |
|----------|------|---------|
| `ToolLoopLoggingInspector` | Inspector | Logs calls before/after LLM invocations, after tool execution, after iteration |
| `ToolResultTruncatingTransformer` | Transformer | Truncates tool call results to manage context size |
| `SlidingWindowTransformer` | Transformer | Maintains a sliding window of messages, preserving system messages |

## Usage

```java
var result = ai.withDefaultLlm()
    .withTools(tools)
    .withToolLoopInspectors(callbackTracker, loggingInspector)
    .withToolLoopTransformers(truncatingTransformer, windowTransformer)
    .respond(userPrompt);
```

## Key Points

- Inspectors are read-only; transformers modify what the LLM sees
- Multiple inspectors and transformers can be chained
- Built-ins cover common patterns: logging, truncation, sliding window
- Transformers can reduce context size and prevent token overflow