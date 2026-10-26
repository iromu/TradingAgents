---
name: embabel-chatbot
description: >-
  Build agentic chatbots with tool-based RAG on the JVM using Embabel. Use when building a conversational agent backed by a long-lived AgentProcess, exposing a Chatbot bean and ChatSession, handling UserMessage/AssistantMessage via @Action(trigger = UserMessage::class), persisting conversations with embabel-chat-store (ConversationStoreType.STORED, ConversationFactoryProvider, AgentProcessChatbot.utilityFromPlatform), or wiring agentic RAG with ToolishRag, SearchOperations, VectorSearch/TextSearch, PropertyFilter/EntityFilter metadata+entity filters, LuceneSearchOperations, TikaHierarchicalContentReader ingestion, AddTitlesChunkTransformer chunking, or persona/objective Jinja prompts (ragbot.jinja). Trigger on: embabel chatbot, embabel rag, ragbot, chatbot with retrieval, ChatSession, conversation store, toolish rag. Do NOT use for the core @Agent/@Action planner framework, MCP publishing, or provider config (see the embabel-agent skill).
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 1.5.1
  category: framework
  tags: [embabel, chatbot, rag, kotlin, java, spring]
---

# Embabel Chatbot Skill (v1.5.1)

Build agentic, RAG-grounded chatbots with the [Embabel](https://github.com/embabel/embabel-agent) framework.

An Embabel chatbot is **not** a thin wrapper around a chat-completions API. It is a long-lived
`AgentProcess` that pauses between user messages: the blackboard keeps state across the whole
session, and the planner picks which `@Action` handles each message (Utility AI is usually the
best fit). Retrieval is **agentic and tool-based** — the LLM decides when and how to search via
`ToolishRag`. This is the mental model that distinguishes Embabel chatbots from LangChain4j-style
`ChatClient`/`ChatbotBuilder` APIs, which **do not exist** in this framework.

> **Scope.** This skill covers the chatbot + RAG layer (`com.embabel.chat.*`, `com.embabel.agent.rag.*`,
> the `@Action(trigger=…)` pattern, and `embabel-chat-store`). For the core agent framework
> (`@Agent`, planners, `@State`, MCP publishing, provider selection) use the companion **embabel-agent** skill.

## Instructions

1. Confirm the task belongs here — core @Agent/@Action planner framework, MCP publishing and provider config go to `embabel-agent`; DICE persistence to `embabel-dice`; observability to `embabel-otel` (see "When NOT to Use").
2. Check "Prerequisites" (starters, versions, required beans) before writing code, then reproduce the minimal working chatbot from "Quick Start" verbatim — don't improvise a new skeleton.
3. Extend a working chatbot via "What to Add Next" (RAG sources, filters, conversation store, persona prompts) rather than rewriting the wiring.
4. Cross-check "Pitfalls" and the "Checklist" before finalizing.

## When to Use

- Build a chatbot / conversational agent on the JVM with Embabel
- Add agentic, tool-based RAG (retrieval the LLM drives) to a chatbot
- Persist conversation history across restarts (persistent `Conversation`)
- Scope retrieval per-user / per-tenant with metadata + entity filters
- Ingest documents (Tika) and tune chunking (title-enriched chunks)
- Drive persona / objective / guardrail behaviour from Jinja prompt templates

## When NOT to Use

| Scenario | Use instead |
|----------|-------------|
| Core `@Agent` / planner / `@State` / MCP publishing / provider config | **embabel-agent** skill |
| A fixed-flow FAQ bot with no planning (no LLM-driven action choice) | A plain state machine / Spring AI `ChatClient` directly |
| Non-JVM chatbots (Python/JS) | LangGraph, LangChain JS, etc. |
| Building an embedding store or vector DB from scratch | A vector DB's own SDK |

## Prerequisites

- JVM (Java 17+ or Kotlin), Spring Boot
- Embabel agent starter (see the **embabel-agent** skill for the provider starter you need)
- RAG modules (documented coordinates, verbatim from the RAG reference chapter):

```xml
<!-- Vector + full-text search store -->
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-rag-lucene</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
<!-- Document parsing (PDF/Word/HTML/Markdown) -->
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-rag-tika</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

- For persistent conversations, **additionally** add the chat store (documented coordinate):

```xml
<dependency>
    <groupId>com.embabel.chat</groupId>
    <artifactId>embabel-chat-store</artifactId>
</dependency>
```

> The chatbot API types (`Chatbot`, `ChatSession`, `Conversation`, the message types, the in-memory
> conversation types, `MessageEvent`) live in `embabel-agent-api` under `com.embabel.chat.*` and are
> available **without** `embabel-chat-store`. `StoredConversationFactory` / `StoredConversation` ship in
> the separate `embabel-chat-store` artifact.

## Quick Start

A minimal, working chatbot: one action that answers a `UserMessage` using a `ToolishRag` reference,
plus a `Chatbot` bean built from the platform. (Kotlin; Java equivalents are in the references.)

```kotlin
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.EmbabelComponent
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.rag.service.SearchOperations
import com.embabel.agent.rag.tools.ToolishRag
import com.embabel.chat.Chatbot
import com.embabel.chat.Conversation
import com.embabel.chat.UserMessage                // UserMessage/AssistantMessage are top-level in com.embabel.chat
import com.embabel.chat.agent.AgentProcessChatbot
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EmbabelComponent
class ChatActions(
    searchOperations: SearchOperations,
    private val properties: RagbotProperties,
) {
    private val toolishRag = ToolishRag(
        "sources",
        "Sources for answering user questions",
        searchOperations,
    )

    @Action(canRerun = true, trigger = [UserMessage::class])
    fun respond(conversation: Conversation, context: ActionContext) {
        val assistantMessage = context.ai()
            .withLlm(properties.chatLlm())
            .withReference(toolishRag)
            .rendering("ragbot")
            .respondWithSystemPrompt(conversation, mapOf("properties" to properties))
        context.sendMessage(conversation.addMessage(assistantMessage))
    }
}

@Configuration
class ChatbotConfig {
    @Bean
    fun chatbot(agentPlatform: AgentPlatform): Chatbot =
        AgentProcessChatbot.utilityFromPlatform(agentPlatform)
}
```

Then drive it from a session:

```kotlin
val session = chatbot.createSession(user, outputChannel, contextId = null, conversationId = null)
session.onUserMessage(UserMessage("What does the document say about taxes?"))
// The reply is delivered to the session's OutputChannel and appended to its Conversation.
```

> `trigger = [UserMessage::class]` fires the action only when a `UserMessage` is the last value on
> the blackboard; `canRerun = true` lets it run again for each new message. `ChatActions` and
> `ChatbotConfig` above are **your** class names (annotated with `@EmbabelComponent` /
> `@Configuration`) — they are not framework types to import.
>
> **Load all actions before the first session.** Ensure the `AgentPlatform` has loaded its actions
> before creating a session on an `AgentProcessChatbot`, or the actions needed to respond may not
> be available in the session.

## What to Add Next

| Need | What to do | Reference |
|------|-----------|-----------|
| Full interface surface, message/asset model | `Chatbot`/`ChatSession`/`Conversation`, assets | [01-chatbot-api.md](references/01-chatbot-api.md) |
| Persistent conversations across restarts | `embabel-chat-store`, `ConversationStoreType.STORED` | [02-conversation-store.md](references/02-conversation-store.md) |
| Agentic RAG, filters, chunking, stores | `ToolishRag`, `PropertyFilter`/`EntityFilter`, Tika | [03-rag-architecture.md](references/03-rag-architecture.md) |
| Utility costs, resilient replies, Jinja personas | `@Cost`, `respond(...)`, `ragbot.jinja` | [06-chatbot-patterns.md](references/06-chatbot-patterns.md) |
| Structured replies via assets | `AssistantMessage(assets=…)`, `AssetTracker` | [07-structured-output.md](references/07-structured-output.md) |
| End-to-end, step by step | Full copy-paste examples | [08-quickstart.md](references/08-quickstart.md) |

## Examples

**"Chatbot that answers from our contracts PDF."** "Quick Start" -> the `ToolishRag` + `SearchOperations` chatbot bean, with the Tika reader for ingestion; persona/prompts via the ragbot.jinja wiring shown there.

**"Conversations must survive restarts."** Conversation-store section -> `ChatStoreType.STORED` with `ConversationFactoryProvider` instead of in-memory history.

**"Queries must filter to the requester's department."** Metadata/entity filters section -> `PropertyFilter`/`EntityFilter` plumbing; "When NOT to Use" first if the app has no conversational layer at all.

## Pitfalls

- **Assuming a `ChatbotBuilder`/`ChatClient`/`ChatMemory`/`ChatOptions` API.** These do **not** exist
  in Embabel (they are LangChain4j idioms). The chatbot surface is `Chatbot` + `ChatSession` +
  `Conversation`, backed by `AgentProcessChatbot` and driven by `@Action(trigger = …)`.
- **Actions not loaded before the first session.** The `AgentPlatform` must have loaded all actions
  before you create a session, or the responding action is missing.
- **Unbounded history.** A long-lived session keeps a growing `Conversation`; cap or summarise it in
  your action logic before re-prompting (there is no `maxMessages`/`maxTokens` builder flag).
- **Over-thresholded full-text search silently returns nothing.** Full-text scores were re-scaled to
  `[0,1)`; a cosine-style threshold (e.g. `0.8`) now rejects every full-text hit. Prefer `topK`, or
  pass a low threshold explicitly, or omit it (see the RAG reference).
- **Filters on nested paths.** `PropertyFilter`/`EntityFilter` match top-level keys only;
  `"address.city"` will not match.
- **Eager search on a non-vector store.** `withEagerSearchAbout(...)` throws
  `UnsupportedOperationException` at configuration time if the store does not implement `VectorSearch`.

## Checklist

- [ ] `Chatbot` bean built via `AgentProcessChatbot.utilityFromPlatform(...)`
- [ ] At least one `@Action(canRerun = true, trigger = [UserMessage::class])` responds to messages
- [ ] `ToolishRag` wired to a `SearchOperations` store (`withReference(...)` on the `context.ai()` call)
- [ ] Documents ingested (Tika) and chunked (`AddTitlesChunkTransformer` recommended default)
- [ ] Retrieval scoped with `withMetadataFilter` / `withEntityFilter` where multi-tenant
- [ ] Persona/objective/guardrails expressed in Jinja (`ragbot.jinja`) if needed
- [ ] Persistence enabled via `embabel-chat-store` (`ConversationStoreType.STORED`) if needed
- [ ] Tested end-to-end with real documents and queries (see [08-quickstart.md](references/08-quickstart.md))
