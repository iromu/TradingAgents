# Chatbot Building Reference

Chatbot patterns for Embabel agents. See SKILL.md for the core workflow.

## Core Architecture

Embabel chatbots use a **long-lived `AgentProcess`** that pauses between user messages. This design means:

- The same process responds to user input, system events, or blackboard changes
- The blackboard maintains state across the entire session
- Context compression is not required
- It's a **working context**, not just a chat session

## Key Interfaces

### Chatbot

Manages multiple chat sessions:

```java
public interface Chatbot {
    ChatSession createSession(User user, OutputChannel outputChannel, String contextId, String conversationId);
    ChatSession findSession(String conversationId);
}
```

### ChatSession

Represents an ongoing conversation. Provides methods to send messages, retrieve history, and manage the session lifecycle.

### Conversation

Tracks conversation-level metadata and asset tracking (conversation-level and message-level).

## Building a Chatbot

### Action Methods

Handle `UserMessage` events with action methods:

```java
@EmbabelComponent
public class MyChatbot {

    @Action(trigger = UserMessage.class)
    public AssistantResponse handleMessage(UserMessage msg, Ai ai) {
        return ai.withDefaultLlm()
            .creating(AssistantResponse.class)
            .fromPrompt(msg.getContent());
    }
}
```

The `trigger = UserMessage.class` ensures the action fires only when a user message is the **most recently added** value to the blackboard.

### Chatbot Bean Configuration

```java
@Bean
public Chatbot myChatbot(AgentPlatform platform) {
    return new DefaultChatbot(platform);
}
```

## Context IDs and Session State

The `contextId` parameter pre-populates the session's blackboard with objects from a named context:

```java
// Create a session with a specific context
ChatSession session = chatbot.createSession(
    user, outputChannel, "project-alpha", null
);

// Or anonymous session
ChatSession anonymousSession = chatbot.createSession(
    null, outputChannel, null, null
);
```

Context mechanism:
1. When `createSession` is called with a `contextId`, the platform looks up saved objects
2. Those objects are added to the new session's blackboard
3. Changes are persisted back to the context as the session runs
4. Next time a session uses that `contextId`, the updated state is restored

## Conversation Storage

### IN_MEMORY (default)

Conversations stored in memory. Simple but lost on restart.

```yaml
embabel:
  agent:
    platform:
      conversation-store: IN_MEMORY
```

### STORED (Neo4j)

Persist conversations to Neo4j for stateful sessions across restarts.

```yaml
embabel:
  agent:
    platform:
      conversation-store: STORED
```

## Utility AI for Chatbots

Utility AI is often the best approach for chatbots. Define multiple actions with costs, and the planner selects the highest-value action:

```java
@EmbabelComponent
public class ChatbotActions {

    @Action(cost = 0.1)
    public DirectResponse directAnswer(UserMessage msg, Ai ai) {
        return ai.withDefaultLlm().creating(DirectResponse.class)
            .fromPrompt(msg.getContent());
    }

    @Action(cost = 0.3)
    public RagResponse ragAnswer(UserMessage msg, Ai ai, Blackboard bb) {
        // Search knowledge base
        return ai.withDefaultLlm().creating(RagResponse.class)
            .fromPrompt(msg.getContent());
    }

    @Action(cost = 0.05)
    public ClarificationRequest needsClarification(UserMessage msg) {
        // Check if the message is ambiguous
        return new ClarificationRequest("Could you clarify?");
    }
}
```

## Dynamic Cost Methods

Use `@Cost` for dynamic action selection based on context:

```java
@Cost(name = "ragCost")
public double computeRagCost(@Nullable KnowledgeBase kb) {
    return kb != null ? 0.3 : 0.9;  // Cheaper if knowledge base exists
}

@Action(costMethod = "ragCost")
public RagResponse ragAnswer(UserMessage msg, Ai ai) { ... }
```

## Goals in Chatbots

Typically, chatbot agents **do not need a goal** — the process waits indefinitely.

Define a goal for:
- Transactional conversations (e.g., completing a booking)
- Wizard-style flows with a defined endpoint
- Conversations that should end after collecting specific information

```java
@AchievesGoal(description = "Complete the booking")
@Action
public BookingResult completeBooking(UserMessage msg, BookingData data) { ... }
```

## Key Points

- Chatbots use long-lived `AgentProcess` that pauses between messages
- `trigger = UserMessage.class` for reactive message handling
- `contextId` enables stateful conversations across sessions
- Utility AI is often the best planner for chatbots
- Conversation storage: `IN_MEMORY` (default) or `STORED` (Neo4j)
- Goals optional — omit for open-ended conversations, add for transactional flows