# Reflection Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Generate → Critique → Revise → [Critique → Revise] → Final
```

Generate output, self-evaluate against criteria, revise based on critique,
repeat until quality threshold is met.

## When to Use

- Quality-critical outputs where accuracy and quality are non-negotiable
- Complex reasoning tasks requiring iterative refinement
- Creative work needing multiple rounds of improvement
- Learning systems that improve performance over time
- Error-prone domains where initial attempts often have mistakes
- Compliance requirements where outputs must meet specific standards

## Where It Fits

- Content creation: Blog posts, reports, documentation requiring polish
- Code generation: Producing bug-free, optimized code
- Legal document drafting: Ensuring accuracy and completeness
- Academic writing: Research papers needing fact-checking and citations
- Product descriptions: E-commerce content requiring SEO and accuracy

## Pros

- **Quality improvement** — Systematic enhancement through iterations
- **Error reduction** — Catches and fixes mistakes before delivery
- **Objectivity** — Separation of generation and critique roles
- **Learning capability** — System improves over time from patterns
- **Transparency** — Clear feedback trail for improvements
- **Consistency** — Applies same quality standards uniformly

## Cons

- **Increased latency** — Multiple iterations multiply processing time
- **Higher costs** — Each reflection cycle incurs additional API calls
- **Context window limits** — Long documents may exceed token limits
- **Diminishing returns** — Later iterations may provide minimal improvement
- **Over-optimization** — Risk of making content generic or losing voice
- **API throttling** — Multiple rapid calls may hit rate limits

## Implementation

```
# Example: Reflection loop with quality threshold
MAX_ITERATIONS = 3
QUALITY_THRESHOLD = 0.85

def reflect_generate(prompt, criteria, max_iterations=MAX_ITERATIONS):
    result = generate(prompt)

    for i in range(max_iterations):
        critique = evaluate(result, criteria)
        if critique.score >= QUALITY_THRESHOLD:
            break
        result = revise(result, critique.feedback)

    return result
```

## Real-World Examples

1. **Technical Blog Post**: Draft → Technical review → Code validation → SEO check → Readability → Grammar polish
2. **Contract Generation**: Draft terms → Legal compliance → Risk assessment → Clarity check → Customization → Final review
3. **Educational Content**: Create lesson → Pedagogical review → Factual verification → Age check → Engagement assessment → Accessibility
4. **API Documentation**: Generate docs → Technical review → Code testing → Completeness check → Clarity → Version consistency
5. **Marketing Copy**: Generate copy → Brand voice check → Persuasiveness → Fact verification → SEO → A/B variants
6. **Research Report**: Draft findings → Methodology critique → Statistical validation → Citation check → Logical flow → Executive summary
