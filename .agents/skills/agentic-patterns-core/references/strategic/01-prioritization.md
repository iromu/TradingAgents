# Prioritization Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Score Tasks → Rank → Execute → Reorder → Repeat
```

Dynamically prioritize tasks based on multiple criteria (urgency, value,
dependencies, resources) and continuously re-rank as conditions change.

## When to Use

- Resource constraints with limited processing capacity
- Multiple objectives with competing goals and tasks
- Dynamic environments with constantly changing priorities
- Complex dependencies between tasks
- Time-sensitive operations with deadline-driven work
- Fair scheduling preventing task starvation

## Where It Fits

- Task management systems: Workflow orchestration
- Customer service: Ticket prioritization
- Manufacturing: Production scheduling
- Healthcare: Patient triage systems
- DevOps: Deployment and maintenance prioritization

## Pros

- **Efficiency** — Optimal use of resources
- **Responsiveness** — High-priority items handled first
- **Fairness** — Prevents indefinite delays
- **Adaptability** — Adjusts to changing conditions
- **Transparency** — Clear prioritization logic
- **Goal alignment** — Tasks ranked by business value
- **Scalability** — Handles growing task queues

## Cons

- **Complexity** — Priority calculation can be complex
- **Overhead** — Continuous reordering costs resources
- **Starvation risk** — Low-priority tasks may wait forever
- **Context switching** — Preemption adds overhead
- **Subjective scoring** — Priority factors may be disputed
- **Dependencies** — Complex dependency management
- **Prediction errors** — Effort estimates may be wrong

## Implementation

```
# Example: Priority scoring system
def calculate_priority(task):
    score = 0.0

    # Urgency (0-40 points)
    if task.is_urgent():
        score += 40
    elif task.is_deadline_soon():
        score += 30
    elif task.deadline_in_hours() < 24:
        score += 20

    # Business value (0-30 points)
    score += task.business_value_score() * 0.3

    # Customer tier (0-15 points)
    if task.customer_tier == "premium":
        score += 15
    elif task.customer_tier == "standard":
        score += 10

    # SLA compliance (0-15 points)
    sla_remaining = task.sla_remaining_minutes()
    if sla_remaining < 30:
        score += 15
    elif sla_remaining < 60:
        score += 10

    return score

def prioritize_queue(tasks):
    scored = [(calculate_priority(t), t) for t in tasks]
    scored.sort(reverse=True)
    return [t for _, t in scored]
```

## Real-World Examples

1. **Customer Support**: Premium priority, urgent ranking, age-based escalation, skill-based routing, SLA tracking, load balancing
2. **Dev Pipeline**: Critical bugs first, feature value scoring, technical debt scheduling, dependency resolution, sprint planning, resource allocation
3. **Healthcare Triage**: Emergency severity scoring, wait time consideration, resource availability, specialist routing, test prioritization, appointment scheduling
4. **Manufacturing**: Order value priority, deadline management, resource optimization, setup time minimization, quality requirements, maintenance windows
5. **Content Publishing**: Trending topic priority, editorial calendar, author availability, SEO scoring, social timing, cross-platform coordination
6. **Network Traffic**: QoS packet prioritization, bandwidth allocation, latency-sensitive routing, fair queuing, emergency priority, load balancing
