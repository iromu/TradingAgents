---
name: embabel-agent
description: >-
  Build agentic AI applications on the JVM with Embabel — a Spring-based framework by Rod Johnson for creating agents that mix LLM interactions with code, domain models, and non-LLM planning algorithms (GOAP, Utility AI, Hybrid, Supervisor). Use this skill whenever the user asks about Embabel agent development, agent annotations, agentic tool design, planning configuration, testing, LLM provider setup, execution modes, autonomy modes, state-based workflows, human-in-the-loop patterns, agent skills, guardrails, cost tracking, structured prompts, streaming, DSL builders, domain objects, templates, project scaffolding, production deployment, error handling, troubleshooting, or common pitfalls. Also trigger when the user mentions Embabel, DICE framework, Rod Johnson's agent framework, JVM-based agentic flows, building agents with Spring Boot and LLMs, or MCP integration with Embabel.
---

# Embabel Agent Framework

Build agentic AI applications on the JVM using the **Embabel framework** — a Spring-based framework for authoring agentic flows that seamlessly mix LLM-prompted interactions with code and domain models. Created by Rod Johnson (creator of Spring), it supports intelligent path-finding towards goals using non-LLM AI planning algorithms (GOAP, Utility AI, Hybrid, Supervisor).

## Output Quality

When producing code or documentation:

- **Be comprehensive** — Provide complete, multi-section outputs with thorough explanations
- **Include complete code** — Full classes with imports, domain models, and all annotations
- **Show the "why"** — Explain design decisions (why this planner, why this temperature, why this execution mode)
- **Include configuration** — Always show the full `application.yml` block with all relevant settings
- **Provide testing** — Include unit tests with FakePromptRunner AND integration tests with EmbabelMockitoIntegrationTest
- **Use latest API patterns** — `LlmOptions.withModel()`, `context.ai().withLlm().creating().fromPrompt()`, `.withId()`, `withExample()`

## Core Concepts

Every Embabel agent is built from these building blocks:

- **Actions** (`@Action`) — Steps the agent takes. Each method transforms inputs into outputs.
- **Goals** (`@AchievesGoal`) — What the agent is trying to achieve. Marks goal-satisfaction.
- **Conditions** (`@Condition`) — Predicates evaluated before actions or to determine goal achievement.
- **Domain Model** — Strongly-typed objects carrying both data and behavior (DICE).
- **Blackboard** — Shared memory where actions add results and read inputs by type.
- **Plan** — Dynamically generated sequence of actions. The planner recomputes after each action.
- **Planner** — GOAP (A* search), Utility AI (value-based), Hybrid, or Supervisor (LLM-orchestrated).
- **Agent Skills** — Lazy-loaded, reusable skill packages.
- **Guardrails** — Configurable validation policies for user inputs and LLM responses.
- **Cost Tracking** — Real-time event-based tracking of LLM and embedding costs.

> **Key insight:** Application developers rarely interact with the blackboard directly. Most pre/post conditions are inferred from data flow in method signatures.

## Getting Started

### Setup (Maven)

Add the starter dependency and Embabel snapshot repository. See `reference/configuration.md` for the full Maven setup and all available starters (`embabel-agent-starter`, `embabel-agent-starter-shell`, `embabel-agent-starter-mcp`).

### LLM Providers

Add the provider-specific starter (e.g., `embabel-agent-starter-openai`) and set the corresponding API key environment variable. See `reference/configuration.md` for the full provider list and configuration.

### Environment Variables

