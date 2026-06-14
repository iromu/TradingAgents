---
title: "Emabel Tools and Tool Groups"
type: "concept"
status: "active"
language: "default"
source_paths: ["docs/embabel-agent-docs/tools.md"]
updated_at: "2026-06-14"
---

# Embabel Tools and Tool Groups

## Tools

Tools extend LLM capabilities by letting agents interact with the outside world. Methods annotated with `@LlmTool` (Embabel) or `@Tool` (Spring AI) on classes become tools exposed to the LLM.

### Key Characteristics

- Tools can be stateful — often domain objects with `@LlmTool` methods
- Tools on mapped entities are especially useful, encapsulating state never exposed to the LLM
- Tool methods can have any visibility, static or instance scope
- Return types must be serializable (no Optional, async, reactive, or functional types)
- Tools can produce side effects (database records, API calls, etc.)

### Getting Tool Context

- **`AgentProcess.get()`** — Obtain the current process in a tool method to bind objects to the blackboard
- **`ToolCallContext`** — Immutable key-value bag for infrastructure metadata (auth tokens, tenant IDs, correlation IDs). Injected into `@LlmTool` methods but invisible in the tool's JSON schema. Set via `ProcessOptions.withToolCallContext()` or `PromptRunner.withToolCallContext()`.

## Tool Groups

A level of indirection between user intent and tool selection. Instead of asking for a specific tool, you ask for a category (e.g., "web" tools) which may be resolved differently in different environments.

### Configuration

- **MCP-backed** — Tools from MCP servers, selectively exposed via `McpToolGroup` with filter lambdas
- **Custom** — Any `@Configuration` class can expose `ToolGroup` beans backed by any service
- **Application config** — Select tools by name in `application.yml`

### Using Tools in Actions

Tools are specified on the `PromptRunner` when making LLM calls:

```java
context.ai().withDefaultLlm()
    .withToolGroup("web")
    .withToolObject(myDomainObject)
    .createObject(String.class, model);
```

Available methods: `withToolGroup()`, `withToolGroups()`, `withTools()`, `withToolObject()`, `withTool()`.

## Subagent: Agent Handoffs as Tools

A `Subagent` is a specialized `Tool` that delegates to another Embabel agent. When the LLM invokes this tool, it runs the specified agent as a subprocess, sharing the parent process's blackboard context.

```java
Subagent.ofClass(PerformanceFinder.class).consuming(WorksToFind.class)
```

- The LLM sees the JSON schema for the input type
- The subagent receives a spawned blackboard from the parent
- Objects added by the subagent are available to the parent after completion
- Use `Subagent` for complex specialized tasks with multi-action workflows; use regular `@LlmTool` for simple deterministic calls

## Agentic Tools

Tools that use an LLM to orchestrate other tools. Three implementations:

| Tool Type | Description |
|-----------|-------------|
| **SimpleAgenticTool** | All tools available immediately; simple orchestration |
| **PlaybookTool** | Progressive unlock via conditions (prerequisites, artifacts, blackboard) |
| **StateMachineTool** | State-based availability using enum states |

## Tool Decoration

Embabel uses a decoration pattern to extend tool behavior without modifying the underlying tool. All decorators implement `DelegatingTool`.

- **`ArtifactSinkingTool`** — Captures artifacts from tool results and sends to a sink (blackboard, list, etc.)
- **`OneShotPerLoopTool`** — Ensures a tool fires at most once per agentic loop iteration (useful for skill activators)
- Built-in sinks: `BlackboardSink`, `ListSink`, `CompositeSink`

## Framework-Agnostic Tool Interface

Embabel provides `Tool` interface in `com.embabel.agent.api.tool` for framework-independent tool creation:

- **`Tool.create()`** — Simple tools with basic inputs
- **`Tool.fromFunction()`** — Complex typed inputs/outputs with automatic JSON marshaling
- **`@LlmTool` / `@LlmTool.Param`** — Annotation-based tools using Embabel's abstractions

## Tool Description Modification

Tools provide `withDescription(newDescription)` and `withNote(note)` methods to create copies with modified descriptions for specific contexts.
