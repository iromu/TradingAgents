# Tool Use (Function Calling) Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Agent → [Identify Need → Select Tool → Call Tool → Process Result] → Continue/Output
```

Enable agents to call external tools and APIs to access data, perform actions,
and integrate with existing systems.

## When to Use

- External data access: Agents need real-time or dynamic information
- System integration: Connecting to databases, APIs, or services
- Computational tasks: Precise calculations or data processing needed
- File operations: Reading, writing, or manipulating files
- Action execution: Agents need to perform concrete actions
- Multi-step workflows: Combining AI reasoning with tool execution

## Where It Fits

- Research assistants: Web search, document retrieval, fact-checking
- Data analysis: Database queries, calculations, visualizations
- DevOps automation: System commands, deployment tools, monitoring
- Customer service: CRM access, ticket management, knowledge base
- Content management: File operations, publishing tools, asset management

## Pros

- **Capability extension** — Agents perform actions beyond text generation
- **Real-time data** — Access to current information not in training data
- **Precision** — Exact calculations and deterministic operations
- **Integration** — Seamless connection to existing systems and services
- **Automation** — Complete end-to-end workflows without human intervention
- **Auditability** — Clear log of all tool usage and parameters

## Cons

- **Security risks** — Tool access must be carefully controlled
- **Error propagation** — Tool failures can break entire workflows
- **Latency addition** — Each tool call adds processing time
- **Cost accumulation** — External API calls may incur charges
- **Dependency risks** — Reliance on external services availability
- **Data sensitivity** — Need careful handling of credentials and private data

## Implementation

```
# Example: Tool definition and execution
TOOLS = {
    "search_web": {
        "description": "Search the web for current information",
        "parameters": {"query": {"type": "string", "description": "Search query"}},
        "execute": lambda q: web_search(q)
    },
    "read_file": {
        "description": "Read contents of a file",
        "parameters": {"path": {"type": "string", "description": "File path"}},
        "execute": lambda p: read_file(p)
    },
    "calculate": {
        "description": "Perform precise calculations",
        "parameters": {"expression": {"type": "string", "description": "Math expression"}},
        "execute": lambda e: safe_eval(e)
    },
}

def use_tool(agent, tool_name, params):
    tool = TOOLS[tool_name]
    result = tool["execute"](**params)
    log_tool_use(tool_name, params, result)
    return result
```

## Real-World Examples

1. **Financial Analysis**: Stock price API, portfolio calculator, historical DB queries, chart generation, email distribution
2. **Code Development**: File system access, compiler/interpreter, Git commands, testing frameworks, documentation generators
3. **E-commerce Orders**: Inventory queries, payment processing, shipping integrations, email/SMS notifications, CRM updates
4. **Research Paper**: Academic DB searches, citation management, PDF parsing, reference formatting, plagiarism checking
5. **Smart Home**: IoT device APIs, weather service, calendar access, energy monitoring, security system controls
6. **HR Recruitment**: Resume parsing, job board APIs, calendar scheduling, email automation, background checks, video interviews
