Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.26. Working with Streams
Developer can choose piping data from LLM gradually, using Embabel streaming capabilities.In addition to streaming the raw text output from the LLM, Embabel streams can also include LLM reasoning events, so-called "thinking", and stream of objects created by the LLM.
This feature is well aligned with Embabel focus on object-oriented programming model.
#### 4.26.1. Concepts

- `StreamingEvent` - wraps Thinking or user Object
- ``StreamingPromptRunnerBuilder`` - runner with streaming capabilities
- Spring Reactive Programming Support for Spring AI ChatClient as underlying infrastructure
- All reactive callbacks, such as *doOnNext*, *doOnComplete*, etc. are at developer’s disposal
#### 4.26.2. Example - Simple Thinking and Object Streaming with Callbacks

#### 4.26.3. Example - Simple Raw Text Streaming with Callbacks