## Context

The TradingAgents project uses Embabel (by Rod Johnson) as its agentic AI framework. The committed version is 0.5.0, and the working tree has already bumped the version property to 1.0.0 in `pom.xml` along with updated skill documentation.

During the review cycle, a critical finding emerged: the original proposal's premise was **incorrect**. `@AchievesGoal` was NOT renamed to `@Goal` in 1.0.0 — the annotation still exists and is now **required** on every `@Agent` class.

## Actual Findings

### False Premise: `@AchievesGoal` → `@Goal` rename
The proposal claimed Embabel 1.0.0 renamed `@AchievesGoal` to `@Goal`. **This is false.** The `@AchievesGoal` annotation still exists in 1.0.0 at `com.embabel.agent.api.annotation.AchievesGoal`. There is no `@Goal` annotation in the 1.0.0 API — only a runtime `com.embabel.agent.core.Goal` class (not an annotation).

### New Breaking Change: Mandatory `@AchievesGoal` on every `@Agent`
Embabel 1.0.0 enforces a new validation rule: every `@Agent` class must have at least one `@AchievesGoal` annotation. Agents without it produce `MISSING_GOALS` validation errors at startup.

Three agents were missing `@AchievesGoal`:
- **OrchestratorAgent** — added on `executeDebate()` method
- **InstrumentIdentityAgent** — added on `resolveIdentity()` method
- **CheckpointAgent** — added on `restoreCheckpoint()` method

### Planner Validation: `NO_PATH_TO_GOAL` on `DebateAgent.researchManager`
The planner cannot construct a valid path to achieve the `researchManager` goal due to a complex chain of preconditions (ticker → debateState → riskAssessment → feedback → portfolioDecision → researchManager). This is a **non-fatal** validation warning — the workflow works correctly at runtime via human-in-the-loop interaction, but the automated planner cannot model it.

## Updated Tasks

## 1. Verify pom.xml version bump

- [x] 1.1 Verify `embabel-agent.version` is `1.0.0` in `pom.xml`

## 2. Correct the false premise

- [x] 2.1 Verify `@AchievesGoal` still exists in Embabel 1.0.0 (confirmed — no rename)
- [x] 2.2 Confirm no `@Goal` annotation exists in 1.0.0 API
- [x] 2.3 Update proposal to reflect actual findings

## 3. Add missing `@AchievesGoal` annotations

- [x] 3.1 Add `@AchievesGoal` to `OrchestratorAgent.executeDebate()` — goal: "Execute full research workflow and produce an investment plan"
- [x] 3.2 Add `@AchievesGoal` to `InstrumentIdentityAgent.resolveIdentity()` — goal: "Resolve a ticker symbol to its real company identity to prevent LLM hallucination"
- [x] 3.3 Add `@AchievesGoal` to `CheckpointAgent.restoreCheckpoint()` — goal: "Restore blackboard state from crash checkpoint for recovery"

## 4. Review AgentScanningConfiguration

- [x] 4.1 Verify `AgentScanningConfiguration.java` compiles with 1.0.0 (confirmed — SPI classes still resolve)
- [x] 4.2 Keep the file with existing TODO comment to revisit in next upgrade

## 5. Verify build compiles

- [x] 5.1 Run `./mvnw compile` — compiles cleanly
- [x] 5.2 No deprecation warnings indicating real breakage

## 6. Run tests

- [x] 6.1 Run `./mvnw test` — 519 tests, 0 failures
- [x] 6.2 Run `./mvnw verify` — full build passes

## 7. Document known issues

- [x] 7.1 Document `NO_PATH_TO_GOAL` on `DebateAgent.researchManager` as a known non-fatal planner limitation
- [x] 7.2 Document that the workflow relies on human-in-the-loop interaction which the automated planner cannot model
