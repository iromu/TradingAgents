# Security & Safety Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Action-Selector Pattern (Primary Security Pattern)

### Core Concept

> "Treat the LLM as an instruction decoder, not a live controller. The model maps user intent to a pre-approved action ID plus schema-validated parameters, and execution is handled by deterministic code."

### Key Security Property

**Tool outputs are prevented from re-entering the LLM's context**, breaking the feedback loop that enables cascading prompt injection attacks.

### Architecture

```
User Input (Natural Language)
         ↓
    LLM Decoder
         ↓
    Action ID Selection (from allowlist)
         ↓
    Parameter Schema Validation
         ↓
    Deterministic Execution
         ↓
    Result (NOT fed back to LLM)
```

### Formal Security Guarantees

| Guarantee | Description |
|-----------|-------------|
| **Allowlist guarantee** | Only registered actions can be executed |
| **Fail-closed behavior** | Unknown actions rejected, not executed with default |
| **Schema enforcement** | Parameters must pass validation before execution |
| **Auditability** | Every action logged before execution |
| **No output feedback** | Tool outputs guaranteed NOT to return to LLM |

### What It Protects Against

1. **Prompt injection through untrusted data** — Emails, web pages, API responses cannot influence action selection
2. **Arbitrary code execution** — LLM restricted to selecting from pre-approved allowlist
3. **Unauthorized tool access** — Only explicitly registered actions can be executed
4. **Control-flow hijacking** — Malicious content cannot change which tools are executed next
5. **Multi-step attack chains** — Breaking the feedback loop prevents cascading attacks

### What It Does NOT Protect Against

1. **Parameter poisoning** — Malicious data CAN influence parameters passed to approved tools
2. **Output content attacks** — Actual content produced by tools can still be malicious
3. **Lethal trifecta combinations** — Action selector alone does NOT prevent private data + external communication + untrusted input
4. **Schema validation bypasses** — Weak schemas can allow malicious parameters
5. **Authorization bypass** — Validates WHAT action but not WHETHER user is authorized

### Implementation

#### Allowlist Design

```python
ALLOWLIST = {
    "search_database": search_v2,
    "create_report": create_report,
    "send_notification": send_notification,
    # ... explicitly registered actions only
}
```

#### Parameter Validation

```python
class SearchInput(BaseModel):
    query: str = Field(..., min_length=1)
    max_results: int = Field(default=10, le=100)

class ExecuteSQLInput(BaseModel):
    query_id: str  # Pre-approved query template ID, NOT raw SQL
    params: dict   # Parameterized, never raw SQL string
```

#### Naming Conventions

| Rule | Example |
|------|---------|
| Verb-noun format | `send_email`, `create_issue`, `update_database` |
| Snake_case | lowercase with underscores |
| Domain-specific prefixes | `billing_charge_card`, `inventory_update_stock` |
| Max 30 characters | Keep concise |
| Avoid vague names | NOT `handleRequest`, `doAction`, `execute` |

### Anti-Patterns

| Anti-Pattern | Why Problematic | Solution |
|--------------|-----------------|----------|
| **Over-constraining actions** | Forces users to circumvent system | Include capability-complete actions with parameter constraints |
| **Breaking "No Feedback" rule** | Re-introduces prompt injection vulnerability | Use code-based state transitions for multi-step workflows |
| **Coarse-grained "God actions"** | Defeats security through allowlist validation | Create constrained, specific actions |
| **Dynamic allowlist mutation** | Unpredictable security surface, impossible to audit | Version action contracts, deploy new allowlist version |
| **Neglecting parameter validation** | Attackers can pass malicious parameters | Always validate parameters against schemas |

## Hook-Based Safety Guard Rails

### Concept

Runtime safety hooks that intercept and validate tool execution:

- **Command blocker** — Prevents dangerous shell commands
- **Syntax checker** — Validates code before execution
- **Rate limiter** — Prevents excessive API calls
- **Content filter** — Blocks sensitive data exposure

### When to Use

- As a supplement to Action-Selector (defense in depth)
- For tools that require more nuanced policies than allowlists
- When runtime context matters (e.g., time of day, user role)

## Sandboxed Tool Authorization

### Concept

Enforces capability-based access control:

- Tools are categorized by capability (reader, processor, writer)
- Authorization policies enforce capability-based access
- Action selector ensures only safe combinations can be chained

### Relationship to Action-Selector

Action-Selector operates at planning level; Sandboxed Authorization operates at execution level. They are complementary.

## Zero-Trust Agent Mesh

### Concept

Every agent-tool interaction must be authenticated and authorized, regardless of network location.

### Key Principles

1. **Never trust, always verify** — Every interaction is validated
2. **Least privilege** — Agents get minimum required permissions
3. **Micro-segmentation** — Tools and agents are isolated
4. **Continuous monitoring** — All interactions are logged and analyzed

## Other Security Patterns

| Pattern | Focus | Key Mechanism |
|---------|-------|---------------|
| **PII Tokenization** | Data privacy | Replace PII with tokens before LLM processing |
| **Egress Lockdown** | Prevent data exfiltration | No outbound channels from agent environment |
| **Policy-Gated Tool Proxy** | Access control | Central policy enforcement point |
| **Cryptographic Governance Audit Trail** | Accountability | Tamper-proof logs of all actions |
| **Deterministic Security Scanning** | Code security | Automated security analysis in build loop |
| **External Credential Sync** | Secure auth | Credentials managed externally, never in agent context |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: action-selector-pattern-report.md, action-selector-academic-sources-report.md, action-selector-industry-implementations-report.md, hook-based-safety-guard-rails-report.md, zero-trust-agent-mesh.md*
