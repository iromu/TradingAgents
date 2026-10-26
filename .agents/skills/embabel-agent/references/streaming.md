# Streaming

Embabel supports streaming lets you pipe data from the LLM gradually as it produces output.

Embabel streams can carry raw text, LLM "thinking" (reasoning) events, and structured objects — aligning with Embabel's object-oriented programming model. Under the hood it uses Spring Reactive Programming (Spring AI's `ChatClient`), so all reactive callbacks (`doOnNext`, `doOnComplete`, etc.) are available.

## Key Concepts

| Type | Description |
|------|-------------|
| `StreamingEvent<T>` | Wraps either a thinking event or a user-defined object |
| `StreamingPromptRunnerBuilder` | Builder that adds streaming capabilities to a `PromptRunner` |
| `Flux<T>` | The reactive stream type returned by all streaming methods |

## Basic Streaming — Raw Text

Use `generateStream()` to receive raw text chunks as they arrive from the LLM.

```java
import com.embabel.agent.core.prompt.PromptRunner;
import com.embabel.agent.core.streaming.StreamingPromptRunnerBuilder;
import reactor.core.publisher.Flux;

PromptRunner runner = ai.withDefaultLlm();

Flux<String> results = new StreamingPromptRunnerBuilder(runner)
        .streaming()
        .withPrompt("What is the highest building in Paris?")
        .generateStream();

results
        .doOnNext(chunk -> System.out.println(chunk))
        .doOnError(err -> System.err.println("Error: " + err.getMessage()))
        .doOnComplete(() -> System.out.println("Stream complete"))
        .blockLast(Duration.ofSeconds(6000));
```

## Streaming with Thinking and Objects

Use `createObjectStreamWithThinking()` to receive both reasoning events and structured objects.

```java
import com.embabel.agent.core.prompt.PromptRunner;
import com.embabel.agent.core.streaming.StreamingPromptRunnerBuilder;
import com.embabel.agent.core.streaming.StreamingEvent;
import reactor.core.publisher.Flux;

// Register the object type the LLM should produce
PromptRunner runner = ai.withDefaultLlm()
        .withToolObject(Tooling.class);

Flux<StreamingEvent<MonthItem>> results = new StreamingPromptRunnerBuilder(runner)
        .streaming()
        .withPrompt("What are the two hottest months in Florida and their temperatures?")
        .createObjectStreamWithThinking(MonthItem.class);

results
        .timeout(Duration.ofSeconds(150))
        .doOnSubscribe(sub -> System.out.println("Stream started"))
        .doOnNext(event -> {
            if (event.isThinking()) {
                System.out.println("Thinking: " + event.getThinking());
            } else if (event.isObject()) {
                MonthItem obj = event.getObject();
                System.out.println("Object: " + obj.getName());
            }
        })
        .doOnError(err -> System.err.println("Error: " + err.getMessage()))
        .doOnComplete(() -> System.out.println("Stream complete"))
        .blockLast(Duration.ofSeconds(6000));
```

## Streaming Scalar Types

`createObjectStreamWithThinking` expects the target class to produce a JSON object schema (`{...}`). Passing `String.class` directly makes the LLM emit bare JSON strings such as `"July - 29°C"` instead of JSON objects — the streaming parser cannot recognize these as structured output events and misclassifies them as thinking content.

Wrap scalars in `StringResult` from `com.embabel.common.ai.converters.streaming`:

```java
import com.embabel.common.ai.converters.streaming.StringResult;

Flux<StreamingEvent<StringResult>> results = new StreamingPromptRunnerBuilder(runner)
        .streaming()
        .withPrompt(prompt)
        .createObjectStreamWithThinking(StringResult.class);

results.doOnNext(event -> {
    if (event.isObject()) {
        String value = event.getObject().getValue();  // unwrap the scalar
    }
});
```

`StringResult` produces schema `{"type":"object","properties":{"value":{"type":"string"}}}`, so the LLM returns `{"value":"..."}` which is correctly parsed and emitted as an `isObject()` event.

## Reactive Callbacks

Because Embabel uses Spring Reactive Programming, you can compose streams with any `Flux` operator:

- `doOnSubscribe()` — when the stream subscription begins
- `doOnNext()` — for each event (thinking or object)
- `doOnError()` — on stream errors
- `doOnComplete()` — when the stream finishes
- `timeout()` — guard against hung streams

## Notes

- Always set a timeout to prevent indefinite hangs
- Use `.blockLast()` to synchronously consume the stream (useful in tests)
- For async consumption, use `.subscribe()` instead of `.blockLast()`
- Streaming scalar values? Wrap them in `StringResult` — bare `String.class` targets get misclassified as thinking events
---

*Source: Embabel Agent v1.5.3 documentation — `references/streaming`*
