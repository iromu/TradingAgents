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

## Step 1: Build a Basic Chatbot

Start with the simplest possible chatbot:

```java
import com.embabel.chatbot.builder.ChatbotBuilder;
import com.embabel.chatbot.session.ChatSession;
import com.embabel.chatbot.memory.InMemoryChatMemory;
import com.embabel.chatbot.options.ChatOptions;

// Build the chat memory
var chatMemory = InMemoryChatMemory.builder().build();

// Build the chat session
var chatSession = ChatSession.builder()
    .chatMemory(chatMemory)
    .build();

// Configure options
var chatOptions = ChatOptions.builder()
    .model("qwen-plus")
    .maxTokens(2048)
    .temperature(0.7)
    .build();

// Build the chatbot
var chatbot = ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatSession(chatSession)
    .chatOptions(chatOptions)
    .build();

// Chat!
String response = chatbot.chat("What is RAG?");
System.out.println(response);
```

## Step 2: Add RAG (Retrieval-Augmented Generation)

Give your chatbot access to external knowledge:

```java
import com.embabel.rag.builder.RagBuilder;
import com.embabel.rag.source.FileRagSource;
import com.embabel.rag.filter.FilterBuilder;
import com.embabel.rag.template.PromptTemplate;

// Define document sources
var fileSource = FileRagSource.builder()
    .directory(Paths.get("/path/to/documents"))
    .fileExtensions(List.of(".pdf", ".txt", ".md"))
    .build();

// Build RAG system
var rag = RagBuilder.builder()
    .ragSources(List.of(fileSource))
    .filterBuilder(FilterBuilder.builder().build())
    .build();

// Add RAG to the chatbot
var chatbot = ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatSession(chatSession)
    .chatOptions(chatOptions)
    .rag(rag)
    .build();
```

Now when users ask questions, the chatbot retrieves relevant documents and answers based on them.

## Step 3: Add Guardrails

Protect against prompt injection, jailbreaks, and harmful content:

```java
import com.embabel.chatbot.guardrails.Guardrails;

var guardrails = Guardrails.builder()
    .addPromptInjectionGuard()
    .addJailbreakGuard()
    .addContentModerationGuard()
    .build();

var chatbot = ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatSession(chatSession)
    .chatOptions(chatOptions)
    .guardrails(guardrails)
    .build();
```

## Step 4: Enable Reasoning (Thinking Mode)

For complex reasoning tasks, enable the model's thinking capability:

```java
var chatOptions = ChatOptions.builder()
    .model("qwen-plus")
    .thinking(true)
    .thinkingBudget(2048)
    .build();
```

## Step 5: Structured Output

Force JSON responses for programmatic consumption:

```java
var chatOptions = ChatOptions.builder()
    .responseFormat("json")
    .build();

String jsonResponse = chatClient.chat(chatSession, "List the top 3 features as JSON", chatOptions);
// Parse the JSON response with your preferred library
```

## Step 6: Custom Chat Extension

Add pre-processing logic that runs before every LLM call:

```java
import com.embabel.chatbot.extension.ChatExtension;
import com.embabel.chatbot.message.ChatMessage;

public class MyChatExtension implements ChatExtension {
    @Override
    public List<ChatMessage> apply(List<ChatMessage> messages, ChatOptions options) {
        // Add system context, rewrite messages, inject metadata
        return messages;
    }
}

var chatbot = ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatSession(chatSession)
    .chatOptions(chatOptions)
    .chatExtension(new MyChatExtension())
    .build();
```

## Step 7: Chat Actions (Tool Calling)

Let the chatbot call external tools and functions:

```java
import com.embabel.chatbot.actions.ChatActions;

var chatActions = ChatActions.builder()
    .register("search", (session, args) -> {
        String query = (String) args.get("query");
        return searchService.search(query);
    })
    .register("calculate", (session, args) -> {
        String expression = (String) args.get("expression");
        return calculator.eval(expression);
    })
    .build();

var chatbot = ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatSession(chatSession)
    .chatOptions(chatOptions)
    .chatActions(chatActions)
    .build();
```

