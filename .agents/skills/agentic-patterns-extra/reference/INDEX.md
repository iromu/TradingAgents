# Agentic Patterns Extra — Reference Index

This directory contains detailed reference documentation for agentic AI patterns, sourced
from [nibzard/awesome-agentic-patterns](https://github.com/nibzard/awesome-agentic-patterns).

## Directory structure

| Directory                | Category                | Description                                                                    |
|--------------------------|-------------------------|--------------------------------------------------------------------------------|
| `context-memory/`        | Context & Memory        | Patterns for managing agent context, working memory, and state                 |
| `feedback-loops/`        | Feedback Loops          | Patterns for self-improvement, evaluation, and iterative refinement            |
| `learning-adaptation/`   | Learning & Adaptation   | Patterns for agent evolution, fine-tuning, and skill growth                    |
| `orchestration-control/` | Orchestration & Control | Patterns for task decomposition, multi-agent coordination, workflow management |
| `reliability-eval/`      | Reliability & Eval      | Patterns for ensuring agent reliability, evaluation, and fault tolerance       |
| `security-safety/`       | Security & Safety       | Patterns for securing agent systems, protecting data, ensuring safe operation  |
| `tool-use-environment/`  | Tool Use & Environment  | Patterns for tool discovery, execution, and environment management             |
| `ux-collaboration/`      | UX & Collaboration      | Patterns for human-agent collaboration, handoffs, and workflow design          |

## Pattern files

### Context & Memory

| File                                                       | Pattern                                      | Status                  |
|------------------------------------------------------------|----------------------------------------------|-------------------------|
| `context-memory/agent-powered-codebase-qa-onboarding.md`   | Agent-Powered Codebase Q&A / Onboarding      | validated-in-production |
| `context-memory/context-minimization-pattern.md`           | Context-Minimization Pattern                 | emerging                |
| `context-memory/context-window-anxiety-management.md`      | Context Window Anxiety Management            | emerging                |
| `context-memory/context-window-auto-compaction.md`         | Context Window Auto-Compaction               | validated-in-production |
| `context-memory/curated-code-context-window.md`            | Curated Code Context Window                  | best-practice           |
| `context-memory/curated-file-context-window.md`            | Curated File Context Window                  | best-practice           |
| `context-memory/dynamic-context-injection.md`              | Dynamic Context Injection                    | established             |
| `context-memory/episodic-memory-retrieval.md`              | Episodic Memory Retrieval & Injection        | validated-in-production |
| `context-memory/filesystem-based-agent-state.md`           | Filesystem-Based Agent State                 | validated-in-production |
| `context-memory/layered-configuration-context.md`          | Layered Configuration Context                | validated-in-production |
| `context-memory/memory-synthesis-from-execution-logs.md`   | Memory Synthesis from Execution Logs         | validated-in-production |
| `context-memory/prompt-caching-via-prefix-preservation.md` | Prompt Caching via Exact Prefix Preservation | best-practice           |
| `context-memory/progressive-disclosure-large-files.md`     | Progressive Disclosure for Large Files       | validated-in-production |
| `context-memory/proactive-agent-state-externalization.md`  | Proactive Agent State Externalization        | emerging                |
| `context-memory/schema-guided-graph-retrieval.md`          | Schema-Guided Graph Retrieval                | emerging                |
| `context-memory/self-identity-accumulation.md`             | Self-Identity Accumulation                   | emerging                |
| `context-memory/semantic-context-filtering.md`             | Semantic Context Filtering                   | emerging                |
| `context-memory/session-scoped-context-runtime.md`         | Session-Scoped Context Runtime               | emerging                |
| `context-memory/tool-search-lazy-loading.md`               | Tool Search Lazy Loading                     | emerging                |
| `context-memory/working-memory-via-todos.md`               | Working Memory via TodoWrite                 | emerging                |

### Feedback Loops

| File                                                    | Pattern                                     | Status                  |
|---------------------------------------------------------|---------------------------------------------|-------------------------|
| `feedback-loops/ai-assisted-code-review.md`             | AI-Assisted Code Review / Verification      | emerging                |
| `feedback-loops/background-agent-ci.md`                 | Background Agent with CI Feedback           | validated-in-production |
| `feedback-loops/coding-agent-ci-feedback-loop.md`       | Coding Agent CI Feedback Loop               | best-practice           |
| `feedback-loops/dogfooding-with-rapid-iteration.md`     | Dogfooding with Rapid Iteration             | validated-in-production |
| `feedback-loops/graph-of-thoughts.md`                   | Graph of Thoughts (GoT)                     | emerging                |
| `feedback-loops/incident-to-eval-synthesis.md`          | Incident-to-Eval Synthesis                  | emerging                |
| `feedback-loops/inference-healed-code-review-reward.md` | Inference-Healed Code Review Reward         | proposed                |
| `feedback-loops/iterative-prompt-skill-refinement.md`   | Iterative Prompt & Skill Refinement         | established             |
| `feedback-loops/rich-feedback-loops.md`                 | Rich Feedback Loops > Perfect Prompts       | validated-in-production |
| `feedback-loops/reflection.md`                          | Reflection Loop                             | established             |
| `feedback-loops/self-critique-evaluator-loop.md`        | Self-Critique Evaluator Loop                | emerging                |
| `feedback-loops/self-discover-reasoning-structures.md`  | Self-Discover: LLM Self-Composed Reasoning  | emerging                |
| `feedback-loops/spec-as-test-feedback-loop.md`          | Spec-As-Test Feedback Loop                  | emerging                |
| `feedback-loops/tool-use-incentivization.md`            | Tool Use Incentivization via Reward Shaping | proposed                |

### Learning & Adaptation

| File                                                        | Pattern                                     | Status                  |
|-------------------------------------------------------------|---------------------------------------------|-------------------------|
| `learning-adaptation/agent-reinforcement-fine-tuning.md`    | Agent Reinforcement Fine-Tuning (Agent RFT) | validated-in-production |
| `learning-adaptation/compounding-engineering-pattern.md`    | Compounding Engineering Pattern             | emerging                |
| `learning-adaptation/frontier-focused-development.md`       | Frontier-Focused Development                | emerging                |
| `learning-adaptation/memory-reinforcement-learning.md`      | Memory Reinforcement Learning (MemRL)       | emerging                |
| `learning-adaptation/shipping-as-research.md`               | Shipping as Research                        | emerging                |
| `learning-adaptation/skill-library-evolution.md`            | Skill Library Evolution                     | emerging                |
| `learning-adaptation/variance-based-rl-sample-selection.md` | Variance-Based RL Sample Selection          | validated-in-production |

### Orchestration & Control

| File                                                              | Pattern                                        | Status                   |
|-------------------------------------------------------------------|------------------------------------------------|--------------------------|
| `orchestration-control/agent-driven-research.md`                  | Agent-Driven Research                          | established              |
| `orchestration-control/autonomous-workflow-agent-architecture.md` | Autonomous Workflow Agent Architecture         | established              |
| `orchestration-control/budget-aware-model-routing.md`             | Budget-Aware Model Routing with Hard Cost Caps | established              |
| `orchestration-control/capability-escrow-receipt.md`              | Capability-Escrow-Receipt                      | experimental-but-awesome |
| `orchestration-control/discrete-phase-separation.md`              | Discrete Phase Separation                      | emerging                 |
| `orchestration-control/dual-llm-pattern.md`                       | Dual LLM Pattern                               | emerging                 |
| `orchestration-control/factory-over-assistant.md`                 | Factory over Assistant                         | validated-in-production  |
| `orchestration-control/plan-then-execute.md`                      | Plan-Then-Execute Pattern                      | best-practice            |
| `orchestration-control/planner-worker-separation.md`              | Planner-Worker Separation                      | emerging                 |
| `orchestration-control/sub-agent-spawning.md`                     | Sub-Agent Spawning                             | validated-in-production  |
| `orchestration-control/swarm-migration-pattern.md`                | Swarm Migration Pattern                        | validated-in-production  |
| `orchestration-control/tree-of-thought-reasoning.md`              | Tree-of-Thought Reasoning                      | established              |

### Reliability & Eval

| File                                                  | Pattern                         | Status      |
|-------------------------------------------------------|---------------------------------|-------------|
| `reliability-eval/agent-circuit-breaker.md`           | Agent Circuit Breaker           | emerging    |
| `reliability-eval/output-verification-loop.md`        | Output Verification Loop        | emerging    |
| `reliability-eval/structured-output-specification.md` | Structured Output Specification | established |

### Security & Safety

| File                                               | Pattern                       | Status      |
|----------------------------------------------------|-------------------------------|-------------|
| `security-safety/hook-based-safety-guard-rails.md` | Hook-Based Safety Guard Rails | emerging    |
| `security-safety/pii-tokenization.md`              | PII Tokenization              | established |

### Tool Use & Environment

| File                                                      | Pattern                         | Status        |
|-----------------------------------------------------------|---------------------------------|---------------|
| `tool-use-environment/code-then-execute.md`               | Code-Then-Execute Pattern       | best-practice |
| `tool-use-environment/tool-use-steering-via-prompting.md` | Tool Use Steering via Prompting | best-practice |

### UX & Collaboration

| File                                                   | Pattern                              | Status                  |
|--------------------------------------------------------|--------------------------------------|-------------------------|
| `ux-collaboration/agent-friendly-workflow-design.md`   | Agent-Friendly Workflow Design       | best-practice           |
| `ux-collaboration/human-in-loop-approval-framework.md` | Human-in-the-Loop Approval Framework | validated-in-production |

## Maturity levels

| Status                     | Meaning                                              |
|----------------------------|------------------------------------------------------|
| `best-practice`            | Widely adopted, well-understood, recommended default |
| `validated-in-production`  | Used successfully in production systems              |
| `established`              | Academic or industry backing, proven effective       |
| `emerging`                 | Gaining traction, evidence growing                   |
| `experimental-but-awesome` | Novel, promising, worth watching                     |
| `proposed`                 | Conceptual, needs validation                         |

## How to extend

To add a new pattern:

1. Create a new `.md` file in the appropriate category directory
2. Use the frontmatter template from the source
   repository's [TEMPLATE.md](https://raw.githubusercontent.com/nibzard/awesome-agentic-patterns/main/patterns/TEMPLATE.md)
3. Include: title, status, authors, category, source, tags, summary, problem, solution, how to use, trade-offs,
   references
4. Update this INDEX.md with the new entry
