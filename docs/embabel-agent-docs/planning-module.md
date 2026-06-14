Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 10.1. Abstract
Lower level module for planning and scheduling. Used by Embabel Agent Platform.

### 10.2. A* GOAP Planner Algorithm Overview

```
The A* GOAP (Goal-Oriented Action Planning) Planner is an implementation of the A* search
algorithm specifically designed for planning sequences of actions to achieve specified goals.
The algorithm efficiently finds the optimal path from an initial world state to a goal state by
exploring potential action sequences and minimizing overall cost.
```

#### 10.2.1. Core Algorithm Components

```
The A* GOAP Planner consists of several key components:
```

1.**A** Search*: Finds optimal action sequences by exploring the state space
1.**Forward Planning**: Simulates actions from the start state toward goals
1.**Backward Planning**: Optimizes plans by working backward from goals
1.**Plan Simulation**: Verifies that plans achieve intended goals
1.**Pruning**: Removes irrelevant actions to create efficient plans
1.**Unknown Condition Handling**: Manages incomplete world state information
#### 10.2.2. A* Search Algorithm

```
The A* search algorithm operates by maintaining:
```

- **Open List**: A priority queue of states to explore, ordered by f-score
- **Closed Set**: States already fully explored
- **g-score**: Cost accumulated so far to reach a state
- **h-score**: Heuristic estimate of remaining cost to goal
- **f-score**: Total estimated cost (g-score + h-score)
#### 10.2.3. Process Flow

1.**Initialization**:
- Begin with the start state in the open list
- Set its g-score to 0 and calculate its h-score
1.**Main Loop**:
- While the Open List is not empty:
- Select the state with the lowest f-score from the open list
- If this state satisfies the goal, construct and return the plan
- Otherwise, mark the state as processed (add to closed set)
- For each applicable action, generate the next state and add to open list if it better
than existing paths
1.**Path Reconstruction**:
When a goal state is found, reconstruct the path by following predecessors
- Create a plan consisting of the sequence of actions
```
_Reference: link:goap/AStarGoapPlanner.kt[AStarGoapPlanner]:planToGoalFrom:_
```
#### 10.2.4. Forward and Backward Planning Optimization

```
The planner implements a two-pass optimization strategy to eliminate unnecessary actions:
```

##### Backward Planning Optimization

```
This pass works backward from the goal conditions to identify only actions that contribute to
achieving the goal
```

```
_Reference: link:goap/AStarGoapPlanner.kt[AStarGoapPlanner]:_backwardPlanningOptimization___
```

##### Forward Planning Optimization

```
This pass simulates the plan from the start state and removes actions that don't make progress
toward the goal:
```

```
_Reference: link:goap/AStarGoapPlanner.kt[AStarGoapPlanner]:_forwardPlanningOptimization___
```

##### Plan Simulation

```
Plan simulation executes actions in sequence to verify the plan's correctness:
```

```
_Reference: function simulatePlan(startState, actions)_
```

#### 10.2.5. Pruning Planning Systems

```
The planner can prune entire planning systems to remove irrelevant actions:
```
JavaKotlin
##### Heuristic Function

```
The heuristic function estimates the cost to reach the goal from a given state:
```

#### 10.2.6. Complete Planning Process

1.Initialize with start state, actions, and goal conditions
1.Run A* search to find an initial action sequence
1.Apply backward planning optimization to eliminate unnecessary actions
1.Apply forward planning optimization to further refine the plan
1.Verify the plan through simulation
1.Return the optimized action sequence or null if no valid plan exists

### 10.3. Agent Pruning Process

```
When pruning an agent for specific goals:
```

1.Identify all known conditions in the planning system
1.Set initial state based on input conditions
1.Find all possible plans to each goal
1.Keep only actions that appear in at least one plan
1.Create a new agent with the pruned action set
```
This comprehensive approach ensures agents contain only the actions necessary to achieve their
designated goals, improving efficiency and preventing action leakage between different agents.
```
#### 10.3.1. Progress Determination Logic in A* GOAP Planning

```
The progress determination logic in method *forwardPlanningOptimization* is a critical part of
the forward planning optimization in the A* GOAP algorithm. This logic ensures that only actions
that meaningfully progress the state toward the goal are included in the final plan.
```

##### Progress Determination Expression
JavaKotlin
##### Detailed Explanation

```
The expression evaluates to true only when an action makes meaningful progress toward achieving
the goal state. Let's break down each component:
```

1.`nextState != currentState`
- Verifies that the action actually changes the world state
- Prevents including actions that have no effect
1.`action.effects.any { …​ }`
- Examines each effect the action produces
- Returns true if ANY effect satisfies the inner condition
1.`goal.preconditions.containsKey(key)`
- Ensures we only consider effects that relate to conditions required by the goal
- Ignores effects that modify conditions irrelevant to our goal
1.`currentState[key] != goal.preconditions[key]`
- Checks that the current condition value differs from what the goal requires
- Only counts progress if we’re changing a condition that needs changing
1.`(value == goal.preconditions[key] || key not in nextState)`
- This checks one of two possible ways an action can make progress:
- `value == goal.preconditions[key]`
- The action changes the condition to exactly match what the goal requires
- Direct progress toward goal achievement
- `key not in nextState`
- The action removes the condition from the state entirely
- This is considered progress if the condition was previously in an incorrect state
- Allows for actions that clear obstacles or reset conditions