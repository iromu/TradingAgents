Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.28. Working with Callbacks (Interceptors)

#### 4.28.1. Tool Loop Callbacks
LLM invocations in Embabel take place inside `ToolLoop` (see Advanced: Custom LLM Integration).
Embabel Tool Loop is highly customizable, offering clear extension points with a separation between observation (inspectors) and transformation (transformers).Inspectors observe the loop without modifying it. Implement any callback:
- `beforeLlmCall`,
- `afterLlmCall`,
- `afterToolResult`,
- `afterIteration`.Inspectors are perfect for logging, collecting metrics, debugging, and are read-only by design.Transformers modify data flowing through the loop. Use them to truncate large tool results, apply sliding window to conversation history, redact sensitive content. They change what the LLM sees.
##### Tool Loop Callbacks Interfaces
Below are callbacks interfaces (in Kotlin):
##### Simple Out-of-Box Tool Loop Callbacks
Framework provides with simple out-of-box callbacks:
- `ToolLoopLoggingInspector` — logs calls details before and after LLM invocations, after Tool Execution, and after Tool Loop Iteration
- `ToolResultTruncatingTransformer` — truncates tool call results
- `SlidingWindowTransformer` — maintains a sliding window of messages to manage context size, while preserving conversation context system messages
##### Usage: Tool Loop Callbacks

#### 4.28.2. Tool Call Interceptors
While tool loop callbacks provide with powerful features for observing LLM invocations, conversation history and Tools execution, there is also a practical need for the trimmed version of inspector callbacks.
##### Motivation: streaming mode
Streaming is event-driven, see Working with Streams. Streaming model provides with callbacks for getting thinking blocks and structured object.Framework also provides with additional type of streaming callbacks - Tool Call callbacks.Tool Call callback includes info about tool definition, tool result, and tool execution duration.
##### Tool Call Interface

##### Simple Out-of-Box Tool Call Interceptor
Please refer to `ToolCallLoggingInspector` for collecting tool call metrics.
##### Usage: Tool Call Interceptors

| | Tool Call Interceptors are applicable to both streaming and non-streaming modes. |