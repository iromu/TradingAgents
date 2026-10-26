# Conversation Store (Persistent Chat History)

Source: Embabel docs `reference/chatbots/page.adoc` ("Conversation Storage", "Restoring
Conversations"), `embabel/embabel-agent` → `embabel-agent-docs/src/main/asciidoc/`, tag `v1.5.1`.

> **There is no `ChatHistoryStore` / `InMemoryChatHistoryStore` / `ChatHistory` type in Embabel.**
> Persistent chat history is provided by the separate **`embabel-chat-store`** artifact and configured
> through `ConversationFactoryProvider` + `ConversationStoreType`. There is no `ChatStore` type either.

## Storage Types

`ConversationStoreType` (package `com.embabel.chat`, in `ConversationFactory.kt`) — enum values
confirmed in source:

| Value | Description |
|-------|-------------|
| `IN_MEMORY` | In-memory only. Fast, simple; testing and ephemeral sessions. |
| `STORED` | Persisted to a backing store (e.g. Neo4j). Requires `embabel-chat-store`. |

By default chatbots use **in-memory conversations** lost when the session ends. The in-memory types
and `MessageEvent` live in `embabel-agent-api` and need **no** `embabel-chat-store` dependency.

## Add the dependency

```xml
<dependency>
    <groupId>com.embabel.chat</groupId>
    <artifactId>embabel-chat-store</artifactId>
</dependency>
```

This artifact provides (documented there; the class names are referenced from
`AgentProcessChatbot.kt` KDoc and the chatbots chapter, and ship in this separate module — they are
not in the core `embabel-agent-api` tree):

- `StoredConversationFactory` — creates conversations that persist to Neo4j
- `StoredConversation` — conversation implementation with async persistence
- Persistence lifecycle events (`MessageEvent` with `PERSISTED` / `PERSISTENCE_FAILED` status) for UI updates
- Title generation for conversation sessions

## Configure persistent storage

Inject `ConversationFactoryProvider` and pass the `STORED` factory when constructing the chatbot.
Storage type is fixed **once at creation time**, not per call.

```kotlin
@Configuration
class ChatbotConfig {
    @Bean
    fun chatbot(
        agentPlatform: AgentPlatform,
        conversationFactoryProvider: ConversationFactoryProvider,   // <1>
    ): Chatbot {
        val factory = conversationFactoryProvider
            .getFactory(ConversationStoreType.STORED)               // <2>

        return AgentProcessChatbot(
            agentPlatform,
            { _ -> createAgent(agentPlatform) },
            factory,                                                // <3>
            // ... other configuration
        )
    }
}
```

<1> Inject the provider via Spring DI. <2> Get the factory for the chosen storage type.
<3> Pass the factory to the chatbot.

## Restoring Conversations

Pass the `conversationId` when creating a session; if the conversation exists in storage it is
loaded automatically, otherwise a new one is created with that id.

```kotlin
val session = chatbot.createSession(
    user,
    outputChannel,
    contextId = null,
    conversationId = conversationId,   // loaded if present, else created
)
val history = session.conversation.messages
```

This enables resuming across server restarts, showing history to returning users, and continuing
multi-turn interactions. For lower-level access, `ConversationFactory.load(conversationId)` can check
whether a conversation exists before you create a session.

## Persistence lifecycle events

`MessageEvent` (package `com.embabel.chat.event`) publishes a `MessageStatus`:

- In-memory: `ADDED`
- Persistent: `ADDED` → `PERSISTED` **or** `PERSISTENCE_FAILED`

In-memory conversations can also publish `ADDED` events by passing a Spring
`ApplicationEventPublisher` to `InMemoryConversationFactory`. Subscribe with a Spring
`@EventListener` to drive UI updates (e.g. refresh a title once a message is `PERSISTED`).

## Custom stores

If you need a store other than the provided one, implement `ConversationFactory` (attested
interface) and expose it through your own `ConversationFactoryProvider`; the chatbot only ever asks
the provider for a factory by `ConversationStoreType`.
