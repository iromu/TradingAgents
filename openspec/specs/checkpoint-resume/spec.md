# Spec: Checkpoint / Resume

## Purpose

Enable crash recovery for the trading pipeline by saving and restoring blackboard state at each phase boundary.

## Requirements

### Requirement: CheckpointAgent saves blackboard snapshots

The system SHALL save blackboard snapshots after each major pipeline phase for crash recovery.

The `CheckpointAgent` SHALL have an action `saveCheckpoint` that:
- Has a `@Condition(post = "isPhaseCompleted(phase)")` guard
- Serializes the current blackboard state to JSON using Jackson's `ObjectMapper`
- Saves the snapshot to a file keyed by `(ticker, tradeDate, phase)`
- Uses atomic writes (temp file + rename) to prevent corruption on crash
- The checkpoint file path SHALL be `data/checkpoints/<TICKER>.json`

Phases SHALL be: `researchPlan`, `debate`, `riskDebate`, `completed`.

#### Scenario: Save checkpoint after research plan
- **WHEN** `OrchestratorAgent.generateResearchPlan()` completes
- **THEN** a checkpoint is saved with phase `researchPlan` and the blackboard state

#### Scenario: Save checkpoint after debate
- **WHEN** `DebateAgent.runDebate()` completes (DebateLoopAgent returns)
- **THEN** a checkpoint is saved with phase `debate` and the blackboard state

#### Scenario: Save checkpoint after risk debate
- **WHEN** `DebateAgent.runRiskDebate()` completes (RiskDebateAgent returns)
- **THEN** a checkpoint is saved with phase `riskDebate` and the blackboard state

### Requirement: CheckpointAgent restores from checkpoint

When a process starts for a ticker that has an existing checkpoint, the system SHALL restore the blackboard from the last saved checkpoint and skip completed phases.

The `CheckpointAgent` SHALL have an action `restoreCheckpoint` that:
- Has a `@Condition(pre = "store.hasCheckpoint(ticker, tradeDate)")` guard
- Reads the checkpoint file and deserializes the blackboard JSON to typed objects
- Restores the blackboard with all previously completed phase data
- Returns the last completed phase so the orchestrator can skip it

The restore SHALL happen at the start of the `OrchestratorAgent.start()` action, before any pipeline phases execute.

#### Scenario: Restore from checkpoint
- **WHEN** a process starts for NVDA on 2026-01-15 and a checkpoint exists
- **THEN** the blackboard is restored with all data from completed phases

#### Scenario: Skip completed phase
- **WHEN** the checkpoint shows phase `debate` was completed
- **THEN** the pipeline skips directly to `riskDebate` (the next incomplete phase)

#### Scenario: Fresh start when no checkpoint
- **WHEN** a process starts for a new ticker with no existing checkpoint
- **THEN** no restore occurs and the pipeline starts from the beginning

### Requirement: CheckpointAgent clears checkpoint on completion

When a pipeline completes successfully, the system SHALL clear the checkpoint for that ticker/date combination.

The `CheckpointAgent` SHALL have an action `clearCheckpoint` that:
- Deletes the checkpoint file for the given `(ticker, tradeDate)`
- Runs after the `researchManager()` action (terminal goal) completes
- Prevents stale checkpoints from interfering with future runs for the same ticker

#### Scenario: Clear checkpoint on success
- **WHEN** `DebateAgent.researchManager()` completes successfully
- **THEN** the checkpoint for that ticker/date is deleted

#### Scenario: Do not clear checkpoint on failure
- **WHEN** the pipeline fails mid-execution
- **THEN** the checkpoint remains for resume on the next run

### Requirement: Checkpoint persistence format

The checkpoint file SHALL use JSON format with the following structure:

```json
{
  "ticker": "NVDA",
  "tradeDate": "2026-01-15",
  "lastCompletedPhase": "debate",
  "phases": {
    "researchPlan": {
      "blackboard": {
        "Ticker": { ... },
        "ResearchPlan": { ... },
        "InvestmentPlan": { ... }
      }
    },
    "debate": {
      "blackboard": {
        "Ticker": { ... },
        "DebateBriefs": { ... },
        "InvestmentDebateState": { ... }
      }
    }
  },
  "savedAt": "2026-06-15T10:30:00"
}
```

Each phase's `blackboard` field SHALL contain the serialized blackboard state at the time of the checkpoint.

#### Scenario: Serialize a blackboard snapshot
- **WHEN** `saveCheckpoint` is called with a blackboard containing `Ticker`, `DebateBriefs`, and `InvestmentDebateState`
- **THEN** each record is serialized to JSON and stored under its class name

#### Scenario: Deserialize a blackboard snapshot
- **WHEN** `restoreCheckpoint` reads a checkpoint with serialized `DebateBriefs`
- **THEN** the JSON is deserialized back to the `DebateBriefs` record type

### Requirement: Checkpoint configuration

The system SHALL support checkpoint enable/disable via configuration.

The `app.checkpoint.enabled` config property (default: `false`) SHALL control whether checkpoints are saved/restored.

When disabled:
- No checkpoint files are created
- No restore is attempted
- No checkpoint clear occurs

When enabled:
- Checkpoints are saved after each phase
- Restore is attempted at process start
- Checkpoint is cleared on success

#### Scenario: Checkpoints disabled
- **WHEN** `app.checkpoint.enabled` is `false`
- **THEN** `saveCheckpoint` does nothing

#### Scenario: Checkpoints enabled
- **WHEN** `app.checkpoint.enabled` is `true`
- **THEN** checkpoints are saved, restored, and cleared as specified