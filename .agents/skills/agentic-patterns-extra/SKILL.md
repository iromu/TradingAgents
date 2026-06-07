---
name: agentic-patterns-extra
description: >-
  Curated catalogue of 140+ agentic AI patterns — real-world tricks, workflows,
  and mini-architectures that help autonomous or semi-autonomous AI agents get
  useful work done in production. Use this skill whenever the user asks about
  agent architecture, agent design patterns, how to solve an agent-related
  challenge, or wants recommendations for patterns. Trigger on questions about
  context management, multi-agent coordination, reliability, security, tool use,
  feedback loops, learning & adaptation, or human-agent collaboration. Also
  trigger when the user wants to compare patterns, find the right pattern for a
  use case, understand trade-offs between different agent approaches, or wants
  to know which patterns work best together. Don't hesitate to suggest patterns
  even when the user doesn't explicitly mention "patterns" — if they describe an
  agent problem, this skill likely applies.
---

# Agentic Patterns Extra

A curated catalogue of agentic AI patterns sourced
from [nibzard/awesome-agentic-patterns](https://github.com/nibzard/awesome-agentic-patterns), backed by public
references (blog posts, talks, repos, papers).

> **Why?** Tutorials show toy demos. Real products hide the messy bits. This catalogue surfaces the repeatable patterns
> that bridge the gap so you can ship smarter, faster agents.

## What counts as a pattern

- **Repeatable** — more than one team is using it.
- **Agent-centric** — improves how an AI agent senses, reasons, or acts.
- **Traceable** — backed by a public reference.

## Pattern catalog

### Context & Memory

Patterns for managing agent context, working memory, state persistence, and knowledge retrieval.

| Pattern                                                                                                      | Status                  | Description                                                                                                                                                                           |
|--------------------------------------------------------------------------------------------------------------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Agent-Powered Codebase Q&A / Onboarding](reference/context-memory/agent-powered-codebase-qa-onboarding.md)  | validated-in-production | Leverage AI agent with retrieval and QA capabilities to assist developers understanding codebases                                                                                     |
| [Context-Minimization Pattern](reference/context-memory/context-minimization-pattern.md)                     | emerging                | Purge or redact untrusted segments from context once they have served their purpose to prevent delayed prompt injection and reduce context bloat                                      |
| [Context Window Anxiety Management](reference/context-memory/context-window-anxiety-management.md)           | emerging                | Mitigate models that become aware of approaching context window limits and proactively summarize or rush task completion through buffer strategies and counter-prompting              |
| [Context Window Auto-Compaction](reference/context-memory/context-window-auto-compaction.md)                 | validated-in-production | Automatic session compaction triggered by context overflow errors, with smart reserve tokens and lane-aware retry to preserve essential information while staying within token limits |
| [Curated Code Context Window](reference/context-memory/curated-code-context-window.md)                       | best-practice           | Maintain minimal, high-signal code context using search subagent for code discovery                                                                                                   |
| [Curated File Context Window](reference/context-memory/curated-file-context-window.md)                       | best-practice           | Sterile curated "main" context window with helper sub-agents for file ranking                                                                                                         |
| [Dynamic Context Injection](reference/context-memory/dynamic-context-injection.md)                           | established             | File/folder at-mentions and custom slash commands for on-demand context loading                                                                                                       |
| [Episodic Memory Retrieval](reference/context-memory/episodic-memory-retrieval.md)                           | validated-in-production | Vector-backed episodic memory store with structured memory blobs                                                                                                                      |
| [Filesystem-Based Agent State](reference/context-memory/filesystem-based-agent-state.md)                     | validated-in-production | Persist intermediate results to files for durable checkpoints and workflow resumption                                                                                                 |
| [Layered Configuration Context](reference/context-memory/layered-configuration-context.md)                   | validated-in-production | Hierarchical CLAUDE.md files for enterprise, user, project, and local context layers                                                                                                  |
| [Memory Synthesis from Execution Logs](reference/context-memory/memory-synthesis-from-execution-logs.md)     | validated-in-production | Two-tier memory system: task diaries + periodic synthesis agents for pattern extraction                                                                                               |
| [Proactive Agent State Externalization](reference/context-memory/proactive-agent-state-externalization.md)   | emerging                | Structured self-documentation framework with hybrid memory architecture                                                                                                               |
| [Progressive Disclosure for Large Files](reference/context-memory/progressive-disclosure-large-files.md)     | validated-in-production | Load file metadata first, provide tools to load content on-demand                                                                                                                     |
| [Prompt Caching via Prefix Preservation](reference/context-memory/prompt-caching-via-prefix-preservation.md) | best-practice           | Preserve exact prefixes, append new messages, order static vs variable content                                                                                                        |
| [Schema-Guided Graph Retrieval](reference/context-memory/schema-guided-graph-retrieval.md)                   | emerging                | Shared domain schema aligns graph construction, query decomposition, and typed retrieval                                                                                              |
| [Self-Identity Accumulation](reference/context-memory/self-identity-accumulation.md)                         | emerging                | Dual-hook architecture for persistent agent identity/profile across sessions                                                                                                          |
| [Semantic Context Filtering](reference/context-memory/semantic-context-filtering.md)                         | emerging                | Extract semantic elements from raw data, filter out noise before LLM consumption                                                                                                      |
| [Session-Scoped Context Runtime](reference/context-memory/session-scoped-context-runtime.md)                 | emerging                | Context runtime that caches structured reads and normalizes tool output                                                                                                               |
| [Tool Search Lazy Loading](reference/context-memory/tool-search-lazy-loading.md)                             | emerging                | Dynamically load tools via search instead of preloading all available tools                                                                                                           |
| [Working Memory via TodoWrite](reference/context-memory/working-memory-via-todos.md)                         | emerging                | Externalize working memory with explicit task tracking and dependency management                                                                                                      |

