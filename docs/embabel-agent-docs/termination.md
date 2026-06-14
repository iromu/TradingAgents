Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.31. Agent and Action Termination
Embabel provides mechanisms to terminate agents and actions early, either gracefully (at the next
 checkpoint) or immediately.
 This is useful for scenarios like user cancellation, timeout handling, resource limits, or ctitical error
 handling.
#### 4.31.1. Choosing Between Signal and Exception

| Mechanism | When to Use | Behavior || **Graceful (Signal)** | "Let me finish my work, then stop" - side effects need to complete | Terminates at next checkpoint; current operation completes normally || **Immediate (Exception)** | "Stop now, nothing left to do" - no further processing needed | Terminates immediately; nothing executes after the exception being thrown |

#### 4.31.2. Agent Termination
Agent termination stops the entire agent process. The process status becomes `TERMINATED`.
##### Graceful Agent Termination (Signal)
Use `terminateAgent()` when the current operation should complete before stopping.
 The agent terminates before the next checkpoint tick.Kotlin
##### Immediate Agent Termination (Exception)
Use `TerminateAgentException` when the agent must stop immediately.
No further tool calls or actions execute.KotlinJava
#### 4.31.3. Action Termination
Action termination stops only the current action. The agent continues with the next planned action.
 This is useful for skipping problematic steps while allowing the overall goal to proceed.
| | For action termination to allow retry, the action must be defined with `canRerun =
 true`. |

##### Graceful Action Termination (Signal)
Use `terminateAction()` when the current tool call should complete before stopping the action.
 The action terminates between tool calls.
| | Graceful action termination only works for LLM-based actions that use a tool loop.
 For simple transformation actions, use `TerminateActionException` instead. |
KotlinJava
##### Immediate Action Termination (Exception)
Use `TerminateActionException` when the action must stop immediately.
Remaining tool calls in the current batch are skipped.KotlinJava
#### 4.31.4. Catching Both Exception Types
Both `TerminateAgentException` and `TerminateActionException` extend `TerminationException`,
allowing you to catch them together:
#### 4.31.5. Summary

| Scope | Mechanism | Method/Exception | Use Case || Agent | Graceful | `processContext.terminateAgent(reason)` | "Finish current work, then stop agent" || Agent | Immediate | `throw TerminateAgentException(reason)` | "Stop now - critical error, no recovery" || Action | Graceful | `processContext.terminateAction(reason)` | "Finish current tool, then stop action" || Action | Immediate | `throw TerminateActionException(reason)` | "Stop now - try different approach" |