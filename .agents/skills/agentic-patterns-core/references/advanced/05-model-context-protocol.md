# Model Context Protocol (MCP) Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Registry → Discover → Authorize → Call → Log
```

Implement a standardized protocol for agent-to-resource communication with
discovery, authorization, versioning, and observability — enabling agents to
dynamically find, authenticate, and use external tools and data sources.

## When to Use

- Enterprise systems: Building scalable, production-grade AI applications
- Multi-tool integration: Connecting to diverse external resources
- Standardization needs: Ensuring consistent communication interfaces
- Security requirements: Managing access control and permissions
- Dynamic environments: Resources that change or evolve over time
- Interoperability: Enabling different AI systems to work together

## Where It Fits

- Enterprise AI platforms: Standardizing tool and data access
- Multi-vendor integrations: Connecting different AI services
- Microservices architectures: AI agents accessing distributed services
- Cloud-native applications: Managing resources across environments
- API gateways: Centralizing AI system access to external resources

## Pros

- **Standardization** — Universal interface for all integrations
- **Discoverability** — Agents can dynamically find available resources
- **Security** — Built-in authentication and authorization
- **Versioning** — Graceful handling of API evolution
- **Observability** — Comprehensive logging and tracing
- **Reusability** — Write once, use across multiple agents
- **Scalability** — Designed for enterprise-grade deployments

## Cons

- **Implementation overhead** — Requires upfront protocol setup
- **Complexity** — Additional abstraction layer to manage
- **Learning curve** — Teams need to understand MCP concepts
- **Migration effort** — Existing integrations need conversion
- **Performance overhead** — Protocol layer adds latency
- **Vendor support** — Requires ecosystem adoption

## Implementation

```
# Example: MCP-style resource registry
class MCPRegistry:
    def __init__(self):
        self.resources = {}
        self.tools = {}
        self.auth = AuthorizationManager()

    def register_resource(self, name, resource):
        """Register a resource (database, API, file system)."""
        self.resources[name] = {
            **resource,
            "version": resource.get("version", "1.0.0"),
            "access": resource.get("access", "public"),
        }

    def discover(self, query, user_role):
        """Discover resources matching query with role-based filtering."""
        results = []
        for name, resource in self.resources.items():
            if self.auth.can_access(name, user_role):
                if self.matches_query(name, resource, query):
                    results.append(resource)
        return results

    def call(self, resource_name, method, params, user):
        """Call a resource with authorization and logging."""
        self.auth.verify_access(resource_name, user)
        result = self.resources[resource_name]["execute"](method, params)
        self.log_call(resource_name, method, user)
        return result
```

## Real-World Examples

1. **Enterprise Data Platform**: Unified access to databases/APIs/files, role-based access control, audit logging, version management, discovery service
2. **Multi-Cloud AI Services**: Standardized interface to AWS/Azure/GCP, credential management, service discovery, cost tracking, failover handling
3. **Healthcare AI**: HIPAA-compliant data access, patient privacy controls, medical device integrations, EHR connections, audit trails
4. **Financial Services**: Market data feeds, trading system connections, risk management tools, compliance checking, transaction audit logging
5. **Manufacturing IoT**: Sensor data collection, equipment control interfaces, quality assurance connections, supply chain data, predictive maintenance
6. **Research Computing**: Scientific database access, compute cluster jobs, experiment tracking, collaboration tools, version control
