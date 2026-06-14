Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.18. Invoking Embabel Agents
While many examples show Embabel agents being invoked via `UserInput` through the Embabel shell, they can also be invoked programmatically with strong typing.This is usually how they’re used in web applications.
It is also the most deterministic approach as code, rather than LLM assessment of user input, determines which agent is invoked and how.
#### 4.18.1. Creating an AgentProcess Programmatically
You can create and execute agent processes directly using the `AgentPlatform`:JavaKotlinYou can create processes and populate their input map from varargs objects:JavaKotlin
#### 4.18.2. Using AgentInvocation
`AgentInvocation` provides a higher-level, type-safe API for invoking agents.
It automatically finds the appropriate agent based on the expected result type.
##### Basic Usage
JavaKotlin
##### Invocation with Named Inputs
JavaKotlin
##### Custom Process Options
Configure verbosity, budget, and other execution options:JavaKotlin
##### Passing Tool Call Context at Invocation Time
Use `ProcessOptions.withToolCallContext()` to attach out-of-band metadata that flows through the entire agent run to every tool invoked — including remote MCP tools, where it becomes MCP `_meta` on the wire.
This is the right place for cross-cutting infrastructure concerns such as auth tokens, tenant IDs, and correlation IDs that come from the incoming request.JavaKotlinContext set here can be read by any `@LlmTool` method that declares a `ToolCallContext` parameter.
It can also be supplemented per-interaction inside `@Action` methods using `PromptRunner.withToolCallContext()`; interaction-level values win on conflict.
See Receiving Out-of-Band Context in Tools for the full context pipeline.
##### Asynchronous Invocation
For long-running operations, use async invocation:JavaKotlin
##### Agent Selection
`AgentInvocation` automatically finds agents by examining their goals:
- Searches all registered agents in the platform
- Finds agents with goals that produce the requested result type
- Uses the first matching agent found
- Throws an error if no suitable agent is available
##### Real-World Web Application Example
Here’s how `AgentInvocation` is used in the Tripper travel planning application with htmx for asynchronous UI updates:JavaKotlin**Key Patterns:**
- **Async Execution**: Uses `invokeAsync()` to avoid blocking the web request
- **Job Tracking**: Maintains a map of active futures for status polling
- **htmx Integration**: Returns status updates that htmx can consume for UI updates
- **Error Handling**: Proper exception handling and user feedback
- **Resource Cleanup**: Removes completed jobs from memory
- **Process Options**: Configures verbosity and debugging for production use
##### Alternative: Direct AgentProcess Creation
For simpler use cases, you can create and start an `AgentProcess` directly without `AgentInvocation`.
This approach is used in the Tripper application and works well with webhooks or form submissions where you want to:
- Start a long-running agent process
- Return immediately with a process ID
- Poll for status using the platform’s built-in controllersJavaKotlinThe platform provides built-in REST endpoints for status checking:
- `GET /api/v1/process/{processId}` - Returns process status, result, and URLs
- `DELETE /api/v1/process/{processId}` - Terminates a running process
- `GET /events/process/{processId}` - SSE stream of process eventsEach endpoint can be individually disabled via configuration (see Configuration).
Set the corresponding property to `false` to have the endpoint respond with HTTP 404:A simple status polling controller can check completion and redirect to results:JavaKotlin**When to Use Each Approach:**
| Approach | Best For || `AgentInvocation.invokeAsync()` | When you need a `CompletableFuture` for programmatic handling, chaining, or integration with reactive frameworks || Direct `AgentProcess` creation | Webhooks, form submissions, or UI flows where you poll for status via REST/SSE |

##### Webhook Integration Example
For webhook-triggered workflows (e.g., JIRA, GitHub), the direct approach works well:JavaKotlinThe webhook caller can then poll `/api/v1/process/{processId}` or subscribe to SSE events at `/events/process/{processId}` to track progress.
| | Agents can also be exposed as MCP servers and consumed from tools like Claude Desktop. |

#### 4.18.3. Dynamic Agent and Goal Selection with Autonomy
The `Autonomy` class provides LLM-powered dynamic selection of agents and goals based on user intent.
Rather than programmatically choosing which agent to run, `Autonomy` uses an LLM to rank available agents or goals against the user’s input and select the best match.This is how the Embabel Shell processes natural language commands.
##### Execution Modes
`Autonomy` supports two execution modes:**Closed Mode** (`chooseAndRunAgent`): The LLM selects the most appropriate agent based on the user’s intent.
The selected agent runs in isolation using only its own actions and goals.**Open Mode** (`chooseAndAccomplishGoal`): The LLM selects the most appropriate goal from all available goals across all agents.
Embabel then assembles a dynamic agent that can use any action from any agent to achieve that goal.
##### Closed Mode Example
Use closed mode when you want strict agent boundaries:JavaKotlin
##### Open Mode Example
Use open mode when you want maximum flexibility in achieving goals:JavaKotlin
##### Using Arbitrary Bindings
`chooseAndAccomplishGoal` accepts any bindings, not just `UserInput`.
A `BindingsFormatter` extracts intent text from the bindings for goal ranking:JavaKotlinThe default `BindingsFormatter` extracts text using this priority:
1.`PromptContributor.contribution()` if the object implements `PromptContributor`
1.`HasInfoString.infoString()` if the object implements `HasInfoString`
1.`toString()` otherwiseYou can provide a custom formatter:JavaKotlin
##### Goal Choice Approval
You can require approval before executing a selected goal:JavaKotlin
##### Confidence Thresholds
`Autonomy` uses configurable confidence thresholds to filter matches.
If no agent or goal exceeds the threshold, a `NoAgentFound` or `NoGoalFound` exception is thrown.Configure thresholds in `application.properties`:Or override per-request using `GoalSelectionOptions`:JavaKotlin
##### Shell Usage
The Embabel Shell uses `Autonomy` for the `execute` (`x`) and `choose-goal` commands:
```
# Closed mode (default) - select best agent
x "Find a horoscope for Alice who is a Scorpio"

# Open mode - select best goal, use any actions
x "Find a horoscope for Alice who is a Scorpio" -o

# Show goal rankings without executing
choose-goal "Find a horoscope for Alice"
```
See execute (x) and Shell Commands for full command and flag documentation.
##### Handling Selection Failures
JavaKotlin