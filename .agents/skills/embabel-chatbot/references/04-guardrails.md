# Guardrails Reference

Source: [embabel/embabel-agent-docs/rag.md](https://github.com/embabel/embabel/blob/main/embabel-agent-docs/rag.md)

## Overview

Guardrails protect the chatbot from prompt injection, jailbreaks, and harmful content. They run as a pre-processing step
on user input before it reaches the LLM.

## Guardrail Types

| Type                   | What it Detects                            | When to Use                    |
|------------------------|--------------------------------------------|--------------------------------|
| **Prompt Injection**   | Attempts to override system instructions   | Always                         |
| **Jailbreak**          | Attempts to bypass safety constraints      | Always                         |
| **Content Moderation** | Harmful, illegal, or inappropriate content | Production systems             |
| **PII Detection**      | Personally identifiable information        | Systems handling personal data |
| **Custom**             | Domain-specific checks                     | Any specialized need           |

## Built-in Guardrails

### Prompt Injection Guardrail

```java
Guardrail injectionGuardrail = Guardrails.promptInjection();
```

Detects patterns like:

- "Ignore previous instructions"
- "You are now..."
- "Do not follow the system prompt"
- Hidden/encoded instructions in user input

### Jailbreak Guardrail

```java
Guardrail jailbreakGuardrail = Guardrails.jailbreak();
```

Detects:

- Role-play bypasses ("pretend you're...")
- Multi-layered prompts designed to confuse
- Known jailbreak patterns

### Content Moderation Guardrail

```java
Guardrail contentModeration = Guardrails.contentModeration();
```

Checks for:

- Hate speech
- Violence
- Sexual content
- Self-harm
- Illegal activities

## Custom Guardrails

```java
Guardrail customGuardrail = Guardrail.custom((input) -> {
    // Return true if the input is SAFE, false if it should be blocked
    if (input.contains("blocked-term")) {
        return false;
    }
    return true;
});
```

## Using Guardrails with Chatbot

```java
ChatbotBuilder chatbot = ChatbotBuilder.builder()
    .guardrails(List.of(
        Guardrails.promptInjection(),
        Guardrails.jailbreak(),
        Guardrails.contentModeration()
    ))
    .build();
```

## Guardrail Response

When a guardrail triggers, you can customize the response:

```java
Guardrail guardrail = Guardrails.promptInjection()
    .blockResponse("I can't help with that. Please rephrase your request.");
```

## Combined Example

```java
List<Guardrail> guardrails = List.of(
    Guardrails.promptInjection(),
    Guardrails.jailbreak(),
    Guardrails.contentModeration(),
    Guardrail.custom((input) -> {
        // Block requests for specific topics
        return !input.toLowerCase().contains("restricted-topic");
    })
);

ChatbotBuilder chatbot = ChatbotBuilder.builder()
    .guardrails(guardrails)
    .build();
```
