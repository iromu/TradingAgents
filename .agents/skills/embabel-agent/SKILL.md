---
name: embabel-agent
description: >-
  Build agentic AI on the JVM with Embabel v1.0.0 — Rod Johnson's Spring-based framework for authoring agents that combine LLMs with non-LLM planning (GOAP, Utility AI, Hybrid, Supervisor). Use when creating agents with @Agent or @EmbabelComponent, annotating actions (@Action), goals (@Goal), conditions (@Condition), and tools (@LlmTool, @Tool); publishing agents as MCP servers with @Export(remote=true); managing agent state with @State and transitions; configuring LLM providers (OpenAI, Anthropic, Gemini, GenAI, DeepSeek, Ollama, LM Studio, Bedrock, OCI, Mistral, Z.ai, Docker Models, MiniMax); implementing RAG with ToolishRAG; writing tests with FakeOperationContext; adding interceptors, guardrails, cost tracking, streaming, thinking, and termination; building chatbots with triggers; using DSL builders, OneShotPerLoopTool, ToolCallContext; troubleshooting and migrating from CrewAI, Pydantic AI, or LangGraph.
---

# Embabel Agent Framework (v1.0.0)

Build agentic AI on the JVM with **Embabel** — a Spring-based framework for authoring agentic flows that mix LLM-prompted interactions with code and domain models. Created by Rod Johnson (creator of Spring), it uses non-LLM AI planning (GOAP, Utility AI, Hybrid, Supervisor) to find intelligent paths toward goals.

## Output Quality

- **Be comprehensive** — Full classes with imports, domain models, all annotations
- **Explain design decisions** — Justify planner choice, model selection, temperature, execution mode, and concurrency strategy
- **Include configuration** — Always show full `application.yml` with relevant `embabel:` and `spring.ai:` settings
- **Provide testing** — Unit tests with `FakePromptRunner`/`FakeOperationContext` AND integration tests with `EmbelMockitoIntegrationTest`
- **Use latest API patterns** — `LlmOptions.fromCriteria(ModelSelectionCriteria.getAuto())`, `context.ai().withLlm().creating().fromPrompt()`, `.withId()`, `withExample()`
- **Use few-shot examples** — `.withExample()` on LLM calls improves output quality for structured tasks

## Core Concepts

- **Actions** (`@Action`) — Steps the agent takes. Each method transforms inputs into outputs.
- **Goals** (`@Goal`) — What the agent is trying to achieve. Marks goal-satisfaction.
- **Conditions** (`@Condition`) — Predicates evaluated before actions or to determine goal achievement.
- **Domain Model** — Strongly-typed objects carrying both data and behavior (DICE).
- **Blackboard** — Shared memory where actions add results and read inputs by type.
- **Plan** — Dynamically generated sequence of actions. The planner recomputes after each action.
- **Planner** — GOAP (A* search), Utility AI (value-based), Hybrid, or Supervisor (LLM-orchestrated).

> **Key insight:** Most pre/post conditions are inferred from data flow in method signatures — you rarely deal with the blackboard directly.

## Getting Started

### Setup (Maven/Gradle)

Add the appropriate starter:

| Starter | Use Case |
|---------|----------|
| `embabel-agent-starter` | Basic agent platform (web/console/microservice) |
| `embabel-agent-starter-shell` | Interactive CLI shell |
| `embabel-agent-starter-mcpserver` | MCP server (SSE, Streamable-HTTP) |

Embabel release binaries are published to **Maven Central** — no snapshot repository needed for stable releases.

### LLM Providers

