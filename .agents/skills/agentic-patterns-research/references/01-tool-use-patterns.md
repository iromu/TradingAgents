# Tool Use & Environment Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Agent-First Tooling & Logging

### Core Concept

Design tools specifically for agent consumption:

- **Clear descriptions** — Agents need to understand what each tool does
- **Structured outputs** — Predictable, parseable results
- **Consistent interfaces** — Similar tools follow similar patterns
- **Comprehensive logging** — Every tool call is logged for debugging

### Key Principles

| Principle | Description |
|-----------|-------------|
| **Agent-friendly naming** | Clear, descriptive tool names |
| **Rich descriptions** | Detailed tool descriptions with examples |
| **Structured I/O** | JSON schemas for inputs and outputs |
| **Error handling** | Clear error messages with actionable info |
| **Idempotency** | Same input produces same output |

## Code-First Tool Interface Pattern

### Core Concept

Generate tool interfaces from code rather than defining them manually:

```python
# The tool is automatically discovered from the function signature
def search_database(query: str, max_results: int = 10) -> list:
    """Search the database for matching records."""
    ...
```

### Benefits

- **Less manual work** — No need to write tool descriptions
- **Type safety** — Function signatures provide validation
- **Self-documenting** — Code is the documentation
- **Easy to test** — Regular functions can be unit tested

## Code-Then-Execute Pattern

### Core Concept

1. LLM generates code (not tool calls)
2. Code is statically analyzed
3. If analysis passes, code is executed
4. If analysis fails, code is revised

### Key Properties

- **Flexibility** — Any code can be generated
- **Safety** — Static analysis catches issues before execution
- **Sandboxed** — Code runs in isolated environment

### When to Use

- Complex tool orchestration that can't be expressed as individual tool calls
- When you need agents to compose their own workflows

## Intelligent Bash Tool Execution

### Core Concept

Execute shell commands intelligently:

- **Contextual understanding** — Agent understands what command to run
- **Safety checks** — Dangerous commands are blocked or require approval
- **Output parsing** — Command output is parsed and structured
- **Error handling** — Failed commands trigger retry or escalation

## MCP Pattern Injection

### Core Concept

Inject Model Context Protocol (MCP) tools dynamically:

- **Tool discovery** — Agents discover available tools at runtime
- **Tool registration** — Tools are registered with descriptions and schemas
- **Tool execution** — Agents call tools through MCP protocol
- **Tool updates** — Tools can be added/removed without agent changes

### MCP Architecture

```
Host (Agent) ←→ Client ←→ Server ←→ Tools/Resources
```

## Unified Tool Gateway

### Core Concept

Single entry point for all tool access:

- **Centralized routing** — All tool calls go through gateway
- **Policy enforcement** — Gateway enforces security policies
- **Observability** — All tool calls are logged
- **Caching** — Repeated calls can be cached

## Other Tool Use Patterns

| Pattern | Description | Key Benefit |
|---------|-------------|-------------|
| **Agent-First Tool Discovery** | Agents discover tools autonomously | No manual tool registration |
| **Progressive Tool Discovery** | Tools discovered incrementally | Faster initial context |
| **Dual-Use Tool Design** | Tools work for both humans and agents | Reusable tool definitions |
| **CLI-First Skill Design** | Design tools as CLI commands | Leverage existing tooling |
| **CLI-Native Agent Orchestration** | Orchestrate agents via CLI | Familiar workflow |
| **Shell Command Contextualization** | Provide context for shell commands | Better command selection |
| **Patch Steering via Prompting** | Guide tool selection through prompts | No code changes needed |
| **Tool Use Steering via Prompting** | Direct agent tool selection | Simple, flexible |
| **Agent SDK for Programmatic Control** | Control agents via SDK | Full programmatic access |
| **Cross-Protocol Agent Discovery** | Discover agents across protocols | Interoperability |
| **LLM-Friendly API Design** | Design APIs for LLM consumption | Better agent interactions |
| **Code-Over-API Pattern** | Prefer code over API calls | More flexible |
| **Dynamic Code Injection** | Fetch and execute code on demand | On-demand capabilities |
| **Egress Lockdown** | No outbound channels from agent | Security |
| **Multi-Platform Communication** | Aggregate from multiple platforms | Unified view |
| **Multi-Platform Webhook Triggers** | Trigger agents from multiple platforms | Event-driven |
| **Virtual Machine Operator Agent** | Agent manages VMs | Infrastructure automation |
| **Visual AI Multimodal Integration** | Agent processes images/video | Multimodal capabilities |
| **Agentic Search Over Vector Embeddings** | Agent uses tools instead of vector search | More accurate results |
| **AI Web Search Agent Loop** | Agent performs web searches | Live information |
| **Static Service Manifest** | Define available services for agents | Clear service catalog |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: agent-first-tooling-and-logging-report.md, code-first-tool-interface-pattern-report.md, code-then-execute-pattern-report.md, intelligent-bash-tool-execution-report.md, mcp-pattern-injection-report.md*
