---
description: Iterative multi-agent loop that reviews, critiques, and implements changes until it is fully satisfied and all tests pass.
---

**Input**: Optionally specify a path name (e.g., `/my-review path`). If omitted, check if it can be inferred
from conversation context. If vague or ambiguous you MUST prompt for checking a module/path or uncommited changes.

**Steps**
Run an iterative loop using @gem-reviewer and @gem-critic to evaluate the
current implementation. Where issues are found, invoke subagents @gem-implementer to apply fixes or @gem-code-simplifier
to reduce complexity where appropriate. Repeat this review–critique–implement cycle until @gem-orchestrator confirms
consensus that:

All tests are passing and builds with no errors.

You have to run the agents in this order:

* @gem-reviewer
* @gem-critic
* @gem-implementer
* @gem-code-simplifier

At the end evaluate with @gem-orchestrator and decide if the review is complete.
