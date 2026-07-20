# Chatbot Building Reference

Chatbot patterns for Embabel agents (v1.0.0). See SKILL.md for the core workflow.

## Core Concepts

### Long-Lived AgentProcess

An Embabel chatbot is backed by a **long-lived `AgentProcess`** that pauses between user messages:

- The same process responds to user input, system events, or blackboard changes
- The blackboard maintains state across the entire session
- Context compression is not required
- It's a **working context**, not just a chat session

When a user sends a message, it's added to the blackboard as a `UserMessage`. The `AgentProcess` selects an appropriate action, responds, then pauses again.

### Utility AI for Chatbots

**Utility AI is often the best approach.** Define multiple actions with costs and let the planner select the highest-value action for each message. This enables multiple response strategies (RAG search, direct answer, clarification), dynamic behavior based on context, and easy extensibility.

### Goals in Chatbots

Typically, chatbot agents **do not need a goal** — the process waits indefinitely. Define a goal for transactional conversations (e.g., completing a booking), wizard-style flows with a defined endpoint, or conversations that should end after collecting specific information.

## Key Interfaces

### Chatbot

Manages multiple chat sessions:

```java
public interface Chatbot {
    ChatSession createSession(User user, OutputChannel outputChannel,
        String contextId, String conversationId);
    ChatSession findSession(String conversationId);
}
```

```kotlin
interface Chatbot {
    fun createSession(user: User?, outputChannel: OutputChannel,
        contextId: String? = null, conversationId: String? = null): ChatSession
    fun findSession(conversationId: String): ChatSession?
}
```

### ChatSession

Represents an ongoing conversation:

```java
public interface ChatSession {
    OutputChannel getOutputChannel();
    User getUser();
    Conversation getConversation();
    String getProcessId();
    void onUserMessage(UserMessage userMessage);
    boolean isFinished();
}
```

### Conversation

Holds message history and tracks assets:

```java
public interface Conversation extends StableIdentified, AssetView {
    List<Message> getMessages();
    AssetTracker getAssetTracker();
    List<Asset> getAssets();
    Message addMessage(Message message);
    UserMessage lastMessageIfBeFromUser();
}
```

Message types: `UserMessage`, `AssistantMessage`, `SystemMessage`.

## Building a Chatbot

### Step 1: Create Action Methods

Define action methods in an `@EmbabelComponent` that respond to user messages:

```java
@EmbabelComponent
public class ChatActions {
    private final ToolishRag toolishRag;

    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(Conversation conversation, ActionContext context) {
        var assistantMessage = context.ai()
            .withLlm(properties.chatLlm())
            .withReference(toolishRag)
            .rendering("ragbot")
            .respondWithSystemPrompt(conversation, Map.of("properties", properties));
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
```

```kotlin
@EmbabelComponent
class ChatActions {
    @Action(canRerun = true, trigger = UserMessage::class)
    fun respond(conversation: Conversation, context: ActionContext) {
        val assistantMessage = context.ai()
            .withLlm(properties.chatLlm())
            .withReference(toolishRag)
            .rendering("ragbot")
            .respondWithSystemPrompt(conversation, mapOf("properties" to properties))
        context.sendMessage(conversation.addMessage(assistantMessage))
    }
}
```

Key points:
- `trigger = UserMessage.class` — action fires when a `UserMessage` is the last object on the blackboard
- `canRerun = true` — action can execute multiple times (once per user message)
- `Conversation` is automatically injected from the blackboard
- `context.sendMessage()` sends the response to the output channel

### Step 2: Configure the Chatbot Bean

Use `AgentProcessChatbot.utilityFromPlatform()` to discover all `@Action` methods:

```java
@Configuration
class ChatConfiguration {
    @Bean
    Chatbot chatbot(AgentPlatform agentPlatform) {
        return AgentProcessChatbot.utilityFromPlatform(agentPlatform);
    }
}
```

```kotlin
@Configuration
class ChatConfiguration {
    @Bean
    fun chatbot(agentPlatform: AgentPlatform): Chatbot =
        AgentProcessChatbot.utilityFromPlatform(agentPlatform)
}
```

**IMPORTANT:** Ensure the `AgentPlatform` has loaded all its actions before creating a new session on your `AgentProcessChatbot`. Otherwise the actions needed to respond may not be available.

