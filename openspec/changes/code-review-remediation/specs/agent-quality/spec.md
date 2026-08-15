## ADDED Requirements

### Requirement: Rating extraction handles conflicting keywords
The system SHALL extract investment rating from LLM output using a priority-based approach that considers context, not just first-match-wins.

#### Scenario: Buy/sell conflict resolved by context
- **WHEN** content contains both "buy" and "sell" keywords
- **THEN** the system checks for contextual cues (e.g., "buy recommendation", "sell signal")
- **AND** falls back to priority order (buy > sell > overweight > underweight > hold) if context is ambiguous

#### Scenario: Rating extraction default is Hold
- **WHEN** content contains no rating keywords
- **THEN** the system returns "Hold" as the default rating

### Requirement: Thesis extraction robust against LLM variations
The system SHALL extract thesis from LLM output using multiple fallback strategies to handle variations in LLM output format.

#### Scenario: Thesis found by keyword
- **WHEN** content contains "thesis" or "rationale" in the first half of the text
- **THEN** the system extracts text from that keyword to the next double-newline

#### Scenario: Thesis fallback to summary
- **WHEN** content does not contain "thesis" or "rationale" keywords
- **THEN** the system falls back to extracting the first paragraph as thesis

### Requirement: CODE_FENCE_UNCLOSED regex is non-greedy
The system SHALL use a non-greedy regex pattern for unclosed code fence detection to prevent excessive backtracking.

#### Scenario: Unclosed code fence matched efficiently
- **WHEN** `sanitizeValue()` processes input with an unclosed code fence
- **THEN** the regex `CODE_FENCE_UNCLOSED` matches without excessive backtracking
- **AND** the pattern uses non-greedy quantifiers

### Requirement: AOT hints registered for all structured output types
The system SHALL register all structured output types and custom exceptions in AOT runtime hints for GraalVM native image compatibility.

#### Scenario: TraderProposalOutput registered for reflection
- **WHEN** native image is built
- **THEN** `TraderProposalOutput` is registered for reflection binding
- **AND** structured output serialization works at runtime

#### Scenario: PortfolioDecisionOutput registered for reflection
- **WHEN** native image is built
- **THEN** `PortfolioDecisionOutput` is registered for reflection binding

#### Scenario: BudgetExceededException registered for reflection
- **WHEN** native image is built
- **THEN** `BudgetExceededException` is registered for reflection binding

#### Scenario: SubtractIndicator registered for reflection
- **WHEN** native image is built
- **THEN** `SubtractIndicator` is registered for reflection binding
