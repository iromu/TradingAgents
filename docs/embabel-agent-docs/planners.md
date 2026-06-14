Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.20. Choosing a Planner
Embabel supports multiple planning strategies.
Most are deterministic, but their behaviour differs—​although it is always predictable.All planning strategies are entirely typesafe in Java or Kotlin.The planning strategies currently supported out of the box are:
| Planner | Best For | Description || **GOAP** (default) | Business processes with defined outputs | Goal-oriented, deterministic planning. Plans a path from current state to goal using preconditions and effects. || **Utility** | Exploration and event-driven systems | Selects the highest-value available action at each step. Ideal when you don’t know the outcome upfront. || **Hybrid** | Reducer pipelines (gather many context-producing actions, run one synthesizer, stop) | Like Utility for action picking, but exits as soon as any registered goal is already satisfied. Pair an unsatisfiable goal (e.g. `NIRVANA`) with a real terminal goal: opportunistic research fires while research is profitable; the process completes the moment the real goal is reached. || **Supervisor** | Flexible multi-step workflows | LLM-orchestrated composition. An LLM selects which actions to call based on type schemas and gathered artifacts. |
As most of the documentation covers GOAP, this section discusses the alternative planners and nested workflows.
#### 4.20.1. Utility AI
Utility AI selects the action with the highest *net value* from all available actions at each step.
Unlike GOAP, which plans a path to a goal, Utility AI makes greedy decisions based on immediate value.Utility AI excels in **exploratory scenarios** where you don’t know exactly what you want to achieve.
Consider a GitHub issue triage system: when a new issue arrives, you don’t have a predetermined goal.
Instead, you want to react appropriately based on the issue’s characteristics—​maybe label it, maybe respond, maybe escalate.
The "right" action depends on what you discover as you process it.This makes Utility AI ideal for scenarios where:
- There is no clear end goal—​you’re exploring possibilities
- Multiple actions could be valuable depending on context
- You want to respond to changing conditions as they emerge
- The best outcome isn’t known upfront
##### When to Use Utility AI

