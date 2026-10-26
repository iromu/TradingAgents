# Chatbot Quickstart — the documented build path

> **Verified against** `embabel/embabel-agent` @ `main` (source tarball, 1,733 source files) and the
> `v1.5.1` tag. Primary sources: `embabel-agent-docs/src/main/asciidoc/reference/chatbots/page.adoc`
> (1,087 lines) and `.../reference/rag/page.adoc`. The chatbot lives in the **`com.embabel.chat`**
> package family of the `embabel-agent` repository — there is no standalone `embabel/embabel-chatbot`
> repository to link to.

## Table of Contents

- [The package names, before anything else](#the-package-names-before-anything-else)
- [Step 1 — declare the action class](#step-1--declare-the-action-class)
- [Step 2 — publish a `Chatbot` bean](#step-2--publish-a-chatbot-bean)
- [Step 3 — drive a session](#step-3--drive-a-session)
- [Persisting conversations](#persisting-conversations)
- [Grounding a chatbot with RAG](#grounding-a-chatbot-with-rag)
- [Common Pitfalls](#common-pitfalls)

## The package names, before anything else

Every import in a chatbot example comes from one of these, each attested by a `package` declaration
under `src/main`:

| Package | Attested members |
|---|---|
| `com.embabel.chat` | `Chatbot`, `ChatSession`, `ChatTrigger`, `Conversation`, `Message`, `BaseMessage`, `UserMessage`, `AssistantMessage`, `SystemMessage`, `UserMessageBuilder`, `MessageRole`, `ContentPart` (+ `TextPart`, `MediaPart`, `ImagePart`, `DocumentPart`), `ToolCall`, `ToolResultMessage`, `AssistantMessageWithToolCalls`, `Asset`, `AssetTracker`, `AssetView`, `MergedAssetView`, `ConversationFactory`, `ConversationFactoryProvider`, `ConversationStoreType`, `MapConversationFactoryProvider`, `SimpleMessageFormatter`, `WindowingConversationFormatter`, `TokenBudgetConversationFormatter`, `EmptyLlmResponseException` |
| `com.embabel.chat.agent` | `AgentProcessChatbot`, `AgentProcessChatSession`, `DefaultChatAgentBuilder`, `BlackboardFormatter`, `DefaultBlackboardFormatter`, `BlackboardEntryFormatter`, `DefaultBlackboardEntryFormatter`, `ConversationStatus`, `ConversationContinues`, `ConversationOver` |
| `com.embabel.chat.event` | `MessageEvent`, `MessageStatus` |
| `com.embabel.chat.support` | `InMemoryConversation`, `InMemoryConversationFactory`, `InMemoryAssetTracker`, `EventPublishingConversation`, `AssetAddingTool` |
| `com.embabel.chat.support.console` | `ChatConsole`, `ConsoleOutputChannel` |
| `com.embabel.agent.rag.tools` | `ToolishRag` (`rag/tools/ToolishRag.kt`), `RagOptions`, `RagServiceReference` |
| `com.embabel.agent.rag.service` | `RagService`, `RagRequest`, `RagResponse`, `RagHint`, `RagResponseEnhancer`, `FacetedRagService`, `NavigableRagService` |
| `com.embabel.agent.rag.filter` | the RAG filter surface |

**These names appeared in earlier revisions of this guide and do not exist.** Each was checked against
`src/main` of `embabel/embabel-agent` and has no declaration and no import anywhere in it:
`ChatbotBuilder`, `ChatOptions`, `ChatMessage`, `ChatHistoryStore`, `InMemoryChatMemory`,
`ChatExtension`, `Guardrails`, `RagBuilder`, `FileRagSource`, `FilterBuilder`, `PromptTemplate`. The
packages they were shown under — `com.embabel.chatbot.*`, `com.embabel.agent.chatbot.*`,
`com.embabel.agent.knowledge.*`, `com.embabel.rag.*` — likewise occur zero times, and so does the
`chatbot.yaml` configuration tree those examples implied. The builder idiom they used
(`ChatbotBuilder.builder().chatMemory(…).chatSession(…).chatOptions(…).build()`) belongs to a
*different library* and has never been Embabel's; do not reconstruct it. Note too that there is **no
`ChatStore` type** — the persistence artifact is `embabel-chat-store` and the enum is
`ConversationStoreType`.

## Step 1 — declare the action class

A chatbot is an `@EmbabelComponent` whose `@Action` methods the platform discovers. This is the
documented example, verbatim from `reference/chatbots/page.adoc:432-468`:

```java
@EmbabelComponent
public class ChatActions {

    private final ToolishRag toolishRag;
    private final RagbotProperties properties;

    public ChatActions(
            SearchOperations searchOperations,
            RagbotProperties properties) {
        this.toolishRag = new ToolishRag(
                "sources",
                "Sources for answering user questions",
                searchOperations
        );
        this.properties = properties;
    }

    @Action(canRerun = true, trigger = UserMessage.class)   // <1> <2>
    void respond(
            Conversation conversation,                        // <3>
            ActionContext context) {
        var assistantMessage = context.ai()
                .withLlm(properties.chatLlm())
                .withReference(toolishRag)
                .rendering("ragbot")
                .respondWithSystemPrompt(conversation, Map.of(
                        "properties", properties
                ));
        context.sendMessage(conversation.addMessage(assistantMessage));   // <4>
    }
}
```

`ChatActions` here is the **documentation's own example class name**, not a library type — you write
this class yourself; there is no `ChatActions` to import. `<1>` `canRerun` lets the action run again for
the same input; `<2>` `trigger = UserMessage.class` is what makes it fire on a user message (see *How
Message Triggering Works*, `page.adoc:792`). `<3>` the `Conversation` is injected. `<4>` replies go out
through `context.sendMessage(...)` and reach the session's `OutputChannel`.

## Step 2 — publish a `Chatbot` bean

`Chatbot` is an interface (`page.adoc:60-69`):

```java
public interface Chatbot {
    ChatSession createSession(User user, OutputChannel outputChannel,
                               String contextId, String conversationId);
    ChatSession findSession(String conversationId);
}
```

Publish the shipped implementation from `com.embabel.chat.agent`, verbatim from `page.adoc:512-536`:

```java
@Configuration
class ChatConfiguration {
    @Bean
    Chatbot chatbot(AgentPlatform agentPlatform) {
        return AgentProcessChatbot.utilityFromPlatform(agentPlatform);   // <1> <2>
    }
}
```

`<1>` uses Utility AI planning to select the action; `<2>` action discovery scans
`@EmbabelComponent` classes on the platform. For debugging there is a documented overload taking a
conversation factory and a verbosity config (`page.adoc:552-558`):

```java
@Bean
Chatbot chatbot(AgentPlatform agentPlatform) {
    return AgentProcessChatbot.utilityFromPlatform(
            agentPlatform,
            new InMemoryConversationFactory(),      // <1>
            new Verbosity().showPrompts()           // <2>
    );
}
```

There is **no `ChatbotBuilder`** — the `utilityFromPlatform(...)` overloads are the construction path.
`DefaultChatAgentBuilder` (`com.embabel.chat.agent`) is the agent-process builder the chatbot delegates
to internally, not an entry point to call from your configuration.

## Step 3 — drive a session

Verbatim from `page.adoc:741-780`; the four `createSession` shapes and the message trigger:

```java
// New session (fresh state, generated conversation ID)
ChatSession session = chatbot.createSession(user, outputChannel, null, null);                     // <1>

// Session with context (restores blackboard state)
ChatSession withContext = chatbot.createSession(user, outputChannel, "user-workspace-123", null);  // <2>

// Restore existing conversation by ID
ChatSession restored = chatbot.createSession(user, outputChannel, null, savedConversationId);      // <3>

// Both context and conversation restoration
ChatSession full = chatbot.createSession(user, outputChannel, "user-workspace-123", savedConversationId); // <4>

session.onUserMessage(new UserMessage("What does this document say about taxes?"));                // <5>
// Response is automatically sent to the outputChannel
```

In Kotlin the same call uses named optional arguments:
`chatbot.createSession(user, outputChannel, contextId = "project-alpha")`.

The `contextId` is the state-resumption handle (`page.adoc:89-153`): the platform looks up saved objects
for that context, seeds them into the new session's **blackboard**, and changes can be persisted back so
the next session with the same `contextId` restores them. Blackboard rendering is
`BlackboardFormatter` / `DefaultBlackboardFormatter` and `BlackboardEntryFormatter` /
`DefaultBlackboardEntryFormatter` in `com.embabel.chat.agent`.

## Persisting conversations

Two storage modes, selected through `ConversationStoreType` (`page.adoc:594-606`):

| Value | Meaning |
|---|---|
| `IN_MEMORY` | stored in memory only — fast, for tests and ephemeral sessions |
| `STORED` | persisted to a backing store (e.g. Neo4j); requires the `embabel-chat-store` dependency |

Add the dependency (`groupId` is `com.embabel.chat`, `page.adoc:673-679`):

```xml
<dependency>
    <groupId>com.embabel.chat</groupId>
    <artifactId>embabel-chat-store</artifactId>
</dependency>
```

It provides `StoredConversationFactory` (conversations that persist to Neo4j), `StoredConversation`
(with async persistence), title generation, and the persistence lifecycle events — a `MessageEvent`
(`com.embabel.chat.event`) carrying a `MessageStatus` of `PERSISTED` or `PERSISTENCE_FAILED`. Wire it by
injecting `ConversationFactoryProvider` and passing the matching factory when building the chatbot;
`MapConversationFactoryProvider` is the shipped in-memory provider and
`InMemoryConversationFactory` (`com.embabel.chat.support`) is the in-memory factory. There is **no
`ChatHistoryStore` type**, no `PostgresChatHistoryStore`, and no `ChatExtension` hook — for
pre-processing around an LLM call use an `@Action` on the chatbot or an `AssetAddingTool`.

For the store API in depth see [02-conversation-store.md](./02-conversation-store.md).

## Grounding a chatbot with RAG

The attested grounding collaborator is `ToolishRag` (`com.embabel.agent.rag.tools`, file
`rag/tools/ToolishRag.kt`), constructed as `new ToolishRag(name, description, searchOperations)` and
attached with `.withReference(toolishRag)` on the `Ai` chain, exactly as in Step 1. Passing a reference
through `withReferences(...)` puts the relevant content into the prompt and contributes the search tool
so the model can ask for more.

There is **no `RagBuilder`, no `FileRagSource`, no `FilterBuilder` and no `PromptTemplate`.** Ingestion
goes through `com.embabel.agent.rag.ingestion` (with `.ingestion.policy` and `.ingestion.transform`),
filtering through `com.embabel.agent.rag.filter`, and the service contracts are `RagService` /
`RagRequest` / `RagResponse` / `RagHint` in `com.embabel.agent.rag.service`. Prompt rendering inside a
chatbot action is `.rendering("ragbot")` on the `Ai` chain together with
`respondWithSystemPrompt(conversation, modelMap)` — templates are resolved from the template resources
the platform is configured with, not from a `PromptTemplate` object you build. See
[03-rag-architecture.md](./03-rag-architecture.md) for the RAG surface.

## Common Pitfalls

1. **Importing from `com.embabel.chatbot.*` or `com.embabel.rag.*`.** Neither package exists. The
   chatbot is `com.embabel.chat*`; the RAG surface is `com.embabel.agent.rag.*`.
2. **Looking for a `ChatbotBuilder`.** Use the `AgentProcessChatbot.utilityFromPlatform(...)` overloads.
3. **Hand-rolling the reply path.** `context.sendMessage(conversation.addMessage(msg))` is the
   documented shape; the response reaches the `OutputChannel` you passed to `createSession`.
4. **Omitting `trigger` on `@Action`.** Without a trigger such as `UserMessage.class` the action will
   not fire on user input the way the guide's example does.
5. **Expecting durable conversations without `embabel-chat-store`.** `ConversationStoreType.STORED`
   requires that dependency; the in-memory path is `InMemoryConversationFactory`.
6. **Using `ChatMessage`.** It does not exist. The types are `Message` and its subtypes `UserMessage`,
   `AssistantMessage`, `SystemMessage`, `ToolResultMessage`.
7. **Confusing the blackboard with a store.** The blackboard is per-session state seeded via
   `contextId`; conversation persistence is the `ConversationFactory` concern.
8. **Copying the `@Agent`/`@State` surface from the agent skill.** Those annotations belong to
   embabel-agent; the chatbot side is `@EmbabelComponent` + `@Action` (see
   [06-chatbot-patterns.md](./06-chatbot-patterns.md)).
