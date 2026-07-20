## Context

The TradingAgents project uses Embabel (by Rod Johnson) as its agentic AI framework. The committed version is 0.5.0, and the working tree has already bumped the version property to 1.0.0 in `pom.xml` along with updated skill documentation. The codebase still uses the old `@AchievesGoal` annotation which was renamed to `@Goal` in 1.0.0.

The project has 66 main source files and 62 test files, all using Embabel's `@Agent`, `@Action`, `@Condition`, `@Tool` annotations and the `OperationContext`/`ActionContext` API.

## Goals / Non-Goals

**Goals:**
- Make the codebase compile and pass tests against Embabel 1.0.0
- Update `@AchievesGoal` → `@Goal` annotation rename across all files
- Verify and clean up the SPI-based agent scanning workaround
- Ensure the embabel-agent skill docs accurately reflect 1.0.0 APIs

**Non-Goals:**
- Adding new LLM providers (Bedrock, MiniMax, etc.) — these are available but not required
- Migrating to new DSL builders (RepeatUntil, ScatterGather) — existing RepeatUntilBuilder still works
- Adding few-shot prompting with `.withExample()` — new feature, out of scope
- Native structured output — new feature, out of scope

## Decisions

### D1: Minimal annotation rename over API migration
**Decision**: Rename `@AchievesGoal` → `@Goal` and update imports only. Do not migrate to new DSL builders or fluent API patterns.

**Rationale**: The existing code uses `RepeatUntilBuilder`, `asSubProcess()`, `withLlmByRole()`, `withTemplate()`, `createObject()` — all still valid in 1.0.0. Migrating to new patterns (e.g., `RepeatUntil`, `.creating().fromPrompt()`) would be a separate refactoring effort with no immediate benefit.

**Alternatives considered**:
- Migrate everything to new 1.0.0 APIs at once — too risky, too many changes
- Keep old annotations and rely on compatibility shims — 1.0.0 may not ship them

### D2: Investigate AgentScanningConfiguration before removing
**Decision**: Attempt compilation with `AgentScanningConfiguration` as-is first. Only remove it if the SPI classes don't resolve or if agent scanning works without it.

**Rationale**: The file is a known workaround for 0.5.0-SNAPSHOT. The `embabel-agent-starter-webmvc` dependency in 1.0.0 may auto-wire agent scanning. Removing it prematurely could break agent registration.

**Alternatives considered**:
- Remove immediately — risky, might break agent discovery
- Keep indefinitely — acceptable if it's still needed

### D3: Treat this as a version bump with annotation fixes
**Decision**: The upgrade is fundamentally a version bump plus annotation rename. No architectural changes, no new features, no behavior changes.

**Rationale**: Embabel 1.0.0 is a stable release of the same framework. The annotation rename is the only breaking change affecting this codebase. The new features (Bedrock provider, DSL builders, few-shot examples) are additive.

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| SPI classes in `AgentScanningConfiguration` no longer exist in 1.0.0 | Compile failure | Test compile first; remove the file if scanning is auto-configured |
| `RepeatUntilBuilder` API changed subtly | Runtime failure in debate loops | Integration tests will catch this; verify with `./mvnw verify` |
| Test API changes (`FakeOperationContext`, `EmbabelMockitoIntegrationTest`) | Test compilation failure | Update test imports if needed; the skill docs show the current API |
| Transitive dependency conflicts (Spring Boot, Spring AI) | Build failure | 1.0.0 should be compatible with Spring Boot 3.5.13; check Maven dependency tree if needed |
| LiteLLM endpoint compatibility with 1.0.0 | Runtime LLM call failure | The openai-custom starter still uses the same OpenAI-compatible protocol |

## Migration Plan

1. **Compile** — Run `./mvnw compile` to identify all breakage
2. **Fix `@AchievesGoal` → `@Goal`** — 3 files, 6 lines (import + annotation)
3. **Fix `AgentScanningConfiguration`** — Remove if SPI classes don't resolve
4. **Fix test imports** — Update if test API changed
5. **Verify** — Run `./mvnw verify` to confirm build and tests pass
6. **Rollback** — Revert the pom.xml version change and git checkout the 3 agent files

## Open Questions

- Does `embabel-agent-starter-webmvc` 1.0.0 auto-wire agent scanning, making `AgentScanningConfiguration` redundant?
- Are there any deprecation warnings at compile time we should act on?
- Does the `withTemplate()` method still work, or should we migrate to the new `.creating().fromPrompt()` pattern?
