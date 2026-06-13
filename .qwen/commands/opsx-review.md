---
description: Iterative multi-agent loop that reviews, critiques, and implements changes until the openspec is fully satisfied and all tests pass.
---

**Input**: Optionally specify a change name (e.g., `/opsx-review add-auth`). If omitted, check if it can be inferred
from
conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**
On the current openspec, for each change, run an iterative loop using @gem-reviewer and @gem-critic to evaluate the
current implementation. Where issues are found, invoke subagents @gem-implementer to apply fixes or @gem-code-simplifier
to reduce complexity where appropriate. Repeat this review–critique–implement cycle until @gem-orchestrator confirms
consensus that:

The openspec is fully and correctly implemented with no remaining items
All tests are passing and builds with no errors.
