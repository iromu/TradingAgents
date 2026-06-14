# Design: Refactor Agent Architecture

## Context

`TraderAgent` (784 lines) is a single `@Agent` class that hardcodes the entire research pipeline inside `executeFullResearch()`. It calls every action method directly (not via the planner), uses manual Flux streaming, and manually handles HITL forms. `RiskDebateService` is a `@Component` with a manual for-loop instead of a proper agent.

The project uses Embabel 0.5.0-SNAPSHOT with Spring Boot 3.5.13, OpenAI-compatible LLM via LiteLLM at `http://spark.local:4000`. The existing codebase has:
- 4 researcher classes (Bull/Bear/SocialMedia/FundamentalResearcher)
- 3 tool classes (MarketDataTools, FundamentalDataTools, NewsDataTools)
- 2 data services (AlphaVantageService, YFinService)
- HITL controllers (TradingHtmxController, ProcessStatusController)
- Jinja prompt templates in `src/main/resources/prompts/`

## Goals / Non-Goals

**Goals:**
- Decompose the monolithic `TraderAgent` into 4 focused agents with clear responsibilities
- Use Embabel's `asSubProcess()` for isolated blackboard communication between agents
- Replace manual Flux streaming with synchronous `createObject()` (Embabel's built-in API)
- Replace manual HITL form handling with `WaitFor.formSubmission()` (native Embabel HITL)
- Make the debate loop and risk debate proper agents with their own planners

**Non-Goals:**
- Refactoring controllers, tools, or data services (out of scope)
- Adding new prompt templates or changing existing ones
- Adding tests (testing is a separate concern)
- Maintaining backwards compatibility with existing HTTP endpoints

## Decisions

### D1: Use `asSubProcess()` for inter-agent communication

**Choice:** Orchestrator → DebateAgent → DebateLoopAgent/RiskDebateAgent via `context.asSubProcess(OutputClass.class, agent)`

**Rationale:** `asSubProcess` creates a child process with a spawned (isolated) blackboard. The subagent's planner runs to completion, and the result is returned to the parent. This gives each agent its own planning context while allowing data flow through return values.

**Alternatives considered:**
- Direct method calls between agents — defeats the planner, creates tight coupling
- Shared blackboard without isolation — agents interfere with each other's state
- `@Action`-only decomposition within a single agent — loses the benefits of separate planners

### D2: Synchronous `createObject()` for all LLM calls

**Choice:** Replace the 15-line `streamWithTemplate()` manual Flux pattern with one-line `createObject()` calls:
```java
return context.ai()
    .withLlmByRole(role)
    .withId("actionId")
    .withTemplate("templateName")
    .createObject(String.class, model);
```

**Rationale:** Embabel's `createObject()` handles streaming internally when the model supports it, falls back to non-streaming otherwise. It's the idiomatic Embabel API, works with `FakePromptRunner` in tests, and is 1 line vs 15.

**Alternatives considered:**
- Keep manual Flux with `StreamingPromptRunner` — verbose, doesn't work with test doubles, reinvents what Embabel already provides
- Use Spring AI `ChatClient.stream()` directly — bypasses Embabel's observation/cost tracking

### D3: Debate loop as a separate agent (`DebateLoopAgent`)

**Choice:** The bull/bear iterative debate is a dedicated `@Agent` with its own planner. The `DebateAgent` invokes it via `asSubProcess`.

**Rationale:** The debate loop is a self-contained iterative workflow with its own convergence criteria. Giving it its own agent means:
- Its own planner can optimize the loop independently
- It can potentially be reused by other agents
- The `DebateAgent`'s action list stays focused on orchestration

**Alternatives considered:**
- Keep as `RepeatUntilBuilder.asSubProcess()` inside a single `@Action` — works but loses agent identity and planner independence

### D4: Risk debate as a separate agent (`RiskDebateAgent`)

**Choice:** Extract `RiskDebateService` (`@Component` with for-loop) into `RiskDebateAgent` (`@Agent` with its own planner).

**Rationale:** The risk debate is a 3-round structured debate (bull → bear → judge). As a subagent, it gets:
- Its own process context with isolated blackboard
- Its own planner for action discovery
- Consistent invocation pattern with the rest of the architecture

**Alternatives considered:**
- Keep as `@Component` with manual for-loop — already the problem we're solving
- Merge into `DebateAgent` as an `@Action` — loses isolation and reusability

### D5: Native `WaitFor.formSubmission()` for HITL

**Choice:** Replace manual `FormBindingRequest`/`FormSubmission` handling in controllers with `WaitFor.formSubmission()` in agent actions.

**Rationale:** `WaitFor.formSubmission()` is Embabel's native HITL pattern. The framework:
- Automatically generates the form from the record structure
- Puts the process in WAITING state
- Resumes the process with bound form data on submission

The controllers simplify to: detect WAITING state → render form → on submit → call `onResponse()`.

**Alternatives considered:**
- Keep manual form handling — 50+ lines of boilerplate per checkpoint, duplicated across controllers

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Subagent blackboard isolation loses intermediate state | Planner can't see reports/debate state from parent | Return values carry data between agents; intermediate state stays on subagent's blackboard where it belongs |
| More agents = more process overhead | Slight latency increase for subagent creation | Negligible compared to LLM call latency; processes are lightweight |
| `WaitFor.formSubmission()` changes form structure | Existing web UI templates may need updates | Templates already use Thymeleaf — form structure is driven by the record type, not hardcoded |
| Breaking change — no backwards compatibility | Existing running processes will fail | No backwards compatibility requested; clean break is acceptable |

## Migration Plan

1. Create the 4 new agent classes (Orchestrator, Debate, DebateLoop, RiskDebate)
2. Wire agents in `TraderAgentConfig` (replace `TraderAgent` bean with new agents)
3. Simplify controllers (remove manual HITL, remove "travel" naming)
4. Delete `TraderAgent.java` and `RiskDebateService.java`
5. Update `GekkoApplication.java` if needed

## Open Questions

None — all decisions made in exploration.