## Step 8: Persist Chat History

Store conversations in a database for continuity across restarts:

```java
import com.embabel.chatbot.store.ChatHistoryStore;

// Use a custom store
var chatHistoryStore = new PostgresChatHistoryStore(jdbcTemplate);

var chatMemory = InMemoryChatMemory.builder()
    .chatHistoryStore(chatHistoryStore)
    .build();
```

See `references/02-chat-history-store.md` for the full store API and a PostgreSQL example.

## Step 9: Configure with YAML (Optional)

For configuration-driven chatbots, use `chatbot.yaml`:

```yaml
chatbot:
  model: qwen-plus
  maxTokens: 2048
  temperature: 0.7
  rag:
    sources:
      - type: file
        directory: ./documents
        fileExtensions: [".pdf", ".txt", ".md"]
  guardrails:
    - type: promptInjection
    - type: jailbreak
  thinking:
    enabled: true
    budget: 1024
  memory:
    type: inMemory
    maxMessages: 50
```

## Step 10: Advanced RAG — Filters and Prompt Templates

### Filtered RAG

Retrieve only relevant documents:

```java
import com.embabel.rag.filter.Filter;

Filter filter = FilterBuilder.builder()
    .addFilter("source", "products.pdf")
    .addSimilarityFilter("pricing", 0.7)
    .build();

var rag = RagBuilder.builder()
    .ragSources(List.of(fileSource))
    .filterBuilder(FilterBuilder.builder().build())
    .build();
```

### Custom Prompt Templates

Control exactly how the RAG prompt is constructed:

```java
import com.embabel.rag.template.PromptTemplate;

var promptTemplate = PromptTemplate.builder()
    .template("""
        Context:
        {% for source in sources %}
        [Source: {{ source.metadata.source }}]
        {{ source.content }}
        {% endfor %}

        Question: {{ question }}

        Answer based on the context above:
        """)
    .build();

var rag = RagBuilder.builder()
    .ragSources(List.of(fileSource))
    .promptTemplate(promptTemplate)
    .build();
```

See `references/03-rag-architecture.md` for the full RAG API.

## Reference Files

| File | Content |
|------|---------|
| `references/01-chatbot-api.md` | ChatbotBuilder, ChatOptions, ChatSession, ChatClient, ChatMemory, ChatExtension, ChatActions, ChatConfiguration |
| `references/02-chat-history-store.md` | ChatHistoryStore, InMemoryChatHistoryStore, persistence to databases |
| `references/03-rag-architecture.md` | RAGBuilder, RAGSource, FilterBuilder, Filter, PromptTemplate |
| `references/04-guardrails.md` | Guardrails, prompt injection, jailbreak detection, content moderation |
| `references/05-reasoning.md` | Reasoning, thinking, thinking budget, model reasoning support |
| `references/06-chatbot-patterns.md` | Complete chatbot building guide, chatbot.yaml, ragbot.jinja |
| `references/07-structured-output.md` | Structured output, JSON mode, model capabilities |

## Pitfalls

- **Chat history grows unbounded** — set `maxMessages` or `maxTokens` on `InMemoryChatMemory`
- **RAG retrieves irrelevant docs** — use filters to narrow the source scope
- **Guardrails block legitimate queries** — tune guardrail thresholds; start with just prompt injection guard
- **Thinking mode is slow** — only enable for complex reasoning tasks; disable for simple Q&A
- **JSON mode can fail** — always validate and handle malformed JSON responses
- **Model capabilities vary** — check `supportsJson()`, `supportsTools()`, `supportsVision()` before using features

## Checklist

- [ ] Basic chatbot works (Step 1)
- [ ] RAG sources configured (Step 2)
- [ ] Guardrails added (Step 3)
- [ ] Reasoning enabled if needed (Step 4)
- [ ] Structured output if needed (Step 5)
- [ ] Chat extension for pre-processing (Step 6)
- [ ] Chat actions for tool calling (Step 7)
- [ ] Chat history persisted (Step 8)
- [ ] RAG filtered and prompt templates customized (Step 10)
- [ ] Tested with real documents and user queries