| Provider | Starter | Key Env Var |
|----------|---------|-------------|
| OpenAI | `embabel-agent-starter-openai` | `OPENAI_API_KEY` |
| OpenAI Custom (Groq, Z.AI, OpenRouter) | `embabel-agent-starter-openai-custom` | `OPENAI_CUSTOM_API_KEY` |
| Anthropic | `embabel-agent-starter-anthropic` | `ANTHROPIC_API_KEY` |
| Google Gemini (OpenAI-compatible) | `embabel-agent-starter-gemini` | `GEMINI_API_KEY` |
| Google GenAI (Native, Gemini 3.x) | `embabel-agent-starter-google-genai` | `GOOGLE_API_KEY` |
| DeepSeek | `embabel-agent-starter-deepseek` | `DEEPSEEK_API_KEY` |
| OCI Generative AI | `embabel-agent-starter-oci-genai` | `~/.oci/config` |
| Mistral AI | `embabel-agent-starter-mistral-ai` | `MISTRAL_API_KEY` |
| LM Studio | `embabel-agent-starter-lmstudio` | _(none)_ |
| Ollama | `embabel-agent-starter-ollama` | _(none)_ |
| AWS Bedrock | `embabel-agent-starter-bedrock` | AWS credentials (standard Spring AI Bedrock) |
| Z.ai (Zhipu GLM) | `embabel-agent-starter-zai` | `ZAI_API_KEY` |
| Docker Models | `embabel-agent-starter-dockermodels` | _(none)_ |
| MiniMax | `embabel-agent-starter-minimax` | `MINIMAX_API_KEY` |

See `reference/configuration.md` for full provider config details.

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

    @Goal
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

> **@Action key attributes:** `pre`/`post` conditions, `canRerun`, `readOnly`, `clearBlackboard`, `cost`/`value`, `outputBinding`, `trigger`. See `reference/annotations.md` for full details.

## Domain Objects

Domain objects carry both data and behavior — they are not anemic DTOs. Expose methods to LLMs with `@Tool` (Spring AI) or `@LlmTool` (Embabel). Unannotated methods are **never** exposed.

See `reference/domain.md` for DICE best practices.

## Tools

- **@LlmTool** — Expose JVM methods to LLMs (Embabel)
- **@Tool** — Expose JVM methods to LLMs (Spring AI)
- **ToolCallContext** — Inject infrastructure metadata invisible to the LLM
- **OneShotPerLoopTool** — Prevent repeated tool calls in a single planning loop
- **@Cost / costMethod** — Compute action costs dynamically from blackboard state
- **SpEL Conditions** — Dynamic preconditions in `@Condition` and `@Action(pre = "...")`
- **Subagent** — Let the LLM invoke other agents as tools (`Subagent.ofClass(...).consuming(...)`)
- **Agentic Tools** — SimpleAgenticTool, PlaybookTool, StateMachineTool
- **Tool Groups & Chaining** — Configure in YAML or `@Configuration`; use `withToolGroup()` and `withToolChainingFrom(Class)`

See `reference/tools.md` for full details.

## Planning Algorithms

| Planner | Best For | Determinism |
|---------|----------|-------------|
| **GOAP** (default) | Business processes with defined outputs | High |
| **Utility** | Exploration, event-driven systems | Medium |
| **Hybrid** | Reducer pipelines | Medium-High |
| **Supervisor** | Flexible multi-step workflows | Low |

Set via `@Agent(planner = PlannerType.XXX)`. Choosing: Well-defined goals → GOAP. Deterministic → GOAP or Hybrid. Event-driven → Utility. LLM orchestration → Supervisor.

See `reference/planners.md` for detailed comparison.

## States with @State

Annotate classes with `@State` to trigger state transitions. Previous state objects are hidden, the new state is bound to the blackboard, and planning considers only actions from the new state.

- For looping states, use `@Action(clearBlackboard = true)`
- For staying in the same state, return `this` (requires `canRerun = true`)
- For human-in-the-loop: `WaitFor.formSubmission("...", Feedback.class)`
- Use Java records or Kotlin top-level classes for state types
- `@State` annotation is inherited through class hierarchy

See `reference/states.md` for detailed state patterns, inheritance, WaitFor, and parent state interface.

## DSL Builders (Kotlin/Java)

Use DSL builders (`SimpleAgentBuilder`, `ScatterGatherBuilder`, `ConsensusBuilder`, `RepeatUntil`, `RepeatUntilAcceptable`) for atomic workflows with multiple steps. Register with Spring via `@Bean` method returning `Agent`.

See `reference/dsl.md` for all builder types.

## Execution Modes

- **SIMPLE** (default): Sequential, one action at a time
- **CONCURRENT**: All achievable actions run in parallel

Set: `embabel.agent.platform.process-type: CONCURRENT`

**Autonomy:** Closed (LLM picks one agent) vs Open (LLM picks goal, assembles from all actions).

