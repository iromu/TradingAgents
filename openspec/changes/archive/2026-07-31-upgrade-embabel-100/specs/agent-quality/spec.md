## MODIFIED Requirements

### Requirement: Debate loop uses convergence check
The system SHALL replace the fixed 2-iteration debate loop with a convergence-based loop that continues until the debate stabilizes or reaches a maximum iteration limit.

#### Scenario: Debate converges before max iterations
- **WHEN** the bull and bear responses become similar (within a similarity threshold)
- **THEN** the debate loop terminates early
- **AND** the final state includes all debate history

#### Scenario: Debate reaches max iterations without convergence
- **WHEN** the debate continues for the maximum number of iterations without converging
- **THEN** the loop terminates at the max iteration count
- **AND** the final state includes all debate history up to max

#### Scenario: Convergence is measured by response similarity
- **WHEN** comparing consecutive debate responses
- **THEN** the system uses a similarity metric (e.g., text overlap or semantic similarity) to detect stabilization
- **AND** a configurable similarity threshold determines when to stop

#### Scenario: Default max iterations is reasonable
- **WHEN** the debate loop starts
- **THEN** the default maximum iteration count is at least 5 (configurable)
- **AND** the current value of 2 is replaced

### Requirement: Risk debate system is functional or removed
The system SHALL either wire the existing risk debate prompts into the agent workflow or remove the dead prompt files — no orphaned prompt files SHALL remain.

#### Scenario: Risk debate is integrated into workflow
- **WHEN** the trader agent processes a ticker
- **THEN** a risk debate is conducted using RiskManager, AggresiveDebator, ConservativeDebator, and NeutralDebator prompts
- **AND** the risk debate result influences the final investment plan

#### Scenario: Risk debate result is documented
- **WHEN** the risk debate completes
- **THEN** the risk assessment (Risky/Neutral/Conservative) is included in the debate state
- **AND** the risk assessment is passed to the research manager

### Requirement: MarketAnalyst uses data tools
The system SHALL enable the MarketAnalyst to call stock data and indicator tools for technical analysis.

#### Scenario: MarketAnalyst receives tool names
- **WHEN** `generateMarketReport()` executes
- **THEN** the `tool_names` parameter includes `get_stock_data,get_indicators`
- **AND** the prompt runner is configured with the appropriate tool objects

#### Scenario: MarketAnalyst receives indicator data
- **WHEN** the MarketAnalyst calls indicator tools
- **THEN** the `YFinService` provides TA4J indicator calculations
- **AND** the MarketAnalyst receives stock price data for the requested ticker

### Requirement: Prompt file extensions are consistent
The system SHALL use `.jinja` as the uniform extension for all prompt template files.

#### Scenario: Analyst prompts use .jinja extension
- **WHEN** the system loads analyst prompts
- **THEN** all four analyst prompts (Fundamentals, Market, News, SocialMedia) use the `.jinja` extension
- **AND** the `TraderAgent` resource references are updated accordingly

#### Scenario: No .txt prompt files remain
- **WHEN** the prompts directory is scanned
- **THEN** no `.txt` files exist in `src/main/resources/prompts/`
- **AND** all prompt loading code uses `.jinja` references

### Requirement: Every @Agent has @AchievesGoal on its goal-achieving action
Embabel 1.0.0 enforces that every `@Agent` class must have at least one `@AchievesGoal` annotation on an action method. The annotation `@AchievesGoal` still exists in 1.0.0 at `com.embabel.agent.api.annotation.AchievesGoal` — it was NOT renamed to `@Goal`.

The following actions MUST be annotated with `@AchievesGoal(description = "...")`:
- `DebateAgent.researchManager()` — goal: "Produce final investment plan after debate, risk assessment, and user review"
- `RiskDebateAgent.assessRisk()` — goal: "Produce risk assessment"
- `DebateLoopAgent.debate()` — goal: "Produce investment debate state"
- `OrchestratorAgent.executeDebate()` — goal: "Execute full research workflow and produce an investment plan"
- `InstrumentIdentityAgent.resolveIdentity()` — goal: "Resolve a ticker symbol to its real company identity to prevent LLM hallucination"
- `CheckpointAgent.restoreCheckpoint()` — goal: "Restore blackboard state from crash checkpoint for recovery"
- `DecisionMemoryAgent.generatePastContext()` — goal: "Generate past context from decision memory for prompt injection"

The import `com.embabel.agent.api.annotation.AchievesGoal` MUST be used (not `@Goal` which does not exist as an annotation).

#### Scenario: DebateAgent has @AchievesGoal on researchManager
- **WHEN** `DebateAgent.java` is inspected
- **THEN** the `researchManager` method is annotated with `@AchievesGoal`
- **AND** the import is `com.embabel.agent.api.annotation.AchievesGoal`

#### Scenario: RiskDebateAgent has @AchievesGoal on assessRisk
- **WHEN** `RiskDebateAgent.java` is inspected
- **THEN** the `assessRisk` method is annotated with `@AchievesGoal`
- **AND** the import is `com.embabel.agent.api.annotation.AchievesGoal`

#### Scenario: DebateLoopAgent has @AchievesGoal on debate
- **WHEN** `DebateLoopAgent.java` is inspected
- **THEN** the `debate` method is annotated with `@AchievesGoal`
- **AND** the import is `com.embabel.agent.api.annotation.AchievesGoal`

#### Scenario: All agents have @AchievesGoal
- **WHEN** every `@Agent`-annotated class is inspected
- **THEN** each has at least one method annotated with `@AchievesGoal`
