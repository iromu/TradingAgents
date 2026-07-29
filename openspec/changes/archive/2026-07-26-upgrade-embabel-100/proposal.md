## Why

Embabe 1.0.0 introduces a mandatory `@AchievesGoal` requirement on every `@Agent` class and adds new LLM providers, DSL builders, and testing improvements. The project is currently on 0.5.0 (committed) and needs to upgrade to 1.0.0 to access the stable release with improved LLM integration, new providers (Bedrock, MiniMax, Z.ai), and refined agent APIs.

## What Changes

- **Bump `embabel-agent.version`** from `0.5.0` to `1.0.0` in `pom.xml` — **already done**
- **Add `@AchievesGoal` to 4 agents** that were missing it — Embabel 1.0.0 enforces mandatory `@AchievesGoal` on every `@Agent` class (BREAKING validation change)
  - `OrchestratorAgent.executeDebate()` — "Execute full research workflow and produce an investment plan"
  - `InstrumentIdentityAgent.resolveIdentity()` — "Resolve a ticker symbol to its real company identity to prevent LLM hallucination"
  - `CheckpointAgent.restoreCheckpoint()` — "Restore blackboard state from crash checkpoint for recovery"
  - `DecisionMemoryAgent.generatePastContext()` — "Generate past trading context for injection into agent prompts"
- **Review `AgentScanningConfiguration`** — SPI classes still resolve in 1.0.0, keep the file
- **Update embabel-agent skill docs** to reflect 1.0.0 API (already done in working tree)
- **No `@AchievesGoal` → `@Goal` rename** — this was a false premise; `@AchievesGoal` still exists in 1.0.0

## Correction: False Premise in Original Proposal

The original proposal claimed Embabel 1.0.0 renamed `@AchievesGoal` to `@Goal`. **This is incorrect.** Verification against the 1.0.0 JAR confirms:
- `com.embabel.agent.api.annotation.AchievesGoal` still exists and is not deprecated
- There is NO `@Goal` annotation in the 1.0.0 API
- The only `Goal` class is `com.embabel.agent.core.Goal` — a runtime object, not an annotation

The actual breaking change in 1.0.0 is that `@AchievesGoal` is now **mandatory** on every `@Agent` class.

## Capabilities

### Modified Capabilities
- **`agent-quality`**: Mandatory `@AchievesGoal` on every `@Agent` class (new validation in 1.0.0)
- **`agent-orchestration`**: Agent scanning still requires manual SPI configuration
- **`multi-provider-llm`**: New LLM providers available (Bedrock, MiniMax, Z.ai, Docker Models, Google GenAI)

## Impact

- **Affected code**: 4 agent files with new `@AchievesGoal` (OrchestratorAgent.java, InstrumentIdentityAgent.java, CheckpointAgent.java, DecisionMemoryAgent.java), 1 config file (AgentScanningConfiguration.java — kept)
- **Dependencies**: `embabel-agent-starter`, `embabel-agent-starter-openai-custom`, `embabel-agent-starter-webmvc`, `embabel-agent-test` — all to 1.0.0
- **Tests**: 519 test files, all passing
- **No API surface changes** — all public agent actions, inputs, outputs, and HTTP endpoints remain the same