### Feedback Loops

Patterns for self-improvement, evaluation, iterative refinement, and quality assurance.

| Pattern                                                                                                      | Status                  | Description                                                                          |
|--------------------------------------------------------------------------------------------------------------|-------------------------|--------------------------------------------------------------------------------------|
| [AI-Assisted Code Review](reference/feedback-loops/ai-assisted-code-review.md)                               | emerging                | Multi-agent code review where one agent generates while another critiques            |
| [Background Agent CI Feedback](reference/feedback-loops/background-agent-ci.md)                              | validated-in-production | Run agent asynchronously in background with CI as objective feedback channel         |
| [Coding Agent CI Feedback Loop](reference/feedback-loops/coding-agent-ci-feedback-loop.md)                   | best-practice           | Asynchronous coding agent against CI with partial feedback ingestion                 |
| [Dogfooding with Rapid Iteration](reference/feedback-loops/dogfooding-with-rapid-iteration.md)               | validated-in-production | Development team uses own agent product for daily tasks, rapid feedback loop         |
| [Graph of Thoughts](reference/feedback-loops/graph-of-thoughts.md)                                           | emerging                | Represent reasoning as a graph with branching, aggregation, and refinement           |
| [Incident-to-Eval Synthesis](reference/feedback-loops/incident-to-eval-synthesis.md)                         | emerging                | Convert production incidents into executable eval cases to prevent repeat failures   |
| [Inference-Healed Code Review Reward](reference/feedback-loops/inference-healed-code-review-reward.md)       | proposed                | Decompose code quality into subcriteria with CoT reasoning for explainable feedback  |
| [Iterative Prompt & Skill Refinement](reference/feedback-loops/iterative-prompt-skill-refinement.md)         | established             | Multi-pronged refinement strategy with responsive, owner-led, and dashboard feedback |
| [Reflection Loop](reference/feedback-loops/reflection.md)                                                    | established             | Self-evaluate output, feed critique into revision — repeat until threshold met       |
| [Rich Feedback Loops](reference/feedback-loops/rich-feedback-loops.md)                                       | validated-in-production | Invest in iterative machine-readable feedback infrastructure over prompt perfection  |
| [Self-Critique Evaluator Loop](reference/feedback-loops/self-critique-evaluator-loop.md)                     | emerging                | Agent critiques its own output before final delivery                                 |
| [Self-Discover: LLM Self-Composed Reasoning](reference/feedback-loops/self-discover-reasoning-structures.md) | emerging                | LLM self-composes its own reasoning structures for complex tasks                     |
| [Spec-As-Test Feedback Loop](reference/feedback-loops/spec-as-test-feedback-loop.md)                         | emerging                | Generate executable assertions from specs for continuous spec-code synchronization   |
| [Tool Use Incentivization](reference/feedback-loops/tool-use-incentivization.md)                             | proposed                | Shape rewards to incentivize effective tool use patterns                             |

