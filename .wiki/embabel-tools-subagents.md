---
title: "Emabel Tools and Subagents"
type: "reference"
status: "active"
language: "default"
source_paths: ["docs/embabel-agent-docs/tools.md"]
updated_at: "2026-06-14"
---

# Embabel Tools and Subagents

## Tools

Tools extend LLM capabilities by letting agents interact with the outside world. In Embabel, tools are implemented in two ways:

### Tool Instances

Methods annotated with `@LlmTool` (Embabel) or `@Tool` (Spring AI) on any class. Each annotated method is a distinct tool exposed to the LLM.

- Tool methods can have any visibility, static or instance scope
- Any number of arguments (primitives, POJOs, enums, lists, maps)
- Return type must be serializable (no Optional, CompletableFuture, Flux, Mono, Function)
- Tools can be stateful — often domain objects with `@Tool` methods

### Tool Groups

A level of indirection between user intent and tool selection. Instead of asking for a specific tool, you ask for a group like "web" tools. Tool groups are often backed by MCP servers.

- Configure in `application.yml` by selecting tools by name
- Configure in Spring `@Configuration` with `@Bean` returning `ToolGroup`
- Each tool group has metadata: name, provider, description, permissions

### Tool Call Context

`ToolCallContext` is an immutable key-value bag that flows through the tool pipeline without the LLM seeing it. Similar to HTTP headers on a request.

- Injected into `@LlmTool` methods automatically
- Set at process boundary via `ProcessOptions.withToolCallContext()`
- Can also be set per-interaction via `PromptRunner.withToolCallContext()`
- For MCP servers, use `ToolCallContextMcpMetaConverter` to control what crosses the boundary

### One-Shot Per Loop Tools

Some tools should fire at most once per agentic loop iteration (e.g., skill activators). Wrap with `OneShotPerLoopTool` to mechanically enforce this constraint.

## Subagents

A Subagent is a specialized Tool that delegates to another Embabel agent. When the LLM invokes this tool, it runs the specified agent as a subprocess, sharing the parent process's blackboard context.

### Creating Subagents

```java
Subagent.ofClass(PerformanceFinder.class).consuming(WorksToFind.class)
```

The `consuming()` method specifies the input type that the LLM will provide when invoking this tool. This type generates the JSON schema that guides the LLM's tool invocation.

### Using Subagents

Add to `PromptRunner` with `withTool()`:

```java
context.ai().withDefaultLlm()
    .withTool(Subagent.ofClass(MyAgent.class).consuming(MyInput.class))
    .createObject(String.class, model);
```

### Blackboard Sharing

When a Subagent runs, it receives a spawned blackboard from the parent process:
- The subagent can read objects from the parent's blackboard
- Objects added by the subagent are available to the parent after completion
- The subagent operates in its own process context

### When to Use Subagent vs Regular Tool

| Scenario | Recommendation |
|----------|----------------|
| Complex specialized task with multi-action workflow | Use Subagent — the target agent can plan and execute multiple steps |
| Simple tool call with deterministic logic | Use a regular `@LlmTool` method |
| LLM-orchestrated mini-workflow with sub-tools | Consider AgenticTool |
| Full GOAP planning for the subtask | Subagent is ideal |

## Agentic Tools

An agentic tool is a tool that uses an LLM to orchestrate other tools. Unlike a regular tool which executes deterministic logic, an agentic tool delegates to an LLM that decides which sub-tools to call.

Three implementations:
- **SimpleAgenticTool** — All tools available immediately
- **PlaybookTool** — Progressive unlock via conditions (prerequisites, artifacts, blackboard)
- **StateMachineTool** — State-based availability using enum states

## Key Patterns

- **Tool decoration** — `DelegatingTool` interface allows wrapping tools to add behavior (artifact capture, event publishing, blackboard integration)
- **ArtifactSinkingTool** — Captures artifacts from `Tool.Result.WithArtifact` results and sends to a sink (blackboard, list, composite)
- **Curried tool filtering** — When an action's inputs are already on the blackboard, those parameters are "curried out", simplifying the tool signature for the LLM
