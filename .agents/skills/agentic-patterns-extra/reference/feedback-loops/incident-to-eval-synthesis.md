---
title: "Incident-to-Eval Synthesis"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["OpenAI", "Anthropic", "Meta"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/incident-to-eval-synthesis.md"
tags: [incident-management, evaluation, regression-testing, production-learning]
---

## Problem

Many teams run agent evaluations, but the eval suite drifts away from real failures seen in production. Incidents get
resolved operationally, yet the exact failure mode is rarely converted into a durable regression test.

## Solution

Convert every production incident into one or more executable eval cases, then gate future changes on those cases.

**Pattern mechanics:**

- Capture incident artifacts: inputs, context, tool traces, outputs, and impact
- Normalize sensitive data and derive a minimal reproducible scenario
- Encode expected behavior as objective pass/fail criteria
- Add the case to the evaluation corpus with severity and owner metadata
- Run incident-derived evals in CI and release gates

## Evidence

- **Evidence Grade:** `medium`
- **Key Findings:**
    - Academic research shows 60-80% success rates for automated test generation from failure reports
    - Only 30% of organizations systematically reuse incident data; those that do see fewer repeat incidents
    - Industry adoption at OpenAI, Anthropic, and Meta validates production-derived evals

## How to use it

- Start with P0 (critical) incidents only, using tiered blocking
- Require a linked eval case in incident closure criteria
- Track two metrics: incident recurrence rate and eval-catch rate before release
- Periodically prune or merge redundant incident-derived tests

## Trade-offs

* **Pros:** Aligns evals with real risk, compounds operational learning over time.
* **Cons:** Adds triage overhead and requires discipline in incident data capture.

## References

- https://sre.google/sre-book/postmortem-culture/
- Thummalapenta et al., FSE 2014: Automatic Generation of Test Cases from Bug Reports
- https://martinfowler.com/articles/practical-test-pyramid.html