Set `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, or `GEMINI_API_KEY` as needed.

## Agent Process Lifecycle

An `AgentProcess` manages the complete execution lifecycle: `NOT_STARTED` → `RUNNING` → `COMPLETED` / `FAILED` / `TERMINATED` / `KILLED` / `STUCK` / `WAITING` / `PAUSED`.

Use `tick()` for a single step, `run()` to execute as far as possible.

### Planning (OODA Loop)

After each action execution, the planner: **Observe** → **Orient** → **Decide** → **Act** (then replan).

### Blackboard

The blackboard is shared memory where actions add results and read inputs by type. See `reference/invocation.md` for full blackboard operations and Context persistence.

## Agent Authoring

### @Agent — Direct Agent

Annotate a class with `@Agent(description = "...")` and add `@Action` methods. The planner discovers actions by their input/output types and builds a plan.

### @Agentic — Auto-Discovered Agent

Annotate with `@Agentic(description = "...")` for auto-discovery via component scanning.

### @EmbabelComponent — Action Container

Annotate with `@EmbabelComponent` when you want actions but not a full agent.

> **@Action key attributes:** `pre`/`post` conditions, `canRerun`, `readOnly`, `clearBlackboard`, `cost`/`value`, `outputBinding`, `trigger`. See `reference/annotations.md` for full details and all annotation types.

## Domain Objects

Domain objects carry both data and behavior — they are not anemic DTOs. Expose methods to LLMs with `@Tool`. Unannotated methods are **never** exposed regardless of visibility.

See `reference/domain-objects.md` for DICE best practices and full @Tool rules.

## Tools

### @LlmTool: Expose JVM Methods to LLMs

Annotate methods with `@LlmTool(description = "...")` to make them callable by the LLM.

### ToolCallContext

Inject `ToolCallContext` to access infrastructure metadata (auth tokens, tenant IDs) invisible to the LLM.

### Subagent: Agent Handoffs

Use `Subagent.ofClass(...).consuming(...)` to let the LLM invoke other agents as tools.

### Agentic Tools

- **SimpleAgenticTool** — Flat tool orchestration
- **PlaybookTool** — Progressive tool unlocking with prerequisites
- **StateMachineTool** — State-based tool availability

### Tool Groups

Configure tool groups in YAML or `@Configuration` and use `withToolGroup()` in actions.

### Tool Chaining

Use `withToolChainingFrom(Class)` to dynamically expose tools from returned objects.

> See `reference/tools.md` for full details: tool groups config, framework-agnostic Tool interface, OneShotPerLoopTool, domain tools, and complete code examples.

## Planning Algorithms

| Planner | Best For | Determinism | Algorithm |
|---------|----------|-------------|-----------|
| **GOAP** (default) | Business processes with defined outputs | High | A* search over goal states |
| **Utility** | Exploration, event-driven systems | Medium | Highest net-value action picks |
| **Hybrid** | Reducer pipelines | Medium-High | Utility picking + goal termination |
| **Supervisor** | Flexible multi-step workflows | Low | LLM-orchestrated composition |

Set via `@Agent(planner = PlannerType.XXX)` or `ProcessOptions(plannerType = PlannerType.XXX)`.

**Choosing:** Well-defined goals → GOAP. Deterministic planning → GOAP or Hybrid. Event-driven → Utility. LLM orchestration → Supervisor.

> See `reference/planners.md` for detailed comparison and examples for each planner.

## States with @State

Annotate classes with `@State` to trigger state transitions. Previous state objects are hidden, the new state is bound to the blackboard, and planning considers only actions from the new state.

- For looping states, use `@Action(clearBlackboard = true)`
- For human-in-the-loop: `WaitFor.formSubmission("...", Feedback.class)`
- Use Java records (implicitly static) or Kotlin top-level classes for state types

> See `reference/states.md` for detailed state patterns, inheritance, WaitFor, and parent state interface.

## DSL Builders (Kotlin/Java)

For atomic workflows that contain multiple steps, use DSL builders: `SimpleAgentBuilder`, `ScatterGatherBuilder`, `ConsensusBuilder`, `RepeatUntil`, `RepeatUntilAcceptable`. Register with Spring via `@Bean` method returning `Agent`.

> See `reference/dsl.md` for all builder types, subprocess execution, and Spring bean registration.

## Execution Modes

### Process Execution (SIMPLE vs CONCURRENT)

- **SIMPLE** (default): Sequential, one action at a time
- **CONCURRENT**: All achievable actions run in parallel

Set: `embabel.agent.platform.process-type: CONCURRENT`

### Autonomy (Closed vs Open Mode)

- **Closed**: LLM picks one agent; agent runs in isolation
- **Open**: LLM picks goal; assembles agent from all actions

> See `reference/execution-modes.md` for confidence thresholds, goal approval, and REST endpoints.

## Invocation

Use `AgentInvocation.create(agentPlatform, ResultClass)` for type-safe invocation, or `agentPlatform.createAgentProcess()` for direct process control. See `reference/invocation.md` for Autonomy, GoalSelectionOptions, Shell usage, webhooks, and web app examples.

## LLM Integration

Use `LlmOptions.withModel()` for per-call model/temperature configuration. Inject `Ai` via constructor injection. Use `.withId("...")` for test verification.

> See `reference/llm-integration.md` for full details: LlmOptions, custom LLM providers, embedding services, callbacks, Anthropic caching, streaming, and the complete PromptRunner API.

## Structured Prompts

Use `Persona`, `RoleGoalBackstory`, or custom `PromptContributor` implementations to inject structured content into prompts. Use `LlmReference` when you need both content and tools from the same source.

> See `reference/structured-prompts.md` for full details and YML configuration.

## Streams

Enable streaming with `.streaming()` on the PromptRunner. Handles `Thinking` and `ObjectCreated` events via Spring Reactive callbacks.

> See `reference/streams.md` for raw text streaming and reactive callbacks.

## Agent Skills

Load skills from GitHub (`Skills.fromGitHub(...)`) or local directories (`Skills.fromLocal(...)`). Skills use lazy loading — metadata in system prompt, full content on `activate`.

> See `reference/agent-skills.md` for validation and combining with LlmReference.

## Guardrails

Add guardrails with `.withGuardRails(...)`. `CRITICAL` severity blocks execution and throws `GuardRailViolationException`.

> See `reference/guardrails.md` for custom guardrails, global config, and POJO guardrails.

## Cost Tracking

Listen for `LlmInvocationEvent` to track costs by process, tenant, or user. Combine with guardrails for budget management.

> See `reference/cost-tracking.md` for the full budget guardrail pattern and EarlyTerminationPolicy.

## Configuration

Always include the full `embabel:` block with models, planner settings, execution mode, logging, and tool configuration when producing examples.

> See `reference/configuration.md` for full property reference and provider-specific config.

## Testing

Always provide complete, runnable test classes:

- **Unit tests:** Use `FakeOperationContext.create()` and `context.expectResponse()`
- **Integration tests:** Extend `EmbelMockitoIntegrationTest`, use `whenCreateObject()` and `verifyCreateObjectMatching()`
- **Always use `.withId("...")`** on LLM calls for traceability

> See `reference/testing.md` for more patterns including Mockito stubbing and example verification.

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

## Error Handling

- **Retry:** `createObject()` retries automatically; catch `LlmInvocationException` for custom fallback
- **Non-critical:** Use `createObjectIfPossible()` — returns null instead of throwing
- **Guardrails:** Catch `GuardRailViolationException` for `CRITICAL` blocks
- **Cost caps:** Use `EarlyTerminationPolicy` for process-level termination
- **Monitoring:** Poll `process.getState()` for long-running agents

> See `reference/error-handling.md` for full error handling patterns and process states.

## Troubleshooting

| Issue | Quick Fix |
|-------|-----------|
| Agent not discovered | Check `@Agent`/`@Agentic` annotation and component scan path |
| Planner can't find plan | Verify `@AchievesGoal`, input type matching, enable DEBUG logging |
| Blackboard type not found | Check previous action added the type, not hidden by state transition |
| State transitions broken | Use `@State` annotation, Java records or Kotlin top-level classes |
| LLM calls failing | Check API keys, model names, timeouts, network connectivity |
| High costs | Limit `max-iterations`, use cheaper models, enable `BudgetGuardRail` |

> See `reference/troubleshooting.md` for detailed troubleshooting steps.

## Common Pitfalls

1. **Missing `@Agent`/`@Agentic`** — Agent won't be discovered
2. **Missing `OperationContext`** — Actions can't access AI or blackboard
3. **Confusing execution vs autonomy modes** — They operate at different levels
4. **Missing `@AchievesGoal`** — Planner can't determine completion
5. **Ignoring `max-iterations`** — Agent stops after 20 (default)
6. **Not setting model per-action** — Wastes money or sacrifices quality
7. **Forgetting `.withId()`** — Makes testing and debugging opaque
8. **Non-static inner classes for `@State`** — Serialization issues
9. **Missing `clearBlackboard = true` for loops** — Planner skips existing types
10. **Exposing sensitive methods** — Always gate with `@Tool`/`@LlmTool`
11. **Missing `@Export(remote=true)` on MCP** — Agents invisible to MCP clients
12. **Circular type dependencies** — Planner can't find valid plan path
13. **Custom conditions without `post`** — Other actions can't see them in `pre`

> See `reference/common-pitfalls.md` for detailed explanations and fixes.

## Production Deployment

Use multi-stage Dockerfiles and Kubernetes manifests for production. Always use secrets management for API keys and enable guardrails for safety/compliance.

> See `reference/production-deployment.md` for Docker, K8s, health checks, security, and production checklist.

## Scaffolding

Use the included script to generate a new Embabel project:

```bash
./scripts/project-creator.sh my-agent com.example
```
