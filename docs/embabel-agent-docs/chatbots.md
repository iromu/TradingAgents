Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 1.4. Core Concepts
Agent frameworks break up tasks into separate smaller interactions, making LLM use more predictable and focused.Embabel models agentic flows in terms of:
- **Actions**: Steps an agent takes.
These are the building blocks of agent behavior.
- **Goals**: What an agent is trying to achieve.
- **Conditions**: Conditions to do evaluations while planning.
Conditions are reassessed after each action is executed.
- **Domain Model**: Objects underpinning the flow and informing Actions, Goals and Conditions.This enables Embabel to create a **plan**: A sequence of actions to achieve a goal.
Plans are dynamically formulated by the system, not the programmer.
The system replans after the completion of each action, allowing it to adapt to new information as well as observe the effects of the previous action.
This is effectively an OODA loop.
| | Application developers don’t usually have to deal with conditions and planning directly, as most conditions result from data flow defined in code, allowing the system to infer pre and post conditions to (re-)evaluate the plan. |

#### 1.4.1. Complete Example
Let’s look at a complete example that demonstrates how Embabel infers conditions from input/output types and manages data flow between actions.
This example comes from the Embabel Agent Examples repository:JavaKotlin
| **1** | **Agent Declaration**: The `@Agent` annotation defines this as an agent capable of a multi-step flow. || **2** | **Spring Integration**: Regular Spring dependency injection - the agent uses both LLM services and traditional business services. || **3** | **Service Injection**: `HoroscopeService` is injected like any Spring bean - agents can mix AI and non-AI operations seamlessly. || **4** | **Action Definition**: `@Action` marks methods as steps the agent can take.
Each action represents a capability. || **5** | **Input Condition Inference**: The method signature `extractStarPerson(UserInput userInput, …​)` tells Embabel:

- **Precondition**: "A UserInput object must be available"
- **Required Data**: The agent needs user input to proceed
- **Capability**: This action can extract structured data from unstructured input || **6** | **Output Condition Creation**: Returning `StarPerson` creates:

- **Postcondition**: "A StarPerson object is now available in the world state"
- **Data Availability**: This output becomes input for subsequent actions
- **Type Safety**: The domain model enforces structure || **7** | **Non-LLM Action**: Not all actions use LLMs - this demonstrates hybrid AI/traditional programming. || **8** | **Data Flow Chain**: The method signature `retrieveHoroscope(StarPerson starPerson)` creates:

- **Precondition**: "A StarPerson object must exist" (from previous action)
- **Dependency**: This action can only execute after `extractStarPerson` completes
- **Service Integration**: Uses the injected `horoscopeService` rather than an LLM || **9** | **Regular Service Call**: This action calls a traditional Spring service - demonstrating how agents blend AI and conventional operations. || **10** | **Another Action**: This action uses tools specified at the `PromptRunner` level. || **11** | **Multi-Input Dependencies**: This method requires both `StarPerson` and `Horoscope` - showing complex data flow orchestration. || **12** | **Tool-Enabled LLM**: `withToolGroup(CoreToolGroups.WEB)` adds web search tools to this LLM call, allowing it to search for current news stories. || **13** | **Goal Achievement**: `@AchievesGoal` marks this as a terminal action that completes the agent’s objective. || **14** | **Complex Input Requirements**: The final action requires three different data types, showing sophisticated orchestration. || **15** | **Creative Configuration**: High temperature (0.9) optimizes for creative, entertaining output - appropriate for amusing writeups. || **16** | **Structured Prompt with Data**: The prompt includes both the horoscope summary and formatted news stories using XML-style tags.
This ensures the LLM has all the context it needs from earlier actions. || **17** | **Final Output**: Returns `Writeup`, completing the agent’s goal with personalized content. |
State is managed by the framework, through the process blackboard.
#### 1.4.2. The Inferred Execution Plan for the Example
Based on the type signatures alone, Embabel automatically infers this execution plan for the example agent above:**Goal**: Produce a `Writeup` (final return type of `@AchievesGoal` action)The initial plan:
- To emit `Writeup` → need `writeup()` action
- `writeup()` requires `StarPerson`, `RelevantNewsStories`, and `Horoscope`
- To get `StarPerson` → need `extractStarPerson()` action
- To get `Horoscope` → need `retrieveHoroscope()` action (requires `StarPerson`)
- To get `RelevantNewsStories` → need `findNewsStories()` action (requires `StarPerson` and `Horoscope`)
- `extractStarPerson()` requires `UserInput` → must be provided by userExecution sequence:`UserInput` → `extractStarPerson()` → `StarPerson` → `retrieveHoroscope()` → `Horoscope` → `findNewsStories()` → `RelevantNewsStories` → `writeup()` → `Writeup` and achieves goal.
#### 1.4.3. Key Benefits of Type-Driven Flow
**Automatic Orchestration**: No manual workflow definition needed - the agent figures out the sequence from type dependencies.
This is particularly beneficial if things go wrong, as the planner can re-evaluate the situation and may be able to find an alternative path to the goal.**Dynamic Replanning**: After each action, the agent reassesses what’s possible based on available data objects.**Type Safety**: Compile-time guarantees that data flows correctly between actions.
No magic string keys.**Flexible Execution**: If multiple actions could produce the required input type, the agent chooses based on context and efficiency.
(Actions can have cost and value.)This demonstrates how Embabel transforms simple method signatures into sophisticated multi-step agent behavior, with the complex orchestration handled automatically by the framework.
## 2. Getting Started