- **Event-driven systems**: React to incoming events (issues, stars, webhooks) with the most appropriate action
- **Chatbots**: Where the platform provides multiple response options and selects the best one
- **Exploration**: When you want to discover what’s possible rather than achieve a specific goal
##### Using Utility AI with `@EmbabelComponent`
For Utility AI, actions are typically provided via `@EmbabelComponent` rather than `@Agent`.
This allows the *platform* to select actions across multiple components based on utility, rather than constraining actions to a single agent.Here’s an example from the Shepherd project that reacts to GitHub events:JavaKotlin
| **1** | `@EmbabelComponent` contributes actions to the platform, not a specific agent || **2** | `outputBinding` names the result for later actions to reference || **3** | Add entity status to context, making it available to subsequent actions || **4** | Returning `null` prevents further actions from firing for this issue || **5** | SpEL precondition: only fire if new issues were created || **6** | Use AI to assess the issue via a template || **7** | This action only fires if the assessment shows urgency > 0 |
The platform selects which action to run based on:
1.Which preconditions are satisfied (type availability + SpEL conditions)
1.The `cost` and `value` parameters on `@Action` (net value = value - cost)
##### Action Cost and Value
The `@Action` annotation supports `cost` and `value` parameters (both 0.0 to 1.0):JavaKotlin
| **1** | Cost to execute (0.0 to 1.0) - lower is cheaper || **2** | Value when executed (0.0 to 1.0) - higher is more valuable |
The Utility planner calculates *net value* as `value - cost` and selects the action with the highest net value from all available actions.
##### The Nirvana Goal
Utility AI supports a special "Nirvana" goal that is never satisfied.
This keeps the process running, continuously selecting the highest-value available action until no actions are available.
##### Extensibility
Utility AI fosters extensibility.
For example, multiple groups within an organization can contribute their own `@EmbabelComponent` classes with actions that bring their own expertise to enhance behaviours around shared types, while retaining the ability to own and control their own extended model.
##### Utility and States
Utility AI can combine with the `@State` annotation to implement classification and routing patterns.
This is particularly useful when you need to:
- **Classify input** into different categories at runtime
- **Route processing** through category-specific handlers
- **Achieve different goals** based on classificationThe key pattern is:
1.An entry action classifies input and returns a `@State` type
1.Each `@State` class contains an `@AchievesGoal` action that produces the final output
1.The `@AchievesGoal` output is *not* a `@State` type (to prevent infinite loops)Here’s an example of a ticket triage system that routes support tickets based on severity:JavaKotlin
| **1** | Use `PlannerType.UTILITY` for opportunistic action selection || **2** | Sealed interface as the state supertype || **3** | Entry action classifies and returns a `@State` instance || **4** | Each state has an `@AchievesGoal` action producing the final output |
When a `Ticket` is processed:
1.The `triageTicket` action classifies it into one of the state types
1.Entering a state clears other objects from the blackboard
1.The Utility planner selects the `@AchievesGoal` action for that state
1.The goal is achieved when `ResolvedTicket` is producedThis pattern works well when:
- Classification determines the processing path
- Each category has distinct handling requirements
- The final output type is the same across all categories
##### UtilityInvocation: Lightweight Utility Pattern
For simple utility workflows, you don’t need to create an `@Agent` class.
`UtilityInvocation` provides a fluent API to run utility-based workflows directly from `@EmbabelComponent` actions.Example 1. Invoking with UtilityInvocationJavaKotlin
###### Configuration Options
`UtilityInvocation` supports several configuration methods:
| Method | Description || `.withScope(AgentScopeBuilder)` | Defines which actions are available || `.withAgentName(String)` | Sets a custom name for the created agent (defaults to platform name) || `.withProcessOptions(ProcessOptions)` | Configures process-level options || `.terminateWhenStuck()` | Adds early termination policy when no actions are available |
Example 2. Setting a custom agent nameJavaKotlin
#### 4.20.2. Hybrid
The Hybrid planner combines Utility AI’s value-based action picking with goal-satisfaction termination — the "iterate then stop" mode. It exists for reducer-style pipelines where:
- You want to fire many opportunistic, context-producing actions (research lookups, KG queries, enrichment passes) in netValue order — like Utility AI.
- You then want a single synthesising action to run, producing a terminal result.
- You want the process to **stop** the moment that terminal result is on the blackboard, rather than continuing to fire low-value or negative-value leftover actions.
##### Why a Separate Planner
GOAP minimises path cost to the goal; it skips opportunistic actions that aren’t on the cheapest plan, so research never fires. Pure Utility AI is greedy single-step; when paired with a satisfiable terminal goal it gives up at step 1 if no single action reaches the goal. Pure Utility AI paired with `NIRVANA` iterates beautifully but never terminates — it keeps picking actions even after your real goal is satisfied, burning compute on the way to nowhere.Hybrid is the missing middle. It picks the highest-netValue achievable action each tick (so research happens) **and** checks "is the goal already satisfied?" **before** selecting an action (so termination is clean once a real goal is reached).
##### The Two-Goal Pattern
The hybrid planner is designed to be used with **two** goals on the same agent:
1.The **real terminal goal** — what success looks like (e.g. `attention-candidate-produced`, `report-generated`).
1.`com.embabel.agent.core.support.NIRVANA` — the framework’s pre-built unsatisfiable goal, which keeps Utility-style action picking alive while research is still profitable.At each tick the planner generates one plan per goal. NIRVANA returns the highest-netValue achievable action as a 1-step plan. The real goal returns:
- An **empty plan** (`netValue = 0`) if the goal is already satisfied — beats NIRVANA’s then-only-negative-value leftovers.
- A 1-step plan if a single action reaches the goal.
- `null` otherwise.The host process picks the highest-net-value plan across goals. While research is profitable, NIRVANA’s plan wins. The moment the real goal is satisfied, its empty plan wins and `plan.isComplete()` fires, terminating the process.
##### When to Use Hybrid

