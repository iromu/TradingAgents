# UX & Collaboration Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Human-in-the-Loop Approval Framework

### Core Concept

Structured framework for human oversight of agent actions:

| Approval Level | Description | Use Case |
|---------------|-------------|----------|
| **Pre-approval** | Human approves before execution | High-risk actions |
| **Post-approval** | Human reviews after execution | Medium-risk actions |
| **On-demand** | Human intervenes when needed | Low-risk, exploratory |
| **Continuous** | Human monitors throughout | Critical workflows |

### Key Properties

- **Clear escalation paths** — Agents know when to ask for help
- **Actionable information** — Humans get enough context to decide
- **Easy approval** — One-click approval where possible
- **Audit trail** — All decisions are logged

## Spectrum of Control / Blended Initiative

### Core Concept

The spectrum of control defines how much autonomy an agent has:

```
Human-Controlled ←——————————→ Agent-Controlled
    │                              │
    ├─ Human sets all steps        ├─ Agent decides all steps
    ├─ Human approves each step    ├─ Agent self-corrects
    └─ Human can interrupt anytime └─ Human reviews after
```

### Key Insight

The right position on the spectrum depends on:
- **Risk level** of the action
- **Agent experience** and reliability
- **Task complexity**
- **Urgency**

## Verbose Reasoning Transparency

### Core Concept

Show the agent's reasoning process to humans:

```
Agent: "I'm going to search for the database schema first,
        because I need to understand the table structure
        before I can write the migration."
```

### Benefits

- **Trust** — Humans understand why agents make decisions
- **Debugging** — Easier to identify wrong reasoning
- **Learning** — Humans learn from agent reasoning
- **Intervention** — Humans can correct reasoning early

## Agent-Friendly Workflow Design

### Core Concept

Design workflows that agents can execute effectively:

| Principle | Description |
|-----------|-------------|
| **Clear boundaries** | Define what agents can/cannot do |
| **Structured inputs** | Provide data in agent-friendly formats |
| **Incremental steps** | Break large tasks into small steps |
| **Feedback channels** | Provide clear feedback mechanisms |
| **Recovery paths** | Define how to recover from errors |

## Other UX & Collaboration Patterns

| Pattern | Description | Key Benefit |
|---------|-------------|-------------|
| **Seamless Background-to-Foreground Handoff** | Smooth transition between background and foreground work | Uninterrupted workflow |
| **Proactive Trigger Vocabulary** | Define clear triggers for agent activation | Predictable behavior |
| **Chain-of-Thought Monitoring & Interruption** | Monitor agent reasoning and interrupt if needed | Safety control |
| **Abstracted Code Representation for Review** | Show code changes in agent-friendly format | Easier review |
| **Codebase Optimization for Agents** | Structure codebases for agent consumption | Better agent performance |
| **Democratization of Tooling via Agents** | Make powerful tools accessible through agents | Wider accessibility |
| **Dev Tooling Assumptions Reset** | Challenge assumptions about developer workflows | Better agent design |
| **Milestone Escrow for Agent Resource Funding** | Fund agents based on milestone completion | Resource management |
| **Team-Shared Agent Configuration as Code** | Share agent configs across team | Consistency |
| **Agent-Assisted Scaffolding** | Agents help create project scaffolding | Faster setup |
| **AI-Accelerated Learning and Skill Development** | Agents help humans learn | Skill development |
| **Latent Demand Product Discovery** | Agents discover unmet needs | Product insight |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: human-in-loop-approval-framework-report.md, spectrum-of-control-blended-initiative.md, verbose-reasoning-transparency.md, agent-friendly-workflow-design-report.md*
