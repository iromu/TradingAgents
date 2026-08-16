## ADDED Requirements

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

## MODIFIED Requirements

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