- **Per-signal triage pipelines** that gather multiple context sources before a final assessment LLM call.
- **Research-then-synthesise** workflows where multiple actions contribute typed artifacts to a blackboard, and one final action consumes everything.
- **Pack-extensible reducers** where new opportunistic actions can be added without rewriting the synthesis step — their value alone gets them scheduled.Use Utility AI without Hybrid when you genuinely want a long-running event loop with no terminal goal (chat surfaces, exploratory triage).Use GOAP when you have strict typed-dependency ordering and don’t need opportunistic research.
##### Tuning Values
For the hybrid pattern to work as intended:
- **Research actions** (the opportunistic context producers) should have **high net value** so they win the picker race while their `canRerun=false` slot is still open.
- **The synthesiser** (the final action that produces the terminal output) should have **positive but lower net value**. It wins after research is locked out.
- **Wrap-up actions** (those that lift a verdict into the goal output) should have **value just above the synthesiser** so they fire immediately after.Once the wrap-up writes the terminal artifact, the real goal’s empty plan wins and the process exits — regardless of any remaining negative-value actions.
##### Using Hybrid
Kotlin
| **1** | `NIRVANA` keeps the iterate-by-netValue picking alive across multiple ticks; without it, the planner gives up at step 1 if no single action reaches the real goal. || **2** | The real terminal goal — what makes the process stop. Returns an empty plan once satisfied. || **3** | Per-process opt-in via `ProcessOptions`. The default (`GOAP`) is unchanged for other processes. |

##### Difference from `UTILITY`

| Scenario | `UTILITY` | `HYBRID` || Real goal satisfied, no actions achievable | Empty plan (terminate) | Empty plan (terminate) || Real goal satisfied, other actions still achievable | Picks highest-netValue action and runs it | Empty plan (terminate) — **the load-bearing fix** || Real goal not satisfied, action reaches it in 1 step | 1-step plan | 1-step plan || Real goal not satisfied, no 1-step path | `null` (planner gives up) | `null` (planner gives up — paired NIRVANA handles iteration) || `NIRVANA` goal | Highest-netValue achievable action; `null` when nothing’s available | Identical to `UTILITY` semantics |
`HYBRID` is `UTILITY` with one extra check: **if the real goal is already satisfied, return an empty plan regardless of what actions remain achievable.** This is what enables clean termination of the two-goal pattern.
#### 4.20.3. Supervisor
The Supervisor planner uses an LLM to orchestrate actions dynamically.
This is a popular pattern in frameworks like LangGraph and Google ADK, where a supervisor LLM decides which tools to call and in what order.
| | Unlike GOAP and Utility, the Supervisor planner is **non-deterministic**.
The LLM may choose different action sequences for the same inputs.
This makes it less suitable for business-critical workflows requiring reproducibility. |

