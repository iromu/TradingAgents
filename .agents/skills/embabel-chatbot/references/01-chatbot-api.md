# Chatbot API Reference

Source: Embabel docs `reference/chatbots/page.adoc` and `reference/rag/page.adoc`
(`embabel/embabel-agent` → `embabel-agent-docs/src/main/asciidoc/`, tag `v1.5.1`;
docs site: https://docs.embabel.com/embabel-agent/guide/1.5.1/).

> **No `ChatbotBuilder` / `ChatClient` / `ChatMemory` / `ChatOptions` / `ChatExtension` /
> `ChatActions` builder API exists in Embabel.** Those are LangChain4j idioms. The real surface is
> `Chatbot` + `ChatSession` + `Conversation`, backed by `AgentProcessChatbot`, driven by
> `@Action(trigger = …)`, and answered through the `context.ai()` DSL.

## Attested API Surface (packages verified against the source tree)

| Type | Package | Notes |
|------|---------|-------|
| `Chatbot` | `com.embabel.chat` | `Chatbot.kt:26` |
| `ChatSession` | `com.embabel.chat` | `ChatSession.kt:28` |
| `Conversation` | `com.embabel.chat` | `Conversation.kt:28` — `: StableIdentified, HasInfoString, AssetView` |
| `UserMessage`, `AssistantMessage`, `SystemMessage`, `Message`, `MessageRole` | `com.embabel.chat` | top-level in `Message.kt` |
| `AssetTracker`, `Asset`, `AssetView` | `com.embabel.chat` | `AssetTracker.kt` |
| `ConversationFactory`, `ConversationFactoryProvider`, `ConversationStoreType` | `com.embabel.chat` | see 02-conversation-store.md |
| `InMemoryConversationFactory` | `com.embabel.chat.support` | `support/InMemoryConversationFactory.kt` |
| `MessageEvent` | `com.embabel.chat.event` | status `MessageStatus.{ADDED,PERSISTED,PERSISTENCE_FAILED}` |
| `AgentProcessChatbot` | `com.embabel.chat.agent` | `AgentProcessChatbot.kt:64` |
| `@Action`, `@EmbabelComponent`, `@Cost` | `com.embabel.agent.api.annotation` | `annotation/annotations.kt` |
| `AgentPlatform`, `Blackboard` | `com.embabel.agent.core` | `core/AgentPlatform.kt`, `core/Blackboard.kt` |
| `ActionContext` | `com.embabel.agent.api.common` | `common/ActionContext.kt` |
| `OutputChannel` | `com.embabel.agent.api.channel` | `channel/OutputChannel.kt` |
| `ToolishRag` | `com.embabel.agent.rag.tools` | see 03-rag-architecture.md |

## Core Concepts

- **Long-lived `AgentProcess`.** A chatbot is backed by an `AgentProcess` that pauses between user
  messages. The same process can respond to events beyond user input; the blackboard maintains state
  across the whole session — it is a *working context*, not merely a chat thread.
- **Utility AI is often the best planner for chatbots.** Define several `@Action`s with costs; the
  planner selects the highest-value one per message (see 04-chatbot-patterns.md).
- **Chatbots usually need no goal.** The process waits for messages indefinitely. Define a goal only
  for transactional conversations (e.g. completing a booking).

## `Chatbot`

Manages multiple sessions.

```kotlin
interface Chatbot {
    fun createSession(
        user: User?,
        outputChannel: OutputChannel,
        contextId: String? = null,
        conversationId: String? = null,
    ): ChatSession

    fun findSession(conversationId: String): ChatSession?
}
```

- `contextId` — pre-populates the session blackboard from a named context (see below).
- `conversationId` — restores an existing conversation (or creates one with that id).

### Context IDs and Session State

Pass a `contextId` to load prior blackboard state from the `AgentPlatform`'s context storage:
multiple contexts per user, resuming prior state, or pre-loading domain objects (profile, active
subscription). The mechanism round-trips: `createSession` loads saved objects → the session mutates
them → changes persist back → the next `createSession` with that `contextId` restores them. This
gives **stateful conversations across sessions** without manual state tracking.

## `ChatSession`

```kotlin
interface ChatSession {
    val outputChannel: OutputChannel
    val user: User?
    val conversation: Conversation
    val processId: String? get() = null

    fun onUserMessage(userMessage: UserMessage)
    fun isFinished(): Boolean
}
```

`onUserMessage(...)` appends the message to the `Conversation` and the blackboard, then the planner
runs and a response is sent to the `outputChannel`.

## `Conversation`

```kotlin
interface Conversation : StableIdentified, HasInfoString, AssetView {
    val messages: List<Message>
    val assetTracker: AssetTracker
    val assets: List<Asset>                 // merged view: tracker + message assets
    fun addMessage(message: Message): Message
    fun lastMessageIfBeFromUser(): UserMessage?
}
```

Message types (all in `com.embabel.chat`):

- `UserMessage` — from the user (supports multimodal content)
- `AssistantMessage` — from the chatbot (may carry assets)
- `SystemMessage` — system-level instructions

## Building a Chatbot

### Step 1 — Action methods

```kotlin
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

    @Action(canRerun = true, trigger = [UserMessage::class])   // <1> <2>
    fun respond(conversation: Conversation, context: ActionContext) {   // <3>
        val assistantMessage = context.ai()
            .withLlm(properties.chatLlm())
            .withReference(toolishRag)
            .rendering("ragbot")
            .respondWithSystemPrompt(conversation, mapOf("properties" to properties))
        context.sendMessage(conversation.addMessage(assistantMessage))   // <4>
    }
}
```

<1> `trigger = [UserMessage::class]` — runs when a `UserMessage` is the last blackboard value.
<2> `canRerun = true` — the action may run again for each new message.
<3> `Conversation` is injected from the blackboard.
<4> `context.sendMessage(...)` delivers the reply to the output channel.

> `ChatActions` is a **user-defined** `@EmbabelComponent` class name (it appears as the class name in
> the docs' own examples), not a framework type.

### Step 2 — Configure the `Chatbot` bean

```kotlin
@Configuration
class ChatbotConfig {
    @Bean
    fun chatbot(agentPlatform: AgentPlatform): Chatbot =
        AgentProcessChatbot.utilityFromPlatform(agentPlatform)   // Utility AI; discovers @Action methods
}
```

For debugging, pass an explicit conversation factory and verbosity:

```kotlin
@Bean
fun chatbot(agentPlatform: AgentPlatform): Chatbot =
    AgentProcessChatbot.utilityFromPlatform(
        agentPlatform,
        InMemoryConversationFactory(),
        Verbosity().showPrompts(),
    )
```

> **IMPORTANT:** Ensure the `AgentPlatform` has loaded all its actions before you create a session on
> the `AgentProcessChatbot`; otherwise the actions needed to respond may not be present in the session.

### Step 3 — Use the chatbot

```kotlin
val fresh      = chatbot.createSession(user, outputChannel)                              // new session
val withCtx    = chatbot.createSession(user, outputChannel, contextId = "ws-123")       // restore blackboard state
val restored   = chatbot.createSession(user, outputChannel, conversationId = savedId)   // restore history
val both       = chatbot.createSession(user, outputChannel, "ws-123", savedId)

fresh.onUserMessage(UserMessage("What does this document say about taxes?"))
// response goes to the outputChannel automatically
```

## Asset Tracking

Chatbots track **assets** — structured outputs like generated documents or search results — at two
levels:

- **Conversation-level** via `conversation.assetTracker.addAsset(asset)`; use when assets are created
  by tools/external processes or must persist across messages.
- **Message-level** by constructing `AssistantMessage(content = "...", assets = listOf(asset))`; use
  when an asset belongs to one specific reply.

`Conversation.assets` is a **merged view**: tracker assets first (deduplicated by id, tracker wins),
then message assets chronologically. Assets can be surfaced to the LLM as tools via their
`LlmReference` (see 05-structured-output.md).

## The `context.ai()` DSL (chatbot-relevant methods)

All methods below are attested in the chatbots/rag chapters and the source tree:

| Call | Purpose |
|------|---------|
| `.withLlm(llm)` | Select the model for this call |
| `.withReference(ref)` / `.withReferences(list)` | Attach `LlmReference`s (e.g. a `ToolishRag`, an asset) |
| `.rendering("ragbot")` | Use the named Jinja template for the system prompt |
| `.withSystemPrompt("...")` | Inline system prompt (simple bots) |
| `.respond(messages)` | Return an `AssistantMessage` from messages |
| `.respondWithSystemPrompt(conversation, bindings)` | Render + respond with a template-bound system prompt |
| `.respond(conversation, llm) { error -> … }` | Resilient variant — returns a fallback `AssistantMessage` on failure (see 04) |

`context.sendMessage(...)` (on `ActionContext`) delivers a message to the session's output channel.