See `reference/invoking.md` for confidence thresholds and programmatic invocation.

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

    @Action(cost = 0.05)
    public ClarificationRequest needsClarification(UserMessage msg) { ... }
}
```

### Dynamic Costs with @Cost

```java
@Cost(name = "ragCost")
public double computeRagCost(@Nullable KnowledgeBase kb) {
    return kb != null ? 0.3 : 0.9;
}

@Action(costMethod = "ragCost")
public RagResponse ragAnswer(UserMessage msg, Ai ai) { ... }
```

### Context IDs and Session State

```java
ChatSession session = chatbot.createSession(user, outputChannel, "project-alpha", null);
```

The `contextId` pre-populates the session's blackboard with objects from a named context, enabling stateful conversations across restarts.

### Goals in Chatbots

Typically, chatbot agents **do not need a goal** — the process waits indefinitely. Define a goal only for transactional conversations (e.g., completing a booking).

- **Conversation Storage:** `embabel.agent.platform.conversation-store: STORED` (default: IN_MEMORY)
- **Context IDs:** Pre-populate session blackboard with objects from a named context

See `reference/chatbots.md` for full chatbot patterns.

## RAG (Retrieval-Augmented Generation)

Embabel's RAG is **entirely agentic and tool-based** — the LLM controls retrieval:

- **Autonomous Search**: LLM decides when to search, what queries to use
- **Iterative Refinement**: Multiple searches with different queries
- **Cross-Reference Discovery**: Expand chunks, follow references, zoom out
- **HyDE Support**: Generate hypothetical documents for better semantic search

`ToolishRag` facade auto-discovers store capabilities (vector, text, regex, result expansion) and exposes them as LLM tools via `LlmReference`:

```java
LlmReference ragRef = LlmReference.builder()
    .description("Search the knowledge base")
    .rag(toolishRag)
    .build();
```

See `reference/rag.md` for full RAG architecture.

## MCP Server Publishing

Publish agents as MCP servers with SYNC or ASYNC mode (SSE transport):

```yaml
spring:
  ai:
    mcp:
      server:
        type: SYNC  # or ASYNC
