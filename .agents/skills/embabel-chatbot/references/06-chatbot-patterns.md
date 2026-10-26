# Chatbot Building Patterns

Source: [embabel/embabel-agent → reference/chatbots/page.adoc](https://github.com/embabel/embabel-agent/blob/main/embabel-agent-docs/src/main/asciidoc/reference/chatbots/page.adoc) — this file was originally distilled from a `springrod-blog/09-agentic-rag-chatbot.md` write-up that is **not present in the repository**, so its code has been rebuilt against the shipped docs and the source tree; see the correction note under *Complete Chatbot Example*.

## Complete Chatbot Example

> **Corrected against the source tree.** Earlier revisions of this file built the example on
> `com.embabel.agent.chatbot.api.*` and `com.embabel.agent.knowledge.*` with a
> `ChatbotBuilder.builder().chatMemory(…).chatSession(…).chatOptions(…).build()` idiom. **None of
> that exists.** Every name below was checked against `src/main` of `embabel/embabel-agent` and has no
> declaration and no import anywhere in it:
>
> `ChatbotBuilder`, `ChatMemory`, `ChatOptions`, `ChatExtension`, `ChatMessage`, `InMemoryChatMemory`,
> `RagSystem`, `RagSource`, `RagBuilder`, `FileRagSource`, `FilterBuilder`, `Filter`,
> `PromptTemplate`, `Guardrail`, `Reasoning`
>
> The packages `com.embabel.agent.chatbot.*` and `com.embabel.agent.knowledge.*` occur zero times, as
> do `com.embabel.chatbot.*` and `com.embabel.rag.*`. The `chatbot.yaml` tree and the `guardrails:`
> YAML block shown here previously are likewise unattested — there is no such configuration schema. The
> builder idiom belongs to a different library and has never been Embabel's. The real types live under
> **`com.embabel.chat`** (chatbot) and **`com.embabel.agent.rag.*`** (RAG); see
> [08-quickstart.md](./08-quickstart.md) for the full attested package map.

### 1. Dependency

The chatbot ships inside the `embabel-agent` modules — there is no standalone chatbot artifact to add.
Add the persistent-conversation store only when you need `ConversationStoreType.STORED`
(`groupId` is `com.embabel.chat`):

```xml
<dependency>
    <groupId>com.embabel.chat</groupId>
    <artifactId>embabel-chat-store</artifactId>
</dependency>
```

### 2. Build the chatbot

The attested pattern is an `@EmbabelComponent` action class plus a `Chatbot` bean. The action class is
the documented `page.adoc:432-468` example:

```java
@EmbabelComponent
public class ChatActions {

    private final ToolishRag toolishRag;
    private final RagbotProperties properties;

    public ChatActions(SearchOperations searchOperations, RagbotProperties properties) {
        this.toolishRag = new ToolishRag(
                "sources", "Sources for answering user questions", searchOperations);
        this.properties = properties;
    }

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

and the bean publishes the shipped implementation (`page.adoc:512-558`):

```java
@Configuration
class ChatConfiguration {
    @Bean
    Chatbot chatbot(AgentPlatform agentPlatform) {
        return AgentProcessChatbot.utilityFromPlatform(agentPlatform);
    }
}
```

`ChatActions` and `ChatConfiguration` above are the **documentation's own example class names** — you
write them; there is no `ChatActions`/`ChatConfiguration` type to import. `Chatbot` and `ChatSession`
are the real interfaces in `com.embabel.chat`, and `AgentProcessChatbot` is the implementation in
`com.embabel.chat.agent`. There is **no `ChatbotBuilder`**: the `utilityFromPlatform(...)` overloads are
the construction path, and `DefaultChatAgentBuilder` is internal to the agent process, not a config entry
point. Grounding is `ToolishRag` (`com.embabel.agent.rag.tools`) attached via `.withReference(...)`, not
a `RagBuilder`/`RagSystem`/`FileRagSource` chain.

### 3. Using the chatbot

You drive a `ChatSession`; there is no `chatbot.chat(String)` convenience method and no
`ChatOptions`/`thinking`/`thinkingBudget` knob:

```java
ChatSession session = chatbot.createSession(user, outputChannel, "user-workspace-123", null);
session.onUserMessage(new UserMessage("What is RAG?"));
// the reply is delivered to the OutputChannel you passed in
```

Restore an earlier conversation by passing its id as the fourth argument, or look it up with
`chatbot.findSession(conversationId)`.

## Chatbot Session State and the Blackboard

`createSession(user, outputChannel, contextId, conversationId)` is the state seam
(`page.adoc:89-153`): a `contextId` loads previously saved objects into the new session's **blackboard**,
changes can be persisted back, and the next session with the same `contextId` restores them. The
rendering hooks are `BlackboardFormatter` / `DefaultBlackboardFormatter` and
`BlackboardEntryFormatter` / `DefaultBlackboardEntryFormatter` (`com.embabel.chat.agent`); conversation
status is `ConversationStatus` with `ConversationContinues` / `ConversationOver`.

To pre-process around an LLM call, use an `@Action` on the chatbot component or an `AssetAddingTool`
(`com.embabel.chat.support`) — there is **no `ChatExtension` interface** and no
`implements ChatExtension` seam to override.

## ragbot.jinja Prompt Template

The default RAG chatbot prompt template (ragbot.jinja):

```jinja
{% if chat_history %}
<chat_history>
{% for message in chat_history %}
<{{ message.role }}>
{{ message.content }}
</{{ message.role }}>
{% endfor %}
</chat_history>
{% endif %}

{% if sources %}
<sources>
{% for source in sources %}
<source>
{{ source.content }}
{% if source.metadata %}
Metadata: {{ source.metadata }}
{% endif %}
</source>
{% endfor %}
</sources>
{% endif %}

{% if user_input %}
<user_input>
{{ user_input }}
</user_input>
{% endif %}
```

## Guardrails

There is **no `Guardrail` type, no `Guardrails.builder()`, and no `guardrails:` configuration block** —
the prompt-injection / jailbreak / content-moderation guard trio shown in earlier revisions of this file
was invented, along with the `blockResponse:` keys. Guard-rail behaviour in Embabel is applied on the
LLM-call path rather than declared on a chatbot: the source tree carries
`ChatClientLlmOperationsGuardRailTest` and
`StreamingChatClientOperationsGuardRailTest` under `embabel-agent-api/src/test`, so the seam is the
`ChatClientLlmOperations` / streaming operations layer configured through the agent platform. Do not
present guardrails as a chatbot builder option; if you need them, read those two tests and the
`spi.support` classes they exercise before writing any code.

## Tool Calling

Tools reach a chatbot action through the agent/tool surface, not through a `ChatActions.register(...)`
registry (that builder does not exist — `ChatActions` is only ever the docs' example *class* name):

- annotate the tool method with **`@Tool`** (attested, 49 occurrences; e.g. `CiTools` documents
  "the method is annotated with `@Tool` to make it available as a callable tool");
- the action receives its **`ActionContext`** as a parameter — `ActionContext` is real and is the
  action-specific specialization of `OperationContext` (`reference/diagrams/architecture/
  embabel_execution_context.dot:17,42`), which is what the Step 2 sample's `context.ai()` call hangs
  off;
- the chat-side message types that carry tool traffic are `ToolCall`, `ToolResultMessage` and
  `AssistantMessageWithToolCalls` (`com.embabel.chat`).

Do **not** write `@ToolParam` — that annotation does not exist (0 occurrences); the parameter-level
annotation in this codebase is `@ToolParameter`, and `@ParameterName` is also in use. Read a shipped
`@Tool` provider such as `CiTools` before declaring a tool, rather than guessing the annotation set.

For RAG specifically, `ToolishRag` (`com.embabel.agent.rag.tools`) is the shipped pattern: constructing
it with a name, a description and the `SearchOperations` and then attaching it via
`.withReference(toolishRag)` contributes the search tool to the conversation automatically. See §2 above
and the RAG reference ([03-rag-architecture.md](./03-rag-architecture.md)).
