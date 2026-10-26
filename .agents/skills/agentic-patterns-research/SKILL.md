---
name: agentic-patterns-research
description: >
  Comprehensive reference for agentic AI design patterns, research-backed
  techniques, and production-proven architectures. Use this skill whenever
  building, designing, or evaluating AI agents — including agent architecture,
  context management, planning/reasoning, tooling, safety, CI/CD integration,
  cost optimization, and workflow design. Triggers on mentions of agent
  patterns, agentic systems, agent architecture, prompt injection defense,
  context window management, model routing, multi-agent orchestration,
  autonomous agents, or when designing AI agent systems. Also triggers when
  the user asks about best practices for building reliable, safe, or
  cost-effective agents.
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 1.0.0
---

# Agentic Patterns Research

A research-backed collection of 220 deep-dive research reports on agentic design
patterns, each covering academic foundations, industry implementations, and practical
guidance. The 25 patterns with the strongest production evidence are featured in the
category tables below; the rest are reachable through the category overviews and the
full index.

## When to Use This Skill

- Designing an agent architecture from scratch or refactoring an existing one
- Evaluating trade-offs between different agent patterns
- Implementing safety controls (prompt injection defense, egress lockdown, guardrails)
- Optimizing agent costs (model routing, context minimization, code-over-API)
- Integrating agents into CI/CD pipelines
- Building autonomous agent workflows (research loops, task execution)
- Understanding how production systems (Claude Code, GitHub Copilot, Cursor) implement these patterns

## When NOT to Use This Skill

This skill holds 220 long-form research reports. It is the wrong tool when you
need breadth or speed rather than depth:

- **Browsing or comparing many patterns** — the sibling `agentic-patterns-extra` skill
  catalogues 194 patterns across the same 8 categories with one-line summaries; reach for
  it first when the question is "which pattern fits this situation?" rather than
  "what does the evidence say about this one?"
- **The canonical core pattern set** — prompt chaining, routing, parallelization,
  reflection, tool use, planning and multi-agent collaboration belong to
  `agentic-patterns-core`; this skill's reports assume those are already settled.
- **A quick definitional answer** — each reference here is a long-form report with academic
  sources and production evidence; reading one to settle a one-line question is wasted effort.
- **Non-agentic work** — plain application code, infrastructure, or model-training questions
  with no agent-architecture content should not trigger this skill at all.
- **When the pattern is not listed below** — do not extrapolate a report for a pattern that
  has no row here. Say it is not covered and point at `agentic-patterns-extra` instead of
  inventing findings.

## Pattern Categories

The 25 best-evidenced patterns are organised into 8 categories below. Read the reference
file for the specific pattern you need; the remaining 195 reports are listed in
`references/INDEX.md`.

### 1. Context Management

Patterns for managing the agent's context window efficiently and securely.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Context Minimization** | `references/context-minimization-pattern-report.md` | Validated at Anthropic & OpenAI |
| **Episodic Memory Retrieval** | `references/episodic-memory-retrieval-injection-report.md` | Best practice |
| **Context Window Anxiety** | `references/context-window-anxiety-management-report.md` | Emerging |

**Key insight:** Context minimization eliminates delayed prompt-injection attacks
while reducing token consumption 10-100x. Treat context as a staged pipeline:
ingest untrusted text → transform → aggressively discard.

### 2. Planning & Reasoning

Patterns for how agents reason, plan, and make decisions.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Graph of Thoughts** | `references/graph-of-thoughts-report.md` | Best practice |
| **Posterior-Sampling Planner** | `references/explicit-posterior-sampling-planner-report.md` | Emerging |
| **CoT Monitoring & Interruption** | `references/chain-of-thought-monitoring-interruption-report.md` | Best practice |

**Key insight:** Graph of Thoughts generalizes Chain-of-Thought and Tree-of-Thoughts,
allowing agents to explore multiple reasoning paths, aggregate insights, and refine
their approach iteratively.

### 3. Agent Architecture

Patterns for structuring and orchestrating autonomous agents.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Agent-Driven Research** | `references/agent-driven-research-report.md` | Best practice |
| **Codebase QA & Onboarding** | `references/agent-powered-codebase-qa-onboarding-report.md` | Validated-in-production |
| **Continuous Autonomous Task Loop** | `references/continuous-autonomous-task-loop-pattern-report.md` | Best practice |

