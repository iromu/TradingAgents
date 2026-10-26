# Orchestration Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Core Orchestration Patterns

### 1. Plan-Then-Execute Pattern

**Definition:** Separates planning from execution. The planning phase generates a complete, frozen sequence of actions. The execution phase runs the plan without LLM involvement.

**Key Properties:**
- Control-flow integrity: Plan cannot be modified during execution
- Auditability: Complete plan is logged before execution
- Security: Prevents prompt injection from changing execution flow

**Implementation:**
```
1. PLAN: LLM generates complete action sequence
2. FREEZE: Plan is validated and locked
3. EXECUTE: Deterministic code runs the plan
4. VERIFY: Results are checked against plan
```

**When to use:** Security-critical workflows, complex multi-step tasks

### 2. Sub-Agent Spawning

**Definition:** A primary agent creates and orchestrates subordinate agents to handle specialized tasks in parallel.

**Three Isolation Architectures:**

| Architecture | Scale | Isolation | Use Case |
|--------------|-------|-----------|----------|
| **Virtual File** | 2-4 agents | Same process, explicit file passing | Context management, specialized tools |
| **Git Worktree** | 10-100 agents | Filesystem-level isolation | Code migrations, large refactoring |
| **Cloud Worker** | 100+ agents | Container/VM isolation | Enterprise-scale processing |

**Key Characteristics:**
- **Dynamic Creation:** Sub-agents created on-demand
- **Parallel Execution:** Multiple sub-agents work concurrently
- **Result Synthesis:** Parent aggregates outputs

**Production Examples:**
- **Anthropic Claude Code:** 10+ parallel subagents for code migrations, 10x speedup
- **Cursor AI:** Hundreds of concurrent agents, 1M lines in ~1 week
- **HumanLayer CodeLayer:** 10x-100x speedup for parallelizable tasks

**Scale Selection:**

| Project Characteristics | Recommended Scale |
|------------------------|------------------|
| < 100 files, single task | Sequential |
| 100-500 files, some parallelism | 2-4 subagents |
| 500-5000 files, framework changes | 10+ subagents |
| 5000+ files, multi-week work | 100+ agents |

### 3. Factory over Assistant

**Definition:** Shift from assistant model (one agent, sequential tasks) to factory model (many autonomous agents, parallel tasks).

**Key Properties:**
- Multiple independent agents work simultaneously
- Each agent has its own context and tools
- Results are aggregated by a coordinator

### 4. Dual LLM Pattern

**Definition:** Two LLMs with different privilege levels — one processes untrusted data (quarantined), one performs operations (privileged).

**Key Properties:**
- Privilege separation between LLMs
- Quarantined LLM cannot access tools
- Privileged LLM sees only sanitized outputs

### 5. Planner-Worker Separation

**Definition:** Multi-layer agent system where planners create tasks and workers execute them.

**Architecture:**
```
Planner → [Task 1, Task 2, Task 3, ...] → Workers → Results → Synthesis
```

**Key Properties:**
- Separation of concerns
- Workers can be specialized (different tools/models)
- Planners can be hierarchical

### 6. Autonomous Workflow Agent Architecture

**Definition:** AI agents with sophisticated workflow management for long-running processes with minimal human intervention.

**Key Components:**

| Component | Purpose | Implementation |
|-----------|---------|----------------|
| **Containerized Execution** | Isolated, reproducible execution | Docker/Podman |
| **Session Management** | Parallel process coordination | tmux |
| **Intelligent Monitoring** | Progress tracking | Dynamic sleep, process completion detection |
| **Error Recovery** | Context-aware retry | Error classification, adaptive backoff |
| **Documentation** | Comprehensive logging | Automatic workflow docs |

**Key Distinction:** Traditional automation follows a script; autonomous agents follow a goal.

**Production Benefits:**
- 1.22x-1.37x speedup in workflow execution
- Reduced human intervention through automated error recovery
- Scalability across multiple parallel workflows

## Decision Framework

```
Is task complex?
├── No → ReAct or Simple Reflection
└── Yes → Are there multiple valid solutions?
    ├── No → Linear approach with Reflection Loop
    └── Yes → Need systematic search?
        ├── No → ToT or GoT
        └── Yes → LATS (if cost acceptable)
```

## Pattern Combinations

| Use Case | Recommended Combination |
|----------|------------------------|
| **Large codebase migration** | Sub-Agent Spawning + Factory + Lane-Based Queueing |
| **Multi-domain analysis** | Sub-Agent Spawning + GoT (Aggregate) + Planner-Worker |
| **Autonomous workflow** | Autonomous Workflow Architecture + Checkpointing + Error Recovery |
| **Secure multi-step task** | Plan-Then-Execute + Dual LLM + Action-Selector |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: plan-then-execute-report.md, sub-agent-spawning-report.md, autonomous-workflow-agent-architecture-report.md*
