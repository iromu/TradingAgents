---
title: Output Verification Loop
status: emerging
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Production agent reliability practices"]
category: Reliability & Eval
source: "https://github.com/nibzard/awesome-agentic-patterns"
tags: [verification, validation, reliability, structured-output, quality-gate]
slug: output-verification-loop
id: output-verification-loop
summary: >-
  Verify agent outputs against expected structure and quality criteria before
  accepting results, preventing downstream failures from malformed outputs.
updated_at: '2026-01-05'
---

## Problem

Agent outputs may appear correct but fail downstream validation — missing fields, wrong types, or structural
inconsistencies. Without verification before accepting results, errors propagate through workflows.

## Solution

Implement a verification loop that validates agent outputs against expected schemas and quality criteria before
accepting them as final results.

**Core approach:**

1. **Define output contracts** — explicit schemas for expected output structure
2. **Validate after generation** — check outputs against contracts immediately
3. **Retry on failure** — if validation fails, send the agent the validation error and ask for a corrected output
4. **Limit retries** — cap at 2-3 attempts before escalating to human review

```pseudo
max_retries = 3
for attempt in range(max_retries):
    output = agent.run(task)
    errors = validate(output, schema)
    if errors.empty:
        return output
    agent.run(f"Fix these validation errors: {errors}")
return escalate_to_human(output)
```

## How to use it

**When to apply:**

- Agent outputs feed into automated pipelines
- Structured data extraction tasks
- Any workflow where output correctness is critical

## Trade-offs

**Pros:**

- Prevents downstream failures from malformed outputs
- Self-healing — agent corrects its own mistakes
- Clear failure path with human escalation

**Cons:**

- Extra compute for validation and potential retries
- May mask deeper prompt quality issues

## References

- Related: [Structured Output Specification](structured-output-specification.md)
- Related: [Schema Validation Retry with Cross-Step Learning](schema-validation-retry-cross-step-learning.md)

---
