---
title: "Background Agent with CI Feedback"
status: "validated-in-production"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["GitHub Agentic Workflows", "Cursor Background Agent", "OpenHands"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/background-agent-ci.md"
tags: [background-agent, ci, asynchronous, feedback-loop, git]
---

## Problem

Long-running tasks tie up the editor and require developers to babysit the agent. When the agent must wait on tests,
build jobs, and deployment checks, human attention gets wasted on polling instead of decision-making.

## Solution

Run the agent asynchronously in the background with CI as the objective feedback channel. The agent pushes a branch,
waits for CI results, patches failures, and repeats until policy-defined stopping conditions are met.

**Key mechanics:**

- Branch-per-task isolation (via cloud-based execution or git worktrees)
- CI log ingestion into structured failure signals
- Retry budget and stop rules to avoid infinite churn
- Notification on terminal states (`green`, `blocked`, `needs-human`)

## Evidence

- **Evidence Grade:** `validated-in-production`
- **Production Implementations:** GitHub Agentic Workflows (2026), Cursor Background Agent, OpenHands (72% on SWE-bench
  Verified)

## How to use it

- Start with deterministic tasks: dependency upgrades, lint migrations, flaky test triage
- Define retry budgets (`max_attempts`, `max_runtime`) and escalation triggers
- Use safe defaults: read-only permissions where possible, draft PRs for AI-generated changes
- Gate merge on CI plus at least one human approval for high-risk repos

## Trade-offs

* **Pros:** Better developer focus, lower waiting time, tighter CI-driven iteration loops.
* **Cons:** Requires robust task lifecycle management, failure triage logic, and notification discipline.

## References

* Raising An Agent - Episode 6: Background agents use existing CI as the feedback loop
* GitHub Agentic Workflows (2026) - Agents run within GitHub Actions with safety controls
* OpenHands - Open-source platform achieving 72% on SWE-bench Verified
