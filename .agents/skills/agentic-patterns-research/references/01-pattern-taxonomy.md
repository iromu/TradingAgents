# Agentic Patterns - Master Taxonomy & Relationships

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Complete Pattern Taxonomy

### Pattern Hierarchy

```
                    Agentic Patterns
                           |
          +----------------+----------------+-------------------+
          |
          |                |                |                    |
    Reasoning       Orchestration      Security          Tool Use
          |                |                |                    |
    +-------+-------+  +-------+-------+  +-------+-------+  +-------+-------+
    |       |       |  |       |       |  |       |       |  |       |       |
   CoT    ToT     GoT  PlanExec  Spawn  DualLLM  ActSel  Hooks   Code-First  MCP
    |       |       |  |       |       |       |       |       |       |
   ReAct  LATS   Reflex  Factory  Swarm  Context  PII     Egress  Agent-First
```

## Cross-Category Pattern Relationships

### Reasoning → Orchestration

| Reasoning Pattern | Orchestrates With | Relationship |
|-------------------|-------------------|--------------|
| **Graph of Thoughts** | Sub-Agent Spawning | GoT aggregation combines results from parallel sub-agents |
| **LATS** | Plan-Then-Execute | LATS tree search generates plans, Plan-Then-Execute runs them |
| **Reflection Loop** | Autonomous Workflow | Reflection provides quality control in long-running workflows |
| **ReAct** | Sub-Agent Spawning | ReAct TAO loop can be implemented within each spawned agent |

### Orchestration → Security

| Orchestration Pattern | Security Pattern | Relationship |
|----------------------|------------------|--------------|
| **Sub-Agent Spawning** | Action-Selector | Each sub-agent uses action selector for safe tool access |
| **Dual LLM** | Dual LLM (security) | Quarantined LLM processes untrusted data; privileged LLM executes |
| **Planner-Worker** | Hook-Based Guards | Workers execute with hook-based safety rails |
| **Factory** | Sandboxed Authorization | Each factory agent runs in sandboxed environment |

### Security → Tool Use

| Security Pattern | Tool Use Pattern | Relationship |
|-----------------|------------------|--------------|
| **Action-Selector** | Agent-First Tooling | Action selector requires well-defined tool schemas |
| **PII Tokenization** | Code-First Tool Interface | Code-first tools can include PII handling |
| **Egress Lockdown** | Unified Tool Gateway | Gateway enforces egress policies |
| **Zero-Trust Mesh** | Agent SDK | SDK provides authentication primitives |

### Tool Use → Context & Memory

| Tool Use Pattern | Context Pattern | Relationship |
|-----------------|-----------------|--------------|
| **Progressive Tool Discovery** | Context-Minimization | Discover tools on demand to minimize context |
| **Agent-First Tooling** | Structured Output | Tool outputs are structured, reducing context noise |
| **MCP Pattern Injection** | Dynamic Context Injection | MCP tools provide dynamic context |

## Pattern Composition Guide

### Common Pattern Stacks

#### Stack 1: Secure Coding Agent
```
Action-Selector (security)
    + Plan-Then-Execute (orchestration)
    + Reflection Loop (quality)
    + Hook-Based Safety (runtime security)
    + Structured Output (reliability)
```

#### Stack 2: Research Agent
```
Agent-Driven Research (core)
    + Graph of Thoughts (reasoning)
    + Reflection Loop (self-improvement)
    + Episodic Memory (learning)
    + AI Web Search (tool use)
```

#### Stack 3: Large Codebase Migration
```
Sub-Agent Spawning (orchestration)
    + Factory over Assistant (scale)
    + Lane-Based Queueing (throughput)
    + Subject Hygiene (traceability)
    + Git Worktree Isolation (safety)
```

#### Stack 4: Code Review Agent
```
Reflection Loop (self-critique)
    + AI-Assisted Code Review (verification)
    + Rich Feedback Loops (environmental signals)
    + CriticGPT-Style Evaluation (specialized critique)
```

#### Stack 5: Production Deployment
```
Action-Selector (security)
    + Agent Circuit Breaker (reliability)
    + Structured Output (predictability)
    + LLM Observability (monitoring)
    + Failover-Aware Model Fallback (availability)
```

#### Stack 6: Autonomous Workflow
```
Autonomous Workflow Architecture (core)
    + Checkpointing (recovery)
    + Error Recovery (resilience)
    + Reflection Loop (quality)
    + Filesystem State (persistence)
```

#### Stack 7: Cost-Optimized Agent
```
Context-Minimization (token reduction)
    + Action-Selector (no feedback loop)
    + Deterministic Reduce (no LLM in reduce)
    + Prompt Caching (repeatable operations)
```

## Decision Framework

### Choosing Reasoning Patterns

```
Task Complexity?
├── Simple → CoT or ReAct
├── Medium → ToT or Reflection Loop
└── Complex → GoT or LATS

Need multiple solution paths?
├── Yes → ToT or GoT
└── No → CoT or ReAct

Need insight synthesis?
├── Yes → GoT (Aggregate operation)
└── No → ToT or CoT

Budget constrained?
├── Yes → CoT or ReAct
└── No → GoT or LATS
```

### Choosing Orchestration Patterns

```
Task independence?
├── Independent → Sub-Agent Spawning (parallel)
├── Dependent → Plan-Then-Execute (sequential)
└── Mixed → Autonomous Workflow Architecture

Need specialization?
├── Yes → Sub-Agent Spawning (different tools per agent)
└── No → Single agent with tool selection

Scale needed?
├── 2-4 agents → Virtual File Isolation
├── 10-100 agents → Git Worktree Isolation
└── 100+ agents → Cloud Worker Isolation
```

### Choosing Security Patterns

```
Untrusted input?
├── Yes → Action-Selector (prevent injection)
└── No → Standard tool calling

Need runtime protection?
├── Yes → Hook-Based Safety Guard Rails
└── No → Static allowlists

Sensitive data?
├── Yes → PII Tokenization + Egress Lockdown
└── No → Standard access control
```

## Pattern Evolution Timeline

```
2022: ReAct (Thought → Action → Observation)
       ↓
2023: CoT, ToT, Reflexion, Self-Refine
       ↓
2023-2024: GoT, LATS, Self-Discover
       ↓
2024: Action-Selector, Sub-Agent Spawning, Factory
       ↓
2025: Agent RFT, MemRL, Agent-Driven Research
       ↓
2026: Multi-agent systems, autonomous workflows, production scaling
```

## Pattern Maturity

| Maturity | Patterns | Description |
|----------|----------|-------------|
| **Established** | ReAct, CoT, Reflection, Sub-Agent Spawning | Widely adopted, production-proven |
| **Emerging** | GoT, LATS, Action-Selector, Agent RFT | Growing adoption, some production use |
| **Research** | MemRL, Variance-Based RL, Agent-Driven Research | Academic focus, limited production |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: action-selector-pattern-report.md, sub-agent-spawning-report.md, graph-of-thoughts-report.md, reflection-report.md, autonomous-workflow-agent-architecture-report.md*
