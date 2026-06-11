# Chatbot API Reference

Source: [embabel/embabel-agent-docs/chatbots.md](https://github.com/embabel/embabel/blob/main/embabel-agent-docs/chatbots.md)

## ChatbotBuilder

The entry point for building chatbots. Fluent API with builder pattern.

```java
ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatExtension(chatExtension)
    .chatActions(chatActions)
    .chatConfiguration(chatConfiguration)
    .build();
```

## ChatOptions

Configuration for how the LLM should respond. Set on `ChatSession` or per-request via `ChatClient`.

### Key Properties

| Property           | Type                  | Default     | Description                                |
|--------------------|-----------------------|-------------|--------------------------------------------|
| `model`            | `String`              | from config | Model to use (e.g., "qwen-plus", "gpt-4o") |
| `maxTokens`        | `Integer`             | from model  | Max tokens for the response                |
| `temperature`      | `Double`              | 0.7         | Randomness: 0=deterministic, 1=creative    |
| `topP`             | `Double`              | 1.0         | Nucleus sampling threshold                 |
| `topK`             | `Integer`             | —           | Top-K sampling (model-dependent)           |
| `presencePenalty`  | `Double`              | 0.0         | Penalize new tokens based on presence      |
| `frequencyPenalty` | `Double`              | 0.0         | Penalize tokens based on frequency         |
| `seed`             | `Long`                | —           | Seed for reproducible outputs              |
| `stopSequences`    | `List<String>`        | —           | Sequences that stop generation             |
| `responseFormat`   | `String`              | —           | "text" or "json"                           |
| `thinking`         | `Boolean`             | false       | Enable reasoning/thinking mode             |
| `thinkingBudget`   | `Integer`             | 1024        | Max tokens for thinking (if enabled)       |
| `extraBody`        | `Map<String, Object>` | —           | Extra JSON body params for API             |

### JSON Response Format

```java
ChatOptions options = ChatOptions.builder()
    .responseFormat("json")
    .build();

String jsonResponse = chatClient.chat(chatSession, userMessage, options);
// Response is a JSON string — parse with your preferred library
```

## ChatSession

Manages conversation state (message history) for a single conversation.

```java
ChatSession session = ChatSession.builder()
    .chatMemory(chatMemory)
    .build();

// Add a message
String response = session.chat("What is RAG?");

// Access history
List<ChatMessage> history = session.getHistory();
```

## ChatClient

The main interface for sending messages to the chatbot.

```java
// Basic chat
String response = chatClient.chat(session, "Hello");

// Chat with options
String response = chatClient.chat(session, "Hello", options);

// Stream response (returns Flux<String>)
Flux<String> stream = chatClient.stream(session, "Hello");

// Chat with system prompt override
String response = chatClient.chat(session, "Hello", systemPrompt, options);
```

## ChatMemory

Manages the conversation history. Controls what gets sent to the LLM.

```java
// In-memory history
ChatMemory chatMemory = InMemoryChatMemory.builder().build();

// With a message limit
ChatMemory chatMemory = InMemoryChatMemory.builder()
    .maxMessages(50)  // Keep last 50 messages
    .build();

// With a token limit
ChatMemory chatMemory = InMemoryChatMemory.builder()
    .maxTokens(8000)  // Total tokens across all messages
    .build();
```

## ChatExtension

Hook for pre-processing messages before they reach the LLM.

```java
public class MyExtension implements ChatExtension {
    @Override
    public List<ChatMessage> apply(List<ChatMessage> messages, ChatOptions options) {
        // Pre-process: add context, rewrite messages, etc.
        return messages;
    }
}
```

## ChatActions

Handler for tool/function calling. Maps action names to implementations.

```java
ChatActions chatActions = ChatActions.builder()
    .register("search", (session, args) -> {
        String query = (String) args.get("query");
        return searchService.search(query);
    })
    .register("calculate", (session, args) -> {
        String expression = (String) args.get("expression");
        return calculator.eval(expression);
    })
    .build();
```

## ChatConfiguration

Centralized configuration for the chatbot.

```java
ChatConfiguration config = ChatConfiguration.builder()
    .defaultModel("qwen-plus")
    .defaultTemperature(0.7)
    .defaultMaxTokens(2048)
    .build();
```

## Complete Example

```java
// 1. Build the chat memory
ChatMemory chatMemory = InMemoryChatMemory.builder().build();

// 2. Build the chat session
ChatSession chatSession = ChatSession.builder()
    .chatMemory(chatMemory)
    .build();

// 3. Configure chat options
ChatOptions chatOptions = ChatOptions.builder()
    .model("qwen-plus")
    .maxTokens(2048)
    .temperature(0.7)
    .build();

// 4. Build the chatbot
ChatbotBuilder chatbot = ChatbotBuilder.builder()
    .chatMemory(chatMemory)
    .chatSession(chatSession)
    .chatOptions(chatOptions)
    .build();

// 5. Chat
String response = chatbot.chat("Explain RAG in simple terms");
System.out.println(response);
```
