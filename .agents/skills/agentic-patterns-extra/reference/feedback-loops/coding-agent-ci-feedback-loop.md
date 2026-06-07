---
title: "Coding Agent CI Feedback Loop"
status: "best-practice"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Quinn Slack", "Will Brown (Prime Intellect Talk)"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/coding-agent-ci-feedback-loop.md"
tags: [ci, coding-agent, asynchronous, test-driven, feedback-loop]
---

## Problem

When a coding agent tackles multi-file refactors or feature additions, running tests and waiting for test feedback *
*synchronously** ties up compute and prevents the agent from working on parallel tasks.

## Solution

Run the coding agent **asynchronously** against CI (local or remote):

1. **Push a Branch & Trigger Tests** — Agent commits to a branch and triggers the CI pipeline
2. **Ingest Partial CI Feedback** — Agent periodically polls CI results and receives a small subset of failures
3. **Iterative Patch Refinement** — Agent autonomously applies fixes to specific files or functions
4. **Ping on Final Green** — When all tests pass, send a notification that the PR is ready

**Key concept:** Overlap code generation and test runs across multiple agents or branches for compute efficiency.

## Evidence

- **Evidence Grade:** `best-practice`
- **Key Findings:**
    - Will Brown's emphasis on asynchronous pipelines to avoid idle compute bubbles
    - GitHub Agentic Workflows (2026): Markdown-authored agents that auto-triage CI failures

## How to use it

- **CI Integration:** Provide the agent with a CLI or API key to push branches and trigger tests
- **Error Parsing Modules:** Implement a parser that translates CI logs into structured diagnostics
- **Prioritized Test Runs:** When re-running, only run tests in files that were patched
- **Best Practices:** Use draft PRs by default, enable partial feedback ingestion, add human-in-the-loop for high-risk
  changes

## Trade-offs

- **Pros:** Compute efficiency, faster iteration, reduced need for human intervention until final green.
- **Cons:** CI flakiness can mislead the agent, security considerations with agent permissions.

## References

- Will Brown's emphasis on **asynchronous pipelines** to avoid idle compute bubbles
- GitHub Agentic Workflows (Technical Preview 2026): Markdown-authored agents that auto-triage CI failures
