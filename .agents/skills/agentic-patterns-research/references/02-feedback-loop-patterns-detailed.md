# Feedback Loop Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Reflection Loop (Primary Feedback Pattern)

### Core Algorithm

```python
for attempt in range(max_iters):
    draft = generate(prompt)
    score, critique = evaluate(draft, metric)
    if score >= threshold:
        return draft
    prompt = incorporate(critique, prompt)
```

### Key Findings

| Finding | Value | Source |
|---------|-------|--------|
| **Quality improvement** | 15-45% through 2-4 iterations | Shinn et al. 2023 |
| **HumanEval pass@1** | 91% (Self-Refine) vs 80% (GPT-4 baseline) | Shinn et al. 2023 |
| **Cost vs RLHF** | 100x reduction ($1+ to $0.01 per sample) | Constitutional AI |
| **Production scale** | 600K+ PRs/month at Microsoft | GitHub |

### Implementation Strategies

| Strategy | Cost | Quality | Best For |
|----------|------|---------|----------|
| **Single Pass** | 1x | Baseline | Simple tasks, low stakes |
| **Reflection (2 iters)** | 1.8x | +15% | Standard quality needs |
| **Reflection (3 iters)** | 2.5x | +25% | High quality requirements |
| **Best-of-5** | 4x | +20% | Parallelizable, time-sensitive |
| **Multi-Agent Debate** | 3x | +35% | Decisions requiring scrutiny |

### Configuration Recommendations

| Parameter | Range | Recommended | Notes |
|-----------|-------|-------------|-------|
| **max_iterations** | 1-5 | 2-3 | Beyond 3 shows diminishing returns |
| **threshold** | 0.6-0.95 | 0.75-0.85 | Adjust by task criticality |
| **budget_cap** | 1000-20000 tokens | 3000-5000 | Based on task complexity |
| **early_termination** | boolean | True | Save compute on easy tasks |

### Anti-Patterns

| Anti-Pattern | Description | Fix |
|--------------|-------------|-----|
| **Infinite loops** | No proper termination | Always have max_iters and budget caps |
| **Reward hacking** | Agent satisfies grader without solving task | Multi-criteria continuous scoring |
| **Ignoring critique quality** | Bad critiques propagate | Validate critique before using |
| **No observability** | Can't track scores, critiques, iterations | Track metrics from day one |
| **Binary scoring** | Easy to game | Use continuous scoring with feedback |

## Rich Feedback Loops

### Core Concept

Use **environmental signals** (test failures, CI results, user feedback) instead of self-evaluation:

- **More reliable** than self-critique (external truth)
- **Faster convergence** (no LLM needed for evaluation)
- **Complementary** with Reflection Loop (use both)

### When to Use

- Code with tests available
- Deployments with monitoring
- User-facing products with feedback channels
- CI/CD pipelines

## Self-Critique Evaluator Loop

### Evolution

```
Reflection Loop (basic iterative improvement)
    ↓
Self-Critique Evaluator Loop (trained evaluators from synthetic data)
    ↓
RLAIF (training-time reflection)
    ↓
Agent RFT (end-to-end reflection training)
```

### Key Innovation

Train the evaluator from synthetic data rather than human labels:

1. Generate candidates
2. Compare pairs (with explanations)
3. Fine-tune evaluator on comparisons
4. Use trained evaluator for selection

## RLAIF (Reinforcement Learning from AI Feedback)

### Core Concept

Use AI feedback instead of human feedback for RL training:

- **100x cost reduction** vs RLHF
- **Scalable** — no human annotators needed
- **Constitutional AI** — AI has principles to follow

### Implementation

```
1. Generate responses
2. AI evaluates against constitution (principles)
3. Train policy model on AI feedback
4. Repeat
```

## AI-Assisted Code Review

### Production Deployments

| Company | Scale | Results |
|---------|-------|---------|
| **Microsoft** | 600K+ PRs/month | Standard AI review workflow |
| **Tekion** | Enterprise | 60% faster merge times |
| **Tencent** | Large-scale | 94% AI coverage |
| **Ericsson** | 5,000 engineers | >60% user satisfaction |

### Framework Support

| Framework | Stars | Reflection Support |
|-----------|-------|-------------------|
| **LangChain** | 90,000+ | SelfCritiqueAgent, ReflexionAgent |
| **LlamaIndex** | 40,000+ | Reflection loops for RAG |
| **SWE-agent** | 12,000+ | 12.29% on SWE-bench |
| **Aider** | 41,000+ | Terminal-based, git integration |
| **OpenHands** | 64,000+ | 72% on SWE-bench Verified |

## Multi-Agent Debate (Opponent Processor)

### Core Concept

Multiple agents with opposing views provide adversarial evaluation:

- **2-4 rounds** typically sufficient
- **2-3x cost**, 25-40% quality improvement
- **Reduces bias** through adversarial pressure

### When to Use

- Bias reduction critical
- High-stakes decisions
- Complex problems needing scrutiny
- When self-critique shows diminishing returns

## Pattern Combinations

| Combination | Use Case | Benefit |
|-------------|----------|---------|
| **Reflection + Rich Feedback** | Autonomous improvement + environmental signals | Best of both worlds |
| **Reflection + Multi-Agent Debate** | Self-critique + adversarial pressure | Reduced bias |
| **Reflection + HITL** | Pre-filtered work requiring less human attention | Efficiency |
| **Reflection + Memory Synthesis** | Learn across episodes, not just within them | Long-term improvement |
| **Reflection + Anti-Reward-Hacking** | Robust evaluation that resists gaming | Quality |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: reflection-report.md, rich-feedback-loops.md, self-critique-evaluator-loop.md, rlaif-reinforcement-learning-from-ai-feedback.md*
