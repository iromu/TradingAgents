---
name: agentic-patterns-extra
description: >-
  Curated catalogue of 194 agentic AI patterns across 8 categories — real-world
  tricks, workflows, and mini-architectures for autonomous agents in production.
  Use this skill whenever the user asks about agent architecture, agent design
  patterns, how to solve an agent-related challenge, or wants pattern recommendations.
  Trigger on questions about context management, multi-agent coordination, reliability,
  security, tool use, feedback loops, learning, or human-agent collaboration.
  Also trigger when the user wants to compare patterns, find the right pattern for a
  use case, understand trade-offs, or know which patterns work best together.
  Do NOT use it to write or refactor application code — it only recommends patterns.
  Also trigger on agent failures, token waste, context contamination, or operational
  issues with autonomous agents.
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 1.0.0
---

# Agentic Patterns Extra

A curated catalogue of agentic AI patterns sourced from
[nibzard/awesome-agentic-patterns](https://github.com/nibzard/awesome-agentic-patterns),
backed by public references (blog posts, talks, repos, papers).

## Purpose

Tutorials show toy demos; real products hide the messy bits. This catalogue surfaces
the repeatable patterns that bridge the gap. Use it to look up a proven pattern for
an agent design question, to diagnose an agent that misbehaves in production, or to
compare two candidate approaches. It is an index into `references/`, not an
implementation guide — it never tells you how to write the feature, only which
patterns are known to work there.

## What counts as a pattern

- **Repeatable** — more than one team is using it.
- **Agent-centric** — improves how an AI agent senses, reasons, or acts.
- **Traceable** — backed by a public reference.

## Prerequisites

- Nothing to install and no API keys or services to configure — this skill is
  documentation only.
- `references/INDEX.md` and the pattern files under `references/<category>/` must be
  readable; they hold the detail this index points to.
- Each pattern file carries frontmatter with `status`, `authors`, `source`, and
  `tags`. Read the file before recommending a pattern.

## Catalogue layout

| Category | Prefix under `references/` | Covers |
|----------|--------------------------|--------|
| Context & Memory | `context-memory/` | context budget, working memory, retrieval, compaction |
| Feedback Loops | `feedback-loops/` | self-critique, CI feedback, graders, rendered-UI gates |
| Learning & Adaptation | `learning-adaptation/` | fine-tuning, memory RL, skill-library growth |
| Orchestration & Control | `orchestration-control/` | decomposition, sub-agents, model routing, multi-agent topologies |
| Reliability & Eval | `reliability-eval/` | circuit breakers, fallback chains, observability, eval harnesses |
| Security & Safety | `security-safety/` | tool authorization, credential handling, egress control |
| Tool Use & Environment | `tool-use-environment/` | tool discovery, code-first tools, sandboxed execution |
| UX & Collaboration | `ux-collaboration/` | human approval gates, handoffs, team configuration |

`references/INDEX.md` lists all 194 files with pattern name and maturity label.
Read it to narrow the field, then read the individual pattern file.

## Instructions

1. **Identify the problem domain** — context management, orchestration, reliability,
   security, tool use, feedback, learning, or human collaboration.
2. **Use `references/INDEX.md` to find candidate patterns** in that domain rather
   than answering from memory.
3. **Read the full pattern file** (for example
   `references/orchestration-control/discrete-phase-separation.md`) before
   recommending it. It holds the implementation notes, trade-offs, and "when NOT to
   use" caveats the index omits.
4. **Quote the maturity label** (`best-practice`, `validated-in-production`,
   `established`, `emerging`, `proposed`, `experimental-but-awesome`) with every
   recommendation, and keep the reference path in your answer so the user can open it.
5. **Weigh trade-offs** — present pros and cons honestly, and add a "when NOT to
   use" note where the pattern has one.
6. **Combine patterns** only when the task genuinely needs it; see the table below.
7. **Check the evidence level** — `validated-in-production` and `best-practice` are
   safer defaults than `emerging` or `proposed`.

## Examples

**"What pattern handles fan-out/fan-in over a document set?"** Search
`orchestration-control/` and `tool-use-environment/` for parallel decomposition, then
add a verification pattern only if the fan-out result needs a merge check.

**"My agent repeats the same mistake every run."** Look at `feedback-loops/` and
`context-memory/` memory patterns, and check "When NOT to use patterns" before adding
a layer — if one call suffices, say so instead of bolting on an agent.

## Pattern combinations

Patterns often work best when combined:

| Combination | Why it works |
|-------------|--------------|
| **Circuit Breaker + Failover** | Circuit breaker detects failure, failover routes to backup |
| **Structured Output + Reflection** | Structured output enables reliable parsing, reflection improves quality |
| **Discrete Phase Separation + Sub-Agent Spawning** | Phase separation isolates concerns, sub-agents execute in parallel |
| **Context Minimization + Prompt Caching** | Minimize context to reduce tokens, cache prefixes to save costs |
| **Tool Use Steering + Code-Then-Execute** | Steering guides tool selection, code-then-execute keeps runs auditable |
| **Multi-Agent Brainstorming + Spec-As-Test** | Brainstorming generates ideas, spec-as-test validates them |
| **Agent Circuit Breaker + Adaptive Sandbox Fan-Out** | Circuit breaker prevents waste, fan-out scales parallel work |
| **Inversion of Control + Progressive Autonomy** | Let agents drive their workflow, gradually increase autonomy |

## When NOT to use patterns

Not every problem needs a pattern. Avoid over-engineering:

- **Simple, one-off tasks** — do not apply complex orchestration for a single file edit.
- **Deterministic workflows** — if the solution is purely rule-based, skip the LLM.
- **Tiny codebases** — context window limits do not matter for projects under ~100 files.
- **Real-time systems** — agent loops add latency; unsuitable for sub-second responses.

## Limitations

- Catalogue entries are summaries, not verified code. Treat implementation snippets
  in a pattern file as a starting point and test them before use.
- Maturity labels are editorial. `emerging` and `proposed` entries may describe one
  team's experience rather than a settled practice.
- Coverage is biased toward coding and research agents. Nothing here replaces a
  domain-specific safety or compliance review.
- The catalogue is a snapshot of the upstream repository and drifts from it over time.

## Troubleshooting

| Symptom | Likely cause | What to do |
|---------|--------------|------------|
| A recommended pattern contradicts another | The two were picked from different categories without reading either | Read both pattern files and surface the conflict instead of blending them |
| Recommendation looks generic | Answer came from this index alone | Read the full pattern file under `references/` before answering |
| Recommended pattern needs infrastructure the project lacks | Maturity label ignored | Prefer `validated-in-production`/`best-practice` entries, or state the extra setup cost |
| User wants a single-file fix | Over-engineering | Say that no pattern is needed and describe the direct fix |

## External resources

- **Website**: [agentic-patterns.com](https://agentic-patterns.com) — interactive
  pattern explorer, compare tool, decision guide, graph visualization
- **llms.txt**: [agentic-patterns.com/llms.txt](https://agentic-patterns.com/llms.txt) —
  machine-readable documentation for AI assistants
- **GitHub**: [nibzard/awesome-agentic-patterns](https://github.com/nibzard/awesome-agentic-patterns) —
  upstream catalogue this skill distills from

## Documentation structure

| Layer | Location | Purpose |
|-------|----------|---------|
| **Index** | This file | Category map, lookup procedure, combinations, limits |
| **Cross-reference** | `references/INDEX.md` | Every pattern file with name and maturity label |
| **Reference** | `references/<category>/<pattern>.md` | Pattern detail: problem, solution, implementation, trade-offs, evidence |
