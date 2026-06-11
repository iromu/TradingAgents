# Reasoning Techniques Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Problem → Select Reasoning Method → Execute → Validate → Output
```

Apply structured reasoning techniques (Chain-of-Thought, Tree-of-Thoughts,
Self-Consistency, ReAct, Debate) to improve accuracy and transparency of
agent decision-making.

## When to Use

- Complex problem-solving with multi-step logical challenges
- Mathematical reasoning requiring systematic thinking
- Strategic planning evaluating multiple approaches
- Critical analysis needing deep examination of options
- Decision making weighing alternatives systematically
- Creative exploration generating diverse solutions

## Where It Fits

- Research analysis: Breaking down complex research questions
- Code debugging: Systematic problem identification
- Business strategy: Evaluating strategic options
- Medical diagnosis: Differential diagnosis reasoning
- Legal analysis: Building logical arguments

## Pros

- **Improved accuracy** — Systematic thinking reduces errors
- **Transparency** — Clear reasoning traces
- **Exploration** — Considers multiple solution paths
- **Robustness** — Multiple methods provide validation
- **Learning** — Reasoning traces help improvement
- **Flexibility** — Different techniques for different problems
- **Quality** — Higher quality solutions through deliberation

## Cons

- **Increased latency** — Multiple reasoning steps take time
- **Token consumption** — Verbose reasoning uses more tokens
- **Complexity** — Managing reasoning flows is challenging
- **Overthinking** — Can make simple problems complex
- **Context limits** — Long reasoning may exceed windows
- **Cost multiplication** — Multiple paths increase costs
- **Diminishing returns** — Extra reasoning may not help

## Implementation

```
# Example: Reasoning technique selector
REASONING_METHODS = {
    "chain_of_thought": {
        "description": "Step-by-step reasoning",
        "best_for": ["math", "logic", "sequential"],
        "prompt_template": "Let's think step by step: {problem}",
    },
    "tree_of_thoughts": {
        "description": "Explore multiple reasoning paths",
        "best_for": ["strategy", "creative", "exploration"],
        "prompt_template": "Consider all possible approaches to: {problem}",
    },
    "self_consistency": {
        "description": "Multiple independent reasoning paths",
        "best_for": ["verification", "accuracy-critical"],
        "prompt_template": "Solve {problem} in 5 different ways",
    },
    "react": {
        "description": "Reason + Act loop with tools",
        "best_for": ["tool_use", "research", "dynamic"],
        "prompt_template": "Think about {problem}, then take action",
    },
}

def reason(problem, method="chain_of_thought", max_iterations=3):
    method_config = REASONING_METHODS[method]
    results = []

    for i in range(max_iterations):
        if method == "self_consistency":
            result = generate_multiple(problem, n=5)
            results.append(consensus(result))
        else:
            prompt = method_config["prompt_template"].format(problem=problem)
            results.append(llm.generate(prompt))

    return aggregate_results(results)
```

## Real-World Examples

1. **Math Solver**: Chain-of-Thought step-by-step → Self-consistency multiple approaches → Tree-of-Thoughts exploring branches → Validation → Explanation
2. **Business Advisor**: Tree-of-Thoughts strategy exploration → Debate growth vs efficiency → Self-consistency across analyses → ReAct with data → Synthesis
3. **Code Architect**: Chain-of-Thought design decisions → Tree exploration architectures → Debate design patterns → ReAct with code analysis → Documentation
4. **Medical Diagnostic**: Differential diagnosis tree → Self-consistency across symptoms → Chain-of-Thought treatment plans → Debate treatment options → Evidence traces
5. **Legal Case**: Chain-of-Thought legal arguments → Tree exploration precedents → Debate interpretations → Self-consistency across statutes → Structured reasoning
6. **Investment Analysis**: Tree-of-Thoughts scenarios → Self-consistency valuations → Debate bull vs bear → Chain reasoning DCF → ReAct with market data
