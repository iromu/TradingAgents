Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.27. Working with LLM Reasoning / Thinking

#### 4.27.1. Motivation
Sometimes you need to validate an LLM’s reasoning process in addition to obtaining a structured result.Consider this scenario: a user wants to plan a vacation and specifies that their preferred destinations are Greece and Italy, with available travel dates in June, August, or September. They ask the LLM to find flight options with affordable tickets for a one-week stay. The LLM returns a structured `Flight` object containing departure and return dates, destinations, and prices. Even if the output adheres to the expected schema, you may want to verify that:
- The flight dates fall within the requested months
- The destinations are actually in Greece or Italy rather than somewhere elseIf the flight details fall outside the user’s criteria, access to the LLM’s reasoning process helps you understand why it made those choices.An even more important use case arises when the LLM cannot fulfill a request—for example, when it cannot create the requested object because the user’s criteria are ambiguous or contradictory. In this case, the thinking blocks explain what went wrong, even though no result was produced.
#### 4.27.2. Concepts

- `ThinkingBlock` — An abstraction that carries details about LLM reasoning, including the tag type, tag value, and reasoning text content.
- `ThinkingTagType` — An enum defining the types of reasoning markers: `TAG` (XML-style tags like `<think>`), `PREFIX` (line prefixes like `//THINKING:`), and `NO_PREFIX` (untagged reasoning text before JSON output).
- `ThinkingResponse<T>` — A response wrapper that holds both the result object and a list of `ThinkingBlock` instances.
- `ThinkingException` — An exception that preserves thinking blocks when object instantiation fails, enabling debugging even in error scenarios.
- `thinking()` — The core `PromptRunner` API method that enables thinking extraction.
#### 4.27.3. Example: Handling Objects and Thinking Blocks
JavaKotlin
#### 4.27.4. Example: Handling Failures Gracefully
Use `createObjectIfPossible` when the LLM might not be able to produce a valid result:JavaKotlin
#### 4.27.5. Provider Notes
Embabel exposes thinking through a provider-neutral API:
- `PromptRunner.thinking()`
- `LlmOptions.thinking`This remains the primary public API for enabling reasoning/thinking mode.Under the hood, provider integrations may translate `Thinking` differently to match provider-specific capabilities. For example, Google GenAI maps Embabel thinking options to the corresponding Spring AI Google GenAI chat options such as `includeThoughts` and `thinkingBudget`.No new application-level thinking API is required for callers. In general, existing applications should continue to use Embabel’s generic thinking API rather than provider-specific configuration.Some providers may also define model-level defaults in model YAML, but explicit runtime thinking requests still flow through `LlmOptions.thinking`.Provider behavior may differ slightly depending on how Spring AI surfaces reasoning data:
- Some providers expose reasoning on the assistant message itself
- Others expose it through generation metadataAs a result, the presence and shape of extracted thinking blocks may vary somewhat by provider and Spring AI integration version.