### Learning & Adaptation

Patterns for long-term agent improvement, fine-tuning, skill evolution, and knowledge accumulation.

| Pattern                                                                                                         | Status                  | Description                                                                                 |
|-----------------------------------------------------------------------------------------------------------------|-------------------------|---------------------------------------------------------------------------------------------|
| [Agent Reinforcement Fine-Tuning (Agent RFT)](reference/learning-adaptation/agent-reinforcement-fine-tuning.md) | validated-in-production | Train model weights end-to-end on agentic tasks with real tool calls and custom rewards     |
| [Compounding Engineering Pattern](reference/learning-adaptation/compounding-engineering-pattern.md)             | emerging                | Codify all learnings from each feature into reusable prompts, slash commands, hooks         |
| [Frontier-Focused Development](reference/learning-adaptation/frontier-focused-development.md)                   | emerging                | Always target the state-of-the-art models, design products that evolve as frontier moves    |
| [Memory Reinforcement Learning (MemRL)](reference/learning-adaptation/memory-reinforcement-learning.md)         | emerging                | Transfer RL from parameter space to context space, rank memories by learned utility         |
| [Shipping as Research](reference/learning-adaptation/shipping-as-research.md)                                   | emerging                | Treat shipping as research — release features to learn, not because you're certain          |
| [Skill Library Evolution](reference/learning-adaptation/skill-library-evolution.md)                             | validated-in-production | Agents persist working code as reusable skills that evolve into documented capabilities     |
| [Variance-Based RL Sample Selection](reference/learning-adaptation/variance-based-rl-sample-selection.md)       | validated-in-production | Run multiple baseline evaluations per sample, prioritize high-variance samples for training |

### Orchestration & Control

Patterns for task decomposition, multi-agent coordination, workflow management, and planning.

| Pattern                                                                                     | Status                   | Description                                                                        |
|---------------------------------------------------------------------------------------------|--------------------------|------------------------------------------------------------------------------------|
| [Budget-Aware Model Routing](reference/orchestration-control/budget-aware-model-routing.md) | established              | Route requests to tiered models with explicit budget contracts and hard caps       |
| [Capability-Escrow-Receipt](reference/orchestration-control/capability-escrow-receipt.md)   | experimental-but-awesome | Atomic three-object loop for agent-to-agent commerce (capability, escrow, receipt) |
| [Discrete Phase Separation](reference/orchestration-control/discrete-phase-separation.md)   | emerging                 | Isolate research, planning, and execution into separate conversations              |
| [Dual LLM Pattern](reference/orchestration-control/dual-llm-pattern.md)                     | emerging                 | Split privileged and quarantined LLM roles for clear trust boundaries              |
| [Factory over Assistant](reference/orchestration-control/factory-over-assistant.md)         | validated-in-production  | Spawn multiple autonomous agents in parallel instead of watching one in a sidebar  |
| [Sub-Agent Spawning](reference/orchestration-control/sub-agent-spawning.md)                 | validated-in-production  | Spawn focused sub-agents with isolated contexts for parallel execution             |
| [Swarm Migration Pattern](reference/orchestration-control/swarm-migration-pattern.md)       | validated-in-production  | 10+ parallel sub-agents for large-scale code migrations (10x+ speedup)             |
| [Tree-of-Thought Reasoning](reference/orchestration-control/tree-of-thought-reasoning.md)   | established              | Explore a search tree of intermediate thoughts with branching and pruning          |

### Reliability & Eval

Patterns for ensuring agent reliability, evaluation, fault tolerance, and observability.

