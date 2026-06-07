# Resource-Aware Optimization Pattern

**Source:
** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT
License

## Pattern

```
Classify → Route to Appropriate Model → Execute → Monitor → Optimize
```

Dynamically route tasks to the most cost-effective model or resource tier
based on task complexity, maintaining quality where it matters while
optimizing costs for simpler tasks.

## When to Use

- Cost-sensitive operations managing API or compute costs
- High-volume processing optimizing large-scale operations
- Variable workloads where different tasks need different resources
- Budget constraints operating within financial limits
- Performance requirements balancing speed vs cost
- Multi-tenant systems ensuring fair resource allocation

## Where It Fits

- SaaS platforms: Managing per-customer resource usage
- Batch processing: Optimizing large data processing jobs
- Real-time systems: Balancing latency and cost
- Development environments: Using cheaper models for testing
- Production systems: Optimizing operational costs

## Pros

- **Cost reduction** — Significant savings on API and compute costs
- **Performance optimization** — Right-sized resources for each task
- **Scalability** — Efficient resource use enables growth
- **Flexibility** — Dynamic adjustment to workload changes
- **Budget control** — Predictable operational costs
- **Quality preservation** — Maintains output quality where needed
- **Automatic optimization** — Self-tuning based on patterns

## Cons

- **Complexity increase** — Resource management adds overhead
- **Quality variations** — Different models produce different results
- **Routing overhead** — Classification step adds latency
- **Monitoring requirements** — Need comprehensive tracking
- **Tuning challenges** — Finding optimal thresholds takes time
- **Cache management** — Maintaining cache coherency
- **User experience** — Inconsistent response times

## Implementation

```
# Example: Resource-aware model routing
MODEL_TIER = {
    "simple":  {"model": "gpt-4o-mini",  "cost_per_1k": 0.00015, "speed": "fast"},
    "medium":  {"model": "gpt-4o",      "cost_per_1k": 0.005,   "speed": "medium"},
    "complex": {"model": "o1-preview",   "cost_per_1k": 0.015,   "speed": "slow"},
}

def classify_complexity(task):
    """Classify task complexity to determine model tier."""
    if task.is_simple_qa():
        return "simple"
    elif task.requires_creative_reasoning():
        return "complex"
    else:
        return "medium"

def route_task(task, budget=None):
    tier = classify_complexity(task)
    model = MODEL_TIER[tier]

    # Apply budget constraints
    if budget and model["cost_per_1k"] > budget:
        tier = "simple"
        model = MODEL_TIER["simple"]

    return model
```

## Real-World Examples

1. **Customer Support**: Simple FAQs use lightweight models, complex issues use advanced models, cache common responses,
   prioritize premium customers
2. **Content Generation**: Short posts use fast models, long articles use quality models, reuse templates, batch similar
   requests, monitor cost per piece
3. **Code Assistant**: Syntax fixes use simple models, architecture design uses advanced models, cache common patterns,
   prioritize by project importance
4. **Translation Platform**: Common languages use basic models, rare languages use specialized models, cache frequent
   translations, batch document processing
5. **Data Analysis**: Simple aggregations use basic compute, complex ML uses premium resources, cache intermediate
   results, schedule heavy jobs off-peak
6. **Education Platform**: Basic Q&A uses lightweight models, complex tutoring uses advanced models, cache common
   explanations, allocate by subscription tier
