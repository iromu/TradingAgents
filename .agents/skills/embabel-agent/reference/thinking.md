# Thinking & Reasoning Reference

Extracting LLM reasoning/thinking blocks from LLM responses. See SKILL.md for the core workflow.

## Motivation

Sometimes you need to validate an LLM's reasoning process in addition to obtaining a structured result. Access to thinking blocks helps you understand:

- Why the LLM made certain choices
- What went wrong when the LLM cannot fulfill a request
- Whether the output adheres to constraints (e.g., dates within requested months, destinations in specified countries)

## Core Types

| Type | Description |
|------|-------------|
| `ThinkingBlock` | Carries reasoning details: tag type, tag value, reasoning text |
| `ThinkingTagType` | Enum: `TAG` (XML-style like `<think>`), `PREFIX` (line prefix like `//THINKING:`), `NO_PREFIX` (untagged text before JSON) |
| `ThinkingResponse<T>` | Wrapper holding result object + list of `ThinkingBlock` instances |
| `ThinkingException` | Preserves thinking blocks when object instantiation fails |

## Usage

```java
PromptRunner runner = ai.withDefaultLlm()
    .withToolObject(Tooling.class);

ThinkingResponse<MonthItem> response = runner
    .thinking()
    .createObject(prompt, MonthItem.class);

// Access the structured result
MonthItem result = response.getResult();

// Access the LLM's reasoning process
List<ThinkingBlock> thinkingBlocks = response.getThinkingBlocks();

for (ThinkingBlock block : thinkingBlocks) {
    System.out.println("Type: " + block.getTagType());   // TAG, PREFIX, or NO_PREFIX
    System.out.println("Tag: " + block.getTagValue());   // e.g., "think", "analysis"
    System.out.println("Content: " + block.getContent());
}
```

## Key Points

- Use `.thinking()` on the `PromptRunner` to enable thinking extraction
- `ThinkingResponse` holds both result and thinking blocks
- `ThinkingException` preserves thinking even on object instantiation failure
- Three tag types: XML tags, line prefixes, and untagged text