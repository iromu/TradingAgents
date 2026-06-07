---
title: "Spec-As-Test Feedback Loop"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Jory Pestorius", "Anthropic Engineering"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/spec-as-test-feedback-loop.md"
tags: [spec-as-test, feedback-loop, specification, synchronization, continuous-integration]
---

## Problem

Even in spec-first projects, implementations can drift as code evolves and the spec changes (or vice-versa). Silent
divergence erodes trust.

## Solution

Generate **executable assertions** directly from the spec (e.g., unit or integration tests) and let the agent:

- Watch for any spec or code commit
- Auto-regenerate test suite from latest spec snapshot
- Run tests; if failures appear, open an *agent-authored* PR that either updates code to match spec or flags unclear
  spec segments for human review

**Four-phase architecture:**

1. Specification Layer: Parse specs (YAML/JSON/BDD) into internal representation
2. Test Generation Layer: Create executable tests (unit, integration, property)
3. Execution Layer: Run tests in parallel via CI/CD
4. Feedback Layer: Route failures to auto-fix PRs or human review

## Evidence

- **Evidence Grade:** `emerging`
- **Key Findings:**
    - Jory Pestorius: "Spec-As-Test" pattern for continuous spec-code synchronization
    - Anthropic Engineering: Effective Harnesses for Long-Running Agents

## How to use it

- Use when agent quality improves only after iterative critique or retries
- Start with one objective metric and one feedback loop trigger
- Record failure modes so each loop produces reusable learning artifacts

## Trade-offs

- **Pros:** Catches drift early, immune to "pass by deletion", measurable progress metrics, survives session boundaries
- **Cons:** Heavy CI usage, false positives if spec wording is ambiguous, upfront spec investment required

## References

- Primary source: http://jorypestorious.com/blog/ai-engineer-spec/
- Anthropic
  Engineering: [Effective Harnesses for Long-Running Agents](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents)
- OpenAI Evals: https://github.com/openai/evals
- QuickCheck (Claessen & Hughes, ICFP 2000) - property-based testing foundation
- Constitutional AI (Bai et al., Anthropic 2022) - principles as specifications
