# Routing Pattern

**Source:
** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT
License

## Pattern

```
Request → Router → [Specialized Agent A | Specialized Agent B | Specialized Agent C] → Output
```

Direct incoming requests to the most appropriate specialized handler based on
content, confidence, or context.

## When to Use

- Multi-domain systems handling diverse request types
- Dynamic workflow selection based on input characteristics
- Resource optimization where different requests need different compute
- Specialized tool access required based on request type
- Confidence-based processing for ambiguous requests
- Load balancing across specialized agents

## Where It Fits

- Customer service: Route inquiries to appropriate departments
- Multi-modal AI: Direct requests to text/image/code pipelines
- Enterprise automation: Route tasks to business process workflows
- Content moderation: Direct content to appropriate review pipelines
- Healthcare triage: Route queries to medical specialists

## Pros

- **Specialization** — Each route optimized for specific task types
- **Scalability** — Easy to add new routes without affecting existing ones
- **Efficiency** — Requests handled by most appropriate resources
- **Flexibility** — Dynamic routing based on context and confidence
- **Clarity** — Clear separation of concerns between workflows
- **Maintainability** — Each route updated independently

## Cons

- **Router complexity** — Routing logic can become a bottleneck
- **Misrouting risks** — Incorrect routing leads to poor outcomes
- **Latency overhead** — Additional routing step adds delay
- **Edge cases** — Ambiguous requests may not fit cleanly
- **Monitoring complexity** — Need to track performance across multiple paths

## Implementation

```
# Example: Router configuration
router = {
    "technical_issue":   {"agent": "tech_support",  "confidence_threshold": 0.8},
    "billing_question":  {"agent": "finance",       "confidence_threshold": 0.8},
    "product_inquiry":   {"agent": "sales",         "confidence_threshold": 0.7},
    "complaint":         {"agent": "escalation",    "confidence_threshold": 0.6},
    "default":           {"agent": "faq",           "confidence_threshold": 0.0},
}
```

## Real-World Examples

1. **AI Customer Service**: Technical → Tech Support, Billing → Finance, Product → Sales, Complaints → Escalation,
   General → FAQ
2. **Content Platform**: Blog → Long-form Writer, Social → Short-form, Tech Docs → Technical Writer, Marketing →
   Copywriter, Translations → Localization
3. **Code Assistant**: Bug fixes → Debugging Agent, New features → Development Agent, Refactoring → Code Quality Agent,
   Testing → Test Generation Agent, Docs → Documentation Agent
4. **Financial Services**: Trading → Trading Agent, Risk → Risk Analysis Agent, Compliance → Compliance Agent,
   Reporting → Report Generation Agent, Fraud → Security Agent
5. **Education**: Math → Reasoning Agent, Language → Tutor Agent, Science → Science Expert, History → Research Agent,
   Planning → Learning Strategy Agent
