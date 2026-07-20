# Embabel Agent Examples

Actionable walkthroughs of real Embabel agent workflows in Java. Each example shows how to define actions, use tools, and compose agents around an LLM core.

---

## Overview

Embabel agents are built from `@Action` methods that run through an `Ai` instance. Actions can call LLMs, invoke tools (web search, MCP, etc.), and carry structured context across the full call chain.

> **Full source:** All examples live in the [embabel-agent-examples](https://github.com/embabel/embabel-agent-examples) repository. The snippets below are distilled from the official docs.

---

## Horoscope Agent (StarNewsFinder)

A multi-action agent that takes a person's name and star sign, fetches their horoscope from an API, finds relevant news stories using web tools, and composes an entertaining piece.

### Key pattern: interaction-level tool call context

The `findNewsStories` action attaches domain metadata to every tool invoked within that single LLM call. The context flows through to remote MCP tools as `_meta`:

```java
@Action
public RelevantNewsStories findNewsStories(
        StarPerson person, Horoscope horoscope, Ai ai) {

    var interactionContext = ToolCallContext.of(Map.of(
            "personName", person.name(),
            "starSign",   person.sign(),
            "feature",    "star-news-finder"
    ));

    return ai
            .withDefaultLlm()
            .withId("find_news_stories")
            .withToolGroup(CoreToolGroups.WEB)
            .withToolCallContext(interactionContext)
            .createObject(prompt, RelevantNewsStories.class);
}
```

**Context resolution rules:**

- Interaction-level values (set via `withToolCallContext`) win on conflict.
- Process-level context (set via `ProcessOptions.withToolCallContext()`) is merged in as a fallback.
- Context propagates to remote MCP tools as `_meta`.

### Verifying context propagation

Run the agent from the shell with the diagnostic flag:

```
sc tenantId=acme
x "Natasha is Pisces. Find news for her" -d
```

The `check_tool_call_context` diagnostic tool (bundled in the WEB tool group) logs the full context map:

```
[task-2] INFO ContextDiagnosticTools - ToolCallContext received: {personName=Alex, starSign=Cancer, feature=star-news-finder, tenantId=acme}
```

### What this demonstrates

| Concept | How |
|---------|-----|
| Multi-action agent | `StarPerson`, `Horoscope`, `RelevantNewsStories` as action return types |
| LLM calls | `ai.createObject(prompt, Class)` for structured output |
| Non-LLM actions | Tool groups like `CoreToolGroups.WEB` for web search |
| Context propagation | `withToolCallContext()` carries metadata to every tool in the call |
| Goal achievement | End-to-end: user input → horoscope + news → composed piece |

---

## Movies Agent

The movies example is a companion workflow demonstrating another agent pattern. The source docs contain only a section header with no further detail. See the full examples repository for the complete implementation.

---

## Full Examples Repository

All source code, including the horoscope and movies agents, is available at:

**[github.com/embabel/embabel-agent-examples](https://github.com/embabel/embabel-agent-examples)**

For deeper dives into `ToolCallContext` (including `@LlmTool` injection and `ToolCallContextMcpMetaConverter`), see the Embabel reference documentation.---

*Source: Embabel Agent v1.0.0 documentation*
