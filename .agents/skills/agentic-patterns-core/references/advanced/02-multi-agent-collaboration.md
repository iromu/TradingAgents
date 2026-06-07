# Multi-Agent Collaboration Pattern

**Source:
** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT
License

## Pattern

```
Coordinator → [Agent1 | Agent2 | Agent3 | Agent4] → Merge → Output
```

Coordinate multiple specialized agents working in parallel or sequence, each
optimized for specific tasks, with a coordinator managing communication and
output aggregation.

## When to Use

- Complex, multi-faceted problems requiring diverse expertise
- Parallel workstreams where subtasks can be handled simultaneously
- Specialized knowledge requirements where different aspects need different skills
- Scale and efficiency where large projects benefit from division of labor
- Quality through specialization where expertise depth matters
- Iterative refinement requiring multiple perspectives

## Where It Fits

- Software development: Design, coding, testing, documentation agents
- Content production: Research, writing, editing, publishing agents
- Financial analysis: Data collection, analysis, risk assessment, reporting agents
- Customer service: Triage, technical, billing, escalation agents
- Research projects: Literature review, experimentation, analysis, synthesis agents

## Pros

- **Specialization benefits** — Each agent optimized for specific tasks
- **Parallel processing** — Multiple agents work simultaneously
- **Scalability** — Easy to add new specialist agents
- **Modularity** — Agents developed and updated independently
- **Robustness** — Failure of one agent doesn't crash entire system
- **Quality improvement** — Multiple perspectives and validation steps

## Cons

- **Coordination complexity** — Managing inter-agent communication is challenging
- **Overhead costs** — Multiple agents mean multiple API calls and resources
- **Context management** — Maintaining shared understanding across agents
- **Debugging difficulty** — Tracing issues across multiple agents
- **Latency accumulation** — Handoffs between agents add delays
- **Conflict resolution** — Agents may disagree or produce incompatible outputs

## Implementation

```
# Example: Multi-agent coordination
class Coordinator:
    def __init__(self):
        self.agents = {
            "researcher": ResearchAgent(),
            "writer": WritingAgent(),
            "editor": EditingAgent(),
            "reviewer": ReviewAgent(),
        }
        self.shared_context = {}

    def coordinate(self, task):
        # Phase 1: Research (parallel agents)
        findings = self.parallel_execute(
            [self.agents["researcher"].analyze(topic)
             for topic in task.topics]
        )

        # Phase 2: Write (sequential)
        draft = self.agents["writer"].compose(findings, self.shared_context)

        # Phase 3: Review and edit
        draft = self.agents["editor"].improve(draft)
        draft = self.agents["reviewer"].validate(draft)

        return draft
```

## Real-World Examples

1. **News Production**: Gatherer → Fact Checker → Writer → Editor → SEO → Publisher
2. **Investment Analysis**: Market Data → Fundamental Analysis → Technical Analysis → Risk Assessment → Report
   Generator → Compliance
3. **E-commerce Launch**: Market Research → Product Description → Pricing → Inventory → Marketing → Customer Service
4. **Legal Review**: Document Parser → Clause Analysis → Risk Identifier → Compliance Checker → Summary Generator →
   Recommendation
5. **Bug Resolution**: Bug Triage → Code Analysis → Solution Designer → Implementation → Testing → Documentation
6. **Academic Review**: Literature Review → Methodology Critic → Statistical Validator → Writing Quality → Citation
   Checker → Summary Writer
