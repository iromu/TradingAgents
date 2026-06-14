---
title: "Emabel Testing"
type: "reference"
status: "active"
language: "default"
source_paths: ["docs/embabel-agent-docs/testing.md"]
updated_at: "2026-06-14"
---

# Embabel Testing

## Unit Testing

Unit testing enables testing individual agent actions without real LLM calls. Agents are usually POJOs that can be instantiated with fake or mock objects.

### Fake Objects

- **`FakePromptRunner`** — Mocks LLM interactions, captures prompts, hyperparameters, tools, and tool groups
- **`FakeOperationContext`** — Provides a fake `OperationContext` for testing

### Verifying Prompts

```java
String prompt = context.getLlmInvocations().getFirst().getPrompt();
assertTrue(prompt.contains("expected content"));
```

### Verifying Tools

- `getInteraction().getTools()` — Returns actual `Tool` instances from `withToolObject()` and `withTool()`
- `getInteraction().getToolGroups()` — Returns `ToolGroupRequirement` objects from `withToolGroup()`

### Verifying Fluent API

`FakePromptRunner` fully supports `withId()` for interaction tracing and `creating()` for structured object creation with examples. Use `CreationExample` for reusable examples.

### Multiple LLM Interactions

Call the action method multiple times and verify each invocation's prompt and hyperparameters.

### Using Mockito Directly

For components using direct `Ai` injection, use Mockito or mockk to verify prompt and hyperparameters:

```java
when(ai.withDefaultLlm()).thenReturn(promptRunner);
when(promptRunner.createObject(anyString(), eq(String.class), any(LlmOptions.class)))
    .thenReturn("expected result");
```

## Integration Testing

Exercises complete agent workflows without real LLM calls for predictability and speed.

### EmbabelMockitoIntegrationTest

Base class that simplifies integration testing:
- Handles Spring Boot setup and LLM mocking automatically
- Provides `agentPlatform` and `llmOperations` pre-configured
- Helper methods: `whenCreateObject(prompt, outputClass)`, `whenGenerateText(prompt)`
- Advanced: `verifyCreateObjectMatching()`, `verifyNoMoreInteractions()`
- Call `supportsStreaming(true)` in test setup for streaming tests

### Testing Annotated Agents

Key steps:
1. Create an instance of the annotated agent class
2. Use `AgentMetadataReader` to extract metadata from annotations
3. Create an `AgentProcess` with a dummy `AgentPlatform`
4. Provide input data and run the process
5. Verify the output and side effects

```java
var metadata = AgentMetadataReader.read(agent);
var process = new AgentProcess(metadata, dummyAgentPlatform(), inputMap);
var result = process.run().resultOfType(ExpectedType.class);
```

### Integration Tests with Real LLMs

Embabel includes integration tests that exercise complete workflows with real LLM providers (OpenAI, Anthropic, Gemini). These verify end-to-end functionality including guardrails, thinking responses, and structured output. Located in autoconfiguration modules for each provider.

## Key Testing Patterns

- **Dummy AgentPlatform** — Use `IntegrationTestUtils.dummyAgentPlatform()` for lightweight testing without Spring context
- **Instance State** — Access instance fields directly to verify internal behavior (invocation counts, state changes)
- **Input Map** — Provide action parameters as `Map<String, Object>` where keys match parameter names
- **Result Extraction** — Use `agentProcess.run().resultOfType(ExpectedType.class)` for strongly-typed results
- **Exception Testing** — Use `assertThrows()` to verify failure scenarios and retry exhaustion