**Key insight:** Agent-driven research maintains awareness of what has been covered,
identifies gaps, determines next exploration directions autonomously, and iterates
until sufficient information is gathered — unlike traditional RAG.

### 4. Code & Tooling

Patterns for how agents interact with code and tools effectively.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Agent-First Tooling & Logging** | `references/agent-first-tooling-and-logging-report.md` | Best practice |
| **Code-Over-API** | `references/code-over-api-pattern-report.md` | Established |
| **CLI-First Skill Design** | `references/cli-first-skill-design-report.md` | Best practice |

**Key insight:** Code-over-API achieves 75-2000x token reduction by having agents
write code instead of making direct API calls. Validated at Anthropic, Cloudflare,
and Cognition.

### 5. Safety & Security

Patterns for protecting agents and their outputs.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Egress Lockdown** | `references/egress-lockdown-no-exfiltration-channel-report.md` | Established |
| **Hook-Based Safety Guardrails** | `references/hook-based-safety-guard-rails-report.md` | Best practice |
| **Anti-Reward-Hacking Grader** | `references/anti-reward-hacking-grader-design-report.md` | Best practice |

**Key insight:** Hook-based guardrails run safety checks *outside* the agent's
reasoning loop (PreToolUse/PostToolUse), preventing the agent from rationalizing
past its own safety constraints.

### 6. CI/CD & Testing

Patterns for integrating agents into development workflows.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Coding Agent CI Feedback Loop** | `references/coding-agent-ci-feedback-loop-report.md` | Best practice |
| **Background Agent CI** | `references/background-agent-ci-report.md` | Validated-in-production |

**Key insight:** Branch-per-task isolation with CI log ingestion and retry budgets
enables agents to iterate on code changes autonomously while maintaining reliable
feedback channels.

### 7. Resource Management

Patterns for managing agent costs and model selection.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Budget-Aware Model Routing** | `references/budget-aware-model-routing-with-hard-cost-caps-report.md` | Best practice |
| **Agent Reinforcement Fine-Tuning** | `references/agent-reinforcement-fine-tuning-report.md` | Emerging |

**Key insight:** Hard cost caps with SLA-aware routing (FrugalGST, RouteLLM,
xRouter) enable agents to balance quality and cost dynamically, automatically
selecting the cheapest model that meets quality thresholds.

### 8. Workflow Design

Patterns for designing effective agent workflows and team structures.

| Pattern | Reference File | Status |
|---------|---------------|--------|
| **Agent-Friendly Workflow Design** | `references/agent-friendly-workflow-design-report.md` | Best practice |
| **Factory Over Assistant** | `references/factory-over-assistant-report.md` | Emerging |
| **Burn the Boats** | `references/burn-the-boats-report.md` | Best practice |
| **Compounding Engineering** | `references/compounding-engineering-pattern-report.md` | Emerging |
| **Disposable Scaffolding** | `references/disposable-scaffolding-over-durable-features-report.md` | Best practice |
| **Dual-LLM Pattern** | `references/dual-llm-pattern-report.md` | Best practice |

**Key insight:** The Factory-over-Assistant paradigm moves from sidebar-based agents
to autonomous parallel spawning — agents spawn sub-agents with specific roles,
enabling true multi-agent collaboration (OpenDevin, AutoGen, MetaGPT).

## Category Overviews

Before drilling into a single report, orient with the category overview that covers it.
These are cross-pattern taxonomies: they name every pattern in the category, rank the
options, and show which patterns compose. Start with the master taxonomy when the
category itself is unclear.

