# Structured Output Reference

Source: [embabel/embabel-agent-docs/chatbots.md](https://github.com/embabel/embabel/blob/main/embabel-agent-docs/chatbots.md)

## JSON Response Mode

Force the LLM to return structured JSON output:

```java
ChatOptions options = ChatOptions.builder()
    .responseFormat("json")
    .build();

String jsonResponse = chatClient.chat(chatSession, "List the top 3 features", options);
// Parse the JSON response with your preferred library
```

## Structured Output with ChatCompletionRequest

For more complex structured outputs, use `ChatCompletionRequest`:

```java
ChatCompletionRequest request = ChatCompletionRequest.builder()
    .messages(List.of(
        new ChatMessage("system", "Extract entities from the text"),
        new ChatMessage("user", "John works at Acme Corp")
    ))
    .responseFormat("json")
    .build();

String jsonResponse = chatClient.chatCompletion(request);
```

## Model Capability Detection

Check if a model supports structured output:

```java
boolean supportsJson = chatClient.supportsJson(chatOptions.getModel());
boolean supportsImages = chatClient.supportsImages(chatOptions.getModel());
boolean supportsTools = chatClient.supportsTools(chatOptions.getModel());
boolean supportsVision = chatClient.supportsVision(chatOptions.getModel());
```

## Best Practices

1. **Always validate JSON output** — LLMs can produce malformed JSON
2. **Use system prompts** to guide the JSON structure
3. **Specify the schema** in the system prompt for consistency
4. **Handle errors** — fall back to text mode if JSON fails