##### Type-Informed vs Type-Driven
A key design decision in supervisor architectures is how types relate to composition:
| Approach | Description || **Type-Driven** (GOAP) | Types *constrain* composition. An action requiring `MarketData` can only run after an action produces `MarketData`. This is deterministic but rigid. || **Type-Informed** (Supervisor) | Types *inform* composition. The LLM sees type schemas and decides what to call based on semantic understanding. This is flexible but non-deterministic. |
Embabel’s Supervisor planner takes the **type-informed** approach while maximizing the benefits of types:
- Actions return **typed outputs** that are validated
- The LLM sees **type schemas** to understand what each action produces
- Results are stored on the **typed blackboard** for later actions
- The same actions work with **any planner** (GOAP, Utility, or Supervisor)This is a "typed supervisor" pattern—​a middle ground between fully type-driven (GOAP) and untyped string-passing (typical LangGraph).
##### When to Use Supervisor
Supervisor is appropriate when:
- Action ordering is **context-dependent** and hard to predefine
- You want an LLM to **synthesize information** across multiple sources
- The workflow benefits from **flexible composition** rather than strict sequencing
- Non-determinism is acceptable for your use caseSupervisor is **not** recommended when:
- You need **reproducible**, auditable execution paths
- Actions have strict **dependency ordering** that must be enforced
- Latency and cost matter (each decision requires an LLM call)
##### Using Supervisor
To use Supervisor, annotate your agent with `planner = PlannerType.SUPERVISOR` and mark one action with `@AchievesGoal`:JavaKotlin
| **1** | Tool actions have descriptions visible to the supervisor LLM || **2** | The goal action is called when the supervisor has gathered enough information |
The supervisor LLM sees type schemas for available actions:The LLM decides action ordering based on this information, making informed decisions without being constrained by declared dependencies.
##### Interoperability
Using wrapper request types (like `MarketDataRequest`) enables actions to work with **any planner**:
- **GOAP**: Request types flow through the blackboard based on preconditions/effects
- **Utility**: Actions fire when their request types are available with highest net value
- **Supervisor**: The LLM constructs request objects to call actionsThis means you can switch planners without changing your action code—​useful for testing with deterministic planners (GOAP) and deploying with flexible planners (Supervisor).
##### Comparison with LangGraph
LangGraph’s supervisor pattern is a popular approach for multi-agent orchestration.
Here’s how a similar workflow looks in LangGraph vs Embabel:LangGraph (Python)
| **1** | Tools return strings—​the LLM must parse and interpret results || **2** | All tools always visible—​no filtering based on context || **3** | State is untyped message history |
Example 3. EmbabelJavaKotlin
| **1** | Tools return typed, validated objects--`MarketData`, `CompetitorAnalysis` || **2** | Blackboard holds typed artifacts, not just message strings || **3** | Tools with satisfied inputs are prioritized via currying |

##### Key Advantages
Embabel’s Supervisor offers several advantages over typical supervisor implementations:
| Aspect | Typical Supervisor (LangGraph) | Embabel Supervisor || **Output Types** | Strings—​LLM must parse | Typed objects—​validated and structured || **Tool Visibility** | All tools always available | Tools filtered by blackboard state (currying) || **Domain Awareness** | None—​tools are opaque functions | Type schemas visible to LLM || **Determinism** | Fully non-deterministic | Semi-deterministic: tool availability constrained by types || **State** | Untyped message history | Typed blackboard with named artifacts |

###### Blackboard-Driven Tool Filtering
A key differentiator is **curried tool filtering**.
When an action’s inputs are already on the blackboard, those parameters are "curried out"--the tool signature simplifies.
| | What is Currying?Currying is a functional programming technique where a function with multiple parameters is transformed into a sequence of functions, each taking a single parameter.In Embabel’s context: if an action requires `(MarketDataRequest, Ai)` and `MarketDataRequest` is already on the blackboard, we "curry out" that parameter—​the tool exposed to the LLM only needs to provide any remaining parameters.
This simplifies the LLM’s task and signals which tools are "ready" to run. |
This reduces the LLM’s decision space and guides it toward logical next steps—​tools with satisfied inputs appear "ready" with fewer parameters.
This is more deterministic than showing all tools equally, while remaining more flexible than GOAP’s strict ordering.
###### Semi-Determinism
While still LLM-orchestrated, Embabel’s Supervisor is **more deterministic** than typical implementations:
1.**Type constraints**: Actions can only produce specific types—​no arbitrary string outputs
1.**Input filtering**: Tools unavailable until their input types exist
1.**Schema guidance**: LLM sees what each action produces, not just descriptions
1.**Validated outputs**: Results must conform to declared typesThis makes debugging easier and behaviour more predictable, while retaining the flexibility that makes supervisor patterns valuable.
###### When Embabel’s Approach Excels