```

Goals are automatically published as MCP tools when annotated with `@Export(remote = true)`.

Two-layer security:
- **Layer 1:** HTTP filter chain (JWT auth via `SecurityWebFilterChain`)
- **Layer 2:** `@SecureAgentTool(expression = "hasAuthority('news:read')")` on agents/methods

For clients requiring Streamable HTTP, use the `mcpo` proxy to bridge SSE:
```bash
uvx mcpo --port 8000 --server-type sse -- http://localhost:8080/sse
```

See `reference/integrations.md` for MCP server/client, security, observability, and A2A.

## Testing

- **Unit tests:** Use `FakeOperationContext.create()` and `context.expectResponse()`
- **Integration tests:** Extend `EmbelMockitoIntegrationTest`, use `whenCreateObject()` and `verifyCreateObjectMatching()`
- **Always use `.withId("...")`** on LLM calls for traceability

See `reference/testing.md` for more patterns.

## Deep Dives

For topics not covered in detail above, consult the reference files:

| Topic | Reference |
|-------|-----------|
| LLM options, caching, native structured output | `reference/llm-integration.md` |
| AgentProcess lifecycle, blackboard, planning loop | `reference/flow.md` |
| Core types (`LlmOptions`, `PromptRunner`, `AgentImage`) | `reference/types.md` |
| Structured prompts (`Persona`, `RoleGoalBackstory`) | `reference/structured-prompts.md` |
| Interceptors & transformers | `reference/interceptors.md` |
| Thinking, guardrails, cost tracking, streaming | `reference/thinking.md`, `reference/guardrails.md`, `reference/cost-tracking.md`, `reference/streaming.md` |
| Termination, error handling | `reference/termination.md`, `reference/error-handling.md` |
| Agent skills, API vs SPI | `reference/agent-skills.md`, `reference/api-spi.md` |

> **Rule:** Application code uses only `com.embabel.agent.api.*`. SPI (`com.embabel.agent.spi.*`) is for framework extension only and is subject to change.

## Configuration

Always include the full `embabel:` block with models, planner settings, execution mode, logging, and tool configuration.

See `reference/configuration.md` for full property reference.

## Threading & Async Mode

Enable virtual threads: `spring.threads.virtual.enabled=true`.

| Property | Default | Description |
|----------|---------|-------------|
| `threading.override` | `false` | Flip threading model (virtual↔platform) |
| `threading.shared` | `false` | Share app's executor when models match |

See `reference/async-mode.md` for full behavior matrix.

## Error Handling

- **Retry:** `createObject()` retries automatically; catch `LlmInvocationException` for custom fallback
- **Non-critical:** Use `createObjectIfPossible()` — returns null instead of throwing
- **Guardrails:** Catch `GuardRailViolationException` for `CRITICAL` blocks
- **Cost caps:** Use `EarlyTerminationPolicy` for process-level termination
- **Validation:** Catch `InvalidLlmReturnTypeException` when JSR-380 validation fails after retry
- **Empty responses:** Configure `toolloop.empty-response.max-retries` for weak models (see `reference/llm-integration.md`)

See `reference/error-handling.md` and `reference/flow.md` for full patterns.

## Troubleshooting

| Issue | Quick Fix |
|-------|-----------|
| Agent not discovered | Check `@Agent`/`@EmbabelComponent` annotation and component scan path |
| Planner can't find plan | Verify `@Goal`, input type matching, enable DEBUG logging |
| Blackboard type not found | Check previous action added the type, not hidden by state transition |
| State transitions broken | Use `@State` annotation, Java records or Kotlin top-level classes |
| LLM calls failing | Check API keys, model names, timeouts, network connectivity |
| High costs | Limit `max-iterations`, use cheaper models, enable `BudgetGuardRail` |

See `reference/troubleshooting.md` for detailed steps.

## Migration

Migrating from Python AI frameworks? See `reference/migrating.md` for guidance on migrating from CrewAI, Pydantic AI, and LangGraph.

## Common Pitfalls

A quick checklist of the most frequent issues. See `reference/common-pitfalls.md` for detailed Problem/Impact/Fix treatment.

1. **Missing `@Agent`/`@EmbabelComponent`** — Component won't be discovered by Spring
2. **Missing `@Goal`** — Planner can't determine completion (replaced `@AchievesGoal`)
3. **Missing `clearBlackboard = true` for loops** — Planner skips existing types
4. **Non-static inner classes for `@State`** — Serialization/persistence failures
5. **Missing `OperationContext` parameter** — Actions can't access AI or blackboard
6. **Not setting model per-action** — Wastes money or sacrifices quality
7. **Forgetting `.withId()`** — Makes testing and debugging opaque
8. **Using SPI in production** — `com.embabel.agent.spi.*` is subject to change
9. **Streaming with native structured output** — Not supported by Spring AI currently

## Scaffolding

Use the Embabel template repositories or the `project-creator` tool to scaffold new projects:

- [Java Agent Template](https://github.com/embabel/java-agent-template)
- [Kotlin Agent Template](https://github.com/embabel/kotlin-agent-template)

## Real-World Examples

Explore the [embabel-agent-examples](https://github.com/embabel/embabel-agent-examples) repository for production-quality examples:
- **FactChecker**: Multi-LLM fact-checking with ScatterGather, ConsensusBuilder, parallel execution, and MCP publishing
- **Horoscope**: Human-in-the-loop with WaitFor, cost-based planning, domain tools

## When to Use Embabel

| Scenario | Why Embabel Fits |
|----------|-----------------|
| LLM-driven decision making | Planner picks best action sequence dynamically |
| Domain-driven planning | DICE pattern — data and behavior in one type |
| Multi-step agentic workflows | GOAP/Utility/Hybrid/Supervisor planners |
| Human-in-the-loop workflows | `WaitFor.formSubmission()` with state management |
| Agent composition | `Subagent.ofClass()`, ScatterGather, ConsensusBuilder |
| MCP server publishing | `@Export(remote=true)` auto-publishes goals as tools |
| Chatbots with memory | Long-lived `AgentProcess` with blackboard state |
| Cost-aware planning | `@Cost`/`costMethod` lets planner pick cheapest path |

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

**Use Embabel when:** LLM-driven decision making, domain-driven planning, mixing LLM calls with code, human-in-the-loop workflows, agent composition.

**Avoid Embabel when:** Fully deterministic workflows, sub-millisecond response times, simple CRUD APIs, single LLM call agents.

## Production Deployment

Use multi-stage Dockerfiles and Kubernetes manifests for production. Always use secrets management for API keys and enable guardrails for safety/compliance.

See `reference/production-deployment.md` for Docker, K8s, health checks, security, and production checklist.
