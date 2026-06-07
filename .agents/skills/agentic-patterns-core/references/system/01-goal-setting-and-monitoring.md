# Goal Setting and Monitoring Pattern

**Source:
** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT
License

## Pattern

```
SMART Goal → KPIs → Monitor → Achieve → Report
```

Define specific, measurable objectives with key performance indicators and
continuous monitoring to track progress and enable self-correction.

## When to Use

- Autonomous operations where agents work independently toward objectives
- Complex projects requiring progress tracking
- Resource management within constraints
- Performance optimization for measurable outcomes
- Compliance requirements meeting SLAs and quality standards
- Strategic execution aligning agent actions with business goals

## Where It Fits

- Project automation: Managing milestones and deliverables
- Sales pipelines: Tracking targets and conversion goals
- Content production: Meeting publishing schedules and quality standards
- System optimization: Achieving performance benchmarks
- Cost management: Operating within budget constraints

## Pros

- **Purpose-driven** — Agents work toward clear objectives
- **Self-assessment** — Continuous evaluation of progress
- **Adaptability** — Dynamic adjustment to changing conditions
- **Accountability** — Clear metrics and success criteria
- **Resource efficiency** — Optimal allocation based on priorities
- **Early warning** — Proactive detection of issues
- **Measurable outcomes** — Quantifiable success metrics

## Cons

- **Overhead complexity** — Goal management adds system complexity
- **Rigid constraints** — May limit creative problem-solving
- **Measurement challenges** — Some goals are hard to quantify
- **False metrics** — Risk of optimizing wrong indicators
- **Resource intensive** — Continuous monitoring requires resources
- **Goal conflicts** — Multiple goals may compete

## Implementation

```
# Example: Goal definition with KPIs
GOAL = {
    "objective": "Improve customer satisfaction",
    "kpis": [
        {"name": "csat_score", "target": 4.5, "current": 4.1, "weight": 0.4},
        {"name": "resolution_time", "target": 2.0, "current": 2.5, "weight": 0.3},
        {"name": "first_contact_rate", "target": 0.85, "current": 0.78, "weight": 0.3},
    ],
    "thresholds": {"csat_score": 4.0, "resolution_time": 3.0},
    "escalation": {"trigger": "below_threshold", "action": "notify_manager"},
}

def monitor_goal(goal):
    progress = {}
    for kpi in goal["kpis"]:
        current = get_metric(kpi["name"])
        kpi["current"] = current
        kpi["progress"] = current / kpi["target"]
        if current < goal["thresholds"].get(kpi["name"], float("inf")):
            trigger_escalation(goal, kpi)
        progress[kpi["name"]] = kpi["progress"]
    return progress
```

## Real-World Examples

1. **Sales Automation**: Monthly revenue targets, lead conversion goals, CAC limits, activity metrics, auto-escalation
   for at-risk deals
2. **Content Publishing**: Publication schedules, quality score thresholds, SEO targets, engagement goals, budget
   allocation, deadline alerts
3. **DevOps Pipeline**: Deployment frequency, MTTR goals, test coverage requirements, performance benchmarks, cost
   limits, auto-rollback
4. **Customer Service**: First response SLAs, resolution rate targets, satisfaction scores, ticket volume management,
   cost limits, escalation thresholds
5. **Marketing Campaign**: ROI targets, conversion goals, budget limits, A/B test criteria, channel metrics, real-time
   optimization
6. **Supply Chain**: Inventory targets, fulfillment SLAs, cost reduction goals, delivery objectives, quality rates,
   auto-reorder triggers
