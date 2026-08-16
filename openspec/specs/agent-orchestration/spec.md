# Spec: Agent Orchestration

## Purpose

Define the multi-agent architecture for the trading research platform, decomposing the monolithic TraderAgent into focused agents with clear responsibilities using Embabel's `asSubProcess()` pattern.

## Requirements

### Requirement: OrchestratorAgent delegates to DebateAgent

The system SHALL decompose the monolithic `TraderAgent` into an `OrchestratorAgent` that delegates the debate workflow to a `DebateAgent` via `asSubProcess()`.

The `OrchestratorAgent` SHALL have the following actions:
- `tickerFromForm` / `tickerFromUserInput` — accept ticker input, produce `Ticker`
- `generateResearchPlan` — generate a research plan using the ResearchManager template with `human_approved=false`, produce `ResearchPlan`
- `waitForPlanApproval` — HITL checkpoint using `WaitFor.formSubmission()`, produce `PlanApproval`
- `executeDebate` — delegate to `DebateAgent` via `context.asSubProcess(InvestmentPlan.class, debateAgent)`, produce `InvestmentPlan`

The `executeDebate` action SHALL be the only action that produces `InvestmentPlan`, marking it as the goal achievement action with `@AchievesGoal`.

#### Scenario: Orchestrator accepts ticker and generates plan
- **WHEN** user submits a ticker via form
- **THEN** `tickerFromForm` produces a `Ticker` on the blackboard

#### Scenario: Orchestrator waits for plan approval
- **WHEN** `generateResearchPlan` produces a `ResearchPlan`
- **THEN** `waitForPlanApproval` pauses the process with a HITL form for plan review

#### Scenario: Orchestrator delegates to DebateAgent after approval
- **WHEN** user approves the research plan
- **THEN** `executeDebate` invokes `DebateAgent` via `asSubProcess()` and returns `InvestmentPlan`

### Requirement: DebateAgent orchestrates the full debate workflow

The `DebateAgent` SHALL have the following actions in its action list:
- `generateFundamentalsReport` — produce `FundamentalsReport` from `Ticker`
- `generateMarketReport` — produce `MarketReport` from `Ticker`
- `generateNewsReport` — produce `NewsReport` from `Ticker`
- `generateSocialMediaReport` — produce `SocialMediaReport` from `Ticker`
- `prepareDebateBriefs` — produce `DebateBriefs` from `Ticker` + all 4 reports
- `runDebate` — invoke `DebateLoopAgent` via `asSubProcess()`, produce `InvestmentDebateState`
- `runRiskDebate` — invoke `RiskDebateAgent` via `asSubProcess()`, produce `RiskAssessment`
- `waitForReview` — HITL checkpoint using `WaitFor.formSubmission()`, produce `InvestmentReviewFeedback`
- `researchManager` — produce `InvestmentPlan` from all prior state + feedback, marked with `@AchievesGoal`

The planner SHALL discover and chain these actions based on their input/output type flow on the blackboard.

#### Scenario: DebateAgent generates all analyst reports
- **WHEN** `DebateAgent` is invoked with a `Ticker`
- **THEN** the planner discovers and executes all four report generation actions, producing `FundamentalsReport`, `MarketReport`, `NewsReport`, and `SocialMediaReport`

#### Scenario: DebateAgent prepares debate briefs from reports
- **WHEN** all four reports exist on the blackboard
- **THEN** `prepareDebateBriefs` executes, producing `DebateBriefs` containing distilled arguments for bull and bear positions

#### Scenario: DebateAgent runs debate loop as subagent
- **WHEN** `DebateBriefs` exists on the blackboard
- **THEN** `runDebate` invokes `DebateLoopAgent` via `asSubProcess()`, which returns `InvestmentDebateState`

#### Scenario: DebateAgent runs risk debate as subagent
- **WHEN** `InvestmentDebateState` exists on the blackboard
- **THEN** `runRiskDebate` invokes `RiskDebateAgent` via `asSubProcess()`, which returns `RiskAssessment`

#### Scenario: DebateAgent waits for user review
- **WHEN** `RiskAssessment` exists on the blackboard
- **THEN** `waitForReview` pauses the process with a HITL form for debate review

#### Scenario: DebateAgent generates final plan after approval
- **WHEN** user provides `InvestmentReviewFeedback`
- **THEN** `researchManager` produces `InvestmentPlan` with the debate history, risk assessment, and user feedback

### Requirement: DebateLoopAgent runs iterative bull/bear debate

The `DebateLoopAgent` SHALL have an action `debate` that:
- Takes `Ticker` and `DebateBriefs` as inputs
- Runs an iterative bull/bear debate loop using `RepeatUntilBuilder`
- Converges when the similarity between consecutive responses exceeds the threshold, or when `maxIterations` is reached
- Produces `InvestmentDebateState` containing the full debate history, bull history, bear history, current response, iteration count, briefs, and optional risk assessment

The `DebateLoopAgent` SHALL have its own `@Agent` annotation and its own planner, independent of the `DebateAgent`.

#### Scenario: Debate loop converges before max iterations
- **WHEN** the bull and bear responses become sufficiently similar (similarity > threshold)
- **THEN** the loop terminates early and returns the accumulated debate state

