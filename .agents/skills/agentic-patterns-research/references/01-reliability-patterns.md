# Reliability & Evaluation Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Output Verification Loop

### Core Concept

After generation, verify outputs against expected criteria:

```
1. Generate output
2. Verify against schema/criteria
3. If fails → regenerate or fix
4. If passes → return
```

### Key Properties

- **Deterministic verification** — No LLM needed for checking
- **Structured criteria** — Clear pass/fail conditions
- **Automatic retry** — No human intervention needed

## Action Caching & Replay

### Core Concept

Cache validated action sequences for replay:

- **Schema validation enables reliable caching** — Same inputs always produce same outputs
- **No LLM calls needed for replay** — Deterministic execution
- **Audit trail** — Complete history of actions

### When to Use

- Repeated tasks with similar inputs
- Compliance requirements (need audit trail)
- Cost optimization (avoid redundant LLM calls)

## Agent Circuit Breaker

### Core Concept

Prevent cascading failures in agent workflows:

```
Normal → Degraded → Open (break) → Half-Open (test) → Normal
    │        │         │            │
    │        │         │            └─ If test passes → Normal
    │        │         └─ If test fails → Open
    │        └─ If error rate exceeds threshold → Open
    └─ If error rate exceeds threshold → Degraded
```

### Configuration

| Parameter | Description | Recommended |
|-----------|-------------|-------------|
| **Error threshold** | % errors before tripping | 50% |
| **Timeout** | Time before half-open | 30s |
| **Half-open test calls** | Calls to test recovery | 3 |
| **Failure window** | Time window for counting | 60s |

## Schema Validation Retry with Cross-Step Learning

### Core Concept

When schema validation fails:

1. **Retry** with adjusted parameters
2. **Learn** from previous failures
3. **Apply** learnings to future steps

### Key Benefit

- **Progressive improvement** — Each failure makes the next attempt better
- **Cross-step** — Learnings apply across different steps of the workflow

## Structured Output Specification

### Core Concept

Always define expected output structure:

```json
{
  "type": "object",
  "properties": {
    "status": {"type": "string", "enum": ["success", "error"]},
    "data": {"type": "object"},
    "errors": {"type": "array", "items": {"type": "string"}}
  },
  "required": ["status"]
}
```

### Benefits

- **Predictable** — Consumers know what to expect
- **Validatable** — Can check before processing
- **Self-documenting** — Schema is documentation

## LLM Observability

### Core Concept

Comprehensive logging and monitoring for LLM workflows:

- **Token usage tracking** — Monitor costs
- **Latency measurement** — Identify bottlenecks
- **Quality scoring** — Track output quality over time
- **Error tracking** — Identify failure patterns

### Key Metrics

| Metric | Description | Why It Matters |
|--------|-------------|----------------|
| **Token usage** | Input + output tokens | Cost tracking |
| **Latency** | Time per step | Performance |
| **Error rate** | % of failed steps | Reliability |
| **Quality score** | Automated quality metric | Output quality |
| **Cost per step** | Dollar amount per step | Budget management |

## Other Reliability Patterns

| Pattern | Description | Key Benefit |
|---------|-------------|-------------|
| **Failover-Aware Model Fallback** | Switch models on failure | Availability |
| **Extended Coherence Work Sessions** | Maintain focus over long runs | Consistency |
| **No-Token-Limit Magic** | Work within context limits | Practical constraints |
| **Canary Rollout & Automatic Rollback** | Gradual deployment with rollback | Safe updates |
| **Subagent Compilation Checker** | Verify subagent outputs | Quality gate |
| **Workflow Evals with Mocked Tools** | Test workflows without real tools | Safe testing |
| **Versioned Constitution Governance** | Versioned policy documents | Compliance |
| **Adaptive Sandbox Fan-Out** | Dynamic parallel scaling | Resource efficiency |
| **Anti-Reward-Hacking Grader Design** | Robust evaluation design | Quality assurance |
| **Asynchronous Coding Agent Pipeline** | Async processing pipeline | Throughput |
| **CriticGPT-Style Evaluation** | Specialized critique models | Deep evaluation |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: output-verification-loop.md, action-caching-replay-report.md, agent-circuit-breaker-report.md, structured-output-specification-report.md, llm-observability-report.md*
