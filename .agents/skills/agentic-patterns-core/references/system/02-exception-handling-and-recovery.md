# Exception Handling and Recovery Pattern

**Source:** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT License

## Pattern

```
Try → Error → Classify → [Retry | Fallback | Escalate] → Recover
```

Implement systematic error detection, classification, and recovery strategies
that enable agents to continue operating despite failures.

## When to Use

- Production environments requiring high reliability
- External dependencies on APIs or services
- Critical operations that must not fail completely
- Unpredictable inputs with edge cases and anomalies
- Network operations managing connectivity issues
- Resource constraints dealing with limits and quotas

## Where It Fits

- API integrations: Handling service outages and rate limits
- Data pipelines: Managing corrupt data and processing failures
- User-facing systems: Maintaining service availability
- Financial transactions: Ensuring transaction integrity
- IoT systems: Handling device failures and connectivity issues

## Pros

- **Reliability** — System continues operating despite failures
- **Graceful degradation** — Provides partial functionality when full service unavailable
- **Self-healing** — Automatic recovery from transient issues
- **User experience** — Minimizes disruption to users
- **Debugging support** — Comprehensive error logging
- **State preservation** — Can resume after interruptions

## Cons

- **Complexity increase** — Error handling adds code complexity
- **Performance overhead** — Try/catch and retries add latency
- **False positives** — May retry when unnecessary
- **Resource consumption** — Retries and fallbacks use resources
- **Cascading failures** — Poor handling can worsen problems
- **Testing difficulty** — Hard to test all failure scenarios

## Implementation

```
# Example: Retry with exponential backoff and fallback
import time
import random

def execute_with_recovery(operation, max_retries=3, fallback=None):
    for attempt in range(max_retries):
        try:
            return operation()
        except RateLimitError as e:
            wait = (2 ** attempt) + random.uniform(0, 1)
            time.sleep(wait)
        except TransientError as e:
            time.sleep(1)
        except CriticalError as e:
            if fallback:
                return fallback()
            raise

    # All retries exhausted
    if fallback:
        return fallback()
    raise MaxRetriesExceeded("Operation failed after retries")
```

## Real-World Examples

1. **Payment Processing**: Retry with backoff → Fallback to alternative gateway → Save state for manual review → Auto-refund on persistent failure
2. **Data Pipeline**: Handle malformed data → Retry with jitter → Use cached data when unavailable → Checkpoint for resume → Alert on quality issues
3. **Chatbot**: Fallback to simple responses → Escalate to human → Save conversation state → Retry knowledge base → Default to FAQ
4. **CDN**: Retry origin fetches → Serve stale content → Route to backup → Circuit breakers → Geographic failover
5. **ML Pipeline**: Handle model loading failures → Fallback to simpler model → Retry predictions → Cache frequent predictions → Graceful degradation
6. **IoT Management**: Retry device commands → Queue for offline → Use last known state → Watchdog timers → Auto-reboot protocols