- **Domain-rich workflows**: When your domain has clear types (reports, analyses, forecasts), schemas help the LLM understand relationships
- **Multi-step synthesis**: When actions build on each other’s outputs, typed blackboard tracks progress clearly
- **Hybrid determinism**: When you want more predictability than pure LLM orchestration but more flexibility than GOAP
##### SupervisorInvocation: Lightweight Supervisor Pattern
For simple supervisor workflows, you don’t need to create an `@Agent` class.
`SupervisorInvocation` provides a fluent API to run supervisor-orchestrated workflows directly from `@EmbabelComponent` actions.This is ideal when:
- You have a small set of related actions in an `@EmbabelComponent`
- You want LLM-orchestrated composition without creating a full agent
- You’re prototyping or exploring supervisor patterns before committing to a full agent design
###### Example: Meal Preparation Workflow
Here’s a complete example from the embabel-agent-examples repository:Example 4. Stages - Actions as @EmbabelComponentJavaKotlinExample 5. Invoking with SupervisorInvocationJavaKotlin
###### Configuration Options
`SupervisorInvocation` supports several configuration methods:
| Method | Description || `.returning(Class)` | Specifies the goal type to produce || `.withScope(AgentScopeBuilder)` | Defines which actions are available || `.withAgentName(String)` | Sets a custom name for the created agent (defaults to `{platformName}.supervisor`) || `.withGoalDescription(String)` | Provides a custom description for the goal || `.withProcessOptions(ProcessOptions)` | Configures process-level options |
Example 6. Setting a custom agent nameJavaKotlinThe supervisor LLM sees:
1.**Available actions** with their type signatures and schemas
1.**Current artifacts** on the blackboard (including `UserInput` content)
1.**Goal** to produce a `Meal`It then orchestrates the actions—calling `chooseCook` and `takeOrder` (possibly in parallel), then `prepareMeal` when the dependencies are satisfied.
###### Key Design Points

1.**Actions use UserInput explicitly**: Each action receives `UserInput` and includes it in the LLM prompt, ensuring the actual user request is used.
1.**@AchievesGoal marks the target**: The `prepareMeal` action is marked with `@AchievesGoal` to indicate it produces the final output.
1.**Type-driven dependencies**: `prepareMeal` requires `Cook` and `Order`, which guides the supervisor’s orchestration.
###### SupervisorInvocation vs @Agent with planner = SUPERVISOR

| Aspect | SupervisorInvocation | @Agent(planner = SUPERVISOR) || **Declaration** | Fluent API, no class annotation | Annotated agent class || **Action source** | `@EmbabelComponent` or multiple components | Single `@Agent` class || **Best for** | Quick prototypes, simple workflows | Formalized, reusable agents || **Goal specification** | `.returning(Class)` fluent method | `@AchievesGoal` on action || **Scope** | Explicit via `AgentScopeBuilder` | Implicit from agent class |

###### Comparison with AgenticTool
Both `SupervisorInvocation` and AgenticTool provide LLM-orchestrated composition, but at different levels:
| Aspect | AgenticTool | SupervisorInvocation || **Level** | Tool (can be used within actions) | Invocation (runs a complete workflow) || **Sub-components** | Other `Tool` instances | `@Action` methods from `@EmbabelComponent` || **Output** | `Tool.Result` (text, artifact, or error) | Typed goal object (e.g., `Meal`) || **State management** | Minimal (LLM conversation only) | Full blackboard with typed artifacts || **Type awareness** | Tools have names and descriptions | Actions have typed inputs/outputs with schemas || **Currying** | None | Inputs on blackboard are curried out || **Use case** | Mini-orchestration within an action | Complete multi-step workflow with typed results |
Use `AgenticTool` when you need a tool that internally orchestrates other tools.
Use `SupervisorInvocation` when you need a complete workflow that produces a typed result with full blackboard state management.