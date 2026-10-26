# Context & Memory Patterns

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Context Window Anxiety Management

### Problem

LLMs have limited context windows (typically 128K-200K tokens). As agents accumulate conversation history, tool outputs, and retrieved documents, context window pressure increases, leading to:

- **Lost information** — Important details pushed out of context
- **Increased costs** — Processing unnecessary tokens
- **Degraded quality** — Models perform worse with too much context

### Solutions

| Pattern | Description | When to Use |
|---------|-------------|-------------|
| **Context-Minimization** | Strip untrusted data after processing | Security-focused agents |
| **Context Window Auto-Compaction** | Compress history when approaching limits | Long-running agents |
| **Curated Code Context Window** | Selectively include relevant code | Code agents |
| **Semantic Context Filtering** | Filter context by relevance | RAG agents |
| **Prompt Caching via Prefix Preservation** | Cache exact prompt prefixes | Repeated operations |

## Context-Minimization Pattern

### Core Concept

Strip untrusted data from context after processing to prevent prompt injection:

```
1. Receive untrusted input (email, web page, API response)
2. Process with LLM to extract needed information
3. Strip original untrusted content from context
4. Continue with only extracted, sanitized information
```

### Why It Matters

- Prevents prompt injection through untrusted data
- Reduces context window usage by 10-100x
- Maintains security boundaries between trusted and untrusted data

## Episodic Memory Retrieval & Injection

### Core Concept

Store past agent experiences (episodes) and retrieve relevant ones when making decisions:

```
Episode = {task, output, reflection, outcome}

When making a decision:
1. Retrieve similar past episodes
2. Inject them into context
3. Agent learns from past experience
```

### Memory Hierarchy

| Layer | Type | Duration | Example |
|-------|------|----------|---------|
| **Episodic** | Specific experiences | Long-term | "Last time I did X, it failed because Y" |
| **Semantic** | General knowledge | Long-term | "Python dict.get() returns None by default" |
| **Procedural** | How-to knowledge | Long-term | "To fix this error, run: pip install..." |

## Working Memory via TodoWrite

### Core Concept

Use structured task lists as working memory:

- **TodoWrite** — Create and update task lists
- **Finish** — Signal completion
- **Structured state** — Clear, parseable agent state

### Why It Works

- Provides explicit, structured working memory
- Easy to track progress and priorities
- Compatible with tool-use patterns

## Filesystem-Based Agent State

### Core Concept

Persist agent state to the filesystem for:

- **Crash recovery** — Resume from checkpoint
- **Audit trail** — Track all state changes
- **Long-running workflows** — State survives agent restarts
- **Multi-agent coordination** — Shared state between agents

### Implementation

```python
# Save state
state = {
    'current_step': 3,
    'completed_steps': [1, 2],
    'context': {...},
    'timestamp': now()
}
save_json('/agent/state.json', state)

# Restore state
state = load_json('/agent/state.json')
```

## Other Context & Memory Patterns

| Pattern | Description | Key Benefit |
|---------|-------------|-------------|
| **Dynamic Context Injection** | Inject context only when needed | Reduces token usage |
| **Progressive Disclosure** | Show large files incrementally | Avoids context overflow |
| **Self-Identity Accumulation** | Agent builds self-description over time | Better long-term behavior |
| **Schema-Guided Graph Retrieval** | Use schema to guide multi-hop retrieval | Better RAG quality |
| **Session-Scoped Context Runtime** | Manage context per session | Clean session boundaries |
| **Tool Search Lazy Loading** | Load tool descriptions on demand | Faster initial context |
| **Layered Configuration Context** | Hierarchical config resolution | Clean config management |
| **Memory Synthesis from Logs** | Extract patterns from execution logs | Long-term learning |
| **Proactive State Externalization** | Save state before risky operations | Crash recovery |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: context-window-anxiety-management-report.md, context-minimization-pattern-report.md, episodic-memory-retrieval-injection-report.md, working-memory-via-todos.md, filesystem-based-agent-state-report.md*
