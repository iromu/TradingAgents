---
title: "Checkpoint System"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/checkpoint/CheckpointAgent.java"
  - "src/main/java/com/embabel/gekko/agent/checkpoint/CheckpointStore.java"
  - "src/main/java/com/embabel/gekko/agent/checkpoint/CheckpointData.java"
updated_at: "2026-08-16"
---

# Checkpoint System

The checkpoint system provides crash recovery by persisting blackboard snapshots after each phase of the research workflow. It allows a restarted application to resume from the last completed phase rather than starting over.

## Components

| Component | Role |
|-----------|------|
| `CheckpointAgent` | `@Agent` with three actions: restore, save, clear |
| `CheckpointStore` | JSON file-based persistence with atomic writes |
| `CheckpointData` | Record returned on restore (ticker, tradeDate, lastCompletedPhase, phases) |

## Configuration

| Property | Default | Purpose |
|----------|---------|---------|
| `app.checkpoint.enabled` | `false` | Master switch — all operations are no-ops when disabled |
| `app.checkpoint.dir` | `data/checkpoints` | Directory for checkpoint files |

## File Format

Each ticker gets one JSON file: `data/checkpoints/<TICKER>.json`

```json
{
  "ticker": "AAPL",
  "tradeDate": "2026-08-16",
  "lastCompletedPhase": "debate",
  "phases": {
    "identity": { "blackboard": { ... } },
    "researchPlan": { "blackboard": { ... } },
    "debate": { "blackboard": { ... } }
  },
  "savedAt": "2026-08-16T14:30:00"
}
```

Phases accumulate — each `saveCheckpoint()` call merges the new phase into the existing file.

## Operations

### Restore

`CheckpointAgent.restoreCheckpoint(ticker, tradeDate)` reads the checkpoint file and returns a `CheckpointData` record, or null if no checkpoint exists or checkpoints are disabled.

### Save

`CheckpointAgent.saveCheckpoint(ticker, tradeDate, phase, blackboardState)` is called after each phase completes. The store:

1. Reads the existing file (if any)
2. Updates `lastCompletedPhase` to the new phase
3. Adds/replaces the phase entry in the `phases` map
4. Writes atomically (temp file + rename)

### Clear

`CheckpointAgent.clearCheckpoint(ticker, tradeDate)` deletes the checkpoint file on successful completion.

## Safety

- **Path traversal prevention:** Ticker names are validated against `^[A-Za-z0-9._-]+$`, then normalized and checked to stay within the checkpoint directory
- **Atomic writes:** All saves use temp file + `Files.move(REPLACE_EXISTING)` to prevent corruption
- **Fail-safe:** All operations catch exceptions and log errors without crashing the agent process

## Integration

`OrchestratorAgent` injects `CheckpointAgent` and calls it at key workflow points. The checkpoint system is currently disabled by default (`enabled: false`) and can be enabled via configuration.

See `[[trading-workflow]]` for where checkpoints fit in the overall flow.
