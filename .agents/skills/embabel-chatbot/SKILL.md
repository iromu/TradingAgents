---
name: embabel-chatbot
description: Build agentic chatbots with Retrieval-Augmented Generation (RAG) using the Embabel framework. Use this skill when the user wants to build, configure, or extend a chatbot with RAG, guardrails, reasoning, structured output, or custom chat extensions. Trigger when the user mentions chatbots, conversational AI, RAG chatbots, embabel chat, ragbot, or wants to add chat capabilities to a Java application. Also trigger when the user wants to configure chat memory, chat actions/tool calling, prompt templates, or chat history persistence.
---

# Embabel Chatbot Skill

Build production-ready agentic chatbots with RAG using the [Embabel](https://github.com/embabel/embabel) framework.

## When to Use

- User wants to build a chatbot or conversational AI agent
- User wants to add RAG (Retrieval-Augmented Generation) to a chatbot
- User wants to configure chat memory, guardrails, or reasoning
- User wants structured JSON output from a chatbot
- User wants tool calling / chat actions
- User wants to persist chat history to a database
- User wants to create custom chat extensions

## Prerequisites

- Java 17+
- Spring Boot project (or plain Java with Embabel dependencies)
- Embabel dependency:

```xml
<dependency>
    <groupId>com.embabel</groupId>
    <artifactId>embabel-agent</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

## Quick Start

Here's the minimal chatbot that works out of the box:

```java
import com.embabel.chatbot.builder.ChatbotBuilder;
import com.embabel.chatbot.session.ChatSession;
import com.embabel.chatbot.memory.InMemoryChatMemory;
import com.embabel.chatbot.options.ChatOptions;

var chatMemory = InMemoryChatMemory.builder().build();
var chatSession = ChatSession.builder().chatMemory(chatMemory).build();
var chatOptions = ChatOptions.builder()
    .model("qwen-plus")
    .maxTokens(2048)
    .temperature(0.7)
    .build();

var chatbot = ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatSession(chatSession)
    .chatOptions(chatOptions)
    .build();

String response = chatbot.chat("What is RAG?");
System.out.println(response);
```

That's it — you have a working chatbot. Everything else is opt-in.

## What to Add Next

| Need | What to do | Learn more |
|------|-----------|------------|
| **External knowledge** | Add RAG with document sources | [RAG Architecture](references/03-rag-architecture.md) |
| **Safety** | Add guardrails against injection/jailbreaks | [Guardrails](references/04-guardrails.md) |
| **Deep reasoning** | Enable thinking mode | [Reasoning](references/05-reasoning.md) |
| **Machine-readable output** | Force JSON responses | [Structured Output](references/07-structured-output.md) |
| **Pre-processing** | Add custom chat extensions | [Chatbot API](references/01-chatbot-api.md) → ChatExtension |
| **Tool calling** | Register actions/functions | [Chatbot API](references/01-chatbot-api.md) → ChatActions |
| **Persistence** | Store chat history in a DB | [Chat History Store](references/02-chat-history-store.md) |
| **Config-driven** | Use `chatbot.yaml` | [Chatbot Patterns](references/06-chatbot-patterns.md) |

Each reference file has full API details, code examples, and usage patterns.

## Pitfalls

- **Chat history grows unbounded** — set `maxMessages` or `maxTokens` on `InMemoryChatMemory`
- **RAG retrieves irrelevant docs** — use filters to narrow the source scope
- **Guardrails block legitimate queries** — tune guardrail thresholds; start with just prompt injection guard
- **Thinking mode is slow** — only enable for complex reasoning tasks; disable for simple Q&A
- **JSON mode can fail** — always validate and handle malformed JSON responses
- **Model capabilities vary** — check `supportsJson()`, `supportsTools()`, `supportsVision()` before using features

## Checklist

- [ ] Basic chatbot works (see Quick Start above)
- [ ] RAG sources configured (see [RAG Architecture](references/03-rag-architecture.md))
- [ ] Guardrails added (see [Guardrails](references/04-guardrails.md))
- [ ] Reasoning enabled if needed (see [Reasoning](references/05-reasoning.md))
- [ ] Structured output if needed (see [Structured Output](references/07-structured-output.md))
- [ ] Chat extension for pre-processing (see [Chatbot API](references/01-chatbot-api.md))
- [ ] Chat actions for tool calling (see [Chatbot API](references/01-chatbot-api.md))
- [ ] Chat history persisted (see [Chat History Store](references/02-chat-history-store.md))
- [ ] Tested with real documents and user queries
