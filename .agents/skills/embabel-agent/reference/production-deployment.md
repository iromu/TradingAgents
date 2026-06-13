# Production Deployment Reference

## Docker

The Embabel Spring Boot application can be containerized with a multi-stage Dockerfile:

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t my-agent:latest .
docker run -e OPENAI_API_KEY=$OPENAI_API_KEY -p 8080:8080 my-agent:latest
```

## Kubernetes

Minimal deployment manifest:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-agent
spec:
  replicas: 2
  selector:
    matchLabels:
      app: my-agent
  template:
    metadata:
      labels:
        app: my-agent
    spec:
      containers:
      - name: my-agent
        image: my-agent:latest
        ports:
        - containerPort: 8080
        env:
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: api-keys
              key: openai
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

## Health Checks

Embabel exposes standard Spring Boot actuator endpoints:

- `GET /actuator/health` — Health check
- `GET /actuator/info` — Application info

Add `spring-boot-starter-actuator` for production health monitoring:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

## Monitoring

- **Cost tracking** — Set up a `CostTrackingListener` to monitor LLM spend in production
- **Process monitoring** — Use REST endpoints to track running processes
- **Logging** — Configure `embabel.agent.platform.logging.level` for production-appropriate verbosity
- **SSE events** — Subscribe to `/events/process/{processId}` for real-time process monitoring

## Security

- **API keys** — Never hardcode API keys; use environment variables or a secrets manager
- **Guardrails** — Always enable guardrails in production to filter input/output
- **Cost limits** — Set `EarlyTerminationPolicy` to prevent runaway costs
- **MCP security** — Use `@SecureAgentTool` with SpEL expressions for access control
- **HTTPS** — Always use HTTPS in production for REST and SSE endpoints

## Production Checklist

- [ ] Set `toolloop.max-iterations` to a reasonable limit
- [ ] Configure `EarlyTerminationPolicy` for cost caps
- [ ] Enable guardrails for input/output validation
- [ ] Use environment variables or secrets manager for API keys
- [ ] Set up cost tracking listener
- [ ] Configure appropriate logging level (not DEBUG in production)
- [ ] Enable actuator health checks
- [ ] Set resource requests/limits in Kubernetes
- [ ] Use HTTPS for all endpoints
- [ ] Test with `EarlyTerminationPolicy` to verify cost caps work
- [ ] Set up monitoring/alerting for process failures
- [ ] Configure backup for `ContextRepository` if using persistent context

## Key Points

- Use multi-stage Dockerfiles for smaller production images
- Always use secrets management for API keys
- Guardrails are essential in production for safety/compliance
- `EarlyTerminationPolicy` provides hard cost caps
- Actuator endpoints enable health checks and monitoring
- SSE events enable real-time process tracking
- Configure logging appropriately — DEBUG is useful for debugging but noisy in production
