# Streams Reference

Embabel supports streaming data from the LLM gradually, using Spring Reactive Programming (Spring AI ChatClient as infrastructure).

## Concepts

- **`StreamingEvent`** — wraps `Thinking` or user `Object`
- **`StreamingPromptRunnerBuilder`** — runner with streaming capabilities
- **Spring Reactive** — `doOnNext`, `doOnComplete`, etc. callbacks

## Simple Object Streaming

```java
context.ai().withDefaultLlm()
    .streaming()
    .createObject(Report.class)
    .fromPrompt("Generate report")
    .doOnNext(event -> {
        if (event instanceof Thinking thinking) {
            System.out.println("Thinking: " + thinking.getContent());
        } else if (event instanceof ObjectCreated obj) {
            System.out.println("Created: " + obj.getObject());
        }
    })
    .doOnComplete(() -> {
        System.out.println("Stream complete");
    })
    .subscribe();
```

## Raw Text Streaming

```java
context.ai().withDefaultLlm()
    .streaming()
    .generateText("Write a story")
    .doOnNext(event -> {
        if (event instanceof TextChunk chunk) {
            System.out.print(chunk.getText());
        }
    })
    .doOnComplete(() -> System.out.println())
    .subscribe();
```

## Key Points

- Streaming works with `createObject`, `createObjectIfPossible`, and `generateText`
- `Thinking` events contain LLM reasoning content
- `ObjectCreated` events contain the created object as it's streamed
- Use Spring Reactive callbacks (`doOnNext`, `doOnComplete`, `doOnError`, etc.)
- Must call `.subscribe()` to start the stream
- Underlying infrastructure is Spring AI ChatClient
