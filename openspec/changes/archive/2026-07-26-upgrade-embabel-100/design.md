## Context

The TradingAgents project uses Embabel (by Rod Johnson) as its agentic AI framework. The committed version is 0.5.0, and the working tree has already bumped the version property to 1.0.0 in `pom.xml` along with updated skill documentation.

The project has 66 main source files and 62 test files, all using Embabel's `@Agent`, `@Action`, `@Condition`, `@Tool` annotations and the `OperationContext`/`ActionContext` API.

## Goals / Non-Goals

**Goals:**
- Make the codebase compile and pass tests against Embabel 1.0.0
- Add `@AchievesGoal` to all `@Agent` classes that were missing it (Embabel 1.0.0 enforces mandatory `@AchievesGoal`)
- Verify and clean up the SPI-based agent scanning workaround
- Ensure the embabel-agent skill docs accurately reflect 1.0.0 APIs

**Non-Goals:**
- Adding new LLM providers (Bedrock, MiniMax, etc.) — these are available but not required
- Migrating to new DSL builders (RepeatUntil, ScatterGather) — existing RepeatUntilBuilder still works
- Adding few-shot prompting with `.withExample()` — new feature, out of scope
- Native structured output — new feature, out of scope

## Decisions

### D1: Add `@AchievesGoal` instead of renaming (rename was a false premise)
**Decision**: `@AchievesGoal` still exists in 1.0.0 — it was NOT renamed to `@Goal`. Add `@AchievesGoal` to agents that were missing it.

**Rationale**: Verification against the 1.0.0 JAR confirmed `com.embabel.agent.api.annotation.AchievesGoal` still exists and is not deprecated. The original proposal's claim of a rename was incorrect. The actual breaking change is that `@AchievesGoal` is now mandatory on every `@Agent` class.

**Alternatives considered**:
- Rename to `@Goal` — not possible; no such annotation exists
- Ignore the validation error — not acceptable; agents won't be properly registered

### D2: Investigate AgentScanningConfiguration before removing
**Decision**: Attempt compilation with `AgentScanningConfiguration` as-is first. Only remove it if the SPI classes don't resolve or if agent scanning works without it.

**Rationale**: The file is a known workaround for 0.5.0-SNAPSHOT. The `embabel-agent-starter-webmvc` dependency in 1.0.0 may auto-wire agent scanning. Removing it prematurely could break agent registration.

**Alternatives considered**:
- Remove immediately — risky, might break agent discovery
- Keep indefinitely — acceptable if it's still needed

### D3: Treat this as a version bump with mandatory annotation fixes
**Decision**: The upgrade is fundamentally a version bump plus mandatory `@AchievesGoal` additions. No architectural changes, no new features, no behavior changes.

**Rationale**: Embabel 1.0.0 is a stable release of the same framework. The mandatory `@AchievesGoal` is the only breaking change affecting this codebase. The new features (Bedrock provider, DSL builders, few-shot examples) are additive.

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| SPI classes in `AgentScanningConfiguration` no longer exist in 1.0.0 | Compile failure | Test compile first; remove the file if scanning is auto-configured |
| `RepeatUntilBuilder` API changed subtly | Runtime failure in debate loops | Integration tests will catch this; verify with `./mvnw verify` |
| Test API changes (`FakeOperationContext`, `EmbabelMockitoIntegrationTest`) | Test compilation failure | Update test imports if needed; the skill docs show the current API |
| Transitive dependency conflicts (Spring Boot, Spring AI) | Build failure | 1.0.0 should be compatible with Spring Boot 3.5.13; check Maven dependency tree if needed |
| LiteLLM endpoint compatibility with 1.0.0 | Runtime LLM call failure | The openai-custom starter still uses the same OpenAI-compatible protocol |
| `@AchievesGoal` not on every `@Agent` | MISSING_GOALS validation error | Add `@AchievesGoal` to all agents — this is the actual breaking change |

## Migration Plan

1. **Compile** — Run `./mvnw compile` to identify all breakage
2. **Verify `@AchievesGoal` still exists** — confirmed in 1.0.0 JAR, no rename to `@Goal`
3. **Add `@AchievesGoal` to missing agents** — OrchestratorAgent, InstrumentIdentityAgent, CheckpointAgent, DecisionMemoryAgent
4. **Fix `AgentScanningConfiguration`** — SPI classes still resolve; keep the file
5. **Verify** — Run `./mvnw verify` to confirm build and tests pass
6. **Document known issues** — `NO_PATH_TO_GOAL` on `DebateAgent.researchManager` (non-fatal)

## Open Questions

- ~~Does `embabel-agent-starter-webmvc` 1.0.0 auto-wire agent scanning, making `AgentScanningConfiguration` redundant?~~ Resolved: SPI classes still resolve; file kept with TODO
- ~~Are there any deprecation warnings at compile time we should act on?~~ No deprecation warnings found
- ~~Does the `withTemplate()` method still work, or should we migrate to the new `.creating().fromPrompt()` pattern?~~ `withTemplate()` still works; migration deferred
- **New**: Should we address the `NO_PATH_TO_GOAL` validation on `DebateAgent.researchManager`? (non-fatal, planner limitation)
