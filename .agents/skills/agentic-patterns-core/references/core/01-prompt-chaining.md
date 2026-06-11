# Prompt Chaining Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Input → [Step1 → Step2 → Step3 → ...] → Merge → Output
```

Break a complex task into discrete, sequential steps where each step's output
becomes the next step's input.

## When to Use

- Complex multi-step processes requiring discrete, manageable steps
- Data transformation pipelines with different requirements per stage
- Quality-critical workflows where each step must meet specific criteria
- Debugging requirements needing clear visibility into each stage
- Mixed tool/AI operations (LLM calls + API calls + DB queries)

## Where It Fits

- Document processing: Research → Analysis → Writing → Editing → Publishing
- Data ETL: Extract → Transform → Validate → Load
- Customer service: Intent recognition → Information gathering → Solution → Response
- Code generation: Requirements → Design → Implementation → Testing → Docs
- Content creation: Ideation → Outline → Draft → Review → Finalization

## Pros

- **Modularity** — Each step developed, tested, and optimized independently
- **Debuggability** — Clear visibility into where failures occur
- **Reliability** — Structured data contracts ensure consistent handoffs
- **Reusability** — Individual chain components reused in different workflows
- **Error handling** — Each step has specific retry logic and fallback strategies
- **Incremental progress** — Partial results saved and resumable

## Cons

- **Latency accumulation** — Each step adds processing time
- **Context limitations** — Information may be lost between steps
- **Error propagation** — Early mistakes cascade through subsequent steps
- **Complexity overhead** — Simple tasks become over-engineered
- **Cost multiplication** — Each LLM call incurs costs

## Implementation

```
# Example: 5-step content generation chain
chain = [
    Step("Ideation", prompt=ideation_prompt),
    Step("Outline",  prompt=outline_prompt,   depends_on=0),
    Step("Draft",    prompt=draft_prompt,     depends_on=1),
    Step("Review",   prompt=review_prompt,    depends_on=2),
    Step("Finalize", prompt=finalize_prompt,  depends_on=3),
]
```

## Real-World Examples

1. **Legal Document Analysis**: Extract clauses → Identify risks → Compare templates → Generate summary
2. **E-commerce Descriptions**: Extract features → Research competitors → Generate SEO copy → Create variations → Validate brand compliance
3. **Academic Research**: Parse question → Search papers → Summarize findings → Identify gaps → Generate review
4. **Bug Analysis**: Parse logs → Identify components → Search KB → Generate solutions → Create report
5. **Financial Reports**: Collect data → Analyze → Identify trends → Generate narrative → Format compliance report
