---
title: Code-Then-Execute Pattern
status: best-practice
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Production agent practices"]
category: Tool Use & Environment
source: "https://github.com/nibzard/awesome-agentic-patterns"
tags: [code-first, execution, reliability, tool-use, best-practice]
slug: code-then-execute
id: code-then-execute
summary: >-
  Generate code first, then execute it — never execute without generating code
  first. This ensures every action is explicit, reviewable, and reproducible.
updated_at: '2026-01-05'
---

## Problem

Agents that execute commands or actions directly without first generating explicit code can make mistakes that are hard to debug, review, or reproduce. Direct execution bypasses the safety net of code review.

## Solution

Require agents to **generate code first** (scripts, commands, configurations) and **then execute** that code. This ensures every action is explicit, reviewable, and reproducible.

**Flow:**

1. Agent generates the code/command it wants to run
2. Code is displayed to the user (or logged) for review
3. Agent executes the generated code
4. Results are captured and analyzed

```pseudo
# Agent generates code first
code = agent.generate("Create a script to migrate these files")
# User reviews code
review(code)  # Can approve, modify, or reject
# Then execute
execute(code)
```

## How to use it

**When to apply:**

- Any agent with shell access or file system modification capability
- Production environments where audit trails matter
- Team settings where multiple people review agent actions

## Trade-offs

**Pros:**

- Every action is explicit and reviewable
- Easier debugging when things go wrong
- Reproducible — generated code can be re-run
- Builds trust with human stakeholders

**Cons:**

- Adds a step to the workflow
- Can feel redundant for simple, low-risk operations

## References

- Related: [Tool Use Steering via Prompting](tool-use-steering-via-prompting.md)
- Related: [Code-Over-API Pattern](code-over-api-pattern.md)

---
