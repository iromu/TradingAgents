# Feedback Loop Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Reflection Loop (Primary Feedback Pattern)

### Core Concept

Iterative self-evaluation and refinement:

```
1. GENERATE: Create initial output
2. CRITIQUE: Evaluate output against criteria
3. REFINE: Improve based on critique
4. REPEAT: Until threshold met or max iterations reached
```

### Academic Foundations

| Paper | Authors | Year | Key Contribution |
|-------|---------|------|------------------|
| **Self-Refine** | Shinn et al. | 2023 | Iterative feedback for improvement |
| **Reflexion** | Shinn et al. | 2023 | Self-reflection with episodic memory |
| **Constitutional AI** | Bai et al. (Anthropic) | 2022 | RLAIF - 100x cost reduction vs RLHF |
| **Self-Taught Evaluators** | Wang et al. (Meta AI) | 2024 | Bootstrap from synthetic data |
| **CriticGPT** | OpenAI | 2024 | Specialized models for code critique |

### Benchmark Results

| Metric | Result | Source |
|--------|--------|--------|
| **HumanEval pass@1** | 91% (Self-Refine) vs 80% (GPT-4 baseline) | Shinn et al. 2023 |
| **Cost vs RLHF** | 100x reduction ($1+ to $0.01 per sample) | Constitutional AI |
| **Quality Improvement** | 15-45% through 2-4 iterations | Multiple sources |

### Implementation Patterns

#### Single-Model Self-Critique

```python
def reflection_loop(prompt, max_iterations=3, threshold=0.8):
    for attempt in range(max_iterations):
        # Generate
        draft = generate(prompt)
        
        # Evaluate
        score, critique = evaluate(draft, metric)
        
        # Check threshold
        if score >= threshold:
            return draft
        
        # Refine
        prompt = incorporate(critique, prompt)
    
    return draft  # Return best available
```

#### Reflexion (with Episodic Memory)

```python
def reflexion_loop(task, max_trials=5):
    memory = []  # Store past reflections
    
    for trial in range(max_trials):
        # Format context with memory
        context = format_memory(memory)
        
        # Generate with trace
        output, trace = generate_with_trace(task, context)
        
        # Verify
        if verify(output, task):
            return output
        
        # Reflect
        reflection = reflect(trace, output)
        memory.append({
            'task': task,
            'output': output,
            'reflection': reflection
        })
```

### Configuration Recommendations

| Parameter | Range | Recommended | Notes |
|-----------|-------|-------------|-------|
| **max_iterations** | 1-5 | 2-3 | Beyond 3 shows diminishing returns |
| **threshold** | 0.6-0.95 | 0.75-0.85 | Adjust by task criticality |
| **budget_cap** | 1000-20000 tokens | 3000-5000 | Based on task complexity |
| **early_termination** | boolean | True | Save compute on easy tasks |

### Performance Characteristics

| Strategy | Latency | Cost | Quality | Best For |
|----------|---------|------|---------|----------|
| Single Pass | 1x | 1x | Baseline | Simple tasks, low stakes |
| Reflection (2 iters) | 1.8x | 1.8x | +15% | Standard quality needs |
| Reflection (3 iters) | 2.5x | 2.5x | +25% | High quality requirements |
| Best-of-5 | 1.2x | 4x | +20% | Parallelizable, time-sensitive |
| Multi-Agent Debate | 2-3x | 3x | +35% | Decisions requiring scrutiny |

### Failure Modes

| Failure Mode | Description | Mitigation |
|--------------|-------------|------------|
| **Reward Hacking** | Agent satisfies grader without solving task | Multi-criteria evaluation, adversarial testing |
| **Reflection Collapse** | Critique loses effectiveness over iterations | Detect stereotyped critique, switch strategies |
| **Threshold Mismatch** | Thresholds misaligned with task difficulty | Adaptive thresholds by task type |
| **Evaluation Bias** | Position, length, or formatting bias | Blind evaluation, multiple evaluators |

### Production Deployments

| Company | Scale | Results |
|---------|-------|---------|
| **Microsoft** | 600K+ PRs/month | Standard AI review workflow |
| **Tekion** | Enterprise | 60% faster merge times |
| **Tencent** | Large-scale | 94% AI coverage |
| **Ericsson** | 5,000 engineers | >60% user satisfaction |

## Rich Feedback Loops

### Concept

Environmental feedback signals (tests, failures, CI results) rather than self-evaluation:

- **CI feedback** — Tests run on generated code, results feed back to agent
- **Runtime feedback** — Actual execution results inform next steps
- **User feedback** — Human approval/rejection guides behavior

### When to Use Over Reflection

- When external verification is available (tests, linters, security scanners)
- When environmental signals are more reliable than self-critique
- When you want to avoid reward hacking risks

## Self-Critique Evaluator Loop

### Concept

Extends basic reflection by training evaluators from synthetic data:

1. Generate candidate outputs
2. Compare pairs and generate judgments
3. Fine-tune evaluator on judgments
4. Use trained evaluator for critique

### Key Advantage

Trained evaluators are more accurate than base model self-critique, reducing bias and improving quality.

## RLAIF (Reinforcement Learning from AI Feedback)

### Concept

Elevates reflection from inference-time to training-time:

1. AI generates feedback (like Constitutional AI)
2. Model is fine-tuned on AI feedback
3. Model internalizes the feedback patterns

### Cost Advantage

**100x cost reduction** vs. human feedback (RLHF): $1+ → $0.01 per sample

## AI-Assisted Code Review

### Concept

Specialized AI review of generated code:

- **CriticGPT-style** — Specialized models for code critique
- **Multi-dimensional evaluation** — Correctness, style, performance, security
- **Automated fix suggestions** — Not just critique, but remediation

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: reflection-report.md, self-critique-evaluator-loop.md, rich-feedback-loops.md, anti-reward-hacking-grader-design.md, rlaif-reinforcement-learning-from-ai-feedback.md*