### 4.13. Building Chatbots
Chatbots are an important application of Gen AI, although far from the only use, especially in enterprise.Unlike many other frameworks, Embabel does not maintain a conversation thread to do its core work.
This is a good thing as it means that context compression is not required for most tasks.If you want to build a chatbot you should use the `Conversation` interface explicitly, and expose a `Chatbot` bean, typically backed by action methods that handle `UserMessage` events.
#### 4.13.1. Core Concepts

##### Long-Lived AgentProcess
An Embabel chatbot is backed by a **long-lived `AgentProcess`** that pauses between user messages.
This design has important implications:
- The same `AgentProcess` can respond to events besides user input
- The blackboard maintains state across the entire session
- Actions can be triggered by user messages, system events, or other objects added to the blackboard
- It’s a **working context** rather than just a chat sessionWhen a user sends a message, it’s added to the blackboard as a `UserMessage`.
The `AgentProcess` then runs, selects an appropriate action to handle it, and pauses again waiting for the next event.
##### Utility AI for Chatbots
**Utility AI is often the best approach for chatbots.** Instead of defining a fixed flow, you define multiple actions with costs, and the planner selects the highest-value action to respond to each message.This allows:
- Multiple response strategies (e.g., RAG search, direct answer, clarification request)
- Dynamic behavior based on context
- Easy extensibility by adding new action methods
##### Goals in Chatbots
Typically, chatbot agents **do not need a goal**.
The agent process simply waits for user messages and responds to them indefinitely.However, you can define a goal if you want to ensure the conversation terminates and the `AgentProcess` completes rather than waiting forever.
This is useful for:
- Transactional conversations (e.g., completing a booking)
- Wizard-style flows with a defined endpoint
- Conversations that should end after collecting specific information
#### 4.13.2. Key Interfaces

