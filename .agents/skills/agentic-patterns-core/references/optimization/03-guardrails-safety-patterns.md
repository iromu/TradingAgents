# Guardrails/Safety Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Input → Sanitize → Risk Assessment → Content Moderation → Output
```

Implement multi-layer safety checks that protect users, ensure compliance,
prevent exploitation, and maintain responsible AI behavior.

## When to Use

- Public-facing systems protecting users from harmful content
- Regulated industries ensuring compliance with laws
- Brand protection maintaining company reputation
- Data privacy protecting sensitive information
- Security requirements preventing system exploitation
- Ethical AI ensuring responsible AI behavior

## Where It Fits

- Chatbots and assistants: Customer-facing AI systems
- Content generation: Automated content creation
- Healthcare AI: Medical advice and diagnosis
- Financial services: Trading and advisory systems
- Educational platforms: Student-facing AI tools

## Pros

- **Risk mitigation** — Prevents harmful outputs
- **Compliance** — Meets regulatory requirements
- **Brand protection** — Maintains reputation
- **User safety** — Protects from inappropriate content
- **Security** — Prevents exploitation attempts
- **Consistency** — Uniform safety standards
- **Auditability** — Clear safety decision trails

## Cons

- **False positives** — May block legitimate requests
- **Latency increase** — Safety checks add processing time
- **User frustration** — Over-restrictive filtering
- **Complexity** — Multiple layers of checks
- **Maintenance burden** — Policies need regular updates
- **Context blindness** — May miss nuanced safety issues
- **Cost overhead** — Additional processing and monitoring

## Implementation

```
# Example: Multi-layer safety guardrails
SAFETY_LAYERS = [
    {"name": "input_sanitize", "check": remove_pii, "severity": "high"},
    {"name": "prompt_injection", "check": detect_prompt_injection, "severity": "critical"},
    {"name": "content_filter", "check": filter_inappropriate_content, "severity": "medium"},
    {"name": "output_sanitize", "check": sanitize_output, "severity": "high"},
    {"name": "compliance", "check": verify_compliance, "severity": "critical"},
]

def apply_guardrails(input_text, output_text, user_role):
    for layer in SAFETY_LAYERS:
        if layer["name"] == "input_sanitize":
            input_text = layer["check"](input_text)
        elif layer["name"] == "output_sanitize":
            output_text = layer["check"](output_text)
        elif layer["check"](input_text, output_text, user_role):
            log_violation(layer["name"], user_role)
            if layer["severity"] == "critical":
                return BLOCKED
            elif layer["severity"] == "high":
                return WARN

    return ALLOWED
```

## Real-World Examples

1. **Social Media**: Hate speech detection → PII redaction → Misinformation flagging → Violence blocking → Copyright detection → Appeal process
2. **Healthcare Chatbot**: Medical disclaimers → Emergency detection → Drug interaction warnings → Privacy protection → Scope enforcement → Professional referral
3. **Financial Advisory**: Investment risk warnings → Regulatory compliance → Insider trading prevention → Client suitability → Market manipulation detection → Audit trail
4. **Education AI**: Age-appropriate filtering → Academic integrity → Bullying prevention → Personal info protection → Topic blocking → Parent/teacher overrides
5. **Enterprise AI**: Data classification → Access control → Confidentiality → Compliance checking → Security threat detection → Activity logging
6. **Content Generation**: Copyright prevention → Trademark protection → Defamation blocking → Bias detection → Fact-checking → Quality standards
