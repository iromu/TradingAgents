# Evaluation and Monitoring Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Tests → Monitor → Detect Anomalies → Alert → Optimize → Repeat
```

Implement continuous evaluation and monitoring to ensure system reliability,
track performance metrics, detect issues early, and drive continuous improvement.

## When to Use

- Production systems requiring reliability
- Quality assurance ensuring consistent performance
- Compliance requirements meeting regulatory standards
- Performance optimization identifying bottlenecks
- Cost management tracking resource usage
- Continuous improvement through data-driven optimization

## Where It Fits

- Enterprise AI deployments: Mission-critical systems
- SaaS platforms: Multi-tenant service monitoring
- Healthcare systems: Patient safety monitoring
- Financial services: Trading system oversight
- E-commerce: Transaction and recommendation monitoring

## Pros

- **Reliability** — Early detection of issues
- **Performance visibility** — Clear system insights
- **Quality assurance** — Consistent output standards
- **Cost control** — Resource usage tracking
- **Compliance** — Audit trail maintenance
- **Improvement data** — Metrics guide optimization
- **User trust** — Transparent performance metrics

## Cons

- **Infrastructure overhead** — Monitoring systems require resources
- **Complexity** — Managing multiple metrics and alerts
- **Alert fatigue** — Too many notifications
- **Storage costs** — Logging and metrics data
- **Performance impact** — Instrumentation adds overhead
- **Maintenance burden** — Keeping tests updated
- **False positives** — Unnecessary alerts and rollbacks

## Implementation

```
# Example: Monitoring dashboard
MONITORING = {
    "metrics": [
        {"name": "latency_p99", "unit": "ms", "alert_threshold": 5000},
        {"name": "error_rate", "unit": "%", "alert_threshold": 5.0},
        {"name": "cost_per_request", "unit": "USD", "alert_threshold": 0.10},
        {"name": "quality_score", "unit": "score", "alert_threshold": 3.0},
    ],
    "tests": [
        {"name": "smoke_test", "frequency": "every_request", "type": "automated"},
        {"name": "regression_suite", "frequency": "daily", "type": "automated"},
        {"name": "human_review", "frequency": "weekly", "type": "manual"},
    ],
    "alerts": [
        {"metric": "error_rate", "condition": "> threshold", "action": "page_oncall"},
        {"metric": "cost_per_request", "condition": "> threshold", "action": "notify_team"},
    ],
}

def monitor_request(request, response):
    metrics = collect_metrics(request, response)
    for test in MONITORING["tests"]:
        if test["frequency"] == "every_request":
            result = run_test(test, request, response)
            if not result.passed:
                log_failure(test, result)

    for alert in MONITORING["alerts"]:
        value = metrics.get(alert["metric"])
        if value and value > get_threshold(alert["metric"]):
            send_alert(alert, value)
```

## Real-World Examples

1. **Recommendation Engine**: Click-through rate, conversion tracking, A/B test evaluation, latency monitoring, cost per recommendation, drift detection
2. **Customer Chatbot**: Resolution rate, satisfaction scores, response time, escalation rate, cost per interaction, quality sampling
3. **Trading System**: Trade execution, slippage tracking, risk compliance, latency measurements, P/L attribution, regulatory audit logs
4. **Content Moderation**: Precision/recall, false positive rates, processing time, human agreement, cost per moderation, violation trends
5. **Medical AI**: Diagnostic accuracy, false negative monitoring, time to diagnosis, clinician agreement, system availability, patient outcomes
6. **Code Generation**: Code quality metrics, compilation success, test pass rates, developer acceptance, generation time, usage patterns
