---
name: embabel-agent
version: 1.5.3
description: >-
  Build agentic AI on the JVM with Embabel v1.5.3 — Spring-based framework combining LLMs with non-LLM planning
  (GOAP, Utility AI, Hybrid, Supervisor). Use when creating agents with @Agent, @Action, @AchievesGoal, @Condition,
  @LlmTool, @Tool; publishing agents as MCP servers with @Export(remote=true) over Streamable HTTP; agent process
  persistence (AgentProcessSnapshotStore, JDBC, cache-backed, HITL); state with @State; providers (OpenAI, Anthropic,
  Gemini, DeepSeek, Ollama, Bedrock, OCI, Mistral, Z.ai, DashScope, MiniMax, Atlas Cloud, BYOK) and per-provider roles
  (embabel.models.roles, RoleResolver, role-named default-llm, embedding-roles); RAG with ToolishRAG; testing with
  FakeOperationContext; chatbots with triggers and durable assets; interceptors, guardrails, cost tracking, streaming
  (StringResult); custom LLMs (LlmMessageSender, EmbeddingService); migrating from CrewAI, Pydantic AI, or LangGraph.
  Do NOT use for Chatbot/ChatSession/conversation-store patterns (see the embabel-chatbot skill).
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 1.5.3
  tags: [embabel, agents, jvm, spring, kotlin]
---

# Embabel Agent Framework (v1.5.3)

Build agentic AI on the JVM with **Embabel** — a Spring-based framework for authoring agentic flows that mix LLM-prompted interactions with code and domain models. Created by Rod Johnson (creator of Spring), it uses non-LLM AI planning (GOAP, Utility AI, Hybrid, Supervisor) to find intelligent paths toward goals.

## Purpose

This skill is the authoritative, version-pinned guide for writing correct Embabel code. Use it to author agents, tools, and domain models; wire providers and roles; publish MCP servers; build chatbots and RAG pipelines; and test and deploy — always grounded in the real v1.5.3 API rather than guesswork.

## Prerequisites

- **JDK 17+** and **Maven** or **Gradle** (the template projects use `./mvnw` / `./gradlew`)
- **Spring Boot 4.1.1** — pulled in by the `embabel-agent-starter-*` artifacts
- **At least one LLM provider key** — a deployment key (`OPENAI_API_KEY`, etc.) or BYOK keys supplied at runtime (see "Bring-Your-Own-Key" in `references/providers.md`)
- **`uvx`** (and `git`) only if you use the `project-creator.sh` scaffolding script

## Instructions

1. Confirm the task belongs on Embabel at all — check "When NOT to Use Embabel" first (plain Spring AI apps, non-planner chatbots, observability config → other skills).
2. Identify the framework version you're writing against (this guide targets v1.5.3); "New in v1.5.3" in `references/providers.md` lists what changed recently and "Migration" covers moves from CrewAI/Pydantic AI/LangGraph.
3. For a new agent, follow "Getting Started" → "Agent Authoring" → "Tools"; consult "Domain Objects" for state modeling, "Planning Algorithms"/"Execution Modes"/"States with @State" for multi-step flows, and "DSL Builders (Kotlin/Java)" when the annotations-only style doesn't fit.
4. For specific APIs — chatbots, RAG, MCP publishing, testing, threading/async, error handling — use the correspondingly named section; deeper material lives in "Deep Dives" and `references/`.
5. Cross-check "Common Pitfalls" and "Troubleshooting" before finalizing code, and never assert an API from memory — if a symbol isn't named here or in `references/`, treat it as unverified.

## Output Quality