### Step 3: Use the Chatbot

```java
// New session (fresh state, auto-generated conversation ID)
ChatSession session = chatbot.createSession(user, outputChannel, null, null);

// Session with context (restores blackboard state)
ChatSession withContext = chatbot.createSession(user, outputChannel, "user-workspace-123", null);

// Restore existing conversation by ID
ChatSession restored = chatbot.createSession(user, outputChannel, null, savedConversationId);

// Both context and conversation restoration
ChatSession full = chatbot.createSession(user, outputChannel, "user-workspace-123", savedConversationId);

session.onUserMessage(new UserMessage("What does this document say about taxes?"));
// Response is automatically sent to the outputChannel
```

```kotlin
// New session (fresh state, auto-generated conversation ID)
val session = chatbot.createSession(user, outputChannel)

// Session with context (restores blackboard state)
val withContext = chatbot.createSession(user, outputChannel, contextId = "user-workspace-123")

// Restore existing conversation by ID
val restored = chatbot.createSession(user, outputChannel, conversationId = savedConversationId)

// Both context and conversation restoration
val full = chatbot.createSession(user, outputChannel, "user-workspace-123", savedConversationId)

session.onUserMessage(UserMessage("What does this document say about taxes?"))
// Response is automatically sent to the outputChannel
```

## Utility AI for Chatbots

Define multiple actions with different costs — the planner picks the highest-value (lowest cost) action:

```java
@EmbabelComponent
public class ChatbotActions {

    @Action(canRerun = true, trigger = UserMessage.class, cost = 0.05)
    void directAnswer(Conversation conversation, ActionContext context) {
        var msg = context.ai().withLlm(properties.chatLlm())
            .respondWithSystemPrompt(conversation, Map.of());
        context.sendMessage(conversation.addMessage(msg));
    }

    @Action(canRerun = true, trigger = UserMessage.class, cost = 0.3)
    void ragAnswer(Conversation conversation, ActionContext context, ToolishRag rag) {
        var msg = context.ai().withLlm(properties.chatLlm()).withReference(rag)
            .respondWithSystemPrompt(conversation, Map.of());
        context.sendMessage(conversation.addMessage(msg));
    }

    @Action(canRerun = true, trigger = UserMessage.class, cost = 0.02)
    void askClarification(Conversation conversation, ActionContext context) {
        var msg = new AssistantMessage("Could you clarify your question?");
        context.sendMessage(conversation.addMessage(msg));
    }
}
```

Lower cost = higher priority. The planner selects the most efficient action for the current context.

## Dynamic Costs with @Cost

For sophisticated action selection, use `@Cost` methods that inspect the blackboard:

```java
@Cost
double dynamic(Blackboard bb) {
    return bb.getObjects().size() > 5 ? 100 : 10;
}

@Action(canRerun = true, trigger = UserMessage.class, costMethod = "dynamic")
void respond(Conversation conversation, ActionContext context) { ... }
```

```kotlin
@Cost
fun dynamic(bb: Blackboard): Double =
    if (bb.objects.size > 5) 100.0 else 10.0

@Action(canRerun = true, trigger = UserMessage::class, costMethod = "dynamic")
fun respond(conversation: Conversation, context: ActionContext) { ... }
```

- `@Cost` marks a method as a cost calculator
- Receives the `Blackboard` to inspect current state
- Returns cost value — lower costs mean higher priority
- `costMethod` links the action to the cost calculation method

## Context IDs and Session State

The `contextId` parameter **pre-populates the session's blackboard** with objects from a named context:

- **Multiple user contexts** — different projects, accounts, or workspaces each maintain their own state
- **Resuming prior state** — restore user preferences, in-progress work, or conversation history
- **Pre-loading domain objects** — current user profile, active subscription, configuration

How it works:
1. When `createSession` is called with a `contextId`, the platform looks up saved objects
2. Those objects are added to the new session's blackboard
3. As the session runs, blackboard changes are persisted back to the context
4. Next session with that `contextId` restores the updated state

This enables **stateful conversations across sessions** without manual state management.

## Goals in Chatbots

Typically, chatbot agents **do not need a goal** — the process waits for user messages indefinitely.

