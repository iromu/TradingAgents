# Planning Pattern

**Source:
** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT
License

## Pattern

```
Goal → Decompose → Milestones → Execute → Monitor → Adapt
```

Transform reactive agents into proactive planners by decomposing complex goals
into structured, executable plans with dependencies, milestones, and monitoring.

## When to Use

- Complex multi-step projects with multiple dependencies and phases
- Goal-oriented workflows working toward specific, measurable objectives
- Resource-constrained operations managing budgets, time, or compute limits
- Uncertain environments requiring adaptability to changing conditions
- Collaborative tasks coordinating multiple agents or tools
- Long-running processes spanning extended timeframes

## Where It Fits

- Project management automation: Breaking down projects into executable tasks
- Software development: Planning features from requirements to deployment
- Research projects: Organizing literature review, experimentation, and analysis
- Content production: Planning multi-part content series or campaigns
- Business process automation: Orchestrating complex business workflows

## Pros

- **Strategic execution** — Transforms reactive agents into proactive planners
- **Dependency management** — Handles complex task interdependencies
- **Resource optimization** — Allocates resources efficiently across steps
- **Adaptability** — Can adjust plans based on new information
- **Progress visibility** — Clear tracking of milestone completion
- **Risk mitigation** — Early identification of blockers and issues
- **Reusability** — Plans can be templated and reused

## Cons

- **Upfront overhead** — Planning phase adds initial latency
- **Rigidity risk** — Over-planning can reduce flexibility
- **Complexity** — Managing plan state and dependencies is challenging
- **Prediction errors** — Initial plans may be based on incorrect assumptions
- **Replanning costs** — Adjusting plans mid-execution can be expensive
- **Context limitations** — Long plans may exceed context windows

## Implementation

```
# Example: Planning structure
PLAN = {
    "goal": "Launch new feature",
    "milestones": [
        {
            "id": "m1",
            "name": "Requirements",
            "tasks": ["gather_requirements", "design_spec", "review"],
            "dependencies": [],
            "status": "completed"
        },
        {
            "id": "m2",
            "name": "Development",
            "tasks": ["implement_feature", "unit_tests", "code_review"],
            "dependencies": ["m1"],
            "status": "in_progress"
        },
        {
            "id": "m3",
            "name": "Testing",
            "tasks": ["integration_tests", "user_acceptance", "bug_fixes"],
            "dependencies": ["m2"],
            "status": "pending"
        },
    ],
    "resources": {"budget": 10000, "team": 5},
    "risks": [{"id": "r1", "description": "Scope creep", "mitigation": "Change control process"}],
}

def execute_plan(plan):
    for milestone in plan["milestones"]:
        if all(dep["status"] == "completed" for dep in get_dependencies(milestone, plan)):
            for task in milestone["tasks"]:
                execute_task(task)
            milestone["status"] = "completed"
```

## Real-World Examples

1. **Software Feature**: Requirements → Design → Development → Testing → Deployment → Documentation → Rollback plan
2. **Marketing Campaign**: Research → Content creation → Channel selection → Budget allocation → Monitoring → A/B
   testing
3. **Academic Research**: Lit review → Hypothesis → Experiment design → Data collection → Analysis → Publication
   timeline
4. **Data Migration**: Data audit → Schema design → Migration scripts → Testing → Rollout → Validation checkpoints
5. **Product Launch**: Development milestones → Marketing prep → Sales enablement → Support docs → Launch event →
   Post-launch monitoring
6. **Compliance Audit**: Requirement identification → Document gathering → Gap analysis → Remediation → Review → Report
   generation