#### Scenario: Debate loop reaches max iterations
- **WHEN** the loop has not converged after `maxIterations` rounds
- **THEN** the loop terminates and returns the accumulated debate state

### Requirement: RiskDebateAgent runs configurable-round risk debate

The `RiskDebateAgent` SHALL have an action `assessRisk` that:
- Takes `Ticker`, `DebateBriefs`, `InvestmentDebateState`, and `String traderProposal` as inputs
- Runs a configurable-number-of-rounds structured debate: aggressive argues for high risk, conservative for low risk, neutral for moderate, judge decides
- Produces `RiskAssessment` containing the risk level (`LOW`, `MEDIUM`, `HIGH`) and reasoning
- Is invoked as a direct delegation from `DebateAgent` (the `String traderProposal` parameter makes type-based `asSubProcess()` matching ambiguous)

The `RiskDebateAgent` SHALL have its own `@Agent` annotation and its own planner, independent of the `DebateAgent`.

The maximum number of debate rounds SHALL be configurable via `TraderAgentConfig.maxRiskDebateRounds` (default: 3).

#### Scenario: Risk debate produces LOW risk assessment
- **WHEN** the bull arguments strongly outweigh the bear arguments
- **THEN** the judge produces a `RiskAssessment` with level `LOW`

#### Scenario: Risk debate produces HIGH risk assessment
- **WHEN** the bear arguments strongly outweigh the bull arguments
- **THEN** the judge produces a `RiskAssessment` with level `HIGH`

#### Scenario: Risk debate respects configurable round count
- **WHEN** `maxRiskDebateRounds` is set to 5
- **THEN** the debate runs 5 rounds before the judge decides

### Requirement: Subagent blackboard isolation

When an agent invokes another agent via `asSubProcess()`:
- The subagent receives a **spawned copy** of the parent's blackboard at invocation time
- The subagent's actions execute within the parent's process context (same `AgentProcess`)
- The subagent can read initial state from the spawned blackboard
- The return value of `asSubProcess()` carries the result back to the parent
- The parent can then use the result in subsequent actions

#### Scenario: Subagent reads parent's initial state
- **WHEN** `DebateAgent` invokes `DebateLoopAgent` via `asSubProcess()`
- **THEN** `DebateLoopAgent` can read `Ticker` and `DebateBriefs` from its spawned blackboard (copied from parent)

#### Scenario: Subagent result flows back to parent
- **WHEN** `DebateLoopAgent` returns `InvestmentDebateState`
- **THEN** `DebateAgent`'s `runDebate` action receives the result and can pass it to `runRiskDebate`

### Requirement: Agent scanning configuration is retained

The system SHALL retain `AgentScanningConfiguration.java` for agent bean registration. Embabel 1.0.0's `embabel-agent-starter-webmvc` provides auto-configuration, but the manual SPI configuration in `AgentScanningConfiguration` continues to work and provides an explicit registration path.

`AgentScanningConfiguration` SHOULD be kept with a TODO comment to revisit whether auto-configuration alone suffices in a future upgrade.

#### Scenario: Agent scanning works with AgentScanningConfiguration
- **WHEN** `AgentScanningConfiguration.java` exists and registers the SPI post-processors
- **THEN** all `@Agent`-annotated classes are discovered and registered as agent beans

#### Scenario: AgentScanningConfiguration compiles with Embabel 1.0.0
- **WHEN** the project is compiled against Embabel 1.0.0
- **THEN** `AgentScanningConfiguration.java` compiles without errors (SPI classes still exist)

### Requirement: Risk debate rounds configurable
The system SHALL make the maximum risk debate rounds configurable via `TraderAgentConfig` instead of hardcoding the value.

#### Scenario: Risk debate uses configurable rounds
- **WHEN** `RiskDebateAgent.assessRisk()` is called
- **THEN** the loop runs for `config.maxRiskDebateRounds` iterations (default: 3)
- **AND** the value is read from `TraderAgentConfig`, not a hardcoded constant

#### Scenario: Default risk debate rounds is 3
- **WHEN** `TraderAgentConfig` is created without specifying `maxRiskDebateRounds`
- **THEN** the default value is 3

### Requirement: DebateLoopAgent validates briefs input
The system SHALL validate that `briefs` is not null before entering the debate loop.

#### Scenario: Null briefs throws exception
- **WHEN** `DebateLoopAgent.debate()` is called with null `briefs`
- **THEN** an `IllegalArgumentException` is thrown with a descriptive message
- **AND** the debate loop does not execute

### Requirement: Debate cache keys use namespace delimiter
The system SHALL prefix debate cache keys with a namespace delimiter to prevent collision with ticker names containing the delimiter.

#### Scenario: Cache key has namespace prefix
- **WHEN** `DebateLoopAgent` generates a cache key for bull response
- **THEN** the key format is `debate:{ticker}:bull:{count}` (or similar delimited format)
- **AND** a ticker containing `_debate_` does not cause key collision

### Requirement: Debate similarity computed once per iteration
The system SHALL compute similarity between consecutive responses exactly once per iteration and reuse the result for both logging and convergence check.

#### Scenario: Similarity computed once
- **WHEN** `DebateLoopAgent` runs one iteration
- **THEN** `computeSimilarity()` is called exactly once for bull and once for bear
- **AND** the result is reused for logging and convergence check