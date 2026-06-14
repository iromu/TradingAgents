Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 3.1. How to Use the Shell
The Embabel Shell is built on Spring Shell and provides an interactive command-line interface for running and developing agents.
It is the fastest way to try agents, iterate on prompts, and observe agent behaviour in detail.
#### 3.1.1. Starting the Shell
With the `embabel-agent-starter-shell` dependency and API keys configured, start your application normally.
The shell prompt appears automatically:
```
shell:>
```
If you are using the Embabel example or template projects, use the provided convenience script:
#### 3.1.2. Navigating the Shell
Type `help` to list all available commands with a short description of each:
```
shell:> help
```
Tab completion is supported for command names and, where applicable, for option values.
Press `<Tab>` after typing a partial command or flag to see available completions.The shell maintains a persistent command history across restarts.
Use the up/down arrow keys to navigate previous commands, or type `!!` to repeat the last command — especially handy when iterating on an agent prompt:
```
shell:> !!
```

#### 3.1.3. How User Input Reaches an Agent
When you run `execute "some text"`, the shell wraps the quoted string in a `UserInput` object and places it on the agent process blackboard.
Agents declare a dependency on `UserInput` by accepting it as a parameter in their first `@Action` method.
Embabel’s planner sees that `UserInput` is available and selects the appropriate action automatically.This is the same mechanism used in web controllers and webhook handlers — only the source of `UserInput` changes (shell vs. HTTP request vs. event payload).
See Invoking Embabel Agents for programmatic invocation patterns.
#### 3.1.4. Logging Verbosity
The `-p` and `-r` flags on `execute` control what gets logged during a run:
| Flag | Effect || `-p` | Log LLM prompts sent by the agent || `-r` | Log raw LLM responses received by the agent |
Omit both flags for quiet output showing only the final result.
Use both together for maximum visibility when debugging a misbehaving agent.

### 3.2. Shell Commands

#### 3.2.1. Agent Execution Commands

##### execute (x)
Run the most appropriate agent for the given natural-language input.
Embabel uses Autonomy to rank all registered agents and selects the best match.
```
execute "Lynda is a Scorpio, find news for her"

# Shorthand alias
x "Lynda is a Scorpio, find news for her"
```

| Option | Description || `-p` | Log LLM prompts during execution || `-r` | Log raw LLM responses during execution || `-o` / `--open` | Open mode: select the best *goal* across all agents, then assemble a dynamic agent from any available actions to achieve it.
Without this flag, closed mode is used: a single agent is selected and runs in isolation. || `-c key=val,…​` | Per-execution tool call context entries, merged with any persistent context set via `set-context`.
Per-execution entries win on conflict. |
Example combining flags:
```
# Verbose closed-mode run
x "fact check: the Eiffel Tower is in Berlin" -p -r

# Open mode with prompt logging
x "Research the latest Go release" -o -p

# Per-execution context override
x "Find news for Alice" -c "tenantId=beta,correlationId=req-456"
```

##### chat
Start an interactive back-and-forth conversation with the most appropriate agent.
The agent responds to each message in turn, maintaining state across the session.
```
shell:> chat
chat:> What does the document say about taxes?
chat:> Summarise that in three bullet points.
chat:> exit
```
Type `exit` to end the chat session and return to the main shell prompt.
##### choose-goal
Use the LLM to rank all available goals across all agents against the provided input, and display the rankings without executing anything.
Useful for understanding which goal Embabel would select for a given input, and for tuning agent descriptions.
```
choose-goal "Find a horoscope for Alice who is a Scorpio"
```
The output shows each candidate goal, its score, and the agent it belongs to.
If the top-ranked goal is not what you expected, revisit the `description` attribute on the relevant `@AchievesGoal` or `@Agent` annotation.See Dynamic Agent and Goal Selection with Autonomy for the confidence threshold configuration that controls when a goal is considered a strong enough match to execute.
#### 3.2.2. Tool Call Context Commands
The shell supports setting out-of-band metadata that is passed to all tools during agent execution.
This is the shell interface for ToolCallContext.
##### set-context (sc)
Set persistent tool call context as comma-separated `key=value` pairs.
This context is passed to every subsequent `execute` / `x` invocation until cleared.
```
# Set persistent context
set-context tenantId=acme,authToken=bearer-xyz123

# Shorthand alias
sc tenantId=acme,authToken=bearer-xyz123

# Clear persistent context
set-context clear
```

##### show-context
Display the current persistent tool call context.
```
shell:> show-context
Tool call context: {tenantId=acme, authToken=bearer-xyz123}
```

##### Per-Execution Context Override
The `execute` command accepts a `-c` / `--context` flag for one-off context entries.
These are merged with the persistent context; per-execution entries win on conflict.
```
# Persistent context: tenantId=acme
# Per-execution override adds correlationId and could override tenantId
x "Find news for Alice" -c "correlationId=req-456,tenantId=beta"
```
In this example the effective context for that single execution is `{tenantId=beta, authToken=bearer-xyz123, correlationId=req-456}`.
The persistent context remains `{tenantId=acme, authToken=bearer-xyz123}` for future invocations.
#### 3.2.3. Implementing Custom Shell Commands
During development you may want to add your own shell commands to invoke specific agents or flows directly, bypassing the natural-language routing of `execute`.
Because the Embabel Shell is a standard Spring Shell application, any `@ShellComponent` bean is discovered and registered automatically by Spring.Inject `AgentPlatform` and use `AgentInvocation` to call agents with strong typing:JavaKotlin
| | Custom shell commands are particularly useful when you want to pre-populate inputs with test data or invoke a specific agent directly rather than relying on Autonomy’s natural-language selection.
For full `AgentInvocation` API details see Invoking Embabel Agents. |

### 3.3. Embabel Modules
Embabel spans multiple modules, in this and other repositories in the `embabel` organization.The status of these modules varies.
There are three statuses:
- **Stable**: these modules are considered production ready.
We strive to avoid breaking changes.
- **Incubating**: these modules are under active development and may have breaking changes in minor releases.
However, they are considered generally usable and can be expected to graduate to stable.
Use with caution.
- **Experimental**: these modules are early stage and may have breaking changes in any release.
They are not recommended for production use.
These modules may be removed without replacement and there is no guarantee of them graduating to a more stable status.Of course, contributions are welcome to all modules!
#### 3.3.1. Module Directory
The following are modules intended for direct use (versus supporting infrastructure).
##### Core Modules

| Name | Location | Purpose | Notes | Status || `embabel-agent-api` | This repo | Core API | Main programming interface for building agents | Stable || `embabel-agent-domain` | This repo | Domain types and entities | Shared domain model | Incubating |

##### Feature Modules

| Name | Location | Purpose | Notes | Status || `embabel-agent-a2a` | This repo | Agent-to-Agent protocol support | Google A2A protocol implementation | Incubating || `embabel-agent-code` | This repo | Coding domain library | Code analysis and generation utilities | Stable || `embabel-agent-discord` | This repo | Discord bot integration | Build agents as Discord bots | Experimental || `embabel-agent-eval` | This repo | Agent evaluation framework | Assess agent performance on tasks | Experimental || `embabel-agent-mcpserver` | This repo | MCP server support | Export agents as MCP servers | Stable || `embabel-agent-openai` | This repo | OpenAI-specific utilities | Structured outputs, response format | Stable || `embabel-agent-onnx` | This repo | Local ONNX Runtime inference | Local embedding models via ONNX Runtime. Default: `all-MiniLM-L6-v2` | Incubating || `embabel-agent-remote` | This repo | Remote action support | Execute actions on remote systems, enabling dynamic registration to extend the capabilities of an Embabel server | Experimental || `embabel-agent-shell` | This repo | Command-line interface | Interactive shell for agent development | Stable || `embabel-agent-skills` | This repo | Support for emerging Agent Skills standard | Composable agent skills | Experimental || `embabel-agent-spec` | This repo | Serializable action and goal definitions | Enables agents to be defined in YML or otherwise persisted in a serialized format | Experimental |

##### RAG and Context Engineering Modules

| Name | Location | Purpose | Notes | Status || `embabel-agent-rag-core` | This repo | Core RAG abstractions | Base interfaces for RAG, encompassing programming model (`ToolishRag`), storage abstractions (`SearchOperations`) and document model. | Stable || `embabel-agent-rag-lucene` | This repo | Lucene RAG store | Local storage with Apache Lucene supporting vector and text search | Stable || `embabel-agent-rag-tika` | This repo | Apache Tika integration | Document parsing (Markdown, PDF, Word, etc.) | Incubating || `embabel-agent-rag-neo-drivine` | `embabel/embabel-agent-rag-neo-drivine` | Neo4j graph RAG | RAG store for Neo4j graph database | Incubating || `embabel-rag-pgvector` | `embabel/embabel-rag-pgvector` | PostgreSQL pgvector RAG | RAG store for PostgreSQL with pgvector extension supporting hybrid search (vector, full-text, fuzzy) | Incubating || `dice` | `embabel/dice` | Support for Domain Oriented Context Engineering | Sophisticated pipeline for context engineering and integration with enterprise data. Incorporates proposition extraction and projection into knowledge graphs, memory and experimental representations. | Incubating |

##### Spring Boot Starters

| Name | Location | Purpose | Notes | Status || `embabel-agent-starter` | This repo | Base starter | Core dependencies only (no LLM provider) | Stable || `embabel-agent-starter-anthropic` | This repo | Anthropic starter | Quick start with Claude | Stable || `embabel-agent-starter-openai` | This repo | OpenAI starter | Quick start with GPT | Stable || `embabel-agent-starter-ollama` | This repo | Ollama starter | Quick start with local Ollama | Stable || `embabel-agent-starter-onnx` | This repo | ONNX starter | Add local ONNX embedding models | Incubating || `embabel-agent-starter-shell` | This repo | Shell starter | Add interactive shell for development | Stable || `embabel-agent-starter-a2a` | This repo | A2A starter | Add A2A server support | Incubating || `embabel-agent-starter-mcpserver` | This repo | MCP server starter | Add MCP server support | Stable || `embabel-agent-starter-bedrock` | This repo | Bedrock starter | Quick start with AWS Bedrock | Stable || `embabel-agent-starter-deepseek` | This repo | DeepSeek starter | Quick start with DeepSeek | Stable || `embabel-agent-starter-gemini` | This repo | Gemini starter | Quick start with Vertex AI | Stable || `embabel-agent-starter-google-genai` | This repo | Google GenAI starter | Quick start with AI Studio | Incubating || `embabel-agent-starter-oci-genai` | This repo | OCI Generative AI starter | Quick start with OCI GenAI | Incubating || `embabel-agent-starter-lmstudio` | This repo | LM Studio starter | Quick start with LM Studio | Incubating || `embabel-agent-starter-mistral-ai` | This repo | Mistral AI starter | Quick start with Mistral | Stable || `embabel-agent-starter-dockermodels` | This repo | Docker Models starter | Quick start with Docker Desktop AI | Stable || `embabel-agent-starter-openai-custom` | This repo | Custom OpenAI starter | Quick start with OpenRouter, etc. | Stable |

##### Test Support

| Name | Location | Purpose | Notes | Status || `embabel-agent-test` | This repo | Test utilities | JUnit extensions, test DSL | Incubating |

##### Example Repositories

| Name | Location | Purpose | Notes | Status || `embabel-agent-examples` | `embabel/embabel-agent-examples` | Example agents | Sample implementations and tutorials | Stable || `java-agent-template` | `embabel/java-agent-template` | Java project template | Starter template for Java agents | Stable |

##### Developer Tooling

| Name | Location | Purpose | Notes | Status || `embabel-agent-intellij` | `embabel/embabel-agent-intellij` | IntelliJ IDEA plugin | IDE support for Embabel Agent development. See IntelliJ Plugin. | Stable |

#### 3.3.2. Experimental APIs
While the status of modules may change over time, any module may contain clearly identified experimental functionality.
This enables us to innovate in the open without excessive build complexity.Please try and provide feedback on this functionality, but don’t rely on it and be aware that it may change without notice.
| | Any type or method annotated with the `@ApiStatus.Experimental` annotation is not guaranteed to be stable. |

## 4. Reference