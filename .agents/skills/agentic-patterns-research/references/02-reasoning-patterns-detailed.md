# Reasoning Patterns - Detailed Analysis

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Graph of Thoughts (GoT) - Deep Dive

### Core Operations

| Operation | Description | Use Case |
|-----------|-------------|----------|
| **Branch** | Generate multiple new thoughts from one | Explore multiple solution strategies |
| **Aggregate** | Combine multiple thoughts into one | Merge insights from different approaches |
| **Refine** | Improve a thought based on context | Polish partial solutions |
| **Loop** | Revisit a thought after exploring branches | Iterative improvement with new context |

### Implementation Example

```python
import networkx as nx

class GraphOfThoughts:
    def __init__(self):
        self.graph = nx.DiGraph()
        self.thought_data = {}
        self.thought_scores = {}

    def add_thought(self, thought_id, content, score=0.0):
        self.graph.add_node(thought_id)
        self.thought_data[thought_id] = content
        self.thought_scores[thought_id] = score

    def branch_thought(self, thought_id, prompt_template, llm):
        """Generate multiple new thoughts from a single thought."""
        base_content = self.thought_data[thought_id]
        prompt = prompt_template.format(current_thought=base_content)
        # LLM generates 3 different continuations
        continuations = llm.generate(prompt, n=3)
        for i, continuation in enumerate(continuations):
            new_id = f"{thought_id}_branch_{i}"
            self.add_thought(new_id, continuation)
            self.graph.add_edge(thought_id, new_id, operation="branch")

    def aggregate_thoughts(self, source_ids, llm):
        """Combine multiple thoughts into a unified thought."""
        thoughts = [self.thought_data[vid] for vid in source_ids]
        prompt = f"Combine these insights:\n{thoughts}\n\nCreate a unified thought."
        aggregated = llm.generate(prompt)
        new_id = f"aggregate_{len(self.thought_data)}"
        self.add_thought(new_id, aggregated)
        for sid in source_ids:
            self.graph.add_edge(sid, new_id, operation="aggregate")

    def extract_best_solution(self):
        """Find the highest-scoring terminal thought."""
        terminals = [n for n in self.graph.nodes()
                     if self.graph.out_degree(n) == 0]
        best = max(terminals, key=lambda n: self.thought_scores.get(n, 0))
        return self.thought_data[best]
```

### When to Use GoT

**Use when:**
- Problems have multiple valid solution paths
- Insights from different approaches need to be combined
- Early decisions may need revision based on later findings
- Problem complexity justifies additional computational cost

**Avoid when:**
- Problems are straightforward or linear
- Only one solution path is viable
- Computational resources are limited
- Fast responses are required

## Language Agent Tree Search (LATS) - Deep Dive

### Four-Phase Algorithm

```
1. SELECTION: Traverse tree using UCB (Upper Confidence Bound)
   UCB(node) = Q(node) + c × √(ln(parent_visits) / node_visits)

2. EXPANSION: Generate candidate actions/thoughts using LLM

3. EVALUATION: Assess node quality using LLM self-reflection

4. BACKPROPAGATION: Update value estimates throughout the tree
```

### Configuration Parameters

| Parameter | Range | Recommended | Notes |
|-----------|-------|-------------|-------|
| **Exploration constant (c)** | 0.5-3.0 | 1.414 | Balanced exploration |
| **Tree depth** | 5-30 | 10-20 (math) | Depends on problem complexity |
| **Branching factor** | 2-5 | 4-5 (early), 2 (late) | Broad early, focused late |
| **Iterations** | 10-100+ | 25-50 | Diminishing returns after 50 |
| **Temperature (expand)** | 0.7-1.0 | 0.8 | Encourage diverse actions |
| **Temperature (evaluate)** | 0.1-0.3 | 0.2 | Consistent scoring |

### Performance Characteristics

| Aspect | ReAct | Reflexion | ToT | LATS |
|--------|-------|-----------|-----|------|
| Avg LLM Calls | 10-20 | 20-40 | 50-200 | 50-150 |
| Parallelism | Sequential | Sequential | Parallel | Partial |
| Search Strategy | Linear | Linear+memory | Breadth-first | MCTS-guided |

### Adoption Barriers

| Barrier | Description |
|---------|-------------|
| **Computational Cost** | 5-20x more LLM calls; 200+ calls per task |
| **Implementation Complexity** | Requires correct MCTS, tree state management, UCB scoring |
| **Latency** | Inherently sequential; unsuitable for real-time |
| **Unclear ROI** | Simpler patterns often sufficient |
| **Limited Framework Support** | No "LATS templates" in major frameworks |

## ReAct - Foundational Pattern

### TAO Loop

```
Thought: What do I know about this problem?
Action: Search for relevant information
Observation: [result from search]
Thought: How does this result change my understanding?
Action: [next action]
...
```

### Key Properties

- **Interpretability:** Reasoning trace is visible and debuggable
- **Flexibility:** Any tool/API can be used in the Action step
- **Foundation:** Many advanced patterns build on ReAct

## Self-Consistency

### Pattern

```
1. Generate N independent reasoning paths (same prompt, different temperature)
2. Collect all outputs
3. Use majority voting or verification to select best output
```

### When to Use

- Math and reasoning tasks where multiple paths may converge
- When stochasticity in outputs is a concern
- When compute budget allows for parallel generation

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: graph-of-thoughts-report.md, language-agent-tree-search-lats-report.md, self-discover-reasoning-structures.md*
