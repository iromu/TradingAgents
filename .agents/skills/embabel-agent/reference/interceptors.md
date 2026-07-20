# Interceptors & Callbacks Reference

Tool loop callbacks for observing and transforming LLM interactions. See SKILL.md for the core workflow.

## Overview

LLM invocations run inside a `ToolLoop`. The loop separates **observation** (inspectors) from **transformation** (transformers).

- **Inspectors** — read-only observers. Use for logging, metrics, debugging.
- **Transformers** — modify data flowing through the loop. Use for truncation, sliding windows, redaction.

Both are chainable: pass any number via `.withToolLoopInspectors(...)` / `.withToolLoopTransformers(...)`.

## ToolLoopInspector

A read-only observer for four lifecycle events:

```kotlin
interface ToolLoopInspector : ToolLoopCallback {
    fun beforeLlmCall(context: BeforeLlmCallContext) = Unit
    fun afterLlmCall(context: AfterLlmCallContext) = Unit
    fun afterToolResult(context: AfterToolResultContext) = Unit
    fun afterIteration(context: AfterIterationContext) = Unit
}
```

### Custom Inspector Example

Track tool result sizes in Kotlin:

```kotlin
class SizeTrackingInspector : ToolLoopInspector {
    private val sizes = mutableListOf<Int>()

    override fun afterToolResult(context: AfterToolResultContext) {
        val size = context.resultAsString.length
        sizes += size
        println("Tool result: ${size} chars")
    }

    fun averageSize(): Double = sizes.average()
}
```

Java equivalent:

```java
public class SizeTrackingInspector implements ToolLoopInspector {
    private final List<Integer> sizes = new ArrayList<>();

    @Override
    public void afterToolResult(AfterToolResultContext ctx) {
        int size = ctx.getResultAsString().length();
        sizes.add(size);
        System.out.println("Tool result: " + size + " chars");
    }

    public double averageSize() {
        return sizes.stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}
```

## ToolLoopTransformer

Modify data at four hook points:

```kotlin
interface ToolLoopTransformer : ToolLoopCallback {
    fun transformBeforeLlmCall(context: BeforeLlmCallContext): List<Message> = context.history
    fun transformAfterLlmCall(context: AfterLlmCallContext): Message = context.response
    fun transformAfterToolResult(context: AfterToolResultContext): String = context.resultAsString
    fun transformAfterIteration(context: AfterIterationContext): List<Message> = context.history
}
```

### Custom Transformer Example

Truncate tool results exceeding a character limit (Kotlin):

```kotlin
class MaxLengthTransformer(private val maxChars: Int) : ToolLoopTransformer {
    override fun transformAfterToolResult(context: AfterToolResultContext): String {
        val result = context.resultAsString
        return if (result.length > maxChars) {
            result.take(maxChars) + "\n... (truncated, was ${result.length} chars)"
        } else result
    }
}
```

Java equivalent:

```java
public class MaxLengthTransformer implements ToolLoopTransformer {
    private final int maxChars;

    public MaxLengthTransformer(int maxChars) { this.maxChars = maxChars; }

    @Override
    public String transformAfterToolResult(AfterToolResultContext ctx) {
        String result = ctx.getResultAsString();
        if (result.length() > maxChars) {
            return result.substring(0, maxChars)
                + "\n... (truncated, was " + result.length() + " chars)";
        }
        return result;
    }
}
```

## Built-In Inspectors and Transformers

| Name | Type | Purpose |
|------|------|---------|
| `ToolLoopLoggingInspector` | Inspector | Logs calls before/after LLM invocations, after tool execution, after iteration |
| `ToolResultTruncatingTransformer` | Transformer | Truncates tool call results to a configurable max length |
| `SlidingWindowTransformer` | Transformer | Maintains a sliding window of messages, preserving system messages |
| `ToolCallLoggingInspector` | Inspector | Lightweight tool-call-level logging (no history access); works in streaming mode |

### Tool Call Inspector (Lightweight)

For cases where you only need to observe individual tool calls (no conversation history):

```kotlin
interface ToolCallInspector : ToolLoopCallback {
    fun beforeToolCall(context: BeforeToolCallContext) = Unit
    fun afterToolCall(context: AfterToolCallContext) = Unit
}
```

Usage (Java):

```java
PromptRunner runner = ai.withDefaultLlm()
    .withToolObject(new Tooling())
    .withToolCallInspectors(new ToolCallLoggingInspector(
        ToolLoopLoggingInspector.LogLevel.INFO, logger));
```

Applicable in both streaming and non-streaming modes.

## Configuration

Configure built-in transformers via `application.yml`:

```yaml
embabel:
  agent:
    tool-loop:
      # Truncation
      max-tool-result-length: 2000
      # Sliding window
      sliding-window-size: 50
      # Logging level
      logging-level: INFO
```

### Wiring in Code

```java
var inspector = new SizeTrackingInspector();
var truncator = new ToolResultTruncatingTransformer(2000);
var window = new SlidingWindowTransformer(50);
var logging = new ToolLoopLoggingInspector(ToolLoopLoggingInspector.LogLevel.INFO);

var result = ai.withDefaultLlm()
    .withTools(tools)
    .withToolLoopInspectors(logging, inspector)
    .withToolLoopTransformers(truncator, window)
    .creating(RestaurantRecommendation.class)
    .fromPrompt("Find Italian restaurants near Upper East Side, NYC.");
```

## Key Points

- Inspectors are read-only; transformers modify what the LLM sees
- Multiple inspectors and transformers can be chained
- `ToolCallInspector` is a lightweight alternative to `ToolLoopInspector` when history context is not needed
- Built-ins cover common patterns: logging, truncation, sliding window
---

*Source: Embabel Agent v1.0.0 documentation*
