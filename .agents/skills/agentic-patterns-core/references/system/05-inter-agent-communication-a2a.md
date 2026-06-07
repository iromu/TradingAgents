# Inter-Agent Communication (A2A) Pattern

**Source:
** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT
License

## Pattern

```
Agent → Message Broker → [Agent N] → Response → Orchestrator → Output
```

Enable agents to communicate through structured messages via a broker or
direct channels, supporting modular, composable agent architectures where
each agent has clear responsibilities.

## When to Use

- Complex workflows requiring multiple specialized agents
- Modular systems building composable agent architectures
- Distributed processing where agents run in different locations
- Scalable architectures needing to grow
- Collaborative tasks where agents work together on problems
- Service-oriented design treating agents as microservices

## Where It Fits

- Enterprise automation: Coordinating business process agents
- Research systems: Agents collaborating on analysis
- Content production: Pipeline of content creation agents
- Trading systems: Agents coordinating financial decisions
- Smart city systems: IoT and service agents communicating

## Pros

- **Modularity** — Clear separation of agent responsibilities
- **Scalability** — Easy to add new agents to the system
- **Flexibility** — Different communication patterns available
- **Fault isolation** — Agent failures don't crash system
- **Reusability** — Agents can be reused in different workflows
- **Debugging support** — Message tracing aids troubleshooting
- **Parallel processing** — Agents can work simultaneously

## Cons

- **Complexity overhead** — Communication protocols add complexity
- **Latency accumulation** — Message passing adds delays
- **Coordination challenges** — Managing agent interactions
- **Debugging difficulty** — Tracing distributed conversations
- **State management** — Maintaining consistency across agents
- **Network dependencies** — Vulnerable to communication failures
- **Security concerns** — Inter-agent authentication needed

## Implementation

```
# Example: Agent-to-Agent message passing
class MessageBroker:
    def __init__(self):
        self.queues = {}
        self.handlers = {}

    def register(self, agent_id, handler):
        self.handlers[agent_id] = handler

    def send(self, from_agent, to_agent, message):
        msg = {
            "from": from_agent,
            "to": to_agent,
            "type": message["type"],
            "payload": message["payload"],
            "timestamp": now(),
        }
        self.queues.setdefault(to_agent, []).append(msg)
        self._deliver(to_agent)

    def _deliver(self, agent_id):
        for msg in self.queues.pop(agent_id, []):
            result = self.handlers[agent_id](msg)
            if result and msg["to"] != result.get("to"):
                self.send(agent_id, result["to"], result)

class Agent:
    def __init__(self, agent_id, broker):
        self.id = agent_id
        self.broker = broker
        self.broker.register(agent_id, self.handle_message)

    def handle_message(self, msg):
        if msg["type"] == "check_stock":
            return {"to": msg["from"], "type": "stock_response", "payload": self.check_stock(msg["payload"])}
        elif msg["type"] == "process_order":
            return {"to": msg["from"], "type": "order_response", "payload": self.process(msg["payload"])}
```

## Real-World Examples

1. **E-commerce Order**: Inventory checks stock → Pricing calculates costs → Payment processes → Shipping arranges →
   Notification updates → Orchestrator coordinates
2. **News Production**: Crawler gathers → Fact-check verifies → Writer creates → Editor reviews → Publisher posts →
   Analytics tracks
3. **Financial Analysis**: Data collects → Technical charts → Fundamental analyzes → Risk assesses → Report generates →
   Compliance ensures
4. **Smart Manufacturing**: Sensors monitor → Quality checks → Maintenance schedules → Inventory manages → Planning
   optimizes → Control coordinates
5. **Healthcare**: Triage assesses → Diagnostic suggests → Specialists provide → Treatment recommends → Pharmacy
   manages → Scheduler books
6. **Research Platform**: Literature searches → Data manages → Analysis runs → Visualization creates → Writing drafts →
   Review checks
