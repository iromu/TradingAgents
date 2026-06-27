# Change: fix-embabel-best-practices

## Summary
Fix critical Jinja template bug, remove dead fields from domain records, make Alpha Vantage service optional, and address medium/low severity code quality issues identified by Embabel best practices audit (validated by critic + reviewer agents).

## Artifacts
- [proposal.md](proposal.md) — Why this change, scope, success criteria
- [design.md](design.md) — Architectural decisions and file changes
- [tasks.md](tasks.md) — Implementation tasks organized in waves
- [specs/jinja-template-fix/spec.md](specs/jinja-template-fix/spec.md) — Jinja syntax spec
- [specs/cache-invalidation/spec.md](specs/cache-invalidation/spec.md) — Cache invalidation spec
- [specs/api-hardening/spec.md](specs/api-hardening/spec.md) — API security spec
- [specs/startup-reliability/spec.md](specs/startup-reliability/spec.md) — Startup reliability spec

## Validation Checklist
- [ ] All tasks in tasks.md completed
- [ ] All specs met (check acceptance criteria in each spec.md)
- [ ] `./mvnw verify` passes
- [ ] No regressions in existing agent behavior
- [ ] Jinja variable renders correctly in RiskManager output
- [ ] App starts without Alpha Vantage API key
- [ ] InvestmentDebateState has 7 fields (not 12)