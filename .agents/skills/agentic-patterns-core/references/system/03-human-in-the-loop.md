# Human-in-the-Loop Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
AI → Decision Gate → [Human Review → Approve/Reject/Modify] → Learn
```

Insert human oversight at critical decision points, enabling AI to handle
routine work while humans review high-stakes, ambiguous, or compliance-required
decisions.

## When to Use

- High-stakes decisions where errors have significant consequences
- Regulatory compliance requiring human oversight for legal reasons
- Quality assurance ensuring output meets standards
- Edge cases handling unusual or ambiguous situations
- Training data generation using human feedback to improve
- Trust building through gradual automation with human validation

## Where It Fits

- Content moderation: Reviewing sensitive or borderline content
- Medical diagnosis: Physician verification of AI recommendations
- Financial approvals: Human authorization for large transactions
- Legal document review: Attorney oversight of contracts
- Hiring decisions: Human review of AI-screened candidates

## Pros

- **Quality assurance** — Human judgment catches AI errors
- **Compliance** — Meets regulatory requirements
- **Learning source** — Human feedback improves system
- **Trust** — Users confident in human oversight
- **Flexibility** — Humans handle edge cases well
- **Accountability** — Clear responsibility chain
- **Risk mitigation** — Prevents costly mistakes

## Cons

- **Scalability limits** — Human bandwidth constrains throughput
- **Cost increase** — Human reviewers are expensive
- **Latency addition** — Waiting for human response delays process
- **Inconsistency** — Different humans make different decisions
- **Fatigue effects** — Quality degrades with reviewer tiredness
- **Training requirements** — Reviewers need domain expertise
- **Availability issues** — 24/7 coverage is challenging

## Implementation

```
# Example: HITL decision gate
def human_in_the_loop(ai_decision, confidence, threshold=0.9):
    if confidence >= threshold:
        return ai_decision  # Auto-approve

    # Below threshold → human review
    review_request = {
        "decision": ai_decision,
        "confidence": confidence,
        "reasoning": ai_decision.explain(),
        "priority": get_priority(ai_decision),
    }
    human_response = submit_for_review(review_request)

    # Learn from human decision
    record_feedback(ai_decision, human_response)

    return human_response.approved_decision
```

## Real-World Examples

1. **Content Moderation**: AI flags → Human reviews → Complex cases escalated → Feedback trains AI → Fatigue monitoring
2. **Loan Approval**: AI assesses risk → Human reviews borderline → Large loans manual → Explanations for denials → Audit trail
3. **Medical Imaging**: AI detects abnormalities → Radiologist confirms → Critical findings prioritized → Second opinions → Continuous learning
4. **Resume Screening**: AI filters → HR reviews shortlist → Diversity checks → Feedback improves screening → Human-led interviews
5. **Translation QC**: AI translates → Linguist reviews → Cultural checks → Terminology verification → Style consistency
6. **Autonomous Vehicles**: AI handles normal → Remote operators edge cases → Safety driver takeover → Incident review → Continuous improvement