Define a goal when you want the conversation to terminate:
- Transactional conversations (e.g., completing a booking)
- Wizard-style flows with a defined endpoint
- Conversations that should end after collecting specific information

## Conversation Storage

By default, chatbots use **in-memory conversations** that are lost when the session ends. For production, persist conversations to a backing store.

### Storage Types

| Type | Description |
|------|-------------|
| `IN_MEMORY` | Conversations stored in memory only. Fast and simple, suitable for testing and ephemeral sessions. |
| `STORED` | Conversations persisted to a backing store (e.g., Neo4j). Requires `embabel-chat-store` dependency. |

### Configuring Persistent Storage

Inject `ConversationFactoryProvider` and pass the appropriate factory:

```java
@Configuration
class ChatConfiguration {
    @Bean
    Chatbot chatbot(AgentPlatform agentPlatform,
            ConversationFactoryProvider conversationFactoryProvider) {
        var factory = conversationFactoryProvider
            .getFactory(ConversationStoreType.STORED);
        return new AgentProcessChatbot(agentPlatform,
            user -> createAgent(agentPlatform), factory);
    }
}
```

Storage type is configured once when creating the chatbot, not per-call.

### Adding embabel-chat-store

```xml
<dependency>
    <groupId>com.embabel.chat</groupId>
    <artifactId>embabel-chat-store</artifactId>
</dependency>
```

Provides:
- `StoredConversationFactory` — creates conversations that persist to Neo4j
- `StoredConversation` — conversation implementation with async persistence
- Persistence lifecycle events (`MessageEvent` with `PERSISTED` / `PERSISTENCE_FAILED` status)
- Title generation for conversation sessions

### Restoring Conversations

Pass the `conversationId` when creating a session:

```java
ChatSession session = chatbot.createSession(user, outputChannel, null, conversationId);
List<Message> history = session.getConversation().getMessages();
```

```kotlin
val session = chatbot.createSession(user, outputChannel, conversationId = conversationId)
val history = session.conversation.messages
```

If the conversation exists in storage, it loads automatically. If not, a new conversation is created with that ID. This allows resuming conversations across server restarts, displaying conversation history to returning users, and continuing multi-turn interactions from where they left off.

### Asset Tracking

Chatbots track **assets** (generated documents, search results, user content) at two levels:

**Conversation-level** (explicit tracking):
```java
conversation.getAssetTracker().addAsset(myAsset);
List<Asset> trackedAssets = conversation.getAssetTracker().getAssets();
```

**Message-level** (tied to specific responses):
```java
var message = new AssistantMessage("Here's the report", null, null,
    List.of(reportAsset, summaryAsset));
conversation.addMessage(message);
```

`Conversation.assets` provides a **merged view** — tracker assets first, then message assets in chronological order, with duplicates removed by ID (tracker version wins).

### Resilient Responses

Use `respond()` instead of `respondWithSystemPrompt()` for error handling:

```java
var assistantMessage = context.ai().rendering("ragbot")
    .respond(conversation, model, error -> {
        logger.error("Failed to generate response", error);
        return new AssistantMessage("Sorry, something went wrong. Please try again.");
    });
```

```kotlin
val assistantMessage = context.ai().rendering("ragbot")
    .respond(conversation, model) { error ->
        logger.error("Failed to generate response", error)
        AssistantMessage("Sorry, something went wrong. Please try again.")
    }
```

This wraps the LLM call so an infrastructure failure still returns an `AssistantMessage` to the user.

## Complete Example

See the [rag-demo project](https://github.com/embabel/rag-demo) for a complete chatbot implementation including `ChatActions.java`, `ChatConfiguration.java`, Spring Shell integration, Jinja templates, and RAG integration.

## Key Points

- Chatbots use a long-lived `AgentProcess` that pauses between messages
- `trigger = UserMessage.class` for reactive message handling
- `contextId` enables stateful conversations across sessions
- Utility AI is often the best planner for chatbots
- Conversation storage: `IN_MEMORY` (default) or `STORED` (Neo4j)
- Goals are optional — omit for open-ended conversations, add for transactional flows
- Use `@Cost` methods for dynamic action selection based on blackboard state
- Assets track structured outputs at conversation and message levels
---

*Source: Embabel Agent v1.0.0 documentation*
