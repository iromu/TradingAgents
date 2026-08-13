---
title: "State Models"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/domain/ResearchTypes.java"
  - "src/main/java/com/embabel/gekko/htmx/HitlService.java"
  - "src/main/java/com/embabel/gekko/agent/memory/DecisionMemoryRepository.java"
  - "src/main/java/com/embabel/gekko/agent/identity/InstrumentContextPromptContributor.java"
updated_at: "2026-08-13"
---

# State Models (Entities)

Gekko uses Java records to pass structured data between agents. These records form the **state models** of the system.

## Core State Records (in TraderAgent)

### Ticker
```java
record Ticker(String content, String feedback)
```
- `content` — the stock ticker symbol (e.g., "AAPL")
- `feedback` — optional user feedback passed through the workflow

### DebateBriefs
```java
record DebateBriefs(String fundamentalsBrief, String marketBrief, String newsBrief, String socialBrief)
```
- Condensed versions of each analyst report, used as input to the debate

### InvestmentDebateState
```java
record InvestmentDebateState(List<String> history, List<String> bullHistory, List<String> bearHistory,
                             String currentResponse, int count, DebateBriefs briefs)
```
- `history` — full conversation history
- `bullHistory` — only bull arguments
- `bearHistory` — only bear arguments
- `count` — number of turns completed
- `briefs` — the original debate briefs

### InvestmentPlan
```java
record InvestmentPlan(String judgeDecision, InvestmentDebateState investmentDebateState)
```
- `judgeDecision` — the final investment recommendation
- `investmentDebateState` — the debate that led to this plan

### ResearchPlan
```java
record ResearchPlan(String content)
```
- A high-level plan generated before the full workflow

### InvestmentReviewFeedback
```java
record InvestmentReviewFeedback(String feedback, boolean approved)
```
- Used in the debate review HITL checkpoint
- `feedback` — user's comments
- `approved` — whether to proceed

### PlanApproval
```java
record PlanApproval(String feedback, boolean approved)
```
- Used in the research plan approval HITL checkpoint

## Risk Assessment

### RiskAssessment
```java
record RiskAssessment(RiskLevel level, String reasoning)
```
- `level` — the overall risk level
- `reasoning` — the LLM's explanation for the risk assessment

### RiskLevel
```java
enum RiskLevel { RISKY, NEUTRAL, CONSERVATIVE }
```
- **RISKY** — Aggressive stance, high risk tolerance
- **NEUTRAL** — Balanced assessment
- **CONSERVATIVE** — Cautious stance, low risk tolerance

## Report Interface

All analyst reports implement `TraderAgent.Report`:
```java
interface Report { String content(); }
```

### Analyst Report Records (in Analysts)

| Record | Purpose |
|--------|---------|
| `FundamentalsReport` | Financial statement analysis |
| `MarketReport` | Price data and technical indicators |
| `NewsReport` | News articles and sentiment |
| `SocialMediaReport` | Social media sentiment |

## HITL Session

```java
record HitlSession(String processId, String agentName, String failureInfo,
                   boolean resolved, String userInput, String feedback,
                   String resolvedProcessId)
```
- Tracks HITL sessions for failure recovery
- `agentName` — which agent failed
- `failureInfo` — what went wrong
- `resolved` — whether the user has taken action

## Decision Memory

`DecisionMemoryRepository` stores trading decisions in a file-based log with cross-process safety:

- **FileLock** via `FileChannel` on a dedicated `.lock` file for mutual exclusion
- **Entry separator** uses control characters (`\u001E...\u001F`) for encoding safety
- **Atomic writes** via temp file + rename
- **`withLock()` pattern** wraps all read-modify-write operations

## Instrument Context

`InstrumentContextPromptContributor` uses pure `ThreadLocal<InstrumentContext>` for thread-safe prompt injection:

- Context is set by `OrchestratorAgent` after identity resolution
- `clear()` method explicitly clears ThreadLocal after each agent turn
- No `ConcurrentHashMap` — simplified from the previous two-level approach