| Pattern                                                                                          | Status      | Description                                                                           |
|--------------------------------------------------------------------------------------------------|-------------|---------------------------------------------------------------------------------------|
| [Agent Circuit Breaker](reference/reliability-eval/agent-circuit-breaker.md)                     | emerging    | Prevent token waste on repeatedly failing tools using a circuit breaker state machine |
| [Output Verification Loop](reference/reliability-eval/output-verification-loop.md)               | emerging    | Verify outputs against expected structure before accepting results                    |
| [Structured Output Specification](reference/reliability-eval/structured-output-specification.md) | established | Constrain outputs using deterministic schemas for reliable validation                 |

### Security & Safety

Patterns for securing agent systems, protecting data, and ensuring safe operation.

| Pattern                                                                                     | Status      | Description                                                               |
|---------------------------------------------------------------------------------------------|-------------|---------------------------------------------------------------------------|
| [Hook-Based Safety Guard Rails](reference/security-safety/hook-based-safety-guard-rails.md) | emerging    | Enforce safety constraints by intercepting agent actions before execution |
| [PII Tokenization](reference/security-safety/pii-tokenization.md)                           | established | Tokenize PII before it reaches the model, untokenize for tool calls       |

### Tool Use & Environment

Patterns for tool discovery, execution, environment management, and integration.

| Pattern                                                                                              | Status        | Description                                                                     |
|------------------------------------------------------------------------------------------------------|---------------|---------------------------------------------------------------------------------|
| [Code-Then-Execute Pattern](reference/tool-use-environment/code-then-execute.md)                     | best-practice | Generate code first, then execute — never execute without generating code first |
| [Tool Use Steering via Prompting](reference/tool-use-environment/tool-use-steering-via-prompting.md) | best-practice | Guide agent tool selection through explicit natural language instructions       |

### UX & Collaboration

Patterns for human-agent collaboration, handoffs, workflow design, and communication.

| Pattern                                                                                                | Status                  | Description                                                         |
|--------------------------------------------------------------------------------------------------------|-------------------------|---------------------------------------------------------------------|
| [Agent-Friendly Workflow Design](reference/ux-collaboration/agent-friendly-workflow-design.md)         | best-practice           | Design workflows that align with agent capabilities and limitations |
| [Human-in-the-Loop Approval Framework](reference/ux-collaboration/human-in-loop-approval-framework.md) | validated-in-production | Insert human approval gates for high-risk agent actions             |

## How to use this skill

When the user asks about agentic patterns, agent architecture, or how to solve a specific agent-related challenge:

1. **Identify the problem domain** — context management, reliability, security, orchestration, tool use, feedback,
   learning, or human collaboration.
2. **Match to relevant patterns** — read the pattern files in `reference/` for detailed guidance.
3. **Consider trade-offs** — every pattern has pros and cons; present them honestly.
4. **Suggest combinations** — patterns often work best when combined (e.g., Structured Output + Reflection Loop).
5. **Check evidence level** — note which patterns are `validated-in-production` vs `emerging` vs `experimental`.

## Pattern metadata

Each reference file includes:

- **title** — Pattern name
- **status** — Maturity: `best-practice`, `validated-in-production`, `established`, `emerging`, `proposed`,
  `experimental-but-awesome`
- **authors** — Contributors and originators
- **source** — Primary reference URL
- **tags** — Searchable keywords
- **Problem** — What challenge does it solve?
- **Solution** — How does it solve it?
- **Evidence** — Evidence grade and key findings
- **How to use it** — Practical implementation guidance
- **Trade-offs** — Pros and cons
- **References** — Additional resources

## External resources

- **Website**: [https://agentic-patterns.com](https://agentic-patterns.com) — Interactive pattern explorer, compare
  tool, decision guide, graph visualization
- **llms.txt**: [https://agentic-patterns.com/llms.txt](https://agentic-patterns.com/llms.txt) — Machine-readable
  documentation for AI assistants
- **GitHub**: [nibzard/awesome-agentic-patterns](https://github.com/nibzard/awesome-agentic-patterns) — Full source with
  140+ patterns