##### Chatbot
The `Chatbot` interface manages multiple chat sessions:JavaKotlin
##### Context IDs and Session State
The `contextId` parameter allows you to **pre-populate the session’s blackboard** with objects from a named context.
This is useful when:
- **Users have multiple contexts** - A user might have different projects, accounts, or workspaces.
Each context can maintain its own state that persists across sessions.
- **Resuming prior state** - When a user returns, you can restore their previous session state
(e.g., user preferences, in-progress work, conversation history from a previous session).
- **Pre-loading domain objects** - You can populate the blackboard with objects that should always be present,
such as the current user’s profile, active subscription, or relevant configuration.JavaKotlinThe context mechanism works with `AgentPlatform’s context storage:
1.When `createSession` is called with a `contextId`, the platform looks up any saved objects for that context
1.Those objects are added to the new session’s blackboard
1.As the session runs, changes to the blackboard can be persisted back to the context
1.The next time a session is created with that `contextId`, the updated state is restoredThis enables **stateful conversations across sessions** without requiring the chatbot to manually track and restore state.
##### ChatSession
Each session represents an ongoing conversation:JavaKotlin
##### Conversation
The `Conversation` interface holds the message history and tracks assets:JavaKotlinMessage types include:
- `UserMessage` - messages from the user (supports multimodal content)
- `AssistantMessage` - responses from the chatbot (can include assets)
- `SystemMessage` - system-level instructions
#### 4.13.3. Asset Tracking
Chatbots can track **assets**—structured outputs like generated documents, search results, or user-created content—at two levels:
##### Conversation-Level Assets
The `Conversation` has an `AssetTracker` for explicitly tracking assets throughout the session:JavaKotlinUse conversation-level tracking when:
- Assets are created by tools or external processes
- Assets should persist across multiple messages
- You want explicit control over what’s tracked
##### Message-Level Assets
`AssistantMessage` implements `AssetView` and can include assets directly:JavaKotlinUse message-level assets when:
- Assets are directly tied to a specific response
- You want assets to appear alongside the message in the UI
- The asset represents output from that specific interaction
##### Combined Asset View
The `Conversation.assets` property provides a **merged view** of all assets:JavaKotlinThe merge follows these rules:
1.**Tracker assets appear first** (explicit tracking takes priority)
1.**Message assets follow** in chronological order
1.**Duplicates are removed** by ID (tracker version wins)This allows flexible asset management:JavaKotlin
##### Using Assets as Tools
Assets can be exposed to the LLM as tools via their `LlmReference`:JavaKotlinThis enables scenarios like:
- Editing previously generated content
- Combining multiple assets
- Querying structured data from earlier in the conversation
#### 4.13.4. Building a Chatbot

##### Step 1: Create Action Methods
Define action methods in an `@EmbabelComponent` that respond to user messages using the `trigger` parameter:JavaKotlin
| **1** | `trigger = UserMessage.class` - action is invoked when a `UserMessage` is the last object added to the blackboard || **2** | `canRerun = true` - action can be executed multiple times (for each user message) || **3** | `Conversation` parameter is automatically injected from the blackboard || **4** | `context.sendMessage()` sends the response to the output channel |

##### Step 2: Configure the Chatbot Bean
Use `AgentProcessChatbot.utilityFromPlatform()` to create a utility-based chatbot that discovers all available actions:JavaKotlin
| **1** | Creates a chatbot using Utility AI planning to select the best action || **2** | Discovers all `@Action` methods from `@EmbabelComponent` classes on the platform |
For debugging, you can pass a custom `Verbosity` configuration:JavaKotlin
| **1** | Conversation factory (required when specifying verbosity) || **2** | `Verbosity` configuration for debugging prompts |

| | Be sure that the `AgentPlatform` has loaded all its actions before creating a new session on your `AgentProcessChatbot`.
Otherwise the actions needed to respond to chat may not be available in the session. |

#### 4.13.5. Conversation Storage
By default, chatbots use **in-memory conversations** that are lost when the session ends.
For production applications, you typically want to **persist conversations** to a backing store.
##### Storage Types
Embabel supports two conversation storage types via `ConversationStoreType`:
| Type | Description || `IN_MEMORY` | Conversations stored in memory only. Fast and simple, suitable for testing and ephemeral sessions. || `STORED` | Conversations persisted to a backing store (e.g., Neo4j). Requires `embabel-chat-store` dependency. |

##### Configuring Persistent Storage
To use persistent conversations, inject `ConversationFactoryProvider` and pass the appropriate factory when creating the chatbot:JavaKotlin
| **1** | Inject the `ConversationFactoryProvider` via Spring DI || **2** | Get the factory for the desired storage type || **3** | Pass the factory to the chatbot - storage is configured once at creation time |

| | Storage type is configured once when creating the chatbot, not per-call.
This ensures consistent behavior across all sessions. |

##### Adding embabel-chat-store
To enable persistent storage, add the `embabel-chat-store` dependency:This provides:
- `StoredConversationFactory` - creates conversations that persist to Neo4j
- `StoredConversation` - conversation implementation with async persistence
- Message lifecycle events (`MessageEvent`) for UI updates
- Title generation for conversation sessions
##### Restoring Conversations
To restore a conversation, pass the `conversationId` when creating a session:JavaKotlin
| **1** | If the conversation exists in storage, it will be loaded automatically. If not, a new conversation is created with this ID. |
This allows applications to:
- Resume conversations across server restarts
- Display conversation history to returning users
- Continue multi-turn interactions from where they left off
| | For lower-level access, you can also use `ConversationFactory.load(conversationId)` directly to check if a conversation exists before creating a session. |

##### Step 3: Use the Chatbot
Interact with the chatbot through its session interface:JavaKotlin
| **1** | Create a new session with fresh blackboard and auto-generated conversation ID || **2** | Load prior blackboard state from the "user-workspace-123" context || **3** | Restore an existing conversation with its message history || **4** | Both: load context state AND restore conversation history || **5** | Send a user message - triggers the agent to select and run an action |

#### 4.13.6. How Message Triggering Works
When you specify `trigger = UserMessage.class` on an action:
1.The chatbot adds the `UserMessage` to both the `Conversation` and the `AgentProcess` blackboard
1.The planner evaluates all actions whose trigger conditions are satisfied
1.For utility planning, the action with the highest value (lowest cost) is selected
1.The action method receives the `Conversation` (with the new message) via parameter injectionThis trigger-based approach means:
- You can have multiple actions that respond to user messages with different costs
- The planner picks the most appropriate response strategy
- Actions can also be triggered by other event types (not just `UserMessage`)
#### 4.13.7. Dynamic Cost Methods
For more sophisticated action selection, use `@Cost` methods:JavaKotlin
| **1** | `@Cost` marks this as a cost calculation method || **2** | Receives the `Blackboard` to inspect current state || **3** | Returns cost value - lower costs mean higher priority || **4** | `costMethod` links the action to the cost calculation method |

#### 4.13.8. Prompt Templates
Chatbots typically use **Jinja prompt templates** rather than inline string prompts.
This isn’t strictly necessary—simple chatbots can use regular string prompts built in code:JavaKotlin
| **1** | Simple inline prompt - fine for basic chatbots |
However, production chatbots often need **longer, more complex prompts** for:
- Personality and tone (personas)
- Guardrails and safety instructions
- Domain-specific objectives
- Dynamic behavior based on configurationFor these cases, Jinja templates are the better choice:JavaKotlin
| **1** | Loads `prompts/ragbot.jinja` from resources || **2** | Template bindings - accessible in Jinja as `properties.persona()` etc. |
Templates allow:
- Separation of prompt engineering from code
- Dynamic persona and objective selection via configuration
- Reusable prompt elements (guardrails, personalization)
- Prompt iteration without code changes
##### Resilient Responses with `respond`
In a chatbot, it’s critical never to leave the user without a reply.
The `respond` method on `Rendering` wraps `respondWithSystemPrompt` with error handling, so that an LLM or infrastructure failure still returns an `AssistantMessage` to the user rather than propagating an exception:JavaKotlin
##### Template Structure Example
A typical chatbot template structure from the rag-demo project:The main template (`ragbot.jinja`) composes from reusable elements:
| **1** | Include safety guardrails first || **2** | Then include persona and objective (which are dynamically selected) |
Guardrails define safety boundaries (`elements/guardrails.jinja`):Personalization dynamically loads persona and objective (`elements/personalization.jinja`):
| **1** | Build template path from `properties.persona()` (e.g., "clause" → "personas/clause.jinja") || **2** | Build template path from `properties.objective()` (e.g., "legal" → "objectives/legal.jinja") |
A persona template (`personas/clause.jinja`):An objective template (`objectives/legal.jinja`):
| **1** | Instructs the LLM to use RAG tools provided via `withReference()` |
This modular approach lets you:
- Switch personas via `application.yml` without code changes
- Share guardrails across multiple chatbot configurations
- Test different objectives independently
#### 4.13.9. Advanced: State Management with @State
For complex chatbots that need to track state across messages, use `@State` classes.
State classes are automatically managed by the agent framework:
- State objects are persisted in the blackboard
- Actions can depend on specific state being present
- State transitions drive the conversation flowCross-reference the @State annotation documentation for details on:
- Defining state classes
- State-dependent actions
- Nested state machines
#### 4.13.10. Complete Example
See the rag-demo project for a complete chatbot implementation including:
- `ChatActions.java` - Action methods responding to user messages
- `ChatConfiguration.java` - Chatbot bean configuration
- `RagbotShell.java` - Spring Shell integration for interactive testing
- Jinja templates for persona-driven prompts
- RAG integration for document-grounded responsesTo run the example: