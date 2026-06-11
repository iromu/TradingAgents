# Reasoning Reference

Source: [embabel/embabel-agent-docs/rag.md](https://github.com/embabel/embabel/blob/main/embabel-agent-docs/rag.md)

## Overview

Reasoning (also called "thinking") enables models to generate a chain-of-thought before producing a final answer. This improves accuracy for complex tasks like math, logic, and multi-step reasoning.

## Enabling Reasoning

### Per-Request

```java
ChatOptions options = ChatOptions.builder()
    .thinking(true)
    .thinkingBudget(2048)  // Max tokens for thinking
    .build();

String response = chatClient.chat(session, "Solve: 23 * 47 + 12", options);
```

### Default

```java
ChatOptions defaultOptions = ChatOptions.builder()
    .thinking(true)
    .thinkingBudget(1024)
    .build();

ChatbotBuilder chatbot = ChatbotBuilder.builder()
    .chatOptions(defaultOptions)
    .build();
```

## Thinking Budget

Controls how many tokens the model can use for its chain-of-thought:

| Budget | Use Case |
|--------|----------|
| 256-512 | Simple reasoning, quick answers |
| 1024-2048 | Moderate complexity, multi-step |
| 4096+ | Complex math, deep analysis |

```java
ChatOptions options = ChatOptions.builder()
    .thinking(true)
    .thinkingBudget(4096)  // Allow extended reasoning
    .build();
```

## Model Support

Not all models support reasoning. Check model documentation:

| Model | Reasoning Support |
|-------|-------------------|
| qwen-plus | Yes |
| qwen-turbo | Limited |
| gpt-4o | Yes |
| gpt-4o-mini | Limited |

When a model doesn't support reasoning, the `thinking` flag is silently ignored.

## Reasoning with RAG

```java
ChatOptions options = ChatOptions.builder()
    .thinking(true)
    .thinkingBudget(2048)
    .build();

// RAG retrieves context, reasoning helps synthesize it
String response = chatClient.chat(session, "Compare the pros and cons of RAG vs fine-tuning based on the documents", options);
```

## Using Reasoning in ChatbotBuilder

```java
ChatbotBuilder chatbot = ChatbotBuilder.builder()
    .thinking(true)
    .thinkingBudget(2048)
    .build();
```
