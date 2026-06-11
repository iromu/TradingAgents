# Chatbot Building Patterns

Source: [embabel/embabel-extra-docs/springrod-blog/09-agentic-rag-chatbot.md](https://github.com/embabel/embabel/blob/main/embabel-extra-docs/springrod-blog/09-agentic-rag-chatbot.md)

## Complete Chatbot Example

### 1. Dependencies

Add to `pom.xml`:

```xml
<dependency>
    <groupId>com.embabel</groupId>
    <artifactId>embabel-agent</artifactId>
</dependency>
```

### 2. Build the Chatbot

```java
import com.embabel.agent.chatbot.api.Chatbot;
import com.embabel.agent.chatbot.api.ChatMemory;
import com.embabel.agent.chatbot.api.ChatSession;
import com.embabel.agent.chatbot.api.ChatOptions;
import com.embabel.agent.chatbot.api.ChatExtension;
import com.embabel.agent.chatbot.api.ChatActions;
import com.embabel.agent.chatbot.api.ChatConfiguration;
import com.embabel.agent.chatbot.impl.InMemoryChatMemory;
import com.embabel.agent.chatbot.impl.ChatbotBuilder;
import com.embabel.agent.knowledge.api.RagSystem;
import com.embabel.agent.knowledge.api.RagSource;
import com.embabel.agent.knowledge.api.PromptTemplate;
import com.embabel.agent.knowledge.impl.RagBuilder;
import com.embabel.agent.knowledge.store.FileRagSource;
import com.embabel.agent.knowledge.filter.FilterBuilder;
import com.embabel.agent.knowledge.filter.Filter;
import com.embabel.agent.knowledge.guardrails.Guardrail;
import com.embabel.agent.knowledge.reasoning.Reasoning;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class MyChatbot {

    public Chatbot build() {
        // 1. Build the RAG system
        RagSource ragSource = FileRagSource.builder()
            .directory(Paths.get("documents"))
            .fileExtensions(List.of(".pdf", ".txt", ".md"))
            .build();

        RagSystem ragSystem = RagBuilder.builder()
            .ragSources(List.of(ragSource))
            .filterBuilder(FilterBuilder.builder().build())
            .build();

        // 2. Build the chat memory
        ChatMemory chatMemory = InMemoryChatMemory.builder().build();

        // 3. Build the chat session
        ChatSession chatSession = ChatSession.builder()
            .chatMemory(chatMemory)
            .build();

        // 4. Configure chat options
        ChatOptions chatOptions = ChatOptions.builder()
            .model("qwen-plus")
            .maxTokens(2048)
            .temperature(0.7)
            .build();

        // 5. Build the chatbot
        return ChatbotBuilder.builder()
            .ragSystem(ragSystem)
            .chatMemory(chatMemory)
            .chatSession(chatSession)
            .chatOptions(chatOptions)
            .build();
    }
}
```

### 3. Using the Chatbot

```java
public class ChatbotApp {
    static void main(String[] args) {
        MyChatbot factory = new MyChatbot();
        Chatbot chatbot = factory.build();

        // Chat with the bot
        String response = chatbot.chat("What is RAG?");
        System.out.println(response);

        // Chat with options
        ChatOptions options = ChatOptions.builder()
            .thinking(true)
            .thinkingBudget(2048)
            .build();
        String detailed = chatbot.chat("Compare RAG vs fine-tuning", options);
        System.out.println(detailed);
    }
}
```

## chatbot.yaml Configuration

You can configure a chatbot entirely in YAML:

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
    filterBuilder:
      type: default
  guardrails:
    - type: promptInjection
    - type: jailbreak
    - type: contentModeration
  thinking:
    enabled: true
    budget: 1024
  memory:
    type: inMemory
    maxMessages: 50
```

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

## Guardrails Configuration

```yaml
guardrails:
  - type: promptInjection
    blockResponse: "I can't help with that. Please rephrase your request."
  - type: jailbreak
    blockResponse: "I can't help with that. Please rephrase your request."
  - type: contentModeration
    blockResponse: "I can't help with that. Please rephrase your request."
```

## Custom Chat Extension

```java
public class MyChatExtension implements ChatExtension {
    @Override
    public List<ChatMessage> apply(List<ChatMessage> messages, ChatOptions options) {
        // Add system context to every request
        ChatMessage systemContext = new ChatMessage(
            "system",
            "You are a helpful assistant specialized in answering questions about the provided documents."
        );
        return List.of(systemContext)
            .stream()
            .filter(m -> !m.role().equals("system"))
            .toList();
    }
}
```

## Chat Actions (Tool Calling)

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

ChatbotBuilder chatbot = ChatbotBuilder.builder()
    .chatActions(chatActions)
    .build();
```