- **Be comprehensive** — Full classes with imports, domain models, all annotations
- **Explain design decisions** — Justify planner choice, model selection, temperature, execution mode, and concurrency strategy
- **Include configuration** — Always show full `application.yml` with relevant `embabel:` and `spring.ai:` settings
- **Provide testing** — Unit tests with `FakePromptRunner`/`FakeOperationContext` AND integration tests with `EmbelMockitoIntegrationTest`
- **Use latest API patterns** — `LlmOptions.fromCriteria(ModelSelectionCriteria.getAuto())`, `context.ai().withLlm().creating().fromPrompt()`, `.withId()`, `withExample()`
- **Use few-shot examples** — `.withExample()` on LLM calls improves output quality for structured tasks

## Core Concepts

- **Actions** (`@Action`) — Steps the agent takes. Each method transforms inputs into outputs.
- **Goals** (`@AchievesGoal`) — What the agent is trying to achieve. Marks goal-satisfaction.
- **Conditions** (`@Condition`) — Predicates evaluated before actions or to determine goal achievement.
- **Domain Model** — Strongly-typed objects carrying both data and behavior (DICE).
- **Blackboard** — Shared memory where actions add results and read inputs by type.
- **Plan** — Dynamically generated sequence of actions. The planner recomputes after each action.
- **Planner** — GOAP (A* search), Utility AI (value-based), Hybrid, or Supervisor (LLM-orchestrated).

> **Key insight:** Most pre/post conditions are inferred from data flow in method signatures — you rarely deal with the blackboard directly.

## Getting Started

Add the platform starter (`embabel-agent-starter`, plus `-shell` for a CLI or
`-mcpserver` for an MCP server) and the starter for each model provider you call.
Release binaries are on **Maven Central**; no snapshot repository is needed.

The full starter/provider matrix (artifact and required key or config per provider,
plus the v1.5.2/v1.5.3 change list) lives in `references/providers.md` — read it before
adding a dependency or asserting that something is new in v1.5.3.

## Agent Authoring

### @Agent — Direct Agent

```java
@Agent(description = "Find news based on a person's star sign")
public class StarNewsFinder {
    @Action
    public StarPerson extractStarPerson(UserInput userInput, OperationContext context) {
        return context.ai()
            .withLlm(OpenAiModels.GPT_41)
            .createObject("Create a person from this input...", StarPerson.class);
    }

    @AchievesGoal
    @Action
    public Writeup writeup(StarPerson person, RelevantNewsStories stories, OperationContext context) {
        return context.ai().withLlm(llm).createObject(prompt, Writeup.class);
    }
}
```

### @EmbabelComponent — Action Container

Use when you want actions but not a full agent. Useful with Utility AI planner.

### @Export — MCP Publishing

Annotate with `@Export(remote = true)` on goals to auto-publish them as MCP tools.

> **@Action key attributes:** `pre`/`post` conditions, `canRerun`, `readOnly`, `clearBlackboard`, `cost`/`value`, `outputBinding`, `trigger`. See `references/annotations.md` for full details.

## Domain Objects

Domain objects carry both data and behavior — they are not anemic DTOs. Expose methods to LLMs with `@Tool` (Spring AI) or `@LlmTool` (Embabel). Unannotated methods are **never** exposed.

See `references/domain.md` for DICE best practices.

## Tools

- **@LlmTool** — Expose JVM methods to LLMs (Embabel); **@Tool** — same via Spring AI
- **ToolCallContext** — Inject infrastructure metadata invisible to the LLM
- **OneShotPerLoopTool** — Prevent repeated tool calls in a single planning loop
- **@Cost / costMethod / valueMethod** — Compute action costs and values dynamically from blackboard state
- **SpEL Conditions** — Dynamic preconditions in `@Condition` and `@Action(pre = "...")`
- **Subagent** — Let the LLM invoke other agents as tools (`Subagent.ofClass(...).consuming(...)`)
- **UtilityInvocation** — Lightweight utility workflows without `@Agent` (see `references/planners.md`)

See `references/tools.md` for full details.

> **IDE:** The Embabel Agent IntelliJ plugin (ID `31142`) suppresses false "never used" warnings on `@Action`/`@Condition`/`@Cost` methods. See `references/tooling.md`.

## Planning Algorithms

| Planner | Best For | Determinism |
|---------|----------|-------------|
| **GOAP** (default) | Business processes with defined outputs | High |
| **Utility** | Exploration, event-driven systems | Medium |
| **Hybrid** | Reducer pipelines | Medium-High |
| **Supervisor** | Flexible multi-step workflows | Low |

Set via `@Agent(planner = PlannerType.XXX)`. Choosing: Well-defined goals → GOAP. Deterministic → GOAP or Hybrid. Event-driven → Utility. LLM orchestration → Supervisor.

See `references/planners.md` for detailed comparison.

## States with @State

Annotate classes with `@State` to trigger state transitions. Previous state objects are hidden, the new state is bound to the blackboard, and planning considers only actions from the new state.

- For looping states, use `@Action(clearBlackboard = true)`
- For human-in-the-loop: `WaitFor.formSubmission("...", Feedback.class)`
- Use Java records or Kotlin top-level classes for state types

See `references/states.md` for full patterns: staying in-state, inheritance, WaitFor, parent state interface.

## DSL Builders (Kotlin/Java)

Use DSL builders (`SimpleAgentBuilder`, `ScatterGatherBuilder`, `ConsensusBuilder`, `RepeatUntil`, `RepeatUntilAcceptable`) for atomic workflows with multiple steps. Register with Spring via `@Bean` method returning `Agent`.

See `references/dsl-builders.md` for all builder types.

## Execution Modes

- **SIMPLE** (default): Sequential, one action at a time — predictable, easy to debug
- **CONCURRENT**: All achievable actions run in parallel via virtual threads — higher throughput, actions must be concurrent-safe

Set: `embabel.agent.platform.process-type: CONCURRENT`. Autonomy: Closed (LLM picks one agent) vs Open (LLM picks goal, assembles from all actions).

See `references/invoking.md` for ConcurrentAgentProcess behavior, confidence thresholds, ProcessOptions, and programmatic invocation.

## Chatbots

Chatbots use a **long-lived `AgentProcess`** that pauses between user messages. The blackboard maintains state across the session.

```java
@EmbabelComponent
public class MyChatbot {
    @Action(trigger = UserMessage.class)
    public AssistantResponse handleMessage(UserMessage msg, Ai ai) {
        return ai.withDefaultLlm()
            .creating(AssistantResponse.class)
            .fromPrompt(msg.getContent());
    }
}
```

The `trigger = UserMessage.class` fires only when a user message is the most recently added blackboard value.

### Utility AI for Chatbots

Utility AI is often the best approach for chatbots — define multiple actions with costs, and the planner picks the highest-value:

```java
@EmbabelComponent
public class ChatbotActions {
    @Action(cost = 0.1)
    public DirectResponse directAnswer(UserMessage msg, Ai ai) { ... }

    @Action(cost = 0.3)
    public RagResponse ragAnswer(UserMessage msg, Ai ai, Blackboard bb) { ... }
}
```

For dynamic costs (`@Cost`/`costMethod`), context IDs (pre-populate the session blackboard from a named context for stateful conversations across restarts), and conversation storage (`embabel.agent.platform.conversation-store: STORED`), see `references/chatbots.md`.

### Goals in Chatbots

Chatbot agents typically **need no goal** — the process waits indefinitely; define one only for transactional conversations (e.g., completing a booking). See `references/chatbots.md` for full patterns.

## RAG (Retrieval-Augmented Generation)

Embabel's RAG is **entirely agentic and tool-based** — the LLM controls retrieval:

- **Autonomous Search**: LLM decides when to search, what queries to use
- **Iterative Refinement**: Multiple searches with different queries
- **Cross-Reference Discovery**: Expand chunks, follow references, zoom out
- **HyDE Support**: Generate hypothetical documents for better semantic search

The `ToolishRag` facade auto-discovers store capabilities (vector, text, regex, result expansion) and exposes them as LLM tools via `LlmReference`.

See `references/rag.md` for the full RAG architecture and wiring.

## MCP Server Publishing

Publish agents as MCP servers with SYNC or ASYNC mode, over **Streamable HTTP transport** (default since v1.5.3, at `http://localhost:8080/mcp`):

```yaml
spring:
  ai:
    mcp:
      server:
        type: SYNC  # or ASYNC
        protocol: STREAMABLE  # default; STATELESS also supported
```

Goals are automatically published as MCP tools when annotated with `@Export(remote = true)`.

Two-layer security:
- **Layer 1:** HTTP filter chain (JWT auth via `SecurityWebFilterChain`)
- **Layer 2:** `@SecureAgentTool(expression = "hasAuthority('news:read')")` on agents/methods

SSE transport (`protocol: SSE`, endpoint `/sse`) is still accepted but deprecated for removal in Spring AI 2.0 — migrate to `protocol: STREAMABLE`.

See `references/integrations.md` for MCP server/client, security, observability, and A2A.

## Testing

- **Unit tests:** Use `FakeOperationContext.create()` and `context.expectResponse()`
- **Integration tests:** Extend `EmbabelMockitoIntegrationTest`, use `whenCreateObject()` and `verifyCreateObjectMatching()`
- **Always use `.withId("...")`** on LLM calls for traceability

See `references/testing.md` for more patterns.

## Deep Dives

For topics not covered in detail above, consult the reference files:

| Topic | Reference |
|-------|-----------|
| LLM options, caching, native structured output, custom LLM/EmbeddingService | `references/llm-integration.md` |
| AgentProcess lifecycle, blackboard, planning loop | `references/flow.md` |
| Core types (`LlmOptions`, `PromptRunner`, `AgentImage`) | `references/types.md` |
| Structured prompts (`Persona`, `RoleGoalBackstory`, LlmReference providers) | `references/structured-prompts.md` |
| Interceptors & transformers | `references/interceptors.md` |
| Thinking, guardrails, cost tracking, streaming | `references/thinking.md`, `references/guardrails.md`, `references/cost-tracking.md`, `references/streaming.md` |
| Termination, error handling | `references/termination.md`, `references/error-handling.md` |
| Agent skills, API vs SPI | `references/agent-skills.md`, `references/api-spi.md` |
| Agent process persistence (HITL checkpoint/restore, JDBC) | `references/persistence.md` |
| Cache providers (AgentCacheProvider, regions, capabilities) | `references/cache.md` |
| DashScope provider (Alibaba Qwen) | `references/dashscope.md` |
| Z.ai provider (Zhipu GLM, native client) | `references/zai.md` |
| Tooling (IntelliJ IDEA plugin) | `references/tooling.md` |
| Production deployment (Docker, K8s, health, security) | `references/production-deployment.md` |
| Starter artifacts, provider keys, v1.5.2/v1.5.3 change list | `references/providers.md` |

> **Rule:** Application code uses only `com.embabel.agent.api.*`. SPI (`com.embabel.agent.spi.*`) is for framework extension only and is subject to change.

## Configuration

Properties live under `embabel:` (models, planner, execution mode, logging, tool loop). See `references/configuration.md` for the full property reference.

## Threading & Async Mode

Enable virtual threads: `spring.threads.virtual.enabled=true`. Flip the threading model with `threading.override` or share the app's executor with `threading.shared`.

See `references/async-mode.md` for the full behavior matrix.

## Error Handling

- **Retry:** `createObject()` retries automatically; catch `LlmInvocationException` for custom fallback
- **Non-critical:** Use `createObjectIfPossible()` — returns null instead of throwing
- **Guardrails:** Catch `GuardRailViolationException` for `CRITICAL` blocks
- **Cost caps:** Use `EarlyTerminationPolicy` for process-level termination

See `references/error-handling.md` and `references/flow.md` for full patterns.

## Troubleshooting

| Issue | Quick Fix |
|-------|-----------|
| Agent not discovered | Check `@Agent`/`@EmbabelComponent` annotation and component scan path |
| Planner can't find plan | Verify `@AchievesGoal`, input type matching, enable DEBUG logging |
| LLM calls failing | Check API keys, model names, timeouts, network connectivity |
| High costs | Limit `max-iterations`, use cheaper models, enable `BudgetGuardRail` |

See `references/troubleshooting.md` for detailed steps.

## Migration

Migrating from Python AI frameworks? See `references/migrating.md` for guidance on migrating from CrewAI, Pydantic AI, and LangGraph.

## Examples

**"Two-step agent: fetch an order, then write the refund note."** "Agent Authoring" -> two `@Action`s with `@State` handoff, GOAP planner unless the domain grows; "Testing" -> the `FakeOperationContext` skeleton before wiring real LLM calls.

**"Expose our invoicing agent to Claude Code."** "MCP Server Publishing" (`@Export(remote = true)`) — not a bespoke socket server.

**"Migrate this LangGraph app."** "Migration"; for a fixed, deterministic flow check "When NOT to Use Embabel" first — a planner earns nothing where the path is already known.

## Common Pitfalls

Top issues — see `references/common-pitfalls.md` for detailed Problem/Impact/Fix treatment.

1. **Missing `@AchievesGoal`** — Planner can't determine completion (it's `@AchievesGoal`, not `@Goal`)
2. **Missing `clearBlackboard = true` for loops** — Planner skips existing types
3. **Non-static inner classes for `@State`** — Serialization/persistence failures
4. **Missing `OperationContext` parameter** — Actions can't access AI or blackboard
5. **Forgetting `.withId()`** — Makes testing and debugging opaque

## Available Scripts

| Script | Purpose | Arguments |
|--------|---------|-----------|
| `scripts/project-creator.sh` | Scaffold a new Embabel agent project from the official Java/Kotlin template repositories (via the `embabel/project-creator` tool) | `--lang java\|kotlin --name <project-name> [--package <package>]` |

Run it with `run_script("scripts/project-creator.sh", "--lang kotlin --name my-agent --package com.acme.research")`, or directly in a shell:

```bash
bash scripts/project-creator.sh --lang java --name my-agent
```

Requires `uvx` and `git`. The template repositories:
- [Java Agent Template](https://github.com/embabel/java-agent-template)
- [Kotlin Agent Template](https://github.com/embabel/kotlin-agent-template)

## When NOT to Use Embabel

| Scenario | Better Alternative | Why |
|----------|-------------------|-----|
| Simple API endpoints | Spring Boot alone | Embabel adds overhead |
| Pure data processing | Spring Batch, Apache Spark | No LLM interaction needed |
| Real-time streaming | WebFlux, gRPC | Embabel is agent-focused |
| Frontend web apps | React, Angular, Vue | Embabel is backend-only |
| Simple chatbot with fixed flow | State machine library | No need for LLM-driven planning |
| High-throughput microservices | Standard Spring Boot | Planning loop adds latency |
| Batch ETL jobs | Spring Batch, Airflow | No agent planning needed |

## Limitations

- **Version-pinned** — this guide targets v1.5.3; APIs in other releases may differ, so verify against the pinned upstream docs before porting code
- **Process persistence** — the snapshot factory supports `InMemoryBlackboard` and assumes at most one pending awaitable per `WAITING` process; writes are not transactional; renaming an agent class requires a snapshot migration (`references/persistence.md`)
- **MCP SSE transport** — deprecated for removal in a future Spring AI release; new deployments should use `protocol: STREAMABLE`
- **Cache-backed persistence** — the Spring `CacheManager` adapter has no atomic compare-and-set, so it is single-node only unless a backend-native `AgentCacheProvider` is used (`references/cache.md`)
