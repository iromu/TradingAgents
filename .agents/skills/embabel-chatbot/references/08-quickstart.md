# Chatbot Quickstart — Complete Code Examples

Source: [embabel/embabel-agent-docs/chatbots.md](https://github.com/embabel/embabel/blob/main/embabel-agent-docs/chatbots.md)

This file contains complete, copy-pasteable code examples for building a chatbot step by step. For the high-level workflow, see the [main SKILL.md](../SKILL.md).

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

See [02-chat-history-store.md](./02-chat-history-store.md) for the full store API and a PostgreSQL example.

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

See [03-rag-architecture.md](./03-rag-architecture.md) for the full RAG API.
