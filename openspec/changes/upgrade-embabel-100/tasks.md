## 1. Rename @AchievesGoal → @Goal

- [x] 1.1 Update DebateAgent.java: change import from `com.embabel.agent.api.annotation.AchievesGoal` to `com.embabel.agent.api.annotation.Goal`
- [x] 1.2 Update DebateAgent.java: change `@AchievesGoal(description = "Generate final investment plan")` to `@Goal(description = "Generate final investment plan")`
- [x] 1.3 Update RiskDebateAgent.java: change import from `com.embabel.agent.api.annotation.AchievesGoal` to `com.embabel.agent.api.annotation.Goal`
- [x] 1.4 Update RiskDebateAgent.java: change `@AchievesGoal(description = "Produce risk assessment")` to `@Goal(description = "Produce risk assessment")`
- [x] 1.5 Update DebateLoopAgent.java: change import from `com.embabel.agent.api.annotation.AchievesGoal` to `com.embabel.agent.api.annotation.Goal`
- [x] 1.6 Update DebateLoopAgent.java: change `@AchievesGoal(description = "Produce investment debate state")` to `@Goal(description = "Produce investment debate state")`
- [x] 1.7 Verify no remaining `@AchievesGoal` references in the entire codebase

## 2. Review AgentScanningConfiguration

- [x] 2.1 Attempt `./mvnw compile` to check if SPI classes resolve in 1.0.0
- [x] 2.2 If SPI classes don't resolve, remove `AgentScanningConfiguration.java`
- [x] 2.3 If SPI classes resolve, keep the file but add a TODO comment to revisit in next upgrade
- [x] 2.4 Verify agent scanning still works (all @Agent classes are registered)

## 3. Update embabel-agent skill docs

- [x] 3.1 Verify SKILL.md description mentions v1.0.0 (already done in working tree)
- [x] 3.2 Verify all reference docs are consistent with 1.0.0 APIs
- [x] 3.3 Verify deleted docs (domain-objects.md, execution-modes.md, guide-server.md, invocation.md, streams.md) are properly removed
- [x] 3.4 Verify new docs (types.md, api-spi.md, flow.md, invoking.md, streaming.md, minimax.md, bedrock.md, domain.md) are present

## 4. Verify build compiles

- [x] 4.1 Run `./mvnw compile` — should compile cleanly
- [x] 4.2 If compilation fails, fix any remaining API incompatibilities
- [x] 4.3 Check for deprecation warnings and address if they indicate real breakage

## 5. Run tests

- [x] 5.1 Run `./mvnw test` — all tests should pass
- [x] 5.2 If test compilation fails, update test imports (e.g., `EmbelabelMockitoIntegrationTest` → `EmbelMockitoIntegrationTest` if the name changed)
- [x] 5.3 If test failures occur, investigate and fix root causes

## 6. Final verification

- [x] 6.1 Run `./mvnw verify` — full build including integration tests
- [x] 6.2 Confirm 62 test files all pass
- [x] 6.3 Verify the application still starts (`./mvnw spring-boot:run` or equivalent)