| Overview | Covers | Use it for |
|----------|--------|-----------|
| `references/01-pattern-taxonomy.md` | Master taxonomy across all categories | Seeing the whole pattern space, cross-category relationships, and a composition guide |
| `references/01-context-memory-patterns.md` | Context & Memory | Choosing between context minimization, window-anxiety management and episodic retrieval |
| `references/01-reasoning-patterns.md` | Reasoning | The pattern hierarchy and a performance comparison across the reasoning options |
| `references/02-reasoning-patterns-detailed.md` | Reasoning | Deep dives on Graph of Thoughts, LATS and ReAct |
| `references/01-orchestration-patterns.md` | Orchestration | Core orchestration options plus a decision framework and combination guidance |
| `references/02-orchestration-patterns-detailed.md` | Orchestration | Technical deep dives on sub-agent spawning and autonomous workflow architectures |
| `references/01-security-patterns.md` | Security & Safety | Comparing the action-selector, hook-based guard rail and sandboxed authorization approaches |
| `references/01-reliability-patterns.md` | Reliability & Evaluation | Output verification, action caching, circuit breaking and schema-validation retry |
| `references/01-tool-use-patterns.md` | Tool Use & Environment | Agent-first tooling, code-first tool interfaces and code-then-execute |
| `references/01-feedback-loop-patterns.md` | Feedback Loops | Surveying reflection, rich feedback, self-critique and RLAIF |
| `references/02-feedback-loop-patterns-detailed.md` | Feedback Loops | The detailed analysis accompanying the survey above |
| `references/01-learning-patterns.md` | Learning & Adaptation | Agent RFT, memory reinforcement learning and skill library evolution |
| `references/01-ux-collaboration-patterns.md` | UX & Collaboration | Human-in-the-loop approval, the spectrum of control, and verbose reasoning transparency |

## The Full Corpus

Beyond the 25 patterns tabled above, `references/` carries the complete upstream research
corpus: 220 reports plus the 13 category overviews. Each is a verbatim copy of its
upstream document, opening with a `<!-- Source: … -->` comment that names the exact path
it came from.

`references/INDEX.md` is the complete registry — every file with its document title and
length. Consult it before recommending a report: if a pattern has no row there, it is not
covered by this skill, and the right answer is `agentic-patterns-extra` rather than a
report that does not exist.

## Instructions

### Quick Start: Choosing a Pattern

1. Identify the problem you're solving (context bloat, security, cost, etc.)
2. Find the matching category above
3. Read the core reference file for that pattern
4. Apply the pattern's guidance to your agent design

### Reading Reference Files

Each reference file follows a consistent structure:
- **Executive Summary** — Key findings and production status
- **Problem Statement** — What issue the pattern solves
- **Solution** — How the pattern works (with code examples)
- **Academic Sources** — Research foundation
- **Industry Implementations** — Real-world examples
- **Trade-offs** — When NOT to use the pattern
- **Related Patterns** — How it connects to other patterns

### Common Pattern Combinations

These combinations work well together in production:

1. **Context Minimization + Egress Lockdown + Hook-Based Guardrails**
   → Full security stack (defense-in-depth for agents)

2. **Code-Over-API + Budget-Aware Routing + Context Minimization**
   → Maximum cost efficiency (75-2000x token reduction)

3. **Factory Over Assistant + Continuous Task Loop + CI Feedback**
   → Autonomous multi-agent development pipeline

4. **Dual-LLM (Privileged/Quarantined) + Context Minimization**
   → Prompt injection defense for sensitive operations

5. **Agent-Driven Research + Graph of Thoughts + Episodic Memory**
   → Deep, iterative research with memory retention

## Examples

**"Find every published pattern for multi-agent debate."** "The Full Corpus" -> sweep the Advanced/coordination category and cite the matching entries; "Category Overviews" gives the decision axes for choosing between them.

**"Is there a proven way to evaluate agent output?"** Optimization category -> evaluation and guardrails entries; "Production Validation" separates what has been shown in production from what only exists on paper.

## Production Validation

Several patterns have been validated-in-production at major companies:

| Pattern | Companies | Evidence |
|---------|-----------|----------|
| Context Minimization | Anthropic (Claude Code), OpenAI (Codex) | Documented 10-100x token reduction |
| Code-Over-API | Anthropic, Cloudflare, Cognition | Documented 75-2000x token reduction |
| Dual-LLM | OpenAI, Anthropic, Sourcegraph | 30-50% bug reduction, 90% cost reduction |
| CI Feedback Loop | GitHub Agentic Workflows, Cursor, OpenHands | Production deployment |
| Egress Lockdown | Microsoft, GitHub, GitLab, AWS, Azure, GCP | Vendor implementations |

## Source Attribution

All patterns are researched and documented from the
[awesome-agentic-patterns](https://github.com/nibzard/awesome-agentic-patterns)
repository, which compiles academic research (arXiv), industry sources, and
production implementations. Each reference file includes source attribution
and links to the original research papers.
