## Why

Embabe 1.0.0 introduces a breaking annotation change (`@AchievesGoal` → `@Goal`) and adds new LLM providers, DSL builders, and testing improvements. The project is currently on 0.5.0 (committed) and needs to upgrade to 1.0.0 to access the stable release with improved LLM integration, new providers (Bedrock, MiniMax, Z.ai), and refined agent APIs.

## What Changes

- **Bump `embabel-agent.version`** from `0.5.0` to `1.0.0` in `pom.xml`
- **Rename `@AchievesGoal` → `@Goal`** in 3 agent files (DebateAgent, RiskDebateAgent, DebateLoopAgent) — **BREAKING**
- **Update `@AchievesGoal` import** from `com.embabel.agent.api.annotation.AchievesGoal` to `com.embabel.agent.api.annotation.Goal`
- **Review `AgentScanningConfiguration`** — the SPI-based manual wiring may no longer be needed in 1.0.0 (the `webmvc` starter likely auto-wires agent scanning now)
- **Update embabel-agent skill docs** to reflect 1.0.0 API (already done in working tree)
- **No new capabilities** — this is a version upgrade with annotation-level breaking changes only

## Capabilities

### Modified Capabilities
- **`agent-quality`**: Annotation model changes — `@AchievesGoal` → `@Goal` across all agents
- **`agent-orchestration`**: Agent scanning may no longer require manual SPI configuration
- **`multi-provider-llm`**: New LLM providers available (Bedrock, MiniMax, Z.ai, Docker Models, Google GenAI)

## Impact

- **Affected code**: 3 agent files (DebateAgent.java, RiskDebateAgent.java, DebateLoopAgent.java), 1 config file (AgentScanningConfiguration.java)
- **Dependencies**: `embabel-agent-starter`, `embabel-agent-starter-openai-custom`, `embabel-agent-starter-webmvc`, `embabel-agent-test` — all to 1.0.0
- **Tests**: 62 test files that use `EmbabelMockitoIntegrationTest`, `FakeOperationContext`, `FakePromptRunner` — may need updates if test API changed
- **No API surface changes** — all public agent actions, inputs, outputs, and HTTP endpoints remain the same
