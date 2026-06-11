# Memory Management Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Input → Classify → Store → [Retrieve when needed] → Output
```

Implement multi-tier memory systems (short-term, episodic, long-term) that
enable agents to maintain context, personalize interactions, and accumulate
knowledge across sessions.

## When to Use

- Conversational continuity: Maintaining context across interactions
- Personalization: Remembering user preferences and history
- Learning systems: Accumulating knowledge over time
- Complex workflows: Tracking state across multiple steps
- User sessions: Managing multi-turn conversations
- Knowledge accumulation: Building domain expertise over time

## Where It Fits

- Customer service bots: Remembering previous interactions and issues
- Personal assistants: Tracking user preferences and routines
- Educational tutors: Remembering student progress and weaknesses
- Project management: Maintaining project context and history
- Research assistants: Accumulating findings across sessions

## Pros

- **Context preservation** — Maintains conversation continuity
- **Personalization** — Enables tailored responses based on history
- **Learning capability** — Improves performance through experience
- **Efficiency** — Avoids repeating previous work
- **User experience** — More natural, human-like interactions
- **Knowledge building** — Accumulates valuable information over time

## Cons

- **Storage costs** — Memory systems require database infrastructure
- **Privacy concerns** — Storing user data raises privacy issues
- **Context window limits** — Must manage finite token budgets
- **Retrieval complexity** — Finding relevant memories can be challenging
- **Data staleness** — Old memories may become outdated or irrelevant
- **Performance overhead** — Memory operations add latency

## Implementation

```
# Example: Three-tier memory system
class AgentMemory:
    def __init__(self):
        self.short_term = {}      # Current session context
        self.episodic = []         # Recent interactions
        self.long_term = {}        # Persistent knowledge

    def store(self, memory_type, key, value, ttl=None):
        if memory_type == "short_term":
            self.short_term[key] = value
        elif memory_type == "episodic":
            self.episodic.append({"key": key, "value": value, "timestamp": now()})
        elif memory_type == "long_term":
            self.long_term[key] = value
            if ttl:
                self.schedule_cleanup(key, ttl)

    def retrieve(self, query, scope="all"):
        results = []
        if scope in ("short_term", "all"):
            results.extend(self._search(self.short_term, query))
        if scope in ("episodic", "all"):
            results.extend(self._search_recent(self.episodic, query))
        if scope in ("long_term", "all"):
            results.extend(self._search(self.long_term, query))
        return self.rank_relevance(results, query)
```

## Real-World Examples

1. **Customer Support**: Short-term (current conversation) → Episodic (past tickets) → Long-term (preferences and history)
2. **Shopping Assistant**: Short-term (current session) → Episodic (past purchases) → Long-term (style preferences and sizes)
3. **Code Assistant**: Short-term (current session) → Episodic (recent fixes) → Long-term (project architecture and conventions)
4. **Medical Bot**: Short-term (current symptoms) → Episodic (recent appointments) → Long-term (medical history and allergies)
5. **Educational Tutor**: Short-term (current lesson) → Episodic (recent quiz results) → Long-term (learning style and pace)
6. **Project Manager**: Short-term (current task) → Episodic (recent meetings) → Long-term (project goals and constraints)
