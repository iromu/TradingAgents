# Test Infrastructure

## Purpose

Ensure all test files compile and pass by updating `TraderAgentConfig` constructor calls to match the current 16-field record signature.

## Requirements

### Requirement: Test files use correct TraderAgentConfig constructor
All test files that instantiate `TraderAgentConfig` SHALL pass all 16 fields in the correct order matching the record signature.

#### Scenario: DebateLoopAgentTest compiles
- **WHEN** `DebateLoopAgentTest.java` is compiled
- **THEN** the `TraderAgentConfig` constructor call passes 16 arguments
- **AND** the test compiles without "cannot be applied to given types" errors

#### Scenario: TraderAgentConfigTest compiles
- **WHEN** `TraderAgentConfigTest.java` is compiled
- **THEN** the `makeConfig()` helper passes 16 arguments to the constructor
- **AND** the test compiles without type mismatch errors

#### Scenario: DebateLoopAgentIntegrationTest compiles
- **WHEN** `DebateLoopAgentIntegrationTest.java` is compiled
- **THEN** all 5 test methods pass 16 arguments to `TraderAgentConfig`
- **AND** the test compiles without constructor mismatch errors

### Requirement: All tests pass after constructor fix
The full test suite SHALL pass after updating constructor calls.

#### Scenario: Maven verify passes
- **WHEN** `./mvnw verify` is executed
- **THEN** all tests pass with 0 failures
- **AND** no compilation errors related to `TraderAgentConfig` constructor